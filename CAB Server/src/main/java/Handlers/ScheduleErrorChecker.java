package Handlers;

import Scheduler.ScheduleGenerator;
import Scheduler.SchedulerUtils;
import java.util.stream.Stream;
import spark.Request;

import java.util.*;
import java.util.stream.Collectors;

public final class ScheduleErrorChecker {

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

  public static Params parseParams(Request req, List<String> errors) {
    Params p = new Params();
    p.term = req.queryParams("term");
    p.classes = req.queryParams("classes");
    p.taken = req.queryParams("taken");
    p.remaining = req.queryParams("remaining");
    p.needed = req.queryParams("needed");
    p.times = req.queryParams("times");
    p.depts = req.queryParams("depts");
    p.writParam = req.queryParams("writ");
    p.days = req.queryParams("days");
    p.mwfStr = req.queryParams("mwf");
    p.tthStr = req.queryParams("tth");
    p.reqThisSemStr = req.queryParams("reqThisSem");

    if (Stream.of(p.term, p.classes, p.taken, p.remaining, p.needed, p.times, p.depts, p.writParam, p.days, p.mwfStr,
        p.tthStr, p.reqThisSemStr).anyMatch(Objects::isNull)) {errors.add("All parameters (term, classes, taken, "
        + "remaining, needed, times, depts, writ, days, mwf, tth, reqThisSem) must be filled");
      return p;
    }

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

  public static void validatePreFilter(Params p, List<String> errors) {
    if (p.classesPerSemester < 3)
      errors.add("The number of classes per semester must be at least 3");

    if (p.mwfCnt + p.tthCnt > p.classesPerSemester)
      errors.add("The total of MWF (" + p.mwfCnt + ") and TTh (" + p.tthCnt +
          ") classes exceeds the indicated number of " + p.classesPerSemester);

    if (p.necessaryCourses.size() > p.classesPerSemester)
      errors.add("You have specified " + p.necessaryCourses.size() +
          " necessary courses, but the maximum allowed is " + p.classesPerSemester);

    if (p.mwfCnt + p.tthCnt != p.classesPerSemester)
      errors.add("The total of MWF (" + p.mwfCnt + ") and TTh (" + p.tthCnt +
          ") classes must exactly match the indicated number of " + p.classesPerSemester);

    if (p.requiredThisSem > p.remainingRequired.size())
      errors.add("You requested " + p.requiredThisSem +
          " required courses this semester, but only " + p.remainingRequired.size() +
          " are available in the remaining list");

    if (p.requiredThisSem > p.classesPerSemester)
      errors.add("You requested " + p.requiredThisSem +
          " required courses this semester, but the indicated number of classes is " + p.classesPerSemester);

    Set<String> courses = p.coursesTaken.stream()
        .filter(c -> p.necessaryCourses.contains(c) || p.remainingRequired.contains(c))
        .collect(Collectors.toSet());
    if (!courses.isEmpty())
      errors.add("The following courses are listed in both 'taken' and 'needed' or 'remaining': " +
          String.join(", ", courses));

    Set<String> missingInRemaining = new HashSet<>(p.necessaryCourses);
    missingInRemaining.removeAll(p.remainingRequired);
    if (!missingInRemaining.isEmpty())
      errors.add("The following 'needed' courses are not in 'remaining': " +
          String.join(", ", missingInRemaining));

    if (!p.necessaryCourses.isEmpty() && p.necessaryCourses.size() > p.requiredThisSem)
      errors.add("You listed " + p.necessaryCourses.size() +
          " needed courses but only asked to take " + p.requiredThisSem + " required courses this semester");
  }

  public static void validatePostFilter(ScheduleGenerator gen, Params p, List<String> errors) {
    List<String> droppedRequired = p.remainingRequired.stream()
        .filter(code -> gen.getFilteredCourses().stream().noneMatch(c -> code.equals(c.get("code"))))
        .collect(Collectors.toList());

    int availReq = p.remainingRequired.size() - droppedRequired.size();
    if (availReq < p.requiredThisSem) {
      errors.add("Your request for " + p.requiredThisSem +
          " required course" + (p.requiredThisSem == 1 ? "" : "s") +
          " can’t be met under your current availability. These required course" +
          (droppedRequired.size() == 1 ? " doesn’t" : "s don’t") +
          " fit: " + String.join(", ", droppedRequired));
    }

    for (int i = 0; i < p.necessaryCourses.size(); i++) {
      for (int j = i + 1; j < p.necessaryCourses.size(); j++) {
        String a = p.necessaryCourses.get(i);
        String b = p.necessaryCourses.get(j);
        Map<String,Object> c = gen.courseMap.get(a);
        Map<String,Object> d = gen.courseMap.get(b);
        if (c != null && d != null && SchedulerUtils.hasTimeConflict(c, d)) {
          errors.add("The needed courses " + a + " and " + b + " have a time conflict");
        }
      }
    }

    for (String code : p.necessaryCourses) {
      if (!gen.checkPrerequisites(code)) {
        errors.add("Needed course " + code + " missing prerequisites");
      }
    }
  }

  // Takes string and converts into a List<String>
  private static List<String> parseList(String raw) {
    return raw == null || raw.isBlank() ? List.of() : List.of(raw.split("\\s*,\\s*"));
  }

  private static Map<String,Boolean> buildDayMap(List<String> days) {
    Map<String,Boolean> map = new LinkedHashMap<>();
    for (String d : List.of("M","T","W","Th","F")) map.put(d,false);
    days.forEach(d -> map.put(d,true));
    return map;
  }
}
