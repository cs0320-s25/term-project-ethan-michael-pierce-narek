import { BrowserRouter as Router, Routes, Route, Link } from "react-router-dom";
import './main.css';
import Home from "./pages/Home";
import Profile from "./pages/Profile";
import Schedule from "./pages/Schedule";
import { SignedIn, SignedOut, SignInButton, UserButton } from "@clerk/clerk-react";

export default function App() {
    return (
        <header>
            {/* Always show SignInButton if not logged in */}
            <SignedOut>
                <div style={{ padding: "2rem" }}>
                    <h2>Welcome to CAB Scheduler</h2>
                    <SignInButton mode="modal" />
                </div>
            </SignedOut>

            <SignedIn>
                <UserButton />
                <Router>
                    <nav style={{ padding: "1rem", borderBottom: "1px solid gray" }}>
                        <Link to="/" style={{ marginRight: "1rem" }}>Home</Link>
                        <Link to="/profile" style={{ marginRight: "1rem" }}>Profile</Link>
                        <Link to="/schedule">Schedule</Link>
                    </nav>
                    <Routes>
                        <Route path="/" element={<Home />} />
                        <Route path="/profile" element={<Profile />} />
                        <Route path="/schedule" element={<Schedule />} />
                    </Routes>
                </Router>
            </SignedIn>
        </header>
    );
}
