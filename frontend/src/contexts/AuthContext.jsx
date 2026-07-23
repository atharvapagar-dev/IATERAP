import { useState } from "react";

import { AuthContext } from "./authContext";

export function AuthProvider({ children }) {
    const [token, setToken] = useState(
        () => localStorage.getItem("token") || ""
    );

    const login = (accessToken) => {
        const resolvedToken =
            typeof accessToken === "string"
                ? accessToken
                : accessToken?.accessToken ||
                  accessToken?.token ||
                  "";

        localStorage.setItem("token", resolvedToken);
        setToken(resolvedToken);
    };

    const logout = () => {
        localStorage.removeItem("token");

        setToken(null);
    };

    return (
        <AuthContext.Provider
            value={{
                token,
                isAuthenticated: Boolean(token),
                login,
                logout,
            }}
        >
            {children}
        </AuthContext.Provider>
    );
}
