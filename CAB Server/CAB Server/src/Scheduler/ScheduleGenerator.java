package Scheduler;

import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.Moshi;
import com.squareup.moshi.Types;
import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

import Utilities.IsWRIT;
import Utilities.BrownCourseRequirements;

/**
 * This class generates optimal schedules for Brown students based on their preferences.
 */
public class ScheduleGenerator {

  // Constants
  private static final Moshi moshi = new Moshi.Builder().build();
  private static final String COURSES_FILE = "data/courses_formatted.json";

  // User profile information
  private int classesPerSemester; // 3, 4, or 5
  private List<String> coursesTaken; // List of course codes already taken
  private List<String> remainingRequired; // List of courses required for major(s)

  // User preferences for this semester
  private List<String> necessaryCourses; // Must-take courses this semester
  private Set<String> availableTimes; // Times when student is available for class
  private Map<String, Boolean> dayAvailability; // Day availability (M, T, W, Th, F)
  private DayBalance dayBalance; // Preferred balance between MWF and TTh
  private int requiredCoursesThisSemester; // How many required courses to take
  private List<String> preferredDepts; // Preferred departments for electives
  private boolean needWRIT; // Whether a WRIT course is needed

  // Course data
  private List<Map<String, Object>> allCourses; // All courses for the semester
  private List<Map<String, Object>> filteredCourses; // Courses meeting basic criteria
  private Map<String, Map<String, Object>> courseMap; // For quick lookup

  // Results
  private List<Schedule> generatedSchedules; // List of schedule options

  /**
   * Class representing the balance between MWF and TTh classes
   */
  public static class DayBalance {
    int mwfCount;
    int tthCount;

    public DayBalance(int mwfCount, int tthCount) {
      this.mwfCount = mwfCount;
      this.tthCount = tthCount;
    }
  }

  /**
   * Class representing a complete schedule option
   */
  public static class Schedule {
    List<Map<String, Object>> courses;
    double score; // How well this schedule matches preferences

    public Schedule() {
      this.courses = new ArrayList<>();
      this.score = 0.0;
    }

    public Schedule(List<Map<String, Object>> courses) {
      this.courses = courses;
      this.score = 0.0;
    }

    // Check for time conflicts between courses
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

    // Calculate MWF vs TTh balance
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

    // Count required courses in this schedule
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

    // Check if this schedule has a WRIT course
    public boolean hasWRITCourse(String term) throws Exception {
      for (Map<String, Object> course : courses) {
        String code = (String) course.get("code");
        if (IsWRIT.isClassWRIT(code, term)) {
          return true;
        }
      }
      return false;
    }
  }

  /**
   * Constructor for ScheduleGenerator
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
   * Load course data from the JSON file
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

    System.out.println("Loaded " + this.allCourses.size() + " courses for term " + term);
  }

  /**
   * First pass: filter courses by prerequisites, already taken, and time availability
   */
  public void filterCourses(String term) throws Exception {
    this.filteredCourses = new ArrayList<>();

    for (Map<String, Object> course : this.allCourses) {
      String courseCode = (String) course.get("code");
      String meets = (String) course.get("meets");

      // Skip courses already taken
      if (this.coursesTaken.contains(courseCode)) {
        continue;
      }

      // Check day availability
      boolean dayAvailable = true;
      if (meets.contains("M") && !this.dayAvailability.get("M")) dayAvailable = false;
      if (meets.contains("T") && !this.dayAvailability.get("T")) dayAvailable = false;
      if (meets.contains("W") && !this.dayAvailability.get("W")) dayAvailable = false;
      if (meets.contains("Th") && !this.dayAvailability.get("Th")) dayAvailable = false;
      if (meets.contains("F") && !this.dayAvailability.get("F")) dayAvailable = false;

      if (!dayAvailable) {
        continue;
      }

      // Check time availability
      boolean timeAvailable = true;
      // This would need actual time parsing logic based on your time format
      // For now, assume all times are available if we reach here

      // Check prerequisites (this is a simplified placeholder)
      boolean prerequisitesMet = checkPrerequisites(courseCode, term);

      if (timeAvailable && prerequisitesMet) {
        this.filteredCourses.add(course);
      }
    }

    System.out.println("Filtered to " + this.filteredCourses.size() + " available courses");
  }

