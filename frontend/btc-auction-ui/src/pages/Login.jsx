import { useState } from "react";

import { API_URL } from "../config";

function Login({ onLogin }) {

    const [username, setUsername] =
        useState("");

    const [password, setPassword] =
        useState("");

    const [error, setError] =
        useState("");

    const login = async () => {

        const response =
            await fetch(
                `${API_URL}/api/login`,
                {
                    method: "POST",
                    headers: {
                        "Content-Type":
                            "application/json"
                    },
                    body: JSON.stringify({
                        username,
                        password
                    })
                });

        if (!response.ok) {

            setError(
                "Invalid username or password"
            );

            return;

        }

        const user =
            await response.json();

        if (!user || !user.role) {

            setError(
                "Invalid username or password"
            );

            return;
        }

        localStorage.setItem(
            "role",
            user.role
        );

        localStorage.setItem(
            "username",
            user.username
        );

        onLogin(user.role);
    };

    return (

        <div className="form-card">

            <h1>
                BTC Auction Login
            </h1>

            <div className="form-field">

                <label>
                    Username
                </label>

                <input
                    className="input"
                    value={username}
                    onChange={(e) =>
                        setUsername(
                            e.target.value)}
                />

            </div>

            <div className="form-field">

                <label>
                    Password
                </label>

                <input
                    type="password"
                    className="input"
                    value={password}
                    onChange={(e) =>
                        setPassword(
                            e.target.value)}
                />

            </div>

            <button
                className="button"
                onClick={login}
            >
                Login
            </button>

            {error && (

                <div
                    className="message-success"
                    style={{
                        background:
                            "#fee2e2",
                        color:
                            "#991b1b",
                        marginTop: "10px"
                    }}
                >
                    {error}
                </div>

            )}

        </div>
    );
}

export default Login;