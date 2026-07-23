import { useContext } from "react";
import { NavLink, useNavigate } from "react-router-dom";

import { AuthContext } from "../contexts/authContext";

export default function ManagementLayout({
    title,
    description,
    eyebrow = "IATERAP",
    children,
}) {
    const navigate = useNavigate();
    const { token, logout } = useContext(AuthContext);

    const handleLogout = () => {
        logout();
        navigate("/login");
    };

    return (
        <div className="app-shell">
            <div className="app-shell__frame workspace">
                <header className="topbar">
                    <div className="brand">
                        <div className="brand__mark">IA</div>
                        <div className="brand__meta">
                            <span className="brand__name">
                                IATERAP Control Center
                            </span>
                            <span className="brand__tagline">
                                Project, aircraft, subsystem, and LRU management
                            </span>
                        </div>
                    </div>

                    <nav className="nav-pills">
                        <NavLink
                            to="/projects"
                            className={({ isActive }) =>
                                isActive ? "active" : ""
                            }
                        >
                            Projects
                        </NavLink>
                        <NavLink
                            to="/aircraft"
                            className={({ isActive }) =>
                                isActive ? "active" : ""
                            }
                        >
                            Aircraft
                        </NavLink>
                        <NavLink
                            to="/subsystems"
                            className={({ isActive }) =>
                                isActive ? "active" : ""
                            }
                        >
                            Subsystems
                        </NavLink>
                        <NavLink
                            to="/lrus"
                            className={({ isActive }) =>
                                isActive ? "active" : ""
                            }
                        >
                            LRUs
                        </NavLink>
                        <NavLink
                            to="/health"
                            className={({ isActive }) =>
                                isActive ? "active" : ""
                            }
                        >
                            Health
                        </NavLink>
                    </nav>
                </header>

                <section className="hero-card">
                    <div className="hero-card__topline">
                        <span className="eyebrow">{eyebrow}</span>
                        <div className="hero-actions">
                            <span className="status-banner status-banner--healthy">
                                Session {token ? "active" : "guest"}
                            </span>
                            {token ? (
                                <button
                                    className="button button--ghost"
                                    type="button"
                                    onClick={handleLogout}
                                >
                                    Logout
                                </button>
                            ) : (
                                <button
                                    className="button button--ghost"
                                    type="button"
                                    onClick={() => navigate("/login")}
                                >
                                    Login
                                </button>
                            )}
                        </div>
                    </div>

                    <h1>{title}</h1>
                    <p>{description}</p>
                </section>

                <main className="page-card">{children}</main>
            </div>
        </div>
    );
}
