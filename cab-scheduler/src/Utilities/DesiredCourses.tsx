import React, { useState } from "react";

interface Course {
  code: string;
  title: string;
}

interface DesiredCoursesProps {
  departmentList: string[];
  courses: Course[];
  onAddCourse: (course: Course) => void;
  onRemoveCourse: (code: string) => void;
  departmentCourses: Record<string, Course[]>;
  isLoadingDepartment: (dept: string) => boolean;
  onLoadDepartment: (dept: string) => void;
}

export const DesiredCourses: React.FC<DesiredCoursesProps> = ({
  departmentList,
  courses,
  onAddCourse,
  onRemoveCourse,
  departmentCourses,
  isLoadingDepartment,
  onLoadDepartment,
}) => {
  const [selectedDepartment, setSelectedDepartment] = useState("");
  const [selectedCourse, setSelectedCourse] = useState("");

  const handleDepartmentChange = (e: React.ChangeEvent<HTMLSelectElement>) => {
    const dept = e.target.value;
    setSelectedDepartment(dept);
    setSelectedCourse("");
    if (!departmentCourses[dept]) {
      onLoadDepartment(dept);
    }
  };

  const handleCourseSelect = (e: React.ChangeEvent<HTMLSelectElement>) => {
    const courseCode = e.target.value;
    if (courseCode && selectedDepartment) {
      const course = departmentCourses[selectedDepartment]?.find(
        (c) => c.code === courseCode
      );
      if (course && !courses.some((c) => c.code === course.code)) {
        onAddCourse(course);
      }
    }
    setSelectedCourse("");
  };

  return (
    <>
      <h3 className="section-title">Desired Courses</h3>
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
            disabled={isLoadingDepartment(selectedDepartment)}
          >
            <option value="">-- Select Course --</option>
            {departmentCourses[selectedDepartment]?.map((course) => (
              <option key={course.code} value={course.code}>
                {course.code}: {course.title}
              </option>
            ))}
          </select>
        </div>
      )}

      {courses.length > 0 ? (
        <ul style={{ listStyle: "none", padding: 0 }}>
          {courses.map((course) => (
            <li className="course-card" key={course.code}>
              <span>
                <strong>{course.code}</strong>: {course.title}
              </span>
              <button
                className="remove-btn"
                onClick={() => onRemoveCourse(course.code)}
              >
                Remove
              </button>
            </li>
          ))}
        </ul>
      ) : (
        <p>No courses selected yet</p>
      )}
    </>
  );
};
