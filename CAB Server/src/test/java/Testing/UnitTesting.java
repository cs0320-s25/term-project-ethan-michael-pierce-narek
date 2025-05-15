package Testing;

import static junit.framework.TestCase.assertFalse;
import static junit.framework.TestCase.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;

import Scheduler.ScheduleGenerator;
import Scheduler.SchedulerUtils;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.Test;

public class UnitTesting {

  @Test
  public void testParseMeetingDays() {
    assertEquals(Set.of("M", "W", "F"), SchedulerUtils.parseMeetingDays("MWF 10-10:50a"));

    assertEquals(Set.of("M", "W"), SchedulerUtils.parseMeetingDays("MW 1-1:50p"));

    assertEquals(Set.of("T", "Th"), SchedulerUtils.parseMeetingDays("TTh 2:30-3:50p"));

    assertEquals(Set.of("T"), SchedulerUtils.parseMeetingDays("T 1-2:20p"));
  }

  @Test
  public void testIsAllowedTime() {
    Set<String> allowed = Set.of("9-9:50a", "10-10:50a");
    assertTrue(SchedulerUtils.isAllowedTime("9-9:50a", allowed));
    assertFalse(SchedulerUtils.isAllowedTime("3-4p", allowed));
  }

  @Test
  public void testHasTimeConflict() {
    assertTrue(
        SchedulerUtils.hasTimeConflict(
            courseWithMeetingTimes(
                "[{\"meet_day\":\"1\",\"start_time\":\"1300\",\"end_time\":\"1420\"},{\"meet_day\":\"3\",\"start_time\":\"1300\",\"end_time\":\"1420\"}]"),
            courseWithMeetingTimes(
                "[{\"meet_day\":\"1\",\"start_time\":\"1300\",\"end_time\":\"1420\"},{\"meet_day\":\"3\",\"start_time\":\"1300\",\"end_time\":\"1420\"}]")));

    assertFalse(
        SchedulerUtils.hasTimeConflict(
            courseWithMeetingTimes(
                "[{\"meet_day\":\"1\",\"start_time\":\"1300\",\"end_time\":\"1420\"},{\"meet_day\":\"3\",\"start_time\":\"1300\",\"end_time\":\"1420\"}]"),
            courseWithMeetingTimes(
                "[{\"meet_day\":\"0\",\"start_time\":\"1300\",\"end_time\":\"1350\"},{\"meet_day\":\"2\",\"start_time\":\"1300\",\"end_time\":\"1350\"},{\"meet_day\":\"4\",\"start_time\":\"1300\",\"end_time\":\"1350\"}]")));
  }

  @Test
  public void testArePrerequisitesSatisfied() {
    Map<String, Object> course =
        Map.of("prereqGroups", List.of(List.of("MATH 0100", "CSCI 0150"), List.of("CSCI 0160")));
    List<String> taken = List.of("CSCI 0150", "CSCI 0160");
    assertTrue(SchedulerUtils.arePrerequisitesSatisfied(course, taken));

    Map<String, Object> course2 =
        Map.of("prereqGroups", List.of(List.of("MATH 0100", "CSCI 0150"), List.of("CSCI 0160")));
    List<String> taken2 = List.of("CSCI 0150");
    assertFalse(SchedulerUtils.arePrerequisitesSatisfied(course2, taken2));
  }

  @Test
  public void filterCourses_keepsAllowedTimes() throws Exception {
    Map<String, Object> ok = makeCourse("CSCI 0100", "MWF 10-10:50a", false);
    Map<String, Object> bad = makeCourse("ECON 0110", "MWF 3-4p", false);
    ScheduleGenerator gen = generator(false, Set.of("10-10:50a"));
    gen.allCourses = List.of(ok, bad);
    gen.courseMap = Map.of("CSCI 0100", ok, "ECON 0110", bad);

    gen.filterCourses("202420");
    assertEquals(List.of(ok), gen.getFilteredCourses());
  }

  @Test
  public void filterCourses_excludesWRITWhenNotNeeded() throws Exception {
    Map<String, Object> writCourse = makeCourse("HIST 1999", "MWF 10-10:50a", true);
    ScheduleGenerator gen = generator(false, Set.of("10-10:50a"));
    gen.allCourses = List.of(writCourse);
    gen.courseMap = Map.of("HIST 1999", writCourse);

    gen.filterCourses("202420");
    assertTrue(gen.getFilteredCourses().isEmpty());
  }

  @Test
  public void filterCourses_errorsWhenWRITRequiredButMissing() throws Exception {
    Map<String, Object> nonWrit = makeCourse("ECON 0110", "MWF 10-10:50a", false);
    ScheduleGenerator gen = generator(true, Set.of("10-10:50a"));
    gen.allCourses = List.of(nonWrit);
    gen.courseMap = Map.of("ECON 0110", nonWrit);

    gen.filterCourses("202420");
    assertTrue(gen.errors.stream().anyMatch(e -> e.contains("WRIT")));
  }

  @Test
  public void parseMeetingTimes_singleBlock() {
    Map<String, Object> course =
        courseWithMeetingTimes(
            "[{\"meet_day\":\"0\",\"start_time\":\"900\",\"end_time\":\"950\"},{\"meet_day\":\"2\",\"start_time\":\"900\",\"end_time\":\"950\"},{\"meet_day\":\"4\",\"start_time\":\"900\",\"end_time\":\"950\"}]");
    List<SchedulerUtils.MeetingTime> mts = SchedulerUtils.parseMeetingTimes(course);
    assertEquals(3, mts.size());
    assertEquals(0, mts.get(0).days.get(0));
    assertEquals(9 * 60, mts.get(0).startTime);
    assertEquals(9 * 60 + 50, mts.get(0).endTime);
  }

  private static ScheduleGenerator generator(boolean needWrit, Set<String> allowedTimes) {
    Map<String, Boolean> days = new HashMap<>();
    for (String d : List.of("M", "T", "W", "Th", "F")) days.put(d, true);

    return new ScheduleGenerator(
        3,
        List.of(),
        List.of(),
        List.of(),
        allowedTimes,
        days,
        new ScheduleGenerator.DayBalance(2, 1),
        0,
        List.of(),
        needWrit);
  }

  private static Map<String, Object> makeCourse(String code, String meets, boolean writ) {
    String start = meets.contains("10-") ? "1000" : "0900";
    String end = meets.contains("10-") ? "1050" : "0950";
    String mtJson =
        String.format(
            "[{\"meet_day\":\"0\",\"start_time\":\"%s\",\"end_time\":\"%s\"}]", start, end);

    return new HashMap<>(
        Map.of(
            "code",
            code,
            "title",
            "Mock " + code,
            "meets",
            meets,
            "writ",
            writ,
            "meetingTimes",
            mtJson,
            "srcdb",
            "202420"));
  }

  private static Map<String, Object> courseWithMeetingTimes(String json) {
    Map<String, Object> c = new HashMap<>();
    c.put("meetingTimes", json);
    return c;
  }
}
