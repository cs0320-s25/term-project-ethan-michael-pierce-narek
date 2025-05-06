package Scheduler;

import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.Moshi;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Utility class for handling prerequisites and time conflicts in the scheduler system. */
public class SchedulerUtils {

  private static final Moshi moshi = new Moshi.Builder().build();

  /** Represents a class meeting time. */
  public static class MeetingTime {
    public List<Integer> days; // 0 = Monday, 1 = Tuesday, etc.
    public int startTime; // Minutes from midnight
    public int endTime; // Minutes from midnight

    public MeetingTime(List<Integer> days, int startTime, int endTime) {
      this.days = days;
      this.startTime = startTime;
      this.endTime = endTime;
    }

    /** Check if this meeting time conflicts with another */
    public boolean conflictsWith(MeetingTime other) {
      // Check if there's any day overlap
      boolean dayOverlap = false;
      for (Integer day : this.days) {
        if (other.days.contains(day)) {
          dayOverlap = true;
          break;
        }
      }

      if (!dayOverlap) {
        return false;
      }

      // Check if times overlap
      return !(this.endTime <= other.startTime || this.startTime >= other.endTime);
    }
  }

  /** Parse the meeting times from a course. */
  public static List<MeetingTime> parseMeetingTimes(Map<String, Object> course) {
    List<MeetingTime> meetingTimes = new ArrayList<>();

    try {
      // In the JSON, meetingTimes is a string representing a JSON array
      String meetingTimesJson = (String) course.get("meetingTimes");
      if (meetingTimesJson == null || meetingTimesJson.isBlank()) {
        return meetingTimes;
      }

      // Parse the nested JSON
      JsonAdapter<List> adapter = moshi.adapter(List.class);
      List<Map<String, Object>> meetings = adapter.fromJson(meetingTimesJson);

      if (meetings == null) {
        return meetingTimes;
      }

      for (Map<String, Object> meeting : meetings) {
        String meetDay = (String) meeting.get("meet_day");
        String startTime = (String) meeting.get("start_time");
        String endTime = (String) meeting.get("end_time");

        // Convert day codes to day indices
        List<Integer> days = new ArrayList<>();
        int day = Integer.parseInt(meetDay);
        // Convert from CAB notation (0=Monday) to our notation
        days.add(day);

        // Convert time strings (e.g., "1330" for 1:30pm) to minutes since midnight
        int start = parseTimeToMinutes(startTime);
        int end = parseTimeToMinutes(endTime);

        meetingTimes.add(new MeetingTime(days, start, end));
      }
    } catch (Exception e) {
      System.err.println("Error parsing meeting times: " + e.getMessage());
    }

    return meetingTimes;
  }

  /** Parse a time string like "1330" to minutes since midnight (e.g., 13*60 + 30 = 810) */
  private static int parseTimeToMinutes(String timeStr) {
    if (timeStr == null || timeStr.length() != 4) {
      return 0;
    }

    try {
      int hours = Integer.parseInt(timeStr.substring(0, 2));
      int minutes = Integer.parseInt(timeStr.substring(2, 4));
      return hours * 60 + minutes;
    } catch (NumberFormatException e) {
      return 0;
    }
  }

  /** Check if two courses have conflicting meeting times. */
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
   * Parse prerequisites from a course description. Note: This is a simplified approach and might
   * need refinement based on actual data.
   */
  public static List<String> parsePrerequisites(String description) {
    List<String> prerequisites = new ArrayList<>();

    if (description == null || description.isEmpty()) {
      return prerequisites;
    }

    // Look for the prerequisite section
    int prereqIndex = description.toLowerCase().indexOf("prerequisite");
    if (prereqIndex == -1) {
      return prerequisites;
    }

    // Extract text after "prerequisite:"
    String prereqText = description.substring(prereqIndex);

    // End at the next period or end of string
    int endIndex = prereqText.indexOf('.');
    if (endIndex != -1) {
      prereqText = prereqText.substring(0, endIndex);
    }

    // Regular expression to match course codes like "CSCI 0320" or "MATH 0100"
    Pattern coursePattern = Pattern.compile("([A-Z]{3,4}\\s\\d{4}[A-Z]?)");
    Matcher matcher = coursePattern.matcher(prereqText);

    while (matcher.find()) {
      prerequisites.add(matcher.group(1));
    }

    return prerequisites;
  }

