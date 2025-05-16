package Scheduler;

import static Scheduler.SchedulerUtils.parseMeetingDays;

import Utilities.CourseCatalog;
import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.Moshi;
import com.squareup.moshi.Types;
import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.Collections;
import java.util.stream.Collectors;

/**
 * Core engine for generating optimal course schedules.
 * This class filters courses based on user constraints and preferences,
 * then generates possible schedules that satisfy all requirements.
 */
public class ScheduleGenerator {

  private static final Moshi moshi = new Moshi.Builder().build();
  /** Path to the JSON file containing course data */
  private static final String COURSES_FILE = "data/courses_formatted.json";

  /** Number of courses to include in each generated schedule */
  private int classesPerSemester;

  /** Set to track unique schedules and avoid duplicates */
  private final Set<String> seenKeys = new HashSet<>();
  /** List of courses the student has already taken */
  private List<String> coursesTaken;
  /** List of courses required for the student's degree completion */
  private List<String> remainingRequired;

  /** List of courses that must be included in the generated schedules */
  private List<String> necessaryCourses;
  /** Set of time blocks when the student is available for classes */
  private Set<String> availableTimes;
  /** Map indicating which days of the week the student is available */
  private Map<String, Boolean> dayAvailability;
  /** Desired balance between MWF and TTh classes */
  private DayBalance dayBalance;
  /** Number of required courses to include in this semester's schedule */
  private int requiredCoursesThisSemester;
  /** List of preferred departments for elective courses */
  private List<String> preferredDepts;
  /** Whether a WRIT-designated course is required this semester */
  private boolean needWRIT;

  /** List of all courses for the specified term */
  public List<Map<String, Object>> allCourses;

  /** List of errors encountered during schedule generation */
  public final List<String> errors = new ArrayList<>();
  /** List of courses that pass the initial filtering criteria */
  List<Map<String, Object>> filteredCourses;
  /** Map of course codes to course objects for quick lookups */
  public Map<String, Map<String, Object>> courseMap;

  /** List of generated schedules */
  private List<Schedule> generatedSchedules;

  /**
   * Container class for schedule generation results.
   * Includes both the generated schedules and any errors encountered.
   */
  public static final class Result {
    /** List of generated schedules */
    public final List<Schedule> schedules;
    /** List of errors encountered during generation */
    public final List<String> errors;

    /**
     * Creates a new Result with the specified schedules and errors.
     *
     * @param s List of generated schedules
     * @param e List of errors encountered during generation
     */
    Result(List<Schedule> s, List<String> e) {
      this.schedules = s;
      this.errors = e;
    }
  }

  /**
   * Class representing the balance between MWF and TTh classes.
   */
  public static class DayBalance {
    /** Number of classes on Monday/Wednesday/Friday */
    public int mwfCount;
    /** Number of classes on Tuesday/Thursday */
    public int tthCount;

    /**
     * Creates a new DayBalance with the specified counts.
     *
     * @param mwfCount Number of MWF classes
     * @param tthCount Number of TTh classes
     */
    public DayBalance(int mwfCount, int tthCount) {
      this.mwfCount = mwfCount;
      this.tthCount = tthCount;
    }
  }

  /**
   * Class representing a potential course schedule.
   */
  public static class Schedule {
    /** List of courses in this schedule */
    public List<Map<String, Object>> courses;
    /** Calculated score for this schedule based on how well it meets preferences */
    public double score;

    /**
     * Creates a new Schedule with the specified courses.
     *
     * @param courses List of courses to include in this schedule
     */
    public Schedule(List<Map<String, Object>> courses) {
      this.courses = courses;
      this.score = 0.0;
    }

    /**
     * Checks if this schedule has any time conflicts between courses.
     *
     * @return true if there are time conflicts, false otherwise
     */
    public boolean hasTimeConflicts() {
      for (int i = 0; i < courses.size(); i++) {
        for (int j = i + 1; j < courses.size(); j++) {
          if (SchedulerUtils.hasTimeConflict(courses.get(i), courses.get(j))) {
            return true;
          }
        }
      }
      return false;
    }

