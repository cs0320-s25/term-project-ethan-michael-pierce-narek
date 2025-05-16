import { BrowserRouter as Router, Routes, Route, Link } from "react-router-dom";
import "./main.css";
import Home from "./pages/Home";
import Profile from "./pages/Profile";
import Schedule from "./pages/Schedule";
import About from "./pages/About";
import {
  SignedIn,
  SignedOut,
  SignInButton,
  UserButton,
  useUser,
} from "@clerk/clerk-react";

export default function App() {
  return (
    <>
      <SignedOut>
        <div style={{ padding: "2rem" }}>
          <h2>Welcome to CAB Scheduler</h2>
          <SignInButton mode="modal" />
        </div>
      </SignedOut>

      <SignedIn>
        <Router>
          <nav className="main-nav">
            <div className="nav-links-container">
              <div className="nav-links">
                <Link to="/">Home</Link>
                <Link to="/profile">Profile</Link>
                <Link to="/schedule">Schedule</Link>
                <Link to="/about">About</Link>
              </div>
            </div>
            <UserDisplay />
          </nav>

          <Routes>
            <Route path="/" element={<Home />} />
            <Route path="/profile" element={<Profile />} />
            <Route path="/schedule" element={<Schedule />} />
            <Route path="/about" element={<About />} />
          </Routes>
        </Router>
      </SignedIn>
    </>
  );
}

function UserDisplay() {
  const { user } = useUser();

  if (!user) return null;

  return (
    <div className="user-display-pill">
      <UserButton
        appearance={{
          elements: {
            rootBox: {
              display: "flex",
              alignItems: "center",
              gap: "0.5rem",
              padding: "0.4rem 0.6rem",
              borderRadius: "20px",
              backgroundColor: "rgba(255, 50, 50, 0.1)",
              cursor: "pointer",
              transition: "background-color 0.3s ease",
            },
            avatarBox: {
              width: "28px",
              height: "28px",
              borderRadius: "50%",
            },
          },
        }}
        afterSignOutUrl="/"
      />
      <span className="user-name">{user.firstName}</span>
    </div>
  );
}
