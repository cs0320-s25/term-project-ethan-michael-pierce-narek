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

public class ScheduleGenerator {

  private static final Moshi moshi = new Moshi.Builder().build();
  private static final String COURSES_FILE = "data/courses_formatted.json";

  private int classesPerSemester;

  private final Set<String> seenKeys = new HashSet<>();
  private List<String> coursesTaken;
  private List<String> remainingRequired;

  private List<String> necessaryCourses;
  private Set<String> availableTimes;
  private Map<String, Boolean> dayAvailability;
  private DayBalance dayBalance;
  private int requiredCoursesThisSemester;
  private List<String> preferredDepts;
  private boolean needWRIT;

  public List<Map<String, Object>> allCourses;

  public final List<String> errors = new ArrayList<>();
  List<Map<String, Object>> filteredCourses;
  public Map<String, Map<String, Object>> courseMap;

  private List<Schedule> generatedSchedules;

  public static final class Result {
    public final List<Schedule> schedules;
    public final List<String> errors;

    Result(List<Schedule> s, List<String> e) {
      this.schedules = s;
      this.errors = e;
    }
  }

  public static class DayBalance {

    public int mwfCount;
    public int tthCount;

    public DayBalance(int mwfCount, int tthCount) {
      this.mwfCount = mwfCount;
      this.tthCount = tthCount;
    }
  }

  public static class Schedule {

    public List<Map<String, Object>> courses;
    public double score;

    public Schedule(List<Map<String, Object>> courses) {
      this.courses = courses;
      this.score = 0.0;
    }

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

    public boolean hasWRITCourse() {
      for (Map<String, Object> c : courses) {
        Boolean w = (Boolean) c.get("writ");
        if (Boolean.TRUE.equals(w)) return true;
      }
      return false;
    }
  }

  /** Constructor for ScheduleGenerator */
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

  public void filterCourses(String term) throws Exception {
    this.filteredCourses = new ArrayList<>();

    for (Map<String, Object> course : this.allCourses) {
      String courseCode = (String) course.get("code");
      String meets = (String) course.get("meets");

      if (this.necessaryCourses.contains(courseCode)) {

        Set<String> meetDays = parseMeetingDays(meets);
        boolean badDay = false;
        for (String d : meetDays) {
          if (!Boolean.TRUE.equals(this.dayAvailability.get(d))) {
            errors.add("Needed course " + courseCode + " meets on an unavailable day: " + d);
            badDay = true;
          }
        }
        if (badDay) continue;

        String block = meets.split(" ", 2)[1];
        if (!SchedulerUtils.isAllowedTime(block, this.availableTimes)) {
          errors.add("Needed course " + courseCode + " meets at an unavailable time: " + meets);
          continue;
        }

        if (!checkPrerequisites(courseCode)) {
          errors.add("Needed course " + courseCode + " missing prerequisites");
          continue;
        }


        this.filteredCourses.add(course);
        continue;
      }

      if (meets == null || meets.isBlank() || "TBA".equalsIgnoreCase(meets.trim())) {
        continue;
      }

      if (this.coursesTaken.contains(courseCode)) {
        continue;
      }

      boolean dayAvailable = true;
      if (meets.contains("M") && !this.dayAvailability.get("M")) dayAvailable = false;
      if (meets.contains("T") && !this.dayAvailability.get("T")) dayAvailable = false;
      if (meets.contains("W") && !this.dayAvailability.get("W")) dayAvailable = false;
      if (meets.contains("Th") && !this.dayAvailability.get("Th")) dayAvailable = false;
      if (meets.contains("F") && !this.dayAvailability.get("F")) dayAvailable = false;

      if (!dayAvailable) {
        continue;
      }

      String humanTimeBlock = meets.split(" ", 2).length > 1 ? meets.split(" ", 2)[1] : "TBA";
      boolean timeAvailable = SchedulerUtils.isAllowedTime(humanTimeBlock, this.availableTimes);


      boolean prerequisitesMet = checkPrerequisites(courseCode);

      if (timeAvailable && prerequisitesMet) {
        this.filteredCourses.add(course);
      }
    }

    if (this.needWRIT
        && this.filteredCourses.stream().noneMatch(c -> Boolean.TRUE.equals(c.get("writ")))) {

      this.errors.add("No WRIT course fits the current day/time constraints");
    }

    if (!this.needWRIT) {
      this.filteredCourses.removeIf(c -> Boolean.TRUE.equals(c.get("writ")));
    }

  }

  public boolean checkPrerequisites(String courseCode) {
    Map<String, Object> course = CourseCatalog.getCourse(courseCode);
    if (course == null) return true; // should not happen
    return SchedulerUtils.arePrerequisitesSatisfied(course, this.coursesTaken);
  }

