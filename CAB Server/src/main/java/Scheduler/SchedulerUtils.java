package Scheduler;

import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.Moshi;
import java.util.*;

/**
 * Utility class for schedule-related operations.
 * Provides methods for time conflict detection, parsing meeting times,
 * evaluating prerequisites, and validating schedule constraints.
 */
public class SchedulerUtils {

  /** Moshi instance for JSON parsing */
  private static final Moshi moshi = new Moshi.Builder().build();

  /**
   * Represents a specific time block when a course meets.
   * Contains the days of the week, start time, and end time.
   */
  public static class MeetingTime {
    /** Days of the week (0=Monday, 1=Tuesday, etc.) */
    public List<Integer> days;
    /** Start time in minutes since midnight */
    public int startTime;
    /** End time in minutes since midnight */
    public int endTime;

    /**
     * Creates a new MeetingTime with the specified parameters.
     *
     * @param days List of days when the course meets (0=Monday, 1=Tuesday, etc.)
     * @param startTime Start time in minutes since midnight
     * @param endTime End time in minutes since midnight
     */
    public MeetingTime(List<Integer> days, int startTime, int endTime) {
      this.days = days;
      this.startTime = startTime;
      this.endTime = endTime;
    }

    /**
     * Checks if this meeting time conflicts with another meeting time.
     * A conflict occurs when both times share at least one day and have overlapping time periods.
     *
     * @param other The other meeting time to check against
     * @return true if there is a conflict, false otherwise
     */
    public boolean conflictsWith(MeetingTime other) {
      // Check if there's any overlap in days
      boolean dayOverlap = false;
      for (Integer day : this.days) {
        if (other.days.contains(day)) {
          dayOverlap = true;
          break;
        }
      }

      // If no days overlap, there can't be a conflict
      if (!dayOverlap) {
        return false;
      }

      // Check if time periods overlap
      return !(this.endTime <= other.startTime || this.startTime >= other.endTime);
    }
  }

  /**
   * Parses meeting times from a course object.
   * Extracts meeting days and times from the course's meetingTimes JSON string.
   *
   * @param course Map representing a course with meetingTimes field
   * @return List of MeetingTime objects for the course
   */
  public static List<MeetingTime> parseMeetingTimes(Map<String, Object> course) {
    List<MeetingTime> meetingTimes = new ArrayList<>();

    try {
      String meetingTimesJson = (String) course.get("meetingTimes");
      if (meetingTimesJson == null || meetingTimesJson.isBlank()) {
        return meetingTimes;
      }

      JsonAdapter<List> adapter = moshi.adapter(List.class);
      List<Map<String, Object>> meetings = adapter.fromJson(meetingTimesJson);

      if (meetings == null) {
        return meetingTimes;
      }

      for (Map<String, Object> meeting : meetings) {
        String meetDay = (String) meeting.get("meet_day");
        String startTime = (String) meeting.get("start_time");
        String endTime = (String) meeting.get("end_time");

        List<Integer> days = new ArrayList<>();
        int day = Integer.parseInt(meetDay);

        days.add(day);

        int start = parseTimeToMinutes(startTime);
        int end = parseTimeToMinutes(endTime);

        meetingTimes.add(new MeetingTime(days, start, end));
      }
    } catch (Exception e) {
      System.err.println("Error parsing meeting times: " + e.getMessage());
    }

    return meetingTimes;
  }

  /**
   * Converts a time string to minutes since midnight.
   * Handles both 3-digit (e.g., "930") and 4-digit (e.g., "1430") time formats.
   *
   * @param timeStr String representation of time (e.g., "1430" for 2:30pm)
   * @return Time in minutes since midnight
   */
  private static int parseTimeToMinutes(String timeStr) {
    if (timeStr == null || (timeStr.length() != 4 && timeStr.length() != 3)) {
      return 0;
    }
    if (timeStr.length() == 3) {
      timeStr = "0" + timeStr;
    }
    int hours = Integer.parseInt(timeStr.substring(0, 2));
    int minutes = Integer.parseInt(timeStr.substring(2, 4));
    return hours * 60 + minutes;
  }

