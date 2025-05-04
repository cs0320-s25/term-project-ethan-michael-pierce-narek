import { useUser, SignInButton } from "@clerk/clerk-react";
import React, { useState, useEffect, useCallback } from "react";

function Home() {
    const { user } = useUser();
    const [preferredTime, setPreferredTime] = useState<string>("");
    const [modality, setModality] = useState<string>("");
    

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

          <div className="feature-section">
            <h2 className="section-title">Why use C@B Scheduler?</h2>
            <ul className="feature-list">
              <li>✨ Automatically sync your preferences and courses</li>
              <li>🧠 Personalized filtering by time, modality, and more</li>
              <li>🎓 Built by and for Brown students</li>
            </ul>
          </div>
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
        </div>
      </div>
    );
}

export default Home;