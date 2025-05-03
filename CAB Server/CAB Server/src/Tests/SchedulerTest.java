package Tests;

import java.util.*;
import Scheduler.*;
import Utilities.*;

/**
 * Test class to demonstrate the usage of the ScheduleGenerator.
 */
public class SchedulerTest {

  public static void main(String[] args) {
    try {
      // Set up mock user profile
      int classesPerSemester = 4;
      List<String> coursesTaken = Arrays.asList(
          "CSCI 0150", "CSCI 0160", "CSCI 0180", "CSCI 0220",
          "MATH 0100", "MATH 0520", "MATH 0540",
          "ECON 0110", "ECON 1110", "ECON 1130",
          "ENGL 0100", "ENGL 0511"
      );

      List<String> remainingRequired = Arrays.asList(
          "CSCI 0320", "CSCI 1270", "CSCI 1380",
          "MATH 0180", "MATH 1010",
          "ECON 1210", "ECON 1630"
      );

      // Set up mock preferences
      List<String> necessaryCourses = Arrays.asList("CSCI 0320");

      // Create a set of all available time blocks (simplified)
      Set<String> availableTimes = new HashSet<>();
      for (int hour = 8; hour <= 17; hour++) {
        availableTimes.add(hour + ":00");
        availableTimes.add(hour + ":30");
      }

      // Set day availability
      Map<String, Boolean> dayAvailability = new HashMap<>();
      dayAvailability.put("M", true);
      dayAvailability.put("T", true);
      dayAvailability.put("W", true);
      dayAvailability.put("Th", true);
      dayAvailability.put("F", true);

      // Set preferred day balance
      ScheduleGenerator.DayBalance dayBalance = new ScheduleGenerator.DayBalance(2, 2);

      // Other preferences
      int requiredCoursesThisSemester = 2;
      List<String> preferredDepts = Arrays.asList("CSCI", "MATH");
      boolean needWRIT = true;

      // Initialize the schedule generator
      ScheduleGenerator generator = new ScheduleGenerator(
          classesPerSemester,
          coursesTaken,
          remainingRequired,
          necessaryCourses,
          availableTimes,
          dayAvailability,
          dayBalance,
          requiredCoursesThisSemester,
          preferredDepts,
          needWRIT
      );

      // Load course data (term 202420 for Spring 2025)
      String term = "202420";
      generator.loadCourseData(term);

      // Filter courses based on availability and prerequisites
      generator.filterCourses(term);

      // Check for prerequisite issues with necessary courses
      List<String> missingPrereqs = generator.verifyNecessaryCourses(term);
      if (!missingPrereqs.isEmpty()) {
        System.out.println("WARNING: Missing prerequisites for necessary courses:");
        for (String course : missingPrereqs) {
          System.out.println("  - " + course);
        }
        System.out.println("Cannot generate schedules. Please resolve prerequisite issues.");
        return;
      }

      // Generate schedules
      List<ScheduleGenerator.Schedule> schedules = generator.generateSchedules(term);

      // Display results
      System.out.println("\nGENERATED SCHEDULES (" + schedules.size() + "):");
      System.out.println("=======================");

      for (int i = 0; i < schedules.size(); i++) {
        ScheduleGenerator.Schedule schedule = schedules.get(i);

        System.out.println("\nSCHEDULE OPTION " + (i + 1) + " (Score: " + schedule.score + "):");
        System.out.println("-------------------------------");

        // Get day balance
        ScheduleGenerator.DayBalance balance = schedule.getDayBalance();
        System.out.println("Day Balance: " + balance.mwfCount + " MWF, " + balance.tthCount + " TTh");

        // Check if WRIT requirement is satisfied
        boolean hasWRIT = schedule.hasWRITCourse(term);
        System.out.println("WRIT Course: " + (hasWRIT ? "Yes" : "No"));

        // Print course details
        System.out.println("\nCourses:");
        for (Map<String, Object> course : schedule.courses) {
          String code = (String) course.get("code");
          String title = (String) course.get("title");
          String meets = (String) course.get("meets");
          String instructor = (String) course.get("instr");

          // Mark if course is a required course
          String requiredMarker = remainingRequired.contains(code) ? " (Required)" : "";
          String necessaryMarker = necessaryCourses.contains(code) ? " (Necessary)" : "";

          System.out.println("  - " + code + ": " + title + requiredMarker + necessaryMarker);
          System.out.println("    " + meets + " with " + instructor);

          // Check if this is a WRIT course
          if (IsWRIT.isClassWRIT(code, term)) {
            System.out.println("    (WRIT designated)");
          }
        }

        System.out.println();
      }

    } catch (Exception e) {
      System.err.println("Error testing scheduler: " + e.getMessage());
      e.printStackTrace();
    }
  }
}