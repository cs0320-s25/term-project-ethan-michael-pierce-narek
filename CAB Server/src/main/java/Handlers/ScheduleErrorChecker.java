package Handlers;

import Scheduler.ScheduleGenerator;
import Scheduler.SchedulerUtils;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import spark.Request;

/**
 * Utility class for validating and processing schedule generation parameters. This class handles
 * parameter parsing, validation, and error checking for the course scheduling system. It ensures
 * all constraints are valid before attempting to generate schedules.
 */
public final class ScheduleErrorChecker {

  /**
   * Container class for storing all schedule generation parameters. This class holds both the raw
   * string inputs and their parsed versions.
   */
  public static class Params {
    String term, classes, taken, remaining, needed, times, depts, writParam;
    String days, mwfStr, tthStr, reqThisSemStr;
    int classesPerSemester, mwfCnt, tthCnt, requiredThisSem;
    List<String> coursesTaken, remainingRequired, necessaryCourses, preferredDepts;
    Set<String> availableTimes;
    boolean needWRIT;
    Map<String, Boolean> dayAvailability;
    ScheduleGenerator.DayBalance balance;
  }

  /**
   * Parses and validates request parameters for schedule generation. This method extracts
   * parameters from both query parameters and the provided metadata JSON.
   *
   * @param req The HTTP request containing query parameters
   * @param meta Optional JSON metadata containing course information (may be null)
   * @param errors List to collect any validation errors encountered during parsing
   * @return A Params object containing all parsed schedule generation parameters
   */
  public static Params parseParams(Request req, JsonNode meta, List<String> errors) {
    Params p = new Params();
    p.term = req.queryParams("term");
    p.classes = req.queryParams("classes");
    p.needed = req.queryParams("needed");
    p.times = req.queryParams("times");
    p.depts = req.queryParams("depts");
    p.writParam = req.queryParams("writ");
    p.days = req.queryParams("days");
    p.mwfStr = req.queryParams("mwf");
    p.tthStr = req.queryParams("tth");
    p.reqThisSemStr = req.queryParams("reqThisSem");

    // Extract courses from metadata if available
    if (meta != null) {
      if (meta.has("courses") && meta.get("courses").isArray()) {
        List<String> takenList = new ArrayList<>();
        for (JsonNode c : meta.get("courses")) {
          if (c.has("code")) takenList.add(c.get("code").asText());
        }
        p.coursesTaken = takenList;
        p.taken = String.join(",", takenList);
      }

      if (meta.has("desiredCourses") && meta.get("desiredCourses").isArray()) {
        List<String> remainingList = new ArrayList<>();
        for (JsonNode c : meta.get("desiredCourses")) {
          if (c.has("code")) remainingList.add(c.get("code").asText());
        }
        p.remainingRequired = remainingList;
        p.remaining = String.join(",", remainingList);
      }
    } else {
      // Use standard query parameters if no metadata
      p.taken = req.queryParams("taken");
      p.remaining = req.queryParams("remaining");
    }

    // Check that all required parameters are present
    if (Stream.of(
            p.term,
            p.classes,
            p.taken,
            p.remaining,
            p.needed,
            p.times,
            p.depts,
            p.writParam,
            p.days,
            p.mwfStr,
            p.tthStr,
            p.reqThisSemStr)
        .anyMatch(Objects::isNull)) {
      errors.add(
          "All parameters (term, classes, taken, "
              + "remaining, needed, times, depts, writ, days, mwf, tth, reqThisSem) must be filled");
      return p;
    }

    // Parse numeric and boolean parameters
    try {
      p.classesPerSemester = Integer.parseInt(p.classes);
      p.coursesTaken = parseList(p.taken);
      p.remainingRequired = parseList(p.remaining);
      p.necessaryCourses = parseList(p.needed);
      p.availableTimes = new HashSet<>(parseList(p.times));
      p.preferredDepts = parseList(p.depts);
      p.needWRIT = Boolean.parseBoolean(p.writParam.trim());
      p.mwfCnt = Integer.parseInt(p.mwfStr);
      p.tthCnt = Integer.parseInt(p.tthStr);
      p.requiredThisSem = Integer.parseInt(p.reqThisSemStr);
      p.dayAvailability = buildDayMap(parseList(p.days));
      p.balance = new ScheduleGenerator.DayBalance(p.mwfCnt, p.tthCnt);
    } catch (NumberFormatException e) {
      errors.add("Failed parsing numbers " + e.getMessage());
    }
    return p;
  }

