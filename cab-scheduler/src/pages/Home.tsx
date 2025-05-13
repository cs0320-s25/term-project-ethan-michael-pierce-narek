import { useUser, SignInButton } from "@clerk/clerk-react";
import React, { useState } from "react";

function Home() {
  const { user } = useUser();
  const [excludedClasses, setExcludedClasses] = useState<string[]>([]);
  const [totalClasses, setTotalClasses] = useState<number>(3);
  const [mwfClasses, setMwfClasses] = useState<number>(2);

  const tthClasses = totalClasses - mwfClasses;

  // Options for each day's dropdown
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
    setExcludedClasses((prev) => {
      if (prev.includes(option)) {
        return prev.filter((item) => item !== option);
      }
      return [...prev, option];
    });
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
                      className={`number-option ${
                        totalClasses === num ? "selected" : ""
                      }`}
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

            {/* Existing Time Exclusion Preferences */}
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
                          <option
                            key={option}
                            value={option}
                            className={
                              excludedClasses.includes(option)
                                ? "selected-option"
                                : ""
                            }
                          >
                            {option}
                          </option>
                        )
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
          </>
        )}

      </div>
    </div>
  );
}

export default Home;