  public List<String> verifyNecessaryCourses() {
    List<String> missingPrereqs = new ArrayList<>();

    for (String courseCode : this.necessaryCourses) {
      if (!checkPrerequisites(courseCode)) {
        missingPrereqs.add(courseCode);
      }
    }

    return missingPrereqs;
  }

  public Result generateSchedules(String term) throws Exception {
    if (!this.errors.isEmpty())
    return new Result(List.of(), this.errors);

    errors.clear();
    List<String> missingPrereqs = verifyNecessaryCourses();
    if (!missingPrereqs.isEmpty()) {
      errors.add("Missing prerequisites for: " + String.join(", ", missingPrereqs));
      return new Result(List.of(), errors);
    }


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

    if (necessaryCourseList.size() > this.classesPerSemester) {
      errors.add(
          "Too many necessary courses: " + necessaryCourseList.size() + " > " + classesPerSemester);
      return new Result(List.of(), errors);
    }

    Schedule baseSchedule = new Schedule(necessaryCourseList);

    Set<String> inSchedule =
        baseSchedule.courses.stream().map(c -> (String) c.get("code")).collect(Collectors.toSet());
    List<Map<String, Object>> remainingFilteredCourses =
        filteredCourses.stream()
            .filter(c -> !inSchedule.contains(c.get("code")))
            .collect(Collectors.toList());

    List<Map<String, Object>> requiredCourseOptions =
        remainingFilteredCourses.stream()
            .filter(c -> this.remainingRequired.contains(c.get("code")))
            .limit(this.requiredCoursesThisSemester - baseSchedule.courses.size())
            .collect(Collectors.toList());

    inSchedule.addAll(
        requiredCourseOptions.stream()
            .map(c -> (String) c.get("code"))
            .collect(Collectors.toSet()));

    List<Map<String, Object>> electiveCourseOptions =
        filteredCourses.stream()
            .filter(c -> !inSchedule.contains(c.get("code")))
            .collect(Collectors.toList());

    Collections.shuffle(requiredCourseOptions);
    Collections.shuffle(electiveCourseOptions);

    boolean needToAddWRIT = this.needWRIT && !baseSchedule.hasWRITCourse();

    this.generatedSchedules.clear();
    buildSchedules(
        baseSchedule, requiredCourseOptions, electiveCourseOptions, 0, needToAddWRIT, term);

    this.generatedSchedules.sort((s1, s2) -> Double.compare(s2.score, s1.score));

    int maxOptions = Math.min(9999, this.generatedSchedules.size());
    List<Schedule> top = generatedSchedules.subList(0, maxOptions);
    return new Result(top, errors);
  }


  private void buildSchedules(
      Schedule currentSchedule,
      List<Map<String, Object>> requiredOptions,
      List<Map<String, Object>> electiveOptions,
      int requiredAdded,
      boolean needWRIT,
      String term)
      throws Exception {

    if (this.generatedSchedules.size() >= 9999) {
      return;
    }

    if (currentSchedule.courses.size() >= this.classesPerSemester) {
      String key =
          currentSchedule.courses.stream()
              .map(c -> (String) c.get("code"))
              .sorted()
              .collect(Collectors.joining("|"));

      if (seenKeys.add(key)) {
        calculateScheduleScore(currentSchedule);
        this.generatedSchedules.add(currentSchedule);
      }
      return;
    }

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

    if (electiveOptions.isEmpty()) return;

    List<Map<String, Object>> pool = electiveOptions;
    if (needWRIT) {
      pool =
          electiveOptions.stream()
              .filter(c -> Boolean.TRUE.equals(c.get("writ")))
              .collect(Collectors.toList());
      if (pool.isEmpty()) pool = electiveOptions;
    }


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

  private void calculateScheduleScore(Schedule schedule) {
    double score = 100.0;


    DayBalance actualBalance = schedule.getDayBalance();
    int mwfDiff = Math.abs(actualBalance.mwfCount - this.dayBalance.mwfCount);
    int tthDiff = Math.abs(actualBalance.tthCount - this.dayBalance.tthCount);
    score -= (mwfDiff + tthDiff) * 10;


    int requiredCount = schedule.countRequiredCourses(this.remainingRequired);
    int requiredDiff = Math.abs(requiredCount - this.requiredCoursesThisSemester);
    score -= requiredDiff * 15;

    if (!this.preferredDepts.isEmpty()) {
      for (Map<String, Object> course : schedule.courses) {
        String code = (String) course.get("code");
        String dept = code.split(" ")[0];


        if (this.remainingRequired.contains(code) || this.necessaryCourses.contains(code)) {
          continue;
        }

        if (!this.preferredDepts.contains(dept)) {
          score -= 5;
        }
      }
    }

    if (this.needWRIT && !schedule.hasWRITCourse()) {
      score -= 10;
    }

    schedule.score = Math.max(0, score);
  }

  public List<Map<String, Object>> getFilteredCourses() {
    return Collections.unmodifiableList(this.filteredCourses);
  }
}
