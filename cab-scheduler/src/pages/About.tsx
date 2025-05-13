function About() {
  return (
    <div className="about-page" style={{ backgroundColor: "#fff5f7" }}>
      {" "}
      {/* Light pink background */}
      <div className="about-container">
        <h1 className="about-title">About C@B Scheduler</h1>

        <div className="about-card">
          <h2 className="section-title">Why use C@B Scheduler?</h2>
          <ul className="feature-list">
            <li>✨ Automatically sync your preferences and courses</li>
            <li>🧠 Personalized filtering by time, modality, and more</li>
            <li>🎓 Built by and for Brown students</li>
          </ul>
        </div>

        <div className="about-card">
          <h2 className="section-title">Our Mission</h2>
          <p className="about-text">
            We're dedicated to helping Brown students create optimal class
            schedules that fit their academic goals and personal preferences.
          </p>
        </div>
      </div>
    </div>
  );
}

export default About;