    /**
     * Calculates the day balance for this schedule.
     *
     * @return A DayBalance object with the counts of MWF and TTh classes
     */
    public DayBalance getDayBalance() {
      int mwfCount = 0;
      int tthCount = 0;

      for (Map<String, Object> course : courses) {
        String meets = (String) course.get("meets");
        if (meets.contains("M") || meets.contains("W") || meets.contains("F")) {
          mwfCount++;
        }
        if (meets.contains("T")) {
          tthCount++;
        }
      }

      return new DayBalance(mwfCount, tthCount);
    }

    /**
     * Counts how many required courses are included in this schedule.
     *
     * @param remainingRequired List of courses required for degree completion
     * @return The number of required courses in this schedule
     */
    public int countRequiredCourses(List<String> remainingRequired) {
      int count = 0;
      for (Map<String, Object> course : courses) {
        String code = (String) course.get("code");
        if (remainingRequired.contains(code)) {
          count++;
        }
      }
      return count;
    }

    /**
     * Checks if this schedule includes a WRIT-designated course.
     *
     * @return true if a WRIT course is included, false otherwise
     */
    public boolean hasWRITCourse() {
      for (Map<String, Object> c : courses) {
        Boolean w = (Boolean) c.get("writ");
        if (Boolean.TRUE.equals(w)) return true;
      }
      return false;
    }
  }

  /**
   * Creates a new ScheduleGenerator with the specified parameters.
   *
   * @param classesPerSemester Number of courses to include in each schedule
   * @param coursesTaken List of courses the student has already taken
   * @param remainingRequired List of courses required for degree completion
   * @param necessaryCourses List of courses that must be included in schedules
   * @param availableTimes Set of time blocks when the student is available
   * @param dayAvailability Map indicating which days of the week are available
   * @param dayBalance Desired balance between MWF and TTh classes
   * @param requiredCoursesThisSemester Number of required courses to include
   * @param preferredDepts List of preferred departments for electives
   * @param needWRIT Whether a WRIT-designated course is required
   */
  public ScheduleGenerator(
      int classesPerSemester,
      List<String> coursesTaken,
      List<String> remainingRequired,
      List<String> necessaryCourses,
      Set<String> availableTimes,
      Map<String, Boolean> dayAvailability,
      DayBalance dayBalance,
      int requiredCoursesThisSemester,
      List<String> preferredDepts,
      boolean needWRIT) {

    this.classesPerSemester = classesPerSemester;
    this.coursesTaken = coursesTaken;
    this.remainingRequired = remainingRequired;
    this.necessaryCourses = necessaryCourses;
    this.availableTimes = availableTimes;
    this.dayAvailability = dayAvailability;
    this.dayBalance = dayBalance;
    this.requiredCoursesThisSemester = requiredCoursesThisSemester;
    this.preferredDepts = preferredDepts;
    this.needWRIT = needWRIT;

    this.generatedSchedules = new ArrayList<>();
    this.courseMap = new HashMap<>();
  }

  /**
   * Loads course data for the specified term from the JSON file.
   *
   * @param term The term code (e.g., "202420" for Spring 2025)
   * @throws IOException If an error occurs reading the course data file
   */
  public void loadCourseData(String term) throws IOException {
    String json = Files.readString(Paths.get(COURSES_FILE));
    Type type = Types.newParameterizedType(Map.class, String.class, Object.class);
    JsonAdapter<Map<String, Object>> adapter = moshi.adapter(type);
    Map<String, Object> data = adapter.fromJson(json);

    List<Object> results = (List<Object>) data.get("results");
    this.allCourses = new ArrayList<>();

    for (Object obj : results) {
      Map<String, Object> course = (Map<String, Object>) obj;
      if (course.get("srcdb").equals(term)) {
        this.allCourses.add(course);
        this.courseMap.put((String) course.get("code"), course);
      }
    }
  }