  /**
   * Check if prerequisites are met for a course
   */
  private boolean checkPrerequisites(String courseCode, String term) {
    try {
      // Get the course data from our map
      Map<String, Object> course = this.courseMap.get(courseCode);
      if (course == null) {
        System.err.println("Course not found: " + courseCode);
        return false;
      }

      // Get course description using BrownCourseRequirements
      String description = "";
      try {
        // Call BrownCourseRequirements to get the prerequisites info
        BrownCourseRequirements.getCourseRequirements(courseCode, term);
        // The prerequisite info is printed to System.out in the current implementation
        // In a real implementation, we would modify BrownCourseRequirements to return the info

        // For now, we'll use a placeholder description if the course exists
        description = "Prerequisite: " + courseCode + " requires previous coursework in the department.";
      } catch (Exception e) {
        System.err.println("Error getting course requirements: " + e.getMessage());
        // If we can't get prerequisites, assume none
        return true;
      }

      // Check if prerequisites are satisfied
      return SchedulerUtils.arePrerequisitesSatisfied(courseCode, description, this.coursesTaken);
    } catch (Exception e) {
      System.err.println("Error checking prerequisites for " + courseCode + ": " + e.getMessage());
      return false;
    }
  }

  /**
   * Second pass: Add necessary courses
   */
  public List<String> verifyNecessaryCourses(String term) throws Exception {
    List<String> missingPrereqs = new ArrayList<>();

    for (String courseCode : this.necessaryCourses) {
      if (!checkPrerequisites(courseCode, term)) {
        missingPrereqs.add(courseCode);
      }
    }

    return missingPrereqs;
  }

  /**
   * Generate schedule options
   */
  public List<Schedule> generateSchedules(String term) throws Exception {
    // Check if all necessary courses have prerequisites met
    List<String> missingPrereqs = verifyNecessaryCourses(term);
    if (!missingPrereqs.isEmpty()) {
      throw new IllegalStateException("Missing prerequisites for necessary courses: " +
          String.join(", ", missingPrereqs));
    }

    // Get all necessary courses first
    List<Map<String, Object>> necessaryCourseList = new ArrayList<>();
    for (String code : this.necessaryCourses) {
      Map<String, Object> course = this.courseMap.get(code);
      if (course != null) {
        necessaryCourseList.add(course);
      } else {
        throw new IllegalStateException("Necessary course not found: " + code);
      }
    }

    // If necessary courses already exceed classesPerSemester, we have an issue
    if (necessaryCourseList.size() > this.classesPerSemester) {
      throw new IllegalStateException("Too many necessary courses specified: " +
          necessaryCourseList.size() + " > " + this.classesPerSemester);
    }

    // Start with a schedule containing just the necessary courses
    Schedule baseSchedule = new Schedule(necessaryCourseList);

    // Check if we need a WRIT course, and if the necessary courses already include one
    boolean needToAddWRIT = this.needWRIT && !baseSchedule.hasWRITCourse(term);

    // Filter remaining courses (not already in the schedule)
    Set<String> coursesInSchedule = baseSchedule.courses.stream()
        .map(c -> (String)c.get("code"))
        .collect(Collectors.toSet());

    List<Map<String, Object>> remainingFilteredCourses = this.filteredCourses.stream()
        .filter(c -> !coursesInSchedule.contains((String)c.get("code")))
        .collect(Collectors.toList());

    // Create a priority queue for required courses
    List<Map<String, Object>> requiredCourseOptions = remainingFilteredCourses.stream()
        .filter(c -> this.remainingRequired.contains((String)c.get("code")))
        .collect(Collectors.toList());

    // Elective course options (filtered by preferred departments if specified)
    List<Map<String, Object>> electiveCourseOptions = remainingFilteredCourses.stream()
        .filter(c -> {
          String code = (String)c.get("code");
          String dept = code.split(" ")[0];
          return !this.remainingRequired.contains(code) &&
              (this.preferredDepts.isEmpty() || this.preferredDepts.contains(dept));
        })
        .collect(Collectors.toList());

    // Start recursive schedule building
    this.generatedSchedules.clear();
    buildSchedules(baseSchedule, requiredCourseOptions, electiveCourseOptions,
        0, needToAddWRIT, term);

    // Sort schedules by score
    this.generatedSchedules.sort((s1, s2) -> Double.compare(s2.score, s1.score));

    // Return top options
    int maxOptions = Math.min(5, this.generatedSchedules.size());
    return this.generatedSchedules.subList(0, maxOptions);
  }

