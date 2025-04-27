import { BrowserRouter as Router, Routes, Route, Link } from "react-router-dom";
import './main.css';
import Home from "./pages/Home";
import Profile from "./pages/Profile";
import Preferences from "./pages/Preferences";
import Schedule from "./pages/Schedule";

function App() {
    return (
        <Router>
            <nav style={{ padding: "1rem", borderBottom: "1px solid gray" }}>
                <Link to="/" style={{ marginRight: "1rem" }}>Home</Link>
                <Link to="/profile" style={{ marginRight: "1rem" }}>Profile</Link>
                <Link to="/preferences" style={{ marginRight: "1rem" }}>Preferences</Link>
                <Link to="/schedule">Schedule</Link>
            </nav>
            <Routes>
                <Route path="/" element={<Home />} />
                <Route path="/profile" element={<Profile />} />
                <Route path="/preferences" element={<Preferences />} />
                <Route path="/schedule" element={<Schedule />} />
            </Routes>
        </Router>
    );
}

export default App;
