import { useState } from "react";

function AddPlayer() {

    const [name, setName] = useState("");
    const [seed, setSeed] = useState("C");
    const [message, setMessage] = useState("");

    const addPlayer = async () => {

        const response = await fetch(
            "http://localhost:8080/api/players",
            {
                method: "POST",
                headers: {
                    "Content-Type": "application/json"
                },
                body: JSON.stringify({
                    name,
                    seed
                })
            }
        );

        const result = await response.text();

        setMessage(result);
        setName("");
        setSeed("C");
    };

    return (
        <div className="app-container">

            <div className="page-header">
                <h1 className="page-title">
                    Add Player
                </h1>
            </div>

            <div className="form-card">

                <div className="form-field">
                    <label>Player Name</label>

                    <input
                        className="input"
                        value={name}
                        onChange={(e) =>
                            setName(e.target.value)
                        }
                    />
                </div>

                <div className="form-field">
                    <label>Seed</label>

                    <select
                        className="select"
                        value={seed}
                        onChange={(e) =>
                            setSeed(e.target.value)
                        }
                    >
                        <option value="Z">Z</option>
                        <option value="A">A</option>
                        <option value="B">B</option>
                        <option value="C">C</option>
                    </select>
                </div>

                <button
                    className="button"
                    onClick={addPlayer}
                >
                    ADD PLAYER
                </button>

                {message && (
                    <div className="message-success">
                        {message}
                    </div>
                )}

            </div>
        </div>
    );
}

export default AddPlayer;