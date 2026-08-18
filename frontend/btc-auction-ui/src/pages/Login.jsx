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

        <div className="login-page">

            <section className="login-visual">
                <div className="login-visual-copy">
                    <p className="login-eyebrow">BELGHARIA TURF CRICKET</p>
                    <h1>BTC SEASON 11<br />AUCTION</h1>
                    <p className="login-tagline">Four teams. One champion.<br />The bidding starts here.</p>
                </div>
                <img src={heroImage} alt="BTC auction visual" className="login-visual-art" />
                <span className="login-season-mark">11</span>
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

                        <label>
                            Username
                        </label>

                        <input
                            className="input"
                            value={username}
                            autoComplete="username"
                            onChange={(e) => setUsername(e.target.value)}
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

                <p className="login-footer">Authorized access · BTC Season 11</p>
            </section>

        </div>
    );
}

export default Login;