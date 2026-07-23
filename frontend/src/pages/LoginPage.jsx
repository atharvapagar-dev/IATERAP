import { useContext, useEffect, useState } from "react";
import { useLocation, useNavigate } from "react-router-dom";

import { AuthContext } from "../contexts/authContext";
import { authService } from "../services/authService";

export default function LoginPage() {
    const navigate = useNavigate();
    const location = useLocation();

    const { login, token } = useContext(AuthContext);

    const [username, setUsername] = useState("");
    const [password, setPassword] = useState("");
    const [error, setError] = useState("");

    useEffect(() => {
        if (token) {
            navigate("/projects", { replace: true });
        }
    }, [location, navigate, token]);

    const handleSubmit = async (event) => {
        event.preventDefault();
        setError("");

        try {
            const response = await authService.login({
                username,
                password,
            });

            login(response.data);

            const from = location.state?.from?.pathname || "/projects";
            navigate(from, { replace: true });
        } catch (requestError) {
            setError("Login failed. Please check your credentials.");
            console.error("Login failed:", requestError);
        }
    };

    return (
        <div className="login-shell">
            <div className="login-card">
                <section className="login-card__panel login-card__panel--brand">
                    <span className="login-badge">Secure access</span>
                    <h1>IATERAP</h1>
                    <p>
                        Sign in to manage the engineering hierarchy, keep project
                        data organized, and move quickly through the aircraft
                        workflow.
                    </p>

                    <div className="login-metrics">
                        <div className="login-metrics__item">
                            <span className="login-metrics__dot" />
                            Project, aircraft, and subsystem navigation
                        </div>
                        <div className="login-metrics__item">
                            <span className="login-metrics__dot" />
                            Role-aware workflow foundation
                        </div>
                        <div className="login-metrics__item">
                            <span className="login-metrics__dot" />
                            Shared QA collection for auth and projects
                        </div>
                    </div>
                </section>

                <section className="login-card__panel login-card__panel--form">
                    <div>
                        <h2>Welcome back</h2>
                        <p>
                            Use your team credentials to continue into the platform.
                        </p>
                    </div>

                    <form className="auth-form" onSubmit={handleSubmit}>
                        <div className="form-row">
                            <label htmlFor="username">Username</label>
                            <input
                                id="username"
                                type="text"
                                placeholder="admin"
                                value={username}
                                onChange={(event) =>
                                    setUsername(event.target.value)
                                }
                            />
                        </div>

                        <div className="form-row">
                            <label htmlFor="password">Password</label>
                            <input
                                id="password"
                                type="password"
                                placeholder="Password"
                                value={password}
                                onChange={(event) =>
                                    setPassword(event.target.value)
                                }
                            />
                        </div>

                        <div className="auth-form__footer">
                            <span className="subtle-note">
                                Seeded demo users are ready for testing.
                            </span>
                            <button type="submit">Login</button>
                        </div>
                    </form>

                    {error ? (
                        <div className="status-banner status-banner--error">
                            {error}
                        </div>
                    ) : null}
                </section>
            </div>
        </div>
    );
}