  /**
   * Filters courses based on user constraints and preferences.
   * This method applies filters for day/time availability, prerequisites,
   * and WRIT designation requirements.
   *
   * @param term The term code for filtering
   * @throws Exception If an error occurs during filtering
   */
  public void filterCourses(String term) throws Exception {
    this.filteredCourses = new ArrayList<>();

    for (Map<String, Object> course : this.allCourses) {
      String courseCode = (String) course.get("code");
      String meets = (String) course.get("meets");

      // Special handling for necessary courses - they must be included if possible
      if (this.necessaryCourses.contains(courseCode)) {
        // Check day availability for necessary courses
        Set<String> meetDays = parseMeetingDays(meets);
        boolean badDay = false;
        for (String d : meetDays) {
          if (!Boolean.TRUE.equals(this.dayAvailability.get(d))) {
            errors.add("Needed course " + courseCode + " meets on an unavailable day: " + d);
            badDay = true;
          }
        }
        if (badDay) continue;

        // Check time block availability for necessary courses
        String block = meets.split(" ", 2)[1];
        if (!SchedulerUtils.isAllowedTime(block, this.availableTimes)) {
          errors.add("Needed course " + courseCode + " meets at an unavailable time: " + meets);
          continue;
        }

        // Check prerequisites for necessary courses
        if (!checkPrerequisites(courseCode)) {
          errors.add("Needed course " + courseCode + " missing prerequisites");
          continue;
        }

        this.filteredCourses.add(course);
        continue;
      }

      // Skip courses with undefined meeting times
      if (meets == null || meets.isBlank() || "TBA".equalsIgnoreCase(meets.trim())) {
        continue;
      }

      // Skip courses already taken
      if (this.coursesTaken.contains(courseCode)) {
        continue;
      }

      // Check day availability for optional courses
      boolean dayAvailable = true;
      if (meets.contains("M") && !this.dayAvailability.get("M")) dayAvailable = false;
      if (meets.contains("T") && !this.dayAvailability.get("T")) dayAvailable = false;
      if (meets.contains("W") && !this.dayAvailability.get("W")) dayAvailable = false;
      if (meets.contains("Th") && !this.dayAvailability.get("Th")) dayAvailable = false;
      if (meets.contains("F") && !this.dayAvailability.get("F")) dayAvailable = false;

      if (!dayAvailable) {
        continue;
      }

      // Check time block availability for optional courses
      String humanTimeBlock = meets.split(" ", 2).length > 1 ? meets.split(" ", 2)[1] : "TBA";
      boolean timeAvailable = SchedulerUtils.isAllowedTime(humanTimeBlock, this.availableTimes);

      // Check prerequisites for optional courses
      boolean prerequisitesMet = checkPrerequisites(courseCode);

      // Add course to filtered list if it passes all checks
      if (timeAvailable && prerequisitesMet) {
        this.filteredCourses.add(course);
      }
    }

    // Check if WRIT requirement can be satisfied
    if (this.needWRIT
        && this.filteredCourses.stream().noneMatch(c -> Boolean.TRUE.equals(c.get("writ")))) {
      this.errors.add("No WRIT course fits the current day/time constraints");
    }

    // Remove WRIT courses if not needed (optimization)
    if (!this.needWRIT) {
      this.filteredCourses.removeIf(c -> Boolean.TRUE.equals(c.get("writ")));
    }
  }

  /**
   * Checks if prerequisites are satisfied for a course.
   *
   * @param courseCode The course code to check
   * @return true if prerequisites are satisfied or none exist, false otherwise
   */
  public boolean checkPrerequisites(String courseCode) {
    Map<String, Object> course = CourseCatalog.getCourse(courseCode);
    if (course == null) return true; // should not happen
    return SchedulerUtils.arePrerequisitesSatisfied(course, this.coursesTaken);
  }

  /**
   * Verifies that prerequisites are met for all necessary courses.
   *
   * @return List of necessary courses with missing prerequisites
   */
  public List<String> verifyNecessaryCourses() {
    List<String> missingPrereqs = new ArrayList<>();

    for (String courseCode : this.necessaryCourses) {
      if (!checkPrerequisites(courseCode)) {
        missingPrereqs.add(courseCode);
      }
    }

    return missingPrereqs;
  }