  /**
   * Validates schedule parameters before course filtering. Checks for logical consistency in user
   * preferences and constraints.
   *
   * @param p The parsed parameters to validate
   * @param errors List to collect validation errors
   */
  public static void validatePreFilter(Params p, List<String> errors) {
    // Check minimum course load
    if (p.classesPerSemester < 3)
      errors.add("The number of classes per semester must be at least 3");

    // Check day balance consistency
    if (p.mwfCnt + p.tthCnt > p.classesPerSemester)
      errors.add(
          "The total of MWF ("
              + p.mwfCnt
              + ") and TTh ("
              + p.tthCnt
              + ") classes exceeds the indicated number of "
              + p.classesPerSemester);

    // Check necessary courses limitation
    if (p.necessaryCourses.size() > p.classesPerSemester)
      errors.add(
          "You have specified "
              + p.necessaryCourses.size()
              + " necessary courses, but the maximum allowed is "
              + p.classesPerSemester);

    // Verify day distribution matches total courses
    if (p.mwfCnt + p.tthCnt != p.classesPerSemester)
      errors.add(
          "The total of MWF ("
              + p.mwfCnt
              + ") and TTh ("
              + p.tthCnt
              + ") classes must exactly match the indicated number of "
              + p.classesPerSemester);

    // Check required courses constraints
    if (p.requiredThisSem > p.remainingRequired.size())
      errors.add(
          "You requested "
              + p.requiredThisSem
              + " required courses this semester, but only "
              + p.remainingRequired.size()
              + " are available in the remaining list");

    if (p.requiredThisSem > p.classesPerSemester)
      errors.add(
          "You requested "
              + p.requiredThisSem
              + " required courses this semester, but the indicated number of classes is "
              + p.classesPerSemester);

    // Check for courses listed as both taken and required/needed
    Set<String> courses =
        p.coursesTaken.stream()
            .filter(c -> p.necessaryCourses.contains(c) || p.remainingRequired.contains(c))
            .collect(Collectors.toSet());
    if (!courses.isEmpty())
      errors.add(
          "The following courses are listed in both 'taken' and 'needed' or 'remaining': "
              + String.join(", ", courses));

    // Check if necessary courses are in remaining required list
    Set<String> missingInRemaining = new HashSet<>(p.necessaryCourses);
    missingInRemaining.removeAll(p.remainingRequired);
    if (!missingInRemaining.isEmpty())
      errors.add(
          "The following 'needed' courses are not in 'remaining': "
              + String.join(", ", missingInRemaining));

    // Check necessary vs required course count consistency
    if (!p.necessaryCourses.isEmpty() && p.necessaryCourses.size() > p.requiredThisSem)
      errors.add(
          "You listed "
              + p.necessaryCourses.size()
              + " needed courses but only asked to take "
              + p.requiredThisSem
              + " required courses this semester");
  }

  /**
   * Validates schedule parameters after course filtering. Checks for issues related to course
   * availability, time conflicts, and prerequisites.
   *
   * @param gen The schedule generator containing filtered courses
   * @param p The parsed parameters to validate
   * @param errors List to collect validation errors
   */
  public static void validatePostFilter(ScheduleGenerator gen, Params p, List<String> errors) {
    // Check if required courses are available in filtered results
    List<String> droppedRequired =
        p.remainingRequired.stream()
            .filter(
                code ->
                    gen.getFilteredCourses().stream().noneMatch(c -> code.equals(c.get("code"))))
            .collect(Collectors.toList());

    int availReq = p.remainingRequired.size() - droppedRequired.size();
    if (availReq < p.requiredThisSem) {
      errors.add(
          "Your request for "
              + p.requiredThisSem
              + " required course"
              + (p.requiredThisSem == 1 ? "" : "s")
              + " can't be met under your current availability. These required course"
              + (droppedRequired.size() == 1 ? " doesn't" : "s don't")
              + " fit or don't exist: "
              + String.join(", ", droppedRequired));
    }

    // Check for time conflicts between necessary courses
    for (int i = 0; i < p.necessaryCourses.size(); i++) {
      for (int j = i + 1; j < p.necessaryCourses.size(); j++) {
        String a = p.necessaryCourses.get(i);
        String b = p.necessaryCourses.get(j);
        Map<String, Object> c = gen.courseMap.get(a);
        Map<String, Object> d = gen.courseMap.get(b);
        if (c != null && d != null && SchedulerUtils.hasTimeConflict(c, d)) {
          errors.add("The needed courses " + a + " and " + b + " have a time conflict");
        }
      }
    }

    // Check if prerequisites are met for necessary courses
    for (String code : p.necessaryCourses) {
      if (!gen.checkPrerequisites(code)) {
        errors.add("Needed course " + code + " missing prerequisites");
      }
    }
  }

  /**
   * Validates that all specified courses actually exist in the course catalog.
   *
   * @param p The parsed parameters to validate
   * @param validCodes Set of valid course codes from the catalog
   * @param errors List to collect validation errors
   */
  public static void validateCourseExistence(
      Params p, Set<String> validCodes, List<String> errors) {

    // Find courses that don't exist in the valid codes set
    List<String> bad =
        p.necessaryCourses.stream()
            .filter(code -> !validCodes.contains(code))
            .collect(Collectors.toList());

    if (!bad.isEmpty()) {
      if (bad.size() == 1) {
        errors.add(String.format("Course \"%s\" doesn't seem to exist", bad.get(0)));
      } else {
        errors.add(String.format("These courses don't seem to exist: %s", String.join(", ", bad)));
      }
    }
  }

  /**
   * Parses a comma-separated string into a list of strings.
   *
   * @param raw The comma-separated string to parse
   * @return A list of trimmed strings, or an empty list if input is null or blank
   */
  static List<String> parseList(String raw) {
    return raw == null || raw.isBlank() ? List.of() : List.of(raw.split("\\s*,\\s*"));
  }

  /**
   * Builds a map of day availability based on the provided list of available days.
   *
   * @param days List of days that are available (e.g., "M", "T", "W", "Th", "F")
   * @return A map with day codes as keys and boolean values indicating availability
   */
  private static Map<String, Boolean> buildDayMap(List<String> days) {
    Map<String, Boolean> map = new LinkedHashMap<>();
    for (String d : List.of("M", "T", "W", "Th", "F")) map.put(d, false);
    days.forEach(d -> map.put(d, true));
    return map;
  }
}
