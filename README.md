# C@B Smart Scheduler

##  Project Description
The **C@B Smart Scheduler** is a course scheduling tool for Brown University students that helps them build class 
schedules tailored to their personal preferences. It features both frontend and backend components, allowing users to:
- Filter classes by time of day, days of the week, and modality.
- Indicate WRIT course requirements, required classes, and preferred departments.
- Save preferences persistently using Clerk metadata.
- View dynamically fetched course offerings via a backend API.

---

## Team Members and Contributions

**Ethan McDowell** (`ebmcdowe`)  
**Pierce Leverett** (`pleveret`)  
**Matthew Purcell** (`mpurcel2`)  
**Narek Babayan** (`nbabayan`)

We worked collaboratively on all features.
- **Ethan** led React development, UI/UX design, and Playwright testing.
- **Pierce** built filtering logic for multiple query parameters on the backend and helped clean up frontend state 
management, Clerk integration
- **Matthew** helped write and debug the server-side logic and improve backend modularity.
- **Narek** handled integration and ensured data persistence for stored data.

---

##  Major Funtionality in this Project

### 1. Course Filtering Backend
- Endpoint: `/filter`
- Supports `term`, `dept`, `day`, `time`, `writ`
- Filters real Brown C@B API data on request

### 2. Dynamic Frontend Integration
- Course options populate dynamically based on selected departments
- Uses `getCourseOfferings` from the backend to fetch offerings by department

### 3. Clerk Metadata Persistence
- Saves included times, desired courses, WRIT status, and more
- Metadata loads once and updates on debounce

### 4. UX Improvements
- Redesigned Home page with:
    - Hour-specific sliders (8am–9pm)
    - Card-based layout and drop shadows
    - Static preference visualization

---

##  Estimated Time

| Task | Time Spent                    |
|------|-------------------------------|
| Backend filtering logic | 18–20 hours                   |
| React components | 10–12 hours                   |
| Clerk metadata + auth | 6–8 hours                     |
| Styling + tutorial | 12 hours                      |
| Testing + debugging | 6–8 hours                     |
| **Total** | ~56 hours + unaccounted hours |

---

##  How to Build and Run

### Backend Setup
```bash
cd "CAB Server"
mvn clean package
```

then,
```bash
mvn exec:java -Dexec.mainClass="Server"
```

### Frontend Setup
```bash
cd cab-scheduler
npm install
npm run dev
```

---

##  Testing

### Backend
Use Postman to test `/filter` endpoint. Parameters include:
- `term=202420`
- `dept=CSCI`
- `day=Monday`
- `time=10-10:50a`
- `writ=true`

### Frontend
Run Playwright tests:
```bash
cd cab-scheduler
npx playwright test
```

---

##  Online Resources
- [Clerk.dev](https://clerk.dev/) – Authentication and metadata
- [SparkJava](http://sparkjava.com/) – Backend routing
- [React.js](https://reactjs.org/) – Component architecture
- [Brown C@B API](https://cab.brown.edu/) – Source of course data
- [Postman](https://www.postman.com/) – API testing

---

## ️ Design Choices

- **Separation of Concerns**: Clean distinction between filtering, authentication, and UI logic
- **Dynamic Fetching**: Courses load only when departments are selected
- **Metadata-Driven**: Preferences persist via Clerk metadata, auto-updated on interaction
- **Componentized React**: Preferences grouped into cards for modularity
- **Consistent UX**: Purple palette, rounded inputs, tag-based displays

---

##  Known Bugs

1. **Dropdown Lag**: Course dropdown may briefly appear empty before data loads
3. **Browser Cache**: In some cases, hard refresh is required to reflect updated metadata

---

##  Repository

https://github.com/cs0320-s25/term-project-ethan-michael-pierce-narek.git
---

##  Generative AI Use

**ChatGPT (2025)** – Used for:
- Suggesting defensive programming patterns
- Designing error response formats
- Brainstorming Clerk metadata structure
- Cleaning Javadoc and project organization

---

##  Style Compliance
- Variables: `camelCase`
- Classes: `UpperCamelCase`
- Javadoc: Present on all public methods and classes
- No checkstyle errors on build
