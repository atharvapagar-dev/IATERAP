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
        <div style={{ padding: "24px" }}>
            <header style={{ marginBottom: "20px" }}>
                <p style={{ margin: 0, color: "#666" }}>
                    IATERAP management console
                </p>

                <h1 style={{ marginTop: "8px" }}>{title}</h1>
                <p style={{ marginTop: "8px", maxWidth: "720px" }}>
                    {description}
                </p>

                <nav style={{ marginTop: "16px" }}>
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
                    <Link to="/health" style={linkStyle}>
                        Health
                    </Link>
                </nav>

                <div style={{ marginTop: "16px" }}>
                    <span style={{ marginRight: "12px" }}>
                        Session: {token ? "Active" : "Guest"}
                    </span>
                    {token ? (
                        <button onClick={handleLogout}>Logout</button>
                    ) : (
                        <Link to="/login">Login</Link>
                    )}
                </div>
            </header>

            <main>{children}</main>
        </div>
    );
}
