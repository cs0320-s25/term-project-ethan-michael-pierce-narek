import { useUser, SignInButton } from "@clerk/clerk-react";
import React, { useEffect, useState } from "react";

function Home() {
  const { user } = useUser();
  const [excludedClasses, setExcludedClasses] = useState<string[]>([]);
  const [totalClasses, setTotalClasses] = useState<number>(3);
  const [mwfClasses, setMwfClasses] = useState<number>(2);
  const [requiredClasses, setRequiredClasses] = useState<string[]>([]);
  const [electiveDepartments, setElectiveDepartments] = useState<string[]>([]);
  const [needsWrit, setNeedsWrit] = useState<boolean>(false);

  useEffect(() => {
    if (user?.publicMetadata) {
      setExcludedClasses(user.publicMetadata.excludedClasses ?? []);
      setTotalClasses(user.publicMetadata.totalClasses ?? 3);
      setMwfClasses(user.publicMetadata.mwfClasses ?? 2);
      setRequiredClasses(user.publicMetadata.requiredClasses ?? []);
      setElectiveDepartments(user.publicMetadata.electiveDepartments ?? []);
      setNeedsWrit(user.publicMetadata.needsWrit ?? false);
    }
  }, [user]);

  const tthClasses = totalClasses - mwfClasses;

  const dayOptions = {
    Monday: [
      "MWF 8-8:50a",
      "MWF 9-9:50a",
      "MWF 10-10:50a",
      "MWF 11-11:50a",
      "MWF 12-12:50p",
      "MWF 1-1:50p",
      "MWF 2-2:50p",
      "M 3-5:30p",
    ],
    Tuesday: [
      "TTh 9-10:20a",
      "TTh 10:30-11:50a",
      "TTh 1-2:20p",
      "TTh 2:30-3:50p",
      "TTh 6:40-8p",
      "T 4-6:30p",
    ],
    Wednesday: [
      "MWF 8-8:50a",
      "MWF 9-9:50a",
      "MWF 10-10:50a",
      "MWF 11-11:50a",
      "MWF 12-12:50p",
      "MWF 1-1:50p",
      "MWF 2-2:50p",
      "W 3-5:30p",
    ],
    Thursday: [
      "TTh 9-10:20a",
      "TTh 10:30-11:50a",
      "TTh 1-2:20p",
      "TTh 2:30-3:50p",
      "TTh 6:40-8p",
      "Th 4-6:30p",
    ],
    Friday: [
      "MWF 8-8:50a",
      "MWF 9-9:50a",
      "MWF 10-10:50a",
      "MWF 11-11:50a",
      "MWF 12-12:50p",
      "MWF 1-1:50p",
      "MWF 2-2:50p",
      "F 3-5:30p",
    ],
  };

  const handleOptionSelect = (option: string) => {
    setExcludedClasses((prev) =>
      prev.includes(option)
        ? prev.filter((item) => item !== option)
        : [...prev, option],
    );
  };

  const handleAddRequiredClass = (e: React.KeyboardEvent<HTMLInputElement>) => {
    const value = e.currentTarget.value.trim();
    if (e.key === "Enter" && value !== "") {
      setRequiredClasses([...requiredClasses, value]);
      e.currentTarget.value = "";
    }
  };

  const handleAddElectiveDepartment = (dept: string) => {
    if (!electiveDepartments.includes(dept)) {
      setElectiveDepartments([...electiveDepartments, dept]);
    }
  };

  return (
    <div className="home-container">
      <div className="home-content">
        <h1 className="home-title">Welcome to C@B Smart Scheduler</h1>
        <p className="home-tagline">
          Plan smarter. Learn better. Graduate happier.
        </p>

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
                      <div key={`mwf-${i}`} className="mwf-block">
                        MWF
                      </div>
                    ))}
                    {[...Array(tthClasses)].map((_, i) => (
                      <div key={`tth-${i}`} className="tth-block">
                        TTh
                      </div>
                    ))}
                  </div>
                </div>
              </div>
            </div>

            {/* Exclude Class Times */}
            <div className="preferences-card">
              <h2 className="section-title">Exclude Class Times</h2>
              <div className="day-dropdowns-grid">
                {Object.keys(dayOptions).map((day) => (
                  <div key={day} className="day-preference">
                    <label className="day-label">{day}</label>
                    <select
                      value=""
                      onChange={(e) => handleOptionSelect(e.target.value)}
                      className="day-select"
                    >
                      <option value="" disabled hidden>
                        Select...
                      </option>
                      {dayOptions[day as keyof typeof dayOptions].map(
                        (option) => (
                          <option key={option} value={option}>
                            {option}
                          </option>
                        ),
                      )}
                    </select>
                  </div>
                ))}
              </div>
              <div className="selected-tags-container">
                <div className="selected-tags">
                  {excludedClasses.map((item, index) => (
                    <span key={index} className="tag">
                      {item}
                      <button
                        onClick={() => handleOptionSelect(item)}
                        className="remove-tag"
                      >
                        ×
                      </button>
                    </span>
                  ))}
                </div>
              </div>
            </div>

            {/* Required Classes */}
            <div className="preferences-card">
              <h2 className="section-title">Required Courses</h2>
              <input
                type="text"
                placeholder="Enter course codes (e.g., CSCI 0320)"
                onKeyDown={handleAddRequiredClass}
                className="text-input"
              />
              <div className="selected-tags">
                {requiredClasses.map((course, idx) => (
                  <span key={idx} className="tag">
                    {course}
                    <button
                      onClick={() =>
                        setRequiredClasses(
                          requiredClasses.filter((_, i) => i !== idx),
                        )
                      }
                      className="remove-tag"
                    >
                      ×
                    </button>
                  </span>
                ))}
              </div>
            </div>

            {/* Elective Departments */}
            <div className="preferences-card">
              <h2 className="section-title">
                Preferred Departments for Electives
              </h2>
              <select
                onChange={(e) => handleAddElectiveDepartment(e.target.value)}
                value=""
                className="dropdown"
              >
                <option value="" disabled hidden>
                  Select department...
                </option>
                {[
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
                ].map((dept) => (
                  <option key={dept} value={dept}>
                    {dept}
                  </option>
                ))}
              </select>
              <div className="selected-tags">
                {electiveDepartments.map((dept, idx) => (
                  <span key={idx} className="tag">
                    {dept}
                    <button
                      onClick={() =>
                        setElectiveDepartments(
                          electiveDepartments.filter((_, i) => i !== idx),
                        )
                      }
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
