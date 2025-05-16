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

  /* Test for day balance calculation */
  @Test
  public void testScheduleDayBalance() {
    // Create a schedule with 2 MWF and 1 TTh class
    Map<String, Object> mwf1 = makeCourse("CSCI 0320", "MWF 10-10:50a", false);
    Map<String, Object> mwf2 = makeCourse("MATH 0100", "MWF 1-1:50p", false);
    Map<String, Object> tth = makeCourse("ENGL 0900", "TTh 10:30-11:50a", true);

    ScheduleGenerator.Schedule schedule = new ScheduleGenerator.Schedule(List.of(mwf1, mwf2, tth));
    ScheduleGenerator.DayBalance balance = schedule.getDayBalance();

    assertEquals(2, balance.mwfCount);
    assertEquals(1, balance.tthCount);
  }

  /* Test for empty time slots */
  @Test
  public void testParseEmptyMeetingTimes() {
    // Test handling of null or empty meeting times
    Map<String, Object> nullMeeting = new HashMap<>();
    nullMeeting.put("meetingTimes", null);
    assertTrue(SchedulerUtils.parseMeetingTimes(nullMeeting).isEmpty());

    Map<String, Object> emptyMeeting = new HashMap<>();
    emptyMeeting.put("meetingTimes", "");
    assertTrue(SchedulerUtils.parseMeetingTimes(emptyMeeting).isEmpty());

    Map<String, Object> invalidJsonMeeting = new HashMap<>();
    invalidJsonMeeting.put("meetingTimes", "{bad json}");
    assertTrue(SchedulerUtils.parseMeetingTimes(invalidJsonMeeting).isEmpty());
  }

  /* Test for WRIT course requirements */
  @Test
  public void testScheduleHasWRITCourse() {
    // Create schedules with and without WRIT courses
    Map<String, Object> normal1 = makeCourse("CSCI 0320", "MWF 10-10:50a", false);
    Map<String, Object> normal2 = makeCourse("MATH 0100", "MWF 1-1:50p", false);
    Map<String, Object> writCourse = makeCourse("ENGL 0900", "TTh 10:30-11:50a", true);

    ScheduleGenerator.Schedule noWritSchedule = new ScheduleGenerator.Schedule(List.of(normal1, normal2));
    assertFalse(noWritSchedule.hasWRITCourse());

    ScheduleGenerator.Schedule withWritSchedule = new ScheduleGenerator.Schedule(List.of(normal1, writCourse));
    assertTrue(withWritSchedule.hasWRITCourse());
  }

  /* Test for time block parsing edge cases */
  @Test
  public void testParseTimeBlockEdgeCases() {
    // Test special time formats
    Set<String> allowed = Set.of("8-8:50a", "12-12:50p", "1-1:50p", "TBA");

    assertTrue(SchedulerUtils.isAllowedTime("TBA", allowed));
    assertTrue(SchedulerUtils.isAllowedTime("8-8:50a", allowed));
    assertTrue(SchedulerUtils.isAllowedTime("12-12:50p", allowed));

    // Test null handling
    assertTrue(SchedulerUtils.isAllowedTime(null, allowed));
    assertTrue(SchedulerUtils.isAllowedTime("8-8:50a", null));
    assertTrue(SchedulerUtils.isAllowedTime(null, null));

    // Test whitespace handling
    assertTrue(SchedulerUtils.isAllowedTime(" 8-8:50a ", allowed));
  }

  /* Test for time conflict with overlapping times */
  @Test
  public void testPartialTimeOverlap() {
    // Test for partial time overlaps
    assertTrue(
        SchedulerUtils.hasTimeConflict(
            courseWithMeetingTimes(
                "[{\"meet_day\":\"1\",\"start_time\":\"1000\",\"end_time\":\"1150\"}]"),
            courseWithMeetingTimes(
                "[{\"meet_day\":\"1\",\"start_time\":\"1100\",\"end_time\":\"1220\"}]")));

    // Class ends exactly when another begins - should not conflict
    assertFalse(
        SchedulerUtils.hasTimeConflict(
            courseWithMeetingTimes(
                "[{\"meet_day\":\"1\",\"start_time\":\"1000\",\"end_time\":\"1150\"}]"),
            courseWithMeetingTimes(
                "[{\"meet_day\":\"1\",\"start_time\":\"1150\",\"end_time\":\"1320\"}]")));
  }

  /* Test for required courses count */
  @Test
  public void testCountRequiredCourses() {
    // Test counting of required courses in schedule
    Map<String, Object> req1 = makeCourse("CSCI 0320", "MWF 10-10:50a", false);
    Map<String, Object> req2 = makeCourse("CSCI 0330", "TTh 1-2:20p", false);
    Map<String, Object> elective = makeCourse("ENGL 0900", "MWF 1-1:50p", true);

    List<String> requiredCourses = List.of("CSCI 0320", "CSCI 0330", "MATH 0100");

    ScheduleGenerator.Schedule schedule = new ScheduleGenerator.Schedule(List.of(req1, req2, elective));
    assertEquals(2, schedule.countRequiredCourses(requiredCourses));
  }

  /* Test for schedule time conflicts */
  @Test
  public void testScheduleHasTimeConflicts() {
    // Create courses with and without time conflicts
    Map<String, Object> morning = makeCourse("CSCI 0320", "MWF 10-10:50a", false);
    Map<String, Object> afternoon = makeCourse("MATH 0100", "MWF 1-1:50p", false);
    Map<String, Object> conflicting = courseWithMeetingTimes(
        "[{\"meet_day\":\"0\",\"start_time\":\"1000\",\"end_time\":\"1050\"},{\"meet_day\":\"2\",\"start_time\":\"1000\",\"end_time\":\"1050\"},{\"meet_day\":\"4\",\"start_time\":\"1000\",\"end_time\":\"1050\"}]");
    conflicting.put("code", "PHYS 0070");
    conflicting.put("title", "Mock PHYS 0070");
    conflicting.put("meets", "MWF 10-10:50a");
    conflicting.put("writ", false);
    conflicting.put("srcdb", "202420");

    ScheduleGenerator.Schedule noConflictSchedule = new ScheduleGenerator.Schedule(List.of(morning, afternoon));
    assertFalse(noConflictSchedule.hasTimeConflicts());

    ScheduleGenerator.Schedule withConflictSchedule = new ScheduleGenerator.Schedule(List.of(morning, conflicting));
    assertTrue(withConflictSchedule.hasTimeConflicts());
  }

  /* Test for necessary course handling */
  @Test
  public void testNecessaryCoursesHandling() throws Exception {
    // Test that necessary courses are properly preserved in filtering
    Map<String, Object> necessaryCourse = makeCourse("CSCI 0320", "MWF 10-10:50a", false);
    Map<String, Object> otherCourse = makeCourse("MATH 0100", "MWF 1-1:50p", false);

    ScheduleGenerator gen = generator(false, Set.of("1-1:50p")); // Only afternoon time allowed
    gen.allCourses = List.of(necessaryCourse, otherCourse);
    gen.courseMap = Map.of("CSCI 0320", necessaryCourse, "MATH 0100", otherCourse);

    // Override to make CSCI 0320 necessary
    java.lang.reflect.Field field = ScheduleGenerator.class.getDeclaredField("necessaryCourses");
    field.setAccessible(true);
    field.set(gen, List.of("CSCI 0320"));

    gen.filterCourses("202420");

    // Verify necessary course is kept even though it's outside allowed times
    assertEquals(1, gen.errors.size());
    assertTrue(gen.errors.get(0).contains("Needed course CSCI 0320 meets at an unavailable time"));
  }

  /* Test for prerequisites checking with empty or null lists */
  @Test
  public void testPrerequisitesWithEmptyLists() {
    // Test courses with no prerequisites
    Map<String, Object> noPrerequCourse = new HashMap<>();
    noPrerequCourse.put("prereqGroups", List.of());
    assertTrue(SchedulerUtils.arePrerequisitesSatisfied(noPrerequCourse, List.of()));

    // Test with null prerequisite groups
    Map<String, Object> nullPrerequCourse = new HashMap<>();
    assertTrue(SchedulerUtils.arePrerequisitesSatisfied(nullPrerequCourse, List.of()));

    // Test with prerequisites but empty taken courses
    Map<String, Object> withPrerequCourse = new HashMap<>();
    withPrerequCourse.put("prereqGroups", List.of(List.of("CSCI 0150")));
    assertFalse(SchedulerUtils.arePrerequisitesSatisfied(withPrerequCourse, List.of()));
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