  /**
   * Recursive schedule building
   */
  private void buildSchedules(
      Schedule currentSchedule,
      List<Map<String, Object>> requiredOptions,
      List<Map<String, Object>> electiveOptions,
      int requiredAdded,
      boolean needWRIT,
      String term) throws Exception {

    // Base case: schedule is full
    if (currentSchedule.courses.size() >= this.classesPerSemester) {
      // Check if the schedule meets the WRIT requirement
      if (!needWRIT || currentSchedule.hasWRITCourse(term)) {
        // Calculate score based on preferences
        calculateScheduleScore(currentSchedule);
        this.generatedSchedules.add(currentSchedule);
      }
      return;
    }

    // Try adding required courses first, if we haven't hit the desired number
    if (requiredAdded < this.requiredCoursesThisSemester && !requiredOptions.isEmpty()) {
      for (int i = 0; i < requiredOptions.size(); i++) {
        Map<String, Object> course = requiredOptions.get(i);

        // Create a new schedule with this course
        Schedule newSchedule = new Schedule(new ArrayList<>(currentSchedule.courses));
        newSchedule.courses.add(course);

        // Check for time conflicts
        if (newSchedule.hasTimeConflicts()) {
          continue;
        }

        // Create a new list of required options without this course
        List<Map<String, Object>> newRequiredOptions = new ArrayList<>(requiredOptions);
        newRequiredOptions.remove(i);

        // Continue building the schedule
        buildSchedules(newSchedule, newRequiredOptions, electiveOptions,
            requiredAdded + 1, needWRIT, term);
      }
    }

    // If we've added enough required courses, or have no more required options,
    // continue with electives
    if (!electiveOptions.isEmpty()) {
      // If we need a WRIT course, prioritize those
      List<Map<String, Object>> priorityElectives = electiveOptions;
      if (needWRIT) {
        priorityElectives = new ArrayList<>();

        // Check which electives have WRIT designation
        for (Map<String, Object> course : electiveOptions) {
          String code = (String) course.get("code");
          if (IsWRIT.isClassWRIT(code, term)) {
            priorityElectives.add(course);
          }
        }

        // If no WRIT electives found, use all electives
        if (priorityElectives.isEmpty()) {
          priorityElectives = electiveOptions;
        }
      }

      // Try each elective option
      for (int i = 0; i < priorityElectives.size(); i++) {
        Map<String, Object> course = priorityElectives.get(i);

        // Create a new schedule with this course
        Schedule newSchedule = new Schedule(new ArrayList<>(currentSchedule.courses));
        newSchedule.courses.add(course);

        // Check for time conflicts
        if (newSchedule.hasTimeConflicts()) {
          continue;
        }

        // Check if adding this course satisfies the WRIT requirement
        boolean newNeedWRIT = needWRIT;
        if (needWRIT) {
          String code = (String) course.get("code");
          if (IsWRIT.isClassWRIT(code, term)) {
            newNeedWRIT = false;
          }
        }

        // Create new options lists without this course
        List<Map<String, Object>> newElectiveOptions = new ArrayList<>(electiveOptions);
        newElectiveOptions.remove(course);

        // Continue building the schedule
        buildSchedules(newSchedule, requiredOptions, newElectiveOptions,
            requiredAdded, newNeedWRIT, term);
      }
    }
  }

  /**
   * Calculate a score for a schedule based on how well it meets preferences
   */
  private void calculateScheduleScore(Schedule schedule) {
    double score = 100.0; // Start with a perfect score

    // Check day balance
    DayBalance actualBalance = schedule.getDayBalance();
    int mwfDiff = Math.abs(actualBalance.mwfCount - this.dayBalance.mwfCount);
    int tthDiff = Math.abs(actualBalance.tthCount - this.dayBalance.tthCount);
    score -= (mwfDiff + tthDiff) * 10; // Penalty for imbalance

    // Check required courses
    int requiredCount = schedule.countRequiredCourses(this.remainingRequired);
    int requiredDiff = Math.abs(requiredCount - this.requiredCoursesThisSemester);
    score -= requiredDiff * 15; // Larger penalty for missing required courses

    // Check preferred departments for electives
    if (!this.preferredDepts.isEmpty()) {
      for (Map<String, Object> course : schedule.courses) {
        String code = (String) course.get("code");
        String dept = code.split(" ")[0];

        // Skip required courses and necessary courses
        if (this.remainingRequired.contains(code) || this.necessaryCourses.contains(code)) {
          continue;
        }

        // Penalty for electives not in preferred departments
        if (!this.preferredDepts.contains(dept)) {
          score -= 5;
        }
      }
    }

    // Set the final score
    schedule.score = Math.max(0, score);
  }
}