  /**
   * Generates schedules based on user preferences and constraints.
   * This is the main method that orchestrates the schedule generation process.
   *
   * @param term The term code for which to generate schedules
   * @return A Result object containing generated schedules and any errors
   * @throws Exception If an error occurs during schedule generation
   */
  public Result generateSchedules(String term) throws Exception {
    // Return early if errors already exist
    if (!this.errors.isEmpty()) return new Result(List.of(), this.errors);

    errors.clear();

    // Verify prerequisites for necessary courses
    List<String> missingPrereqs = verifyNecessaryCourses();
    if (!missingPrereqs.isEmpty()) {
      errors.add("Missing prerequisites for: " + String.join(", ", missingPrereqs));
      return new Result(List.of(), errors);
    }

    // Build list of necessary courses
    List<Map<String, Object>> necessaryCourseList = new ArrayList<>();
    for (String code : this.necessaryCourses) {
      Map<String, Object> course = this.courseMap.get(code);
      if (course != null) {
        necessaryCourseList.add(course);
      } else {
        errors.add("Necessary course not found: " + code);
        return new Result(List.of(), errors);
      }
    }

    // Check if too many necessary courses
    if (necessaryCourseList.size() > this.classesPerSemester) {
      errors.add(
          "Too many necessary courses: " + necessaryCourseList.size() + " > " + classesPerSemester);
      return new Result(List.of(), errors);
    }

    // Start with necessary courses as the base schedule
    Schedule baseSchedule = new Schedule(necessaryCourseList);

    // Find courses already in the base schedule
    Set<String> inSchedule =
        baseSchedule.courses.stream().map(c -> (String) c.get("code")).collect(Collectors.toSet());

    // Filter remaining courses to exclude those already in schedule
    List<Map<String, Object>> remainingFilteredCourses =
        filteredCourses.stream()
            .filter(c -> !inSchedule.contains(c.get("code")))
            .collect(Collectors.toList());

    // Identify required courses that can be added
    List<Map<String, Object>> requiredCourseOptions =
        remainingFilteredCourses.stream()
            .filter(c -> this.remainingRequired.contains(c.get("code")))
            .limit(this.requiredCoursesThisSemester - baseSchedule.courses.size())
            .collect(Collectors.toList());

    // Update tracking of courses in schedule
    inSchedule.addAll(
        requiredCourseOptions.stream()
            .map(c -> (String) c.get("code"))
            .collect(Collectors.toSet()));

    // Identify elective options (courses not already in schedule)
    List<Map<String, Object>> electiveCourseOptions =
        filteredCourses.stream()
            .filter(c -> !inSchedule.contains(c.get("code")))
            .collect(Collectors.toList());

    // Randomize options for variety in generated schedules
    Collections.shuffle(requiredCourseOptions);
    Collections.shuffle(electiveCourseOptions);

    // Check if WRIT course needs to be added
    boolean needToAddWRIT = this.needWRIT && !baseSchedule.hasWRITCourse();

    // Generate schedules recursively
    this.generatedSchedules.clear();
    buildSchedules(
        baseSchedule, requiredCourseOptions, electiveCourseOptions, 0, needToAddWRIT, term);

    // Sort schedules by score (highest first)
    this.generatedSchedules.sort((s1, s2) -> Double.compare(s2.score, s1.score));

    // Limit number of returned schedules
    int maxOptions = Math.min(9999, this.generatedSchedules.size());
    List<Schedule> top = generatedSchedules.subList(0, maxOptions);
    return new Result(top, errors);
  }

