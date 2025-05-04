import { useState } from "react";
import "./schedule.css";

function Schedule() {
    const [concentration, setConcentration] = useState("");
    const [requirements, setRequirements] = useState("");
    const [numCourses, setNumCourses] = useState(4);
    const [generated, setGenerated] = useState(false);

    const handleSubmit = (e: React.FormEvent) => {
        e.preventDefault();
        setGenerated(true);
    };

    const mockSchedule = [
        { name: "CSCI0320", day: "Mon", time: "10:00AM" },
        { name: "MATH0100", day: "Tue", time: "11:00AM" },
        { name: "PHIL0200", day: "Wed", time: "1:00PM" },
        { name: "HIST0450", day: "Thu", time: "2:00PM" }
    ];

    return (
        <div className="schedule-page fade-in">
            <div className="scheduler-container">
                <h1>Mock Scheduler</h1>

                {!generated ? (
                    <form className="scheduler-form fade-in" onSubmit={handleSubmit}>
                        <div style={{ marginBottom: "1.5rem" }}>
                            <label htmlFor="concentration">Concentration:</label><br />
                            <input
                                type="text"
                                id="concentration"
                                value={concentration}
                                onChange={(e) => setConcentration(e.target.value)}
                                placeholder="e.g., Computer Science"
                            />
                        </div>

                        <div style={{ marginBottom: "1.5rem" }}>
                            <label htmlFor="requirements">Requirements (comma-separated):</label><br />
                            <input
                                type="text"
                                id="requirements"
                                value={requirements}
                                onChange={(e) => setRequirements(e.target.value)}
                                placeholder="e.g., CSCI0320, MATH0100"
                            />
                        </div>

                        <div style={{ marginBottom: "1.5rem" }}>
                            <label>Number of Courses:</label><br />
                            <label><input type="radio" value={3} checked={numCourses === 3} onChange={() => setNumCourses(3)} /> 3</label>{" "}
                            <label><input type="radio" value={4} checked={numCourses === 4} onChange={() => setNumCourses(4)} /> 4</label>{" "}
                            <label><input type="radio" value={5} checked={numCourses === 5} onChange={() => setNumCourses(5)} /> 5</label>
                        </div>

                        <button type="submit">Build Schedule</button>
                    </form>
                ) : (
                    <div style={{ marginTop: "2rem" }}>
                        <h2>Your Generated Schedule</h2>
                        <div style={{
                            display: "grid",
                            gridTemplateColumns: "repeat(5, 1fr)",
                            gap: "1rem",
                            marginTop: "2rem"
                        }}>
                            {["Mon", "Tue", "Wed", "Thu", "Fri"].map((day) => (
                                <div key={day} style={{
                                    backgroundColor: "#fff",
                                    padding: "1rem",
                                    border: "1px solid #eee",
                                    borderRadius: "8px",
                                    minHeight: "180px"
                                }}>
                                    <h3 style={{ fontSize: "1rem", marginBottom: "0.5rem" }}>{day}</h3>
                                    {mockSchedule.filter(c => c.day === day).map((course) => (
                                        <div key={course.name} style={{ marginBottom: "0.5rem", textAlign: "center" }}>
                                            <strong>{course.name}</strong><br />
                                            <small>{course.time}</small>
                                        </div>
                                    ))}
                                </div>
                            ))}
                        </div>
                    </div>
                )}
            </div>
        </div>
    );
}

export default Schedule;