  /** Check if all prerequisites for a course are satisfied. */
  public static boolean arePrerequisitesSatisfied(
      String courseCode, String description, List<String> coursesTaken) {

    List<String> prerequisites = parsePrerequisites(description);

    // Empty prerequisites means no prerequisites or couldn't parse any
    if (prerequisites.isEmpty()) {
      return true;
    }

    // Check if all prerequisites are in the coursesTaken list
    for (String prereq : prerequisites) {
      if (!coursesTaken.contains(prereq)) {
        return false;
      }
    }

    return true;
  }

  /** Extract the department code from a course code. */
  public static String getDepartmentCode(String courseCode) {
    if (courseCode == null || courseCode.isEmpty()) {
      return "";
    }

    String[] parts = courseCode.split(" ");
    if (parts.length > 0) {
      return parts[0];
    }

    return "";
  }

  /** Parse the human-readable meeting string (e.g., "MWF 10-10:50a") to days of the week. */
  public static Set<String> parseMeetingDays(String meetingString) {
    Set<String> days = new HashSet<>();

    if (meetingString == null || meetingString.isEmpty() || meetingString.equals("TBA")) {
      return days;
    }

    // Split by space to separate days from times
    String[] parts = meetingString.split(" ");
    if (parts.length == 0) {
      return days;
    }

    String daysPart = parts[0];

    // Handle special cases like "MWThF"
    if (daysPart.contains("Th")) {
      if (daysPart.contains("M")) days.add("M");
      if (daysPart.contains("T") && !daysPart.equals("Th") && !daysPart.startsWith("Th"))
        days.add("T");
      if (daysPart.contains("W")) days.add("W");
      days.add("Th");
      if (daysPart.contains("F")) days.add("F");
    } else {
      // For simpler formats like "MWF"
      for (char day : daysPart.toCharArray()) {
        if (day == 'M') days.add("M");
        if (day == 'T') days.add("T");
        if (day == 'W') days.add("W");
        if (day == 'F') days.add("F");
      }
    }

    return days;
  }

  /** Parse the time range from a meeting string (e.g., "10-10:50a" from "MWF 10-10:50a") */
  public static Map<String, Integer> parseTimeRange(String meetingString) {
    Map<String, Integer> result = new HashMap<>();
    result.put("start", -1);
    result.put("end", -1);

    if (meetingString == null || meetingString.isEmpty() || meetingString.equals("TBA")) {
      return result;
    }

    // Split by space to separate days from times
    String[] parts = meetingString.split(" ");
    if (parts.length < 2) {
      return result;
    }

    String timePart = parts[1];

    // Handle time ranges like "10-10:50a" or "1:30-4p"
    String[] timeRange = timePart.split("-");
    if (timeRange.length != 2) {
      return result;
    }

    try {
      // Parse start time
      String startTimeStr = timeRange[0];
      boolean startPM = startTimeStr.endsWith("p");
      if (startPM) startTimeStr = startTimeStr.substring(0, startTimeStr.length() - 1);

      // Parse as hours and minutes
      int startHours, startMinutes;
      if (startTimeStr.contains(":")) {
        String[] startHM = startTimeStr.split(":");
        startHours = Integer.parseInt(startHM[0]);
        startMinutes = Integer.parseInt(startHM[1]);
      } else {
        startHours = Integer.parseInt(startTimeStr);
        startMinutes = 0;
      }

      // Convert to 24-hour format if PM
      if (startPM && startHours < 12) {
        startHours += 12;
      }

      // Parse end time
      String endTimeStr = timeRange[1];
      boolean endPM = endTimeStr.endsWith("p") || endTimeStr.endsWith("a");
      String ampm = "";
      if (endPM) {
        ampm = endTimeStr.substring(endTimeStr.length() - 1);
        endTimeStr = endTimeStr.substring(0, endTimeStr.length() - 1);
      }

      // Parse as hours and minutes
      int endHours, endMinutes;
      if (endTimeStr.contains(":")) {
        String[] endHM = endTimeStr.split(":");
        endHours = Integer.parseInt(endHM[0]);
        endMinutes = Integer.parseInt(endHM[1]);
      } else {
        endHours = Integer.parseInt(endTimeStr);
        endMinutes = 0;
      }

      // Convert to 24-hour format if PM or if not specified but start was PM
      if ((ampm.equals("p") || (ampm.isEmpty() && startPM)) && endHours < 12) {
        endHours += 12;
      }

      // Store as minutes since midnight
      result.put("start", startHours * 60 + startMinutes);
      result.put("end", endHours * 60 + endMinutes);

    } catch (NumberFormatException e) {
      System.err.println("Error parsing time range: " + e.getMessage());
    }

    return result;
  }
}