  /**
   * Recursively builds schedules by adding courses one at a time.
   * This method explores all possible combinations of courses that satisfy the constraints.
   *
   * @param currentSchedule The schedule being built
   * @param requiredOptions List of required courses that can be added
   * @param electiveOptions List of elective courses that can be added
   * @param requiredAdded Number of required courses already added
   * @param needWRIT Whether a WRIT course still needs to be added
   * @param term The term code for which schedules are being generated
   * @throws Exception If an error occurs during schedule building
   */
  private void buildSchedules(
      Schedule currentSchedule,
      List<Map<String, Object>> requiredOptions,
      List<Map<String, Object>> electiveOptions,
      int requiredAdded,
      boolean needWRIT,
      String term)
      throws Exception {

    // Stop if we've generated enough schedules
    if (this.generatedSchedules.size() >= 9999) {
      return;
    }

    // If schedule is complete, add it to results
    if (currentSchedule.courses.size() >= this.classesPerSemester) {
      String key =
          currentSchedule.courses.stream()
              .map(c -> (String) c.get("code"))
              .sorted()
              .collect(Collectors.joining("|"));

      // Only add unique schedules
      if (seenKeys.add(key)) {
        calculateScheduleScore(currentSchedule);
        this.generatedSchedules.add(currentSchedule);
      }
      return;
    }

    // Try adding required courses first
    if (requiredAdded < this.requiredCoursesThisSemester && !requiredOptions.isEmpty()) {
      for (int i = 0; i < requiredOptions.size(); i++) {
        Map<String, Object> course = requiredOptions.get(i);

        Schedule next = new Schedule(new ArrayList<>(currentSchedule.courses));
        next.courses.add(course);
        if (next.hasTimeConflicts()) continue;

        List<Map<String, Object>> nextReq = new ArrayList<>(requiredOptions);
        nextReq.remove(i);

        boolean nextNeedWRIT = needWRIT && !Boolean.TRUE.equals(course.get("writ"));
        buildSchedules(next, nextReq, electiveOptions, requiredAdded + 1, nextNeedWRIT, term);
      }
    }

    // If no elective options, stop here
    if (electiveOptions.isEmpty()) return;

    // Filter electives for WRIT courses if needed
    List<Map<String, Object>> pool = electiveOptions;
    if (needWRIT) {
      pool =
          electiveOptions.stream()
              .filter(c -> Boolean.TRUE.equals(c.get("writ")))
              .collect(Collectors.toList());
      if (pool.isEmpty()) pool = electiveOptions;
    }

    // Try adding elective courses
    for (Map<String, Object> course : pool) {
      Schedule next = new Schedule(new ArrayList<>(currentSchedule.courses));
      next.courses.add(course);
      if (next.hasTimeConflicts()) continue;

      boolean nextNeedWRIT = needWRIT && !Boolean.TRUE.equals(course.get("writ"));

      List<Map<String, Object>> nextElect = new ArrayList<>(electiveOptions);
      nextElect.remove(course);

      buildSchedules(next, requiredOptions, nextElect, requiredAdded, nextNeedWRIT, term);
    }
  }

  /**
   * Calculates a score for a schedule based on how well it meets preferences.
   * Higher scores indicate better matches to the user's preferences.
   *
   * @param schedule The schedule to score
   */
  private void calculateScheduleScore(Schedule schedule) {
    double score = 100.0;

    // Penalize deviation from desired day balance
    DayBalance actualBalance = schedule.getDayBalance();
    int mwfDiff = Math.abs(actualBalance.mwfCount - this.dayBalance.mwfCount);
    int tthDiff = Math.abs(actualBalance.tthCount - this.dayBalance.tthCount);
    score -= (mwfDiff + tthDiff) * 10;

    // Penalize deviation from required course count
    int requiredCount = schedule.countRequiredCourses(this.remainingRequired);
    int requiredDiff = Math.abs(requiredCount - this.requiredCoursesThisSemester);
    score -= requiredDiff * 15;

    // Penalize electives not in preferred departments
    if (!this.preferredDepts.isEmpty()) {
      for (Map<String, Object> course : schedule.courses) {
        String code = (String) course.get("code");
        String dept = code.split(" ")[0];

        // Skip required/necessary courses
        if (this.remainingRequired.contains(code) || this.necessaryCourses.contains(code)) {
          continue;
        }

        // Penalize non-preferred departments
        if (!this.preferredDepts.contains(dept)) {
          score -= 5;
        }
      }
    }

    // Penalize missing WRIT course if needed
    if (this.needWRIT && !schedule.hasWRITCourse()) {
      score -= 10;
    }

    // Ensure score is non-negative
    schedule.score = Math.max(0, score);
  }

  /**
   * Returns an unmodifiable view of the filtered courses.
   *
   * @return List of courses that passed filtering
   */
  public List<Map<String, Object>> getFilteredCourses() {
    return Collections.unmodifiableList(this.filteredCourses);
  }
}