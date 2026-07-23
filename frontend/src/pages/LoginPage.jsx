import { useState, useContext } from "react";
import { useEffect } from "react";
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
    }, [navigate, token]);

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
        } catch (error) {
            setError("Login failed. Please check your credentials.");
            console.error("Login failed:", error);
        }
    };

    return (
        <div style={{ maxWidth: "360px", margin: "48px auto" }}>
            <h1>Login</h1>
            <p>Sign in to access projects, aircraft, and subsystem management.</p>

            <form onSubmit={handleSubmit}>
                <input
                    type="text"
                    placeholder="Username"
                    value={username}
                    onChange={(event) =>
                        setUsername(event.target.value)
                    }
                />

                <br />
                <br />

                <input
                    type="password"
                    placeholder="Password"
                    value={password}
                    onChange={(event) =>
                        setPassword(event.target.value)
                    }
                    />

                <br />
                <br />

                <button type="submit">
                    Login
                </button>
            </form>

            {error ? (
                <p style={{ color: "crimson" }}>{error}</p>
            ) : null}
        </div>
    );
}
