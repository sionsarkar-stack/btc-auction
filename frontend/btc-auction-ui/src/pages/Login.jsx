import { useState } from "react";

import { API_URL } from "../config";
import heroImage from "../assets/hero.png";

function Login({ onLogin }) {

    const [username, setUsername] =
        useState("");

    const [password, setPassword] =
        useState("");

    const [error, setError] =
        useState("");

    const login = async () => {
        setError("");

        try {
            const response = await fetch(`${API_URL}/api/login`, {
                method: "POST",
                headers: { "Content-Type": "application/json" },
                credentials: "include",
                body: JSON.stringify({ username, password }),
            });

            if (response.status === 401) {
                setError("Invalid username or password");
                return;
            }

            if (!response.ok) {
                setError(`Auction server error (HTTP ${response.status}). Please try again.`);
                return;
            }

            const user = await response.json();
            if (!user || !user.role) {
                setError("Invalid username or password");
                return;
            }

            localStorage.setItem("role", user.role);
            localStorage.setItem("username", user.username);
            onLogin(user.role);
        } catch {
            setError("Auction server is unavailable. Start the backend and try again.");
        }
    };

    return (

        <div className="login-page">

            <section className="login-visual">
                <div className="login-visual-copy">
                    <p className="login-eyebrow">BELGHARIA TURF CRICKET</p>
                    <h1>BTC SEASON 13<br />AUCTION</h1>
                    <p className="login-tagline">Four teams. One champion.<br />The bidding starts here.</p>
                </div>
                <img src={heroImage} alt="BTC auction visual" className="login-visual-art" />
                <span className="login-season-mark">13</span>
            </section>

            <section className="login-panel">
                <div className="login-panel-topline">
                    <span className="login-live-dot" />
                    SEASON CONTROL ROOM
                </div>

                <div className="login-heading">
                    <span className="login-bat">🏏</span>
                    <div>
                        <p>Welcome back</p>
                        <h2>Sign in to enter</h2>
                    </div>
                </div>

                <form onSubmit={(event) => {
                    event.preventDefault();
                    login();
                }}>
                    <div className="form-field">

                        <label htmlFor="username">
                            Username
                        </label>

                        <input
                            id="username"
                            className="input"
                            value={username}
                            autoComplete="username"
                            onChange={(e) => setUsername(e.target.value)}
                        />

                    </div>

                    <div className="form-field">

                        <label htmlFor="password">
                            Password
                        </label>

                        <input
                            id="password"
                            type="password"
                            className="input"
                            value={password}
                            autoComplete="current-password"
                            onChange={(e) => setPassword(e.target.value)}
                        />

                    </div>

                    <button className="button login-submit" type="submit">
                        ENTER AUCTION ROOM <span>↗</span>
                    </button>
                </form>

                {error && (

                    <div className="login-error">
                        {error}
                    </div>

                )}

                <p className="login-footer">Authorized access · BTC Season 13</p>
            </section>

        </div>
    );
}

export default Login;