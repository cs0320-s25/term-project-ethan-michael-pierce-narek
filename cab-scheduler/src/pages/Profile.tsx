import React, { useState, useEffect, useCallback } from "react";
import { useUser, useClerk } from "@clerk/clerk-react";
import { CompletedCourses } from "../Utilities/CompletedCourses";
import { DesiredCourses } from "../Utilities/DesiredCourses";
import { getCourseOfferings } from "../Utilities/pastOfferings.tsx";

interface Course {
  code: string; // Simplified - using code as the identifier
  title: string;
}

interface UserMetadata {
  courses?: Course[];
  desiredCourses?: Course[];
  preferredTime?: string;
  modality?: string;
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
  const [isLoading, setIsLoading] = useState(true);
  const [lastSavedTime, setLastSavedTime] = useState<number>(0);
  const [loadingDepartments, setLoadingDepartments] = useState<
    Record<string, boolean>
  >({});

  // Courses state
  const [selectedCoursesList, setSelectedCoursesList] = useState<Course[]>([]);
  const [desiredCoursesList, setDesiredCoursesList] = useState<Course[]>([]);
  const [departmentCourses, setDepartmentCourses] = useState<
    Record<string, Course[]>
  >({});

  useEffect(() => {
    const loadData = async () => {
      if (user) {
        try {
          const metadata = user.unsafeMetadata as UserMetadata;

          // Simplified course loading - no ID generation needed
          setSelectedCoursesList(metadata.courses || []);
          setDesiredCoursesList(metadata.desiredCourses || []);
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
          desiredCourses: desiredCoursesList,
        },
      });
      console.log("Saved metadata");
    } catch (error) {
      console.error("Error saving metadata:", error);
    }
  }, [user, isLoading, selectedCoursesList, desiredCoursesList, lastSavedTime]);

  useEffect(() => {
    const timer = setTimeout(() => saveMetadata(), 500);
    return () => clearTimeout(timer);
  }, [selectedCoursesList, desiredCoursesList, saveMetadata]);

  const loadDepartmentCourses = useCallback(
    async (dept: string) => {
      if (!dept || departmentCourses[dept]) return;
      setLoadingDepartments((prev) => ({ ...prev, [dept]: true }));
      try {
        const offerings = await getCourseOfferings(dept);
        setDepartmentCourses((prev) => ({ ...prev, [dept]: offerings }));
      } catch (error) {
        console.error(`Error loading courses for ${dept}:`, error);
        setDepartmentCourses((prev) => ({ ...prev, [dept]: [] }));
      } finally {
        setLoadingDepartments((prev) => ({ ...prev, [dept]: false }));
      }
    },
    [departmentCourses],
  );

  if (!session)
    return (
      <div style={{ padding: "2rem" }}>
        <h1>Profile Setup</h1>
        <p>Please sign in to manage your preferences and courses.</p>
      </div>
    );

  if (isLoading)
    return (
      <div style={{ padding: "2rem" }}>
        <h1>Profile Setup</h1>
        <p>Loading...</p>
      </div>
    );

  return (
    <div className="profile-page">
      <h1 className="profile-title">Profile Setup</h1>

      <div className="courses-container">
        <div className="course-box">
          <CompletedCourses
            departmentList={departmentList}
            courses={selectedCoursesList}
            onAddCourse={(course) =>
              setSelectedCoursesList((prev) => [...prev, course])
            }
            onRemoveCourse={(code) =>
              setSelectedCoursesList((prev) =>
                prev.filter((c) => c.code !== code),
              )
            }
            departmentCourses={departmentCourses}
            isLoadingDepartment={(dept) => !!loadingDepartments[dept]}
            onLoadDepartment={loadDepartmentCourses}
          />
        </div>

        <div className="course-box">
          <DesiredCourses
            departmentList={departmentList}
            courses={desiredCoursesList}
            onAddCourse={(course) =>
              setDesiredCoursesList((prev) => [...prev, course])
            }
            onRemoveCourse={(code) =>
              setDesiredCoursesList((prev) =>
                prev.filter((c) => c.code !== code),
              )
            }
            departmentCourses={departmentCourses}
            isLoadingDepartment={(dept) => !!loadingDepartments[dept]}
            onLoadDepartment={loadDepartmentCourses}
          />
        </div>
      </div>
    </div>
  );
}

export default Profile;
