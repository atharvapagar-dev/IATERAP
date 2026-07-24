import { useContext } from "react";
import { Link, useNavigate } from "react-router-dom";

import { AuthContext } from "../contexts/authContext";

const linkStyle = {
    marginRight: "12px",
};

export default function ManagementLayout({
    title,
    description,
    children,
}) {
    const navigate = useNavigate();
    const { token, logout } = useContext(AuthContext);

    const handleLogout = () => {
        logout();
        navigate("/login");
    };

    return (
        <div className="management-shell">
            <header className="management-shell__header">
                <p className="eyebrow">
                    IATERAP management console
                </p>

                <h1>{title}</h1>
                <p className="management-shell__description">
                    {description}
                </p>

                <nav className="management-shell__nav">
                    <Link to="/projects" style={linkStyle}>
                        Projects
                    </Link>
                    <Link to="/aircraft" style={linkStyle}>
                        Aircraft
                    </Link>
                    <Link to="/subsystems" style={linkStyle}>
                        Subsystems
                    </Link>
                    <Link to="/lrus" style={linkStyle}>
                        LRUs
                    </Link>
                    <Link to="/signal-libraries" style={linkStyle}>
                        Signal Libraries
                    </Link>
                    <Link to="/protocol-tags" style={linkStyle}>
                        Protocol Tags
                    </Link>
                    <Link to="/health" style={linkStyle}>
                        Health
                    </Link>
                </nav>

                <div className="management-shell__session">
                    <span>
                        Session: {token ? "Active" : "Guest"}
                    </span>
                    {token ? (
                        <button onClick={handleLogout}>Logout</button>
                    ) : (
                        <Link to="/login">Login</Link>
                    )}
                </div>
            </header>

            <main className="management-shell__content">{children}</main>
        </div>
    );
}
