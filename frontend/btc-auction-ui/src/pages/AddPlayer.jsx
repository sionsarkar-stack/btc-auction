import { useState } from "react";
import { API_URL } from "../config";

function AddPlayer() {

    const [name, setName] = useState("");
    const [seed, setSeed] = useState("");
    const [basePrice, setBasePrice] = useState("");
    const [category, setCategory] = useState("");
    const [message, setMessage] = useState("");

    const addPlayer = async () => {

        const response = await fetch(
            `${API_URL}/api/players`,
            {
                method: "POST",
                headers: {
                    "Content-Type": "application/json"
                },
                body: JSON.stringify({
                    name,
                    seed,
                    basePrice: Number(basePrice)
                    , category
                })
            }
        );

        const result = await response.text();

        setMessage(result);
        setName("");
        setSeed("");
        setBasePrice("");
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

                    <input
                        className="select"
                        value={seed}
                        onChange={(e) =>
                            setSeed(e.target.value)
                        }
                        placeholder="e.g. Seed A"
                    />
                </div>

                <div className="form-field">
                    <label>Category</label>

                    <input
                        className="input"
                        value={category}
                        onChange={(e) => setCategory(e.target.value)}
                        placeholder="e.g. Batter, Bowler, All-rounder"
                    />
                </div>

                <div className="form-field">
                    <label>Base Price (optional override)</label>

                    <input
                        className="input"
                        type="number"
                        min="1"
                        value={basePrice}
                        onChange={(e) => setBasePrice(e.target.value)}
                        placeholder="Uses the seed default if blank"
                    />
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
