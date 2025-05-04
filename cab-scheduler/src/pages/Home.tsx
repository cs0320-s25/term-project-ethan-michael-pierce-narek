import { useUser, SignInButton } from "@clerk/clerk-react";

function Home() {
    const { user } = useUser();

    return (
        <div className="home-container">
            <div className="home-content">
                <h1 className="home-title">Welcome to C@B Smart Scheduler</h1>
                <p className="home-tagline">Plan smarter. Learn better. Graduate happier.</p>

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
            </div>
        </div>
    );
}

export default Home;