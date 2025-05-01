import React, { useState, useEffect, useCallback } from "react";
import { useUser, useClerk } from "@clerk/clerk-react";
import { getCourseOfferings } from "../Utilities/pastOfferings";

interface Course {
  code: string;
  title: string;
  id: string; // Ensure unique ID for each course
}

// Predefined department list to avoid dynamic fetching
const departmentList = [
  "AFRI",
  "AMST",
  "ANTH",
  "APMA",
  "ARAB",
  "ARCH",
  "ARTS",
  "ASYR",
  "BHDS",
  "BIOL",
  "CHEM",
  "CHIN",
  "CLAS",
  "CLPS",
  "COLT",
  "CSCI",
  "DATA",
  "EAST",
  "ECON",
  "EDUC",
  "EEPS",
  "EGYT",
  "EINT",
  "EMOW",
  "ENGL",
  "ENGN",
  "ENVS",
  "ETHN",
  "FREN",
  "GNSS",
  "GPHP",
  "GREK",
  "GRMN",
  "HCL",
  "HEBR",
  "HIAA",
  "HISP",
  "HMAN",
  "HNDI",
  "IAPA",
  "ITAL",
  "JAPN",
  "JUDS",
  "KREA",
  "LACA",
  "LANG",
  "LATN",
  "LING",
  "LITR",
  "MATH",
  "MCM",
  "MDVL",
  "MED",
  "MES",
  "MGRK",
  "MPA",
  "MUSC",
  "NEUR",
  "PHIL",
  "PHP",
  "PHYS",
  "POLS",
  "PRSN",
  "RELS",
  "RUSS",
  "SANS",
  "SAST",
  "SIGN",
  "SLAV",
  "SOC",
  "STS",
  "TAPS",
  "TKSH",
  "UNIV",
  "URBN",
  "VISA",
  "YORU",
];

