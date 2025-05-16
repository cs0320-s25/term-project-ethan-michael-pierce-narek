package Scheduler;

import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.Moshi;
import java.util.*;


public class SchedulerUtils {

  private static final Moshi moshi = new Moshi.Builder().build();

  public static class MeetingTime {
    public List<Integer> days;
    public int startTime;
    public int endTime;

    public MeetingTime(List<Integer> days, int startTime, int endTime) {
      this.days = days;
      this.startTime = startTime;
      this.endTime = endTime;
    }

    public boolean conflictsWith(MeetingTime other) {
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

      return !(this.endTime <= other.startTime || this.startTime >= other.endTime);
    }
  }

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


  private static int parseTimeToMinutes(String timeStr) {
    if (timeStr == null || (timeStr.length() != 4 && timeStr.length() != 3)) {
      return 0;
    }
    if (timeStr.length() == 3) {
      timeStr = "0" + timeStr;
    }
    int hours  = Integer.parseInt(timeStr.substring(0, 2));
    int minutes = Integer.parseInt(timeStr.substring(2, 4));
    return hours * 60 + minutes;
  }

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

  public static boolean arePrerequisitesSatisfied(
      Map<String, Object> courseObj, List<String> coursesTaken) {

    @SuppressWarnings("unchecked")
    List<List<String>> groups =
        (List<List<String>>) courseObj.getOrDefault("prereqGroups", List.of());

    if (groups.isEmpty()) return true;

    for (List<String> orSet : groups) {
      boolean satisfied = false;
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

    if (daysPart.contains("Th")) {
      if (daysPart.contains("M")) days.add("M");
      if (daysPart.contains("T") && !daysPart.equals("Th") && !daysPart.startsWith("Th"))
        days.add("T");
      if (daysPart.contains("W")) days.add("W");
      days.add("Th");
      if (daysPart.contains("F")) days.add("F");
    } else {
      for (char day : daysPart.toCharArray()) {
        if (day == 'M') days.add("M");
        if (day == 'T') days.add("T");
        if (day == 'W') days.add("W");
        if (day == 'F') days.add("F");
      }
    }
    return days;
  }

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
