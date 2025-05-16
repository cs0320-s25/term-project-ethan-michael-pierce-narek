import { useState, useEffect } from "react";
import { useUser } from "@clerk/clerk-react";
import "./schedule.css";

interface Course {
  code: string;
  title: string;
  meets: string;
  writ: boolean;
}

interface Schedule {
  score: number;
  courses: Course[];
}

function Schedule() {
  const { user } = useUser();
  const [isLoading, setIsLoading] = useState(false);
  const [schedules, setSchedules] = useState<Schedule[]>([]);
  const [error, setError] = useState<string | null>(null);

  const [mwfClasses, setMwfClasses] = useState<number>(2);
  const [totalClasses, setTotalClasses] = useState<number>(4);
  const [excludedClasses, setExcludedClasses] = useState<string[]>([]);
  const [requiredClasses, setRequiredClasses] = useState<string[]>([]);
  const [electiveDepartments, setElectiveDepartments] = useState<string[]>([]);
  const [needsWrit, setNeedsWrit] = useState<boolean>(false);
  const [NumberDesiredCourses, setNumberDesiredCourses] = useState<number>(3);
  const [hasLoadedMetadata, setHasLoadedMetadata] = useState(false);

  useEffect(() => {
    if (user && !hasLoadedMetadata) {
      const loadMetadata = async () => {
        try {
          const metadata = user.unsafeMetadata as {
            excludedClasses?: string[];
            totalClasses?: number;
            mwfClasses?: number;
            requiredClasses?: string[];
            electiveDepartments?: string[];
            needsWrit?: boolean;
            NumberDesiredClasses?: number;
          };

          setExcludedClasses(metadata.excludedClasses || []);
          setTotalClasses(metadata.totalClasses || 4);
          setMwfClasses(metadata.mwfClasses || 2);
          setRequiredClasses(metadata.requiredClasses || []);
          setElectiveDepartments(metadata.electiveDepartments || []);
          setNeedsWrit(metadata.needsWrit || false);
          setNumberDesiredCourses(metadata.NumberDesiredClasses || 3);
          setHasLoadedMetadata(true);
        } catch (error) {
          console.error("Error loading metadata:", error);
          setError("Failed to load your preferences");
        }
      };

      loadMetadata();
    }
  }, [user, hasLoadedMetadata]);

const generateSchedules = async () => {
  if (!user || !hasLoadedMetadata) return;

  setIsLoading(true);
  setError(null);

  try {
    const excludedTimes = excludedClasses.filter((t) => t).join(",");
    const days = "M,T,W,Th,F";
    const depts = electiveDepartments
      .map((d) => d.trim())
      .filter((d) => d)
      .join(",");
    const url =
      `http://localhost:3232/generate?term=202420&classes=${totalClasses}` +
      `&user=${user.id}` +
      `&needed=${requiredClasses.join(",")}` +
      `&times=${excludedTimes}` +
      `&days=${days}` +
      `&mwf=${mwfClasses}` +
      `&tth=${totalClasses - mwfClasses}` +
      `&reqThisSem=${NumberDesiredCourses}` +
      `&depts=${depts}` +
      `&writ=${needsWrit}`;

    console.log("💬 Fetching schedules from:", url);

    const response = await fetch(url);
    const data = await response.json(); 

    if (data.success === false) {
      const msg = Array.isArray(data.errors)
        ? data.errors.join("; ")
        : "Server returned an error";
      setError(msg);
      setSchedules([]); 
      setIsLoading(false);
      return;
    }

    setSchedules(
      Array.isArray(data.schedules) ? data.schedules.slice(0, 3) : []
    );
  } catch (err) {
    setError(
      err instanceof Error ? err.message : "Failed to generate schedules"
    );
  } finally {
    setIsLoading(false);
  }
};

  const refreshSchedules = () => {
    setSchedules([]);
    setHasLoadedMetadata(false);
  };

  return (
    <div className="schedule-container">
      <h1 className="schedule-title">Generate Your Schedule</h1>

      <div className="schedule-controls">
        <button
          onClick={generateSchedules}
          disabled={isLoading}
          className="generate-btn"
        >
          {isLoading ? "Generating..." : "Generate Schedules"}
        </button>

        {schedules.length > 0 && (
          <button onClick={refreshSchedules} className="refresh-btn">
            Refresh
          </button>
        )}
      </div>

      {error && <p className="error-message">{error}</p>}

      {schedules.length > 0 && (
        <div className="schedules-grid">
          <h2>Top Schedule Options</h2>
          {schedules.map((schedule, index) => (
            <div key={index} className="schedule-card">
              <h3>Schedule Option #{index + 1}</h3>
              <div className="schedule-details">
                <table>
                  <thead>
                    <tr>
                      <th>Course</th>
                      <th>Title</th>
                      <th>Time</th>
                      <th>Type</th>
                    </tr>
                  </thead>
                  <tbody>
                    {schedule.courses.map((course, idx) => (
                      <tr key={idx}>
                        <td className="course-code">{course.code}</td>
                        <td className="course-title">{course.title}</td>
                        <td className="course-time">{course.meets}</td>
                        <td className="course-type">
                          {course.writ ? "WRIT" : "Regular"}
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  );
}

export default Schedule;
