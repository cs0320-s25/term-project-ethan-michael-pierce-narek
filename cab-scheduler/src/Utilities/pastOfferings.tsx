const courseCache: Record<string, Course[]> = {};

interface Course {
  code: string;
  title: string;
}

export async function getCourseOfferings(
  department: string,
): Promise<Course[]> {
  // Return cached results if available
  if (courseCache[department]) {
    return courseCache[department];
  }

  try {
    // Fetch both terms in parallel
    const [response1, response2] = await Promise.all([
      fetch(`http://localhost:3232/filter?term=202410&dept=${department}`),
      fetch(`http://localhost:3232/filter?term=202420&dept=${department}`),
    ]);

    // Check if either response failed
    if (!response1.ok || !response2.ok) {
      throw new Error(`HTTP errors: ${response1.status}/${response2.status}`);
    }

    // Process both responses
    const [data1, data2] = await Promise.all([
      response1.json(),
      response2.json(),
    ]);

    const uniqueCourses = new Map<string, Course>();

    // Combine results from both terms
    [...data1.results, ...data2.results].forEach((course) => {
      if (!uniqueCourses.has(course.code)) {
        uniqueCourses.set(course.code, {
          code: course.code,
          title: course.title,
        });
      }
    });

    // Cache and return
    const courses = Array.from(uniqueCourses.values());
    courseCache[department] = courses;
    return courses;
  } catch (error) {
    console.error(`Error fetching courses for ${department}:`, error);

    // Fallback: Try to return whatever data we can get
    try {
      const fallbackResponse = await fetch(
        `http://localhost:3232/filter?term=202420&dept=${department}`,
      );
      if (fallbackResponse.ok) {
        const data = await fallbackResponse.json();
        return data.results.map((course: any) => ({
          code: course.code,
          title: course.title,
        }));
      }
    } catch (e) {
      console.error("Fallback also failed:", e);
    }

    throw error;
  }
}
