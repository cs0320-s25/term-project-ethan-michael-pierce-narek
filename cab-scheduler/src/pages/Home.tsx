import { useUser, SignInButton } from "@clerk/clerk-react";
import React, { useEffect, useRef, useState, useCallback } from "react";
import { getCourseOfferings } from "../Utilities/pastOfferings";

interface Course {
  code: string;
  title: string;
}

function Home() {
  const { user } = useUser();
  const [excludedClasses, setExcludedClasses] = useState<string[]>([]);
  const [totalClasses, setTotalClasses] = useState<number>(3);
  const [mwfClasses, setMwfClasses] = useState<number>(2);
  const [requiredClasses, setRequiredClasses] = useState<Course[]>([]);
  const [departmentCourses, setDepartmentCourses] = useState<Record<string, Course[]>>({});
  const [loadingDepartments, setLoadingDepartments] = useState<Record<string, boolean>>({});
  const [electiveDepartments, setElectiveDepartments] = useState<string[]>([]);
  const [needsWrit, setNeedsWrit] = useState<boolean>(false);
  const [NumberDesiredClasses, setNumberDesiredClasses] = useState<number>(3);
  const [hasLoadedMetadata, setHasLoadedMetadata] = useState(false);

  const departmentList = [
    "AFRI", "AMST", "ANTH", "APMA", "ARAB", "ARCH", "ARTS", "ASYR", "BHDS",
    "BIOL", "CHEM", "CHIN", "CLAS", "CLPS", "COLT", "CSCI", "DATA", "EAST",
    "ECON", "EDUC", "EEPS", "EGYT", "EINT", "EMOW", "ENGL", "ENGN", "ENVS",
    "ETHN", "FREN", "GNSS", "GPHP", "GREK", "GRMN", "HCL", "HEBR", "HIAA",
    "HISP", "HMAN", "HNDI", "IAPA", "ITAL", "JAPN", "JUDS", "KREA", "LACA",
    "LANG", "LATN", "LING", "LITR", "MATH", "MCM", "MDVL", "MED", "MES",
    "MGRK", "MPA", "MUSC", "NEUR", "PHIL", "PHP", "PHYS", "POLS", "PRSN",
    "RELS", "RUSS", "SANS", "SAST", "SIGN", "SLAV", "SOC", "STS", "TAPS",
    "TKSH", "UNIV", "URBN", "VISA", "YORU"
  ];

  useEffect(() => {
    if (user && !hasLoadedMetadata) {
      const metadata = user.unsafeMetadata as any;
      setExcludedClasses(metadata.excludedClasses ?? []);
      setTotalClasses(metadata.totalClasses ?? 3);
      setMwfClasses(metadata.mwfClasses ?? 2);
      setRequiredClasses(metadata.requiredClasses ?? []);
      setElectiveDepartments(metadata.electiveDepartments ?? []);
      setNeedsWrit(metadata.needsWrit ?? false);
      setNumberDesiredClasses(metadata.NumberDesiredClasses ?? 3);
      setHasLoadedMetadata(true);
    }
  }, [user, hasLoadedMetadata]);

  const saveTimeout = useRef<NodeJS.Timeout | null>(null);

  const saveMetadata = async () => {
    if (user) {
      try {
        const currentMetadata = user.unsafeMetadata || {};
        await user.update({
          unsafeMetadata: {
            ...currentMetadata,
            excludedClasses,
            totalClasses,
            mwfClasses,
            requiredClasses,
            electiveDepartments,
            needsWrit,
            NumberDesiredClasses,
          },
        });
      } catch (error) {
        console.error("Error saving metadata:", error);
      }
    }
  };

  useEffect(() => {
    if (!user || !hasLoadedMetadata) return;
    if (saveTimeout.current) clearTimeout(saveTimeout.current);
    saveTimeout.current = setTimeout(() => {
      saveMetadata();
    }, 500);
  }, [excludedClasses, totalClasses, mwfClasses, requiredClasses, electiveDepartments, needsWrit, hasLoadedMetadata, NumberDesiredClasses]);

  const tthClasses = totalClasses - mwfClasses;

  const dayOptions = {
    Monday: ["8-8:50a", "9-9:50a", "10-10:50a", "11-11:50a", "12-12:50p", "1-1:50p", "2-2:50p", "3-5:30p"],
    Tuesday: ["9-10:20a", "10:30-11:50a", "1-2:20p", "2:30-3:50p", "6:40-8p", "4-6:30p"],
    Wednesday: ["8-8:50a", "9-9:50a", "10-10:50a", "11-11:50a", "12-12:50p", "1-1:50p", "2-2:50p", "3-5:30p"],
    Thursday: ["9-10:20a", "10:30-11:50a", "1-2:20p", "2:30-3:50p", "6:40-8p", "4-6:30p"],
    Friday: ["8-8:50a", "9-9:50a", "10-10:50a", "11-11:50a", "12-12:50p", "1-1:50p", "2-2:50p", "3-5:30p"],
  };

  const handleOptionSelect = (option: string) => {
    setExcludedClasses((prev) => prev.includes(option) ? prev.filter((item) => item !== option) : [...prev, option]);
  };

  const handleAddElectiveDepartment = (dept: string) => {
    if (!electiveDepartments.includes(dept)) {
      setElectiveDepartments([...electiveDepartments, dept]);
    }
  };

  const handleAddRequiredCourse = (course: Course) => {
    if (!requiredClasses.some(c => c.code === course.code)) {
      setRequiredClasses([...requiredClasses, course]);
    }
  };

  const handleRemoveRequiredCourse = (code: string) => {
    setRequiredClasses((prev) => prev.filter((c) => c.code !== code));
  };

  const loadDepartmentCourses = useCallback(async (dept: string) => {
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
  }, [departmentCourses]);

  return (
      <div className="home-container">
        <div className="home-content">
          <h1 className="home-title">Welcome to C@B Smart Scheduler</h1>
          <p className="home-tagline">Plan smarter. Learn better. Graduate happier.</p>

          {!user && (
              <SignInButton mode="modal">
                <button className="primary-btn home-btn">Get Started</button>
              </SignInButton>
          )}

          {user && (
              <>
                {/* Class Distribution Preferences */}
                <div className="preferences-card">
                  <h2 className="section-title">Class Distribution</h2>
                  <div className="preference-group">
                    <h3>Total Classes: {totalClasses}</h3>
                    <div className="number-selector">
                      {[1, 2, 3, 4, 5].map((num) => (
                          <button
                              key={num}
                              className={`number-option ${totalClasses === num ? "selected" : ""}`}
                              onClick={() => setTotalClasses(num)}
                          >
                            {num}
                          </button>
                      ))}
                    </div>
                  </div>

                  <div className="preference-group">
                    <h3>MWF vs TTh Distribution</h3>
                    <div className="distribution-display">
                      <span>MWF: {mwfClasses}</span>
                      <input
                          type="range"
                          min="0"
                          max={totalClasses}
                          value={mwfClasses}
                          onChange={(e) => setMwfClasses(parseInt(e.target.value))}
                          className="distribution-slider"
                      />
                      <span>TTh: {tthClasses}</span>
                    </div>

                    <div className="day-visualization">
                      <div className="day-blocks">
                        {[...Array(mwfClasses)].map((_, i) => (
                            <div key={`mwf-${i}`} className="mwf-block">MWF</div>
                        ))}
                        {[...Array(tthClasses)].map((_, i) => (
                            <div key={`tth-${i}`} className="tth-block">TTh</div>
                        ))}
                      </div>
                    </div>
                  </div>
                </div>

                {/* Exclude Class Times */}
                <div className="preferences-card">
                  <h2 className="section-title">Include Class Times</h2>
                  <div className="day-dropdowns-grid">
                    {Object.keys(dayOptions).map((day) => (
                        <div key={day} className="day-preference">
                          <label className="day-label">{day}</label>
                          <select
                              value=""
                              onChange={(e) => handleOptionSelect(e.target.value)}
                              className="day-select"
                          >
                            <option value="" disabled hidden>Select...</option>
                            {dayOptions[day as keyof typeof dayOptions].map((option) => (
                                <option key={option} value={option}>{option}</option>
                            ))}
                          </select>
                        </div>
                    ))}
                  </div>
                  <div className="selected-tags-container">
                    <div className="selected-tags">
                      {excludedClasses.map((item, index) => (
                          <span key={index} className="tag">
                      {item}
                            <button onClick={() => handleOptionSelect(item)} className="remove-tag">×</button>
                    </span>
                      ))}
                    </div>
                  </div>
                </div>

                {/* Required Courses */}
                <div className="preferences-card">
                  <h2 className="section-title">Required Courses</h2>
                  <div className="department-course-dropdown">
                    <select
                        onChange={async (e) => {
                          const dept = e.target.value;
                          await loadDepartmentCourses(dept);
                        }}
                        defaultValue=""
                    >
                      <option value="" disabled hidden>Select Department...</option>
                      {departmentList.map((dept) => (
                          <option key={dept} value={dept}>{dept}</option>
                      ))}
                    </select>
                    <select
                        onChange={(e) => {
                          const selected = e.target.value;
                          const course = Object.values(departmentCourses)
                              .flat()
                              .find((c) => c.code === selected);
                          if (course) handleAddRequiredCourse(course);
                        }}
                        defaultValue=""
                    >
                      <option value="" disabled hidden>Select Course...</option>
                      {Object.values(departmentCourses)
                          .flat()
                          .map((course) => (
                              <option key={course.code} value={course.code}>
                                {course.code} — {course.title}
                              </option>
                          ))}
                    </select>
                  </div>
                  <div className="selected-tags">
                    {requiredClasses.map((course, idx) => (
                        <span key={idx} className="tag">
                    {course.code}
                          <button
                              onClick={() => handleRemoveRequiredCourse(course.code)}
                              className="remove-tag"
                          >
                      ×
                    </button>
                  </span>
                    ))}
                  </div>
                </div>

                {/* Desired Number of Classes */}
                <div className="preferences-card">
                  <h2 className="section-title">Desired Number of Classes</h2>
                  <label htmlFor="desired-classes-input">
                    Input how many desired classes you would like to take this semester:
                  </label>
                  <input
                      id="desired-classes-input"
                      type="number"
                      min="1"
                      max="8"
                      value={NumberDesiredClasses}
                      onChange={(e) => setNumberDesiredClasses(parseInt(e.target.value))}
                      className="number-input"
                  />
                </div>

                {/* Elective Departments */}
                <div className="preferences-card">
                  <h2 className="section-title">Preferred Departments for Electives</h2>
                  <select
                      onChange={(e) => handleAddElectiveDepartment(e.target.value)}
                      value=""
                      className="dropdown"
                  >
                    <option value="" disabled hidden>Select department...</option>
                    {departmentList.map((dept) => (
                        <option key={dept} value={dept}>{dept}</option>
                    ))}
                  </select>
                  <div className="selected-tags">
                    {electiveDepartments.map((dept, idx) => (
                        <span key={idx} className="tag">
                    {dept}
                          <button
                              onClick={() => setElectiveDepartments(electiveDepartments.filter((_, i) => i !== idx))}
                              className="remove-tag"
                          >
                      ×
                    </button>
                  </span>
                    ))}
                  </div>
                </div>

                {/* WRIT Preference */}
                <div className="preferences-card">
                  <h2 className="section-title">WRIT Requirement</h2>
                  <label className="toggle-label">
                    <input
                        type="checkbox"
                        checked={needsWrit}
                        onChange={() => setNeedsWrit(!needsWrit)}
                    />
                    Require a WRIT course in my schedule
                  </label>
                </div>
              </>
          )}
        </div>
      </div>
  );
}

export default Home;