  /**
   * Checks if two courses have time conflicts.
   * Two courses conflict if they have overlapping meeting times on the same day.
   *
   * @param course1 First course to check
   * @param course2 Second course to check
   * @return true if the courses have a time conflict, false otherwise
   */
  public static boolean hasTimeConflict(Map<String, Object> course1, Map<String, Object> course2) {
    List<MeetingTime> meetingTimes1 = parseMeetingTimes(course1);
    List<MeetingTime> meetingTimes2 = parseMeetingTimes(course2);

    for (MeetingTime mt1 : meetingTimes1) {
      for (MeetingTime mt2 : meetingTimes2) {
        if (mt1.conflictsWith(mt2)) {
          return true;
        }
      }
    }

    return false;
  }

  /**
   * Checks if prerequisites for a course have been satisfied.
   * A course's prerequisites are satisfied if the student has taken at least one course
   * from each prerequisite group.
   *
   * @param courseObj Map representing a course with prereqGroups field
   * @param coursesTaken List of course codes the student has already taken
   * @return true if prerequisites are satisfied or none exist, false otherwise
   */
  public static boolean arePrerequisitesSatisfied(
      Map<String, Object> courseObj, List<String> coursesTaken) {

    @SuppressWarnings("unchecked")
    List<List<String>> groups =
        (List<List<String>>) courseObj.getOrDefault("prereqGroups", List.of());

    if (groups.isEmpty()) return true;

    // Each list in groups represents an "AND" condition
    // (must satisfy at least one course from each group)
    for (List<String> orSet : groups) {
      boolean satisfied = false;
      // Each course in the orSet represents an "OR" condition
      for (String code : orSet) {
        if (coursesTaken.contains(code)) {
          satisfied = true;
          break;
        }
      }
      if (!satisfied) return false;
    }
    return true;
  }

  /**
   * Parses meeting days from a meeting string.
   * Handles special formatting like "Th" for Thursday and combined day strings.
   *
   * @param meetingString String representation of meeting days and times (e.g., "MWF 10-11:20a")
   * @return Set of day codes ("M", "T", "W", "Th", "F")
   */
  public static Set<String> parseMeetingDays(String meetingString) {
    Set<String> days = new HashSet<>();

    if (meetingString == null || meetingString.isEmpty() || meetingString.equals("TBA")) {
      return days;
    }

    String[] parts = meetingString.split(" ");
    if (parts.length == 0) {
      return days;
    }

    String daysPart = parts[0];

    // Special handling for "Th" (Thursday)
    if (daysPart.contains("Th")) {
      if (daysPart.contains("M")) days.add("M");
      if (daysPart.contains("T") && !daysPart.equals("Th") && !daysPart.startsWith("Th"))
        days.add("T");
      if (daysPart.contains("W")) days.add("W");
      days.add("Th");
      if (daysPart.contains("F")) days.add("F");
    } else {
      // Standard handling for M, T, W, F
      for (char day : daysPart.toCharArray()) {
        if (day == 'M') days.add("M");
        if (day == 'T') days.add("T");
        if (day == 'W') days.add("W");
        if (day == 'F') days.add("F");
      }
    }
    return days;
  }

  /**
   * Checks if a time block is allowed based on the user's availability.
   * If no time constraints are provided, all times are considered allowed.
   *
   * @param timeBlock The time block to check (e.g., "10-11:20a")
   * @param allowed Set of allowed time blocks
   * @return true if the time block is allowed, false otherwise
   */
  public static boolean isAllowedTime(String timeBlock, Set<String> allowed) {
    if (allowed == null || allowed.isEmpty() || timeBlock == null) {
      return true;
    }
    if ("TBA".equals(timeBlock)) {
      return true;
    }
    return allowed.contains(timeBlock.trim());
  }
}