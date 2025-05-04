import React, { useState, useEffect, useCallback } from "react";
import { useUser, useClerk } from "@clerk/clerk-react";
import { getCourseOfferings } from "../Utilities/pastOfferings.tsx";

interface Course {
  code: string;
  title: string;
  id: string;
}

interface UserMetadata {
  courses?: Course[];
  preferredTime?: string;
  modality?: string;
}

// Predefined department list to avoid dynamic fetching
const departmentList = [
  "AFRI","AMST","ANTH","APMA","ARAB","ARCH","ARTS","ASYR","BHDS","BIOL","CHEM","CHIN","CLAS","CLPS","COLT","CSCI","DATA","EAST",
  "ECON","EDUC","EEPS","EGYT","EINT","EMOW","ENGL","ENGN","ENVS","ETHN","FREN","GNSS","GPHP","GREK","GRMN","HCL","HEBR","HIAA","HISP",
  "HMAN","HNDI","IAPA","ITAL","JAPN","JUDS","KREA","LACA","LANG","LATN","LING","LITR","MATH","MCM","MDVL","MED","MES","MGRK","MPA",
  "MUSC","NEUR","PHIL","PHP","PHYS","POLS","PRSN","RELS","RUSS","SANS","SAST","SIGN","SLAV","SOC","STS","TAPS","TKSH","UNIV","URBN",
  "VISA","YORU",
];

function Profile() {
  const { user } = useUser();
  const { session } = useClerk();
  const [selectedDepartment, setSelectedDepartment] = useState<string>("");
  const [selectedCourse, setSelectedCourse] = useState<string>("");
  const [selectedCoursesList, setSelectedCoursesList] = useState<Course[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [departmentCourses, setDepartmentCourses] = useState<Record<string, Course[]>>({});
  const [coursesLoading, setCoursesLoading] = useState(false);
  const [lastSavedTime, setLastSavedTime] = useState<number>(0);

  const [preferredTime, setPreferredTime] = useState<string>("");
  const [modality, setModality] = useState<string>("");

  useEffect(() => {
    const loadData = async () => {
      if (user) {
        try {
          const metadata = user.unsafeMetadata as UserMetadata;
          const courses = metadata.courses || [];
          const coursesWithIds = courses.map((course) => ({
            ...course,
            id:
              course.id ||
              `${course.code}-${Math.random().toString(36).substr(2, 9)}`,
          }));
          setSelectedCoursesList(coursesWithIds);
          setPreferredTime(metadata.preferredTime || "");
          setModality(metadata.modality || "");
        } catch (error) {
          console.error("Error loading metadata:", error);
        }
      }
      setIsLoading(false);
    };

    loadData();
  }, [user]);

  const saveMetadata = useCallback(async () => {
    if (!user || isLoading) return;
    const now = Date.now();
    if (now - lastSavedTime < 1000) return;
    setLastSavedTime(now);

    try {
      await user.update({
        unsafeMetadata: {
          courses: selectedCoursesList,
          preferredTime,
          modality,
        },
      });
      console.log("Saved metadata");
    } catch (error) {
      console.error("Error saving metadata:", error);
    }
  }, [user, isLoading, selectedCoursesList, preferredTime, modality, lastSavedTime]);

  useEffect(() => {
    const timer = setTimeout(() => saveMetadata(), 500);
    return () => clearTimeout(timer);
  }, [selectedCoursesList, preferredTime, modality, saveMetadata]);

  const loadDepartmentCourses = useCallback(async (dept: string) => {
    if (!dept || departmentCourses[dept]) return;
    setCoursesLoading(true);
    try {
      const offerings = await getCourseOfferings(dept);
      const uniqueOfferings = offerings.map((course) => ({
        ...course,
        id: `${course.code}-${Math.random().toString(36).substr(2, 9)}`,
      }));
      setDepartmentCourses((prev) => ({ ...prev, [dept]: uniqueOfferings }));
    } catch (error) {
      console.error(`Error loading courses for ${dept}:`, error);
      setDepartmentCourses((prev) => ({ ...prev, [dept]: [] }));
    } finally {
      setCoursesLoading(false);
    }
  }, [departmentCourses]);

  const handleDepartmentChange = (e: React.ChangeEvent<HTMLSelectElement>) => {
    const dept = e.target.value;
    setSelectedDepartment(dept);
    setSelectedCourse("");
    loadDepartmentCourses(dept);
  };

  const handleCourseSelect = (e: React.ChangeEvent<HTMLSelectElement>) => {
    const courseCode = e.target.value;
    if (courseCode && selectedDepartment) {
      const course = departmentCourses[selectedDepartment]?.find((c) => c.code === courseCode);
      if (course && !selectedCoursesList.some((c) => c.id === course.id)) {
        setSelectedCoursesList([...selectedCoursesList, course]);
      }
    }
    setSelectedCourse("");
  };

  const removeCourse = (courseId: string) => {
    setSelectedCoursesList(selectedCoursesList.filter((course) => course.id !== courseId));
  };

  if (!session) return <div style={{ padding: "2rem" }}><h1>Profile Setup</h1><p>Please sign in to manage your preferences and courses.</p></div>;
  if (isLoading) return <div style={{ padding: "2rem" }}><h1>Profile Setup</h1><p>Loading...</p></div>;

  return (
      <div className="profile-page">
        <h1 className="profile-title">Profile Setup</h1>

        <div className="profile-container">
          <h3 className="section-title">Preferences</h3>

          <div className="form-group">
            <label className="form-label">Preferred Class Time:</label>
            <select
                className="form-select"
                value={preferredTime}
                onChange={(e) => setPreferredTime(e.target.value)}
            >
              <option value="">-- Select Time --</option>
              <option value="Morning">Morning</option>
              <option value="Afternoon">Afternoon</option>
              <option value="Evening">Evening</option>
            </select>
          </div>

          <div className="form-group">
            <label className="form-label">Preferred Modality:</label>
            <select
                className="form-select"
                value={modality}
                onChange={(e) => setModality(e.target.value)}
            >
              <option value="">-- Select Modality --</option>
              <option value="In Person">In Person</option>
              <option value="Hybrid">Hybrid</option>
              <option value="Online">Online</option>
            </select>
          </div>

          <h3 className="section-title">Completed Courses</h3>

          <div className="form-group">
            <label className="form-label">Select Department:</label>
            <select
                className="form-select"
                value={selectedDepartment}
                onChange={handleDepartmentChange}
            >
              <option value="">-- Select Department --</option>
              {departmentList.map((dept) => (
                  <option key={dept} value={dept}>
                    {dept}
                  </option>
              ))}
            </select>
          </div>

          {selectedDepartment && (
              <div className="form-group">
                <label className="form-label">Select Course:</label>
                <select
                    className="form-select"
                    value={selectedCourse}
                    onChange={handleCourseSelect}
                    disabled={coursesLoading}
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

          {selectedCoursesList.length > 0 ? (
              <ul style={{ listStyle: "none", padding: 0 }}>
                {selectedCoursesList.map((course) => (
                    <li className="course-card" key={course.id}>
              <span>
                <strong>{course.code}</strong>: {course.title}
              </span>
                      <button
                          className="remove-btn"
                          onClick={() => removeCourse(course.id)}
                      >
                        Remove
                      </button>
                    </li>
                ))}
              </ul>
          ) : (
              <p>No courses selected yet</p>
          )}
        </div>
      </div>
  );
}

export default Profile;