function Profile() {
  const { user } = useUser();
  const { session } = useClerk();
  const [selectedDepartment, setSelectedDepartment] = useState<string>("");
  const [selectedCourse, setSelectedCourse] = useState<string>("");
  const [selectedCoursesList, setSelectedCoursesList] = useState<Course[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [departmentCourses, setDepartmentCourses] = useState<
    Record<string, Course[]>
  >({});
  const [coursesLoading, setCoursesLoading] = useState(false);
  const [lastSavedTime, setLastSavedTime] = useState<number>(0);

  // Load saved courses when component mounts or user changes
  useEffect(() => {
    const loadCourses = async () => {
      if (user) {
        try {
          const metadata = user.unsafeMetadata;
          const courses = (metadata.courses as Course[]) || [];
          // Ensure all saved courses have unique IDs
          const coursesWithIds = courses.map((course) => ({
            ...course,
            id:
              course.id ||
              `${course.code}-${Math.random().toString(36).substr(2, 9)}`,
          }));
          setSelectedCoursesList(coursesWithIds);
        } catch (error) {
          console.error("Error loading courses:", error);
        }
      }
      setIsLoading(false);
    };

    loadCourses();
  }, [user]);

  // Debounced save function to prevent rate limiting
  const saveCourses = useCallback(async () => {
    if (!user || isLoading) return;

    // Rate limiting - don't save more than once per second
    const now = Date.now();
    if (now - lastSavedTime < 1000) return;

    setLastSavedTime(now);

    try {
      await user.update({
        unsafeMetadata: {
          ...user.unsafeMetadata,
          courses: selectedCoursesList,
        },
      });
     console.log(selectedCoursesList)
    } catch (error) {
      console.error("Error saving courses:", error);
    }
  }, [selectedCoursesList, user, isLoading, lastSavedTime]);

  // Save courses when the list changes (with debounce)
  useEffect(() => {
    const timer = setTimeout(() => {
      saveCourses();
    }, 500); // 500ms debounce

    return () => clearTimeout(timer);
  }, [selectedCoursesList, saveCourses]);

  // Load courses only when department is selected
  const loadDepartmentCourses = useCallback(
    async (dept: string) => {
      if (!dept || departmentCourses[dept]) return;

      setCoursesLoading(true);
      try {
        const offerings = await getCourseOfferings(dept);
        // Add unique IDs to courses
        const uniqueOfferings = offerings.map((course) => ({
          ...course,
          id: `${course.code}-${Math.random().toString(36).substr(2, 9)}`,
        }));
        setDepartmentCourses((prev) => ({
          ...prev,
          [dept]: uniqueOfferings,
        }));
      } catch (error) {
        console.error(`Error loading courses for ${dept}:`, error);
        setDepartmentCourses((prev) => ({
          ...prev,
          [dept]: [],
        }));
      } finally {
        setCoursesLoading(false);
      }
    },
    [departmentCourses]
  );

  const handleDepartmentChange = (e: React.ChangeEvent<HTMLSelectElement>) => {
    const dept = e.target.value;
    setSelectedDepartment(dept);
    setSelectedCourse("");
    loadDepartmentCourses(dept);
  };

  const handleCourseSelect = (e: React.ChangeEvent<HTMLSelectElement>) => {
    const courseCode = e.target.value;
    if (courseCode && selectedDepartment) {
      const course = departmentCourses[selectedDepartment]?.find(
        (c) => c.code === courseCode
      );
      if (course && !selectedCoursesList.some((c) => c.id === course.id)) {
        setSelectedCoursesList([...selectedCoursesList, course]);
      }
    }
    setSelectedCourse("");
  };

  const removeCourse = (courseId: string) => {
    setSelectedCoursesList(
      selectedCoursesList.filter((course) => course.id !== courseId)
    );
  };

  if (!session) {
    return (
      <div style={{ padding: "2rem", maxWidth: "600px" }}>
        <h1>Profile Setup</h1>
        <p>Please sign in to manage your courses.</p>
      </div>
    );
  }

  if (isLoading) {
    return (
      <div style={{ padding: "2rem", maxWidth: "600px" }}>
        <h1>Profile Setup</h1>
        <p>Loading your courses...</p>
      </div>
    );
  }

  return (
    <div style={{ padding: "2rem", maxWidth: "600px" }}>
      <h1>Profile Setup</h1>
      <p>Select your department and courses below.</p>

      {/* Department Dropdown */}
      <div style={{ marginBottom: "1.5rem" }}>
        <label
          htmlFor="department"
          style={{ display: "block", marginBottom: "0.5rem" }}
        >
          Select Department:
        </label>
        <select
          id="department"
          value={selectedDepartment}
          onChange={handleDepartmentChange}
          style={{ padding: "8px", fontSize: "16px", width: "100%" }}
        >
          <option value="">-- Select Department --</option>
          {departmentList.map((dept) => (
            <option key={dept} value={dept}>
              {dept}
            </option>
          ))}
        </select>
      </div>

      {/* Course Dropdown */}
      {selectedDepartment && (
        <div style={{ marginBottom: "1.5rem" }}>
          <label
            htmlFor="course"
            style={{ display: "block", marginBottom: "0.5rem" }}
          >
            Select Course:
            {coursesLoading && (
              <span style={{ marginLeft: "8px", color: "#666" }}>
                (Loading...)
              </span>
            )}
          </label>
          <select
            id="course"
            value={selectedCourse}
            onChange={handleCourseSelect}
            style={{ padding: "8px", fontSize: "16px", width: "100%" }}
            disabled={coursesLoading || !departmentCourses[selectedDepartment]}
          >
            <option value="">-- Select Course --</option>
            {departmentCourses[selectedDepartment]?.map((course) => (
              <option key={course.id} value={course.code}>
                {course.code}: {course.title}
              </option>
            ))}
          </select>
        </div>
      )}

      {/* Selected Courses List */}
      <div>
        <h3>Completed Courses:</h3>
        {selectedCoursesList.length === 0 ? (
          <p>No courses selected yet</p>
        ) : (
          <ul style={{ listStyle: "none", padding: 0 }}>
            {selectedCoursesList.map((course) => (
              <li
                key={course.id}
                style={{
                  display: "flex",
                  justifyContent: "space-between",
                  alignItems: "center",
                  padding: "8px",
                  border: "1px solid #ddd",
                  marginBottom: "5px",
                  backgroundColor: "#f9f9f9",
                }}
              >
                <span>
                  <strong>{course.code}</strong>: {course.title}
                </span>
                <button
                  onClick={() => removeCourse(course.id)}
                  style={{
                    background: "#ff4444",
                    color: "white",
                    border: "none",
                    borderRadius: "4px",
                    padding: "4px 8px",
                    cursor: "pointer",
                  }}
                >
                  Remove
                </button>
              </li>
            ))}
          </ul>
        )}
      </div>
    </div>
  );
}

export default Profile;
