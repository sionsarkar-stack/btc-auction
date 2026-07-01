import { useEffect, useState } from "react";

function MyJoker() {

    const username =
        localStorage.getItem("username");

    const role =
        localStorage.getItem("role");

    const [joker, setJoker] =
        useState(null);

    const [allJokers, setAllJokers] =
        useState([]);

    const descriptions = {

        STEAL_BID:
            "Instantly become the current highest bidder by matching the current highest bid.",

        BID_BLOCK:
            "Block one captain from bidding on the current player.",

        VETO:
            "Cancel the current nomination.",

        LAST_STRIKE:

            "After SOLD is called, increase the bid by ₹100 and reopen the auction."

    };

    const loadJoker = () => {

        if (role === "ADMIN") {

            fetch(
                "http://localhost:8080/api/jokers"
            )
                .then(response => response.json())
                .then(setAllJokers)
                .catch(console.error);

            return;
        }

        fetch(
            `http://localhost:8080/api/jokers/${username}`
        )
            .then(response => response.json())
            .then(data => setJoker(data[0]))
            .catch(console.error);

    };

    useEffect(() => {

        loadJoker();

    }, []);

    if (role === "ADMIN") {

        return (

            <div className="app-container">

                <div className="form-card">

                    <h1>🃏 Assigned Jokers</h1>

                    <table className="table">

                        <thead>
                            <tr>
                                <th>Captain</th>
                                <th>Joker</th>
                                <th>Status</th>
                            </tr>
                        </thead>

                        <tbody>

                            {allJokers.map(j => (

                                <tr key={j.id}>
                                    <td>{j.captainName}</td>
                                    <td>{j.jokerType.replaceAll("_", " ")}</td>
                                    <td>{j.used ? "❌ Used" : "✅ Ready"}</td>
                                </tr>

                            ))}

                        </tbody>

                    </table>

                </div>

            </div>

        );

    }

    if (!joker) {

        return <div>Loading...</div>;

    }

    const activateJoker = async () => {

        if (
            !window.confirm(
                `Activate ${joker.jokerType}?\n\nThis action cannot be undone.`
            )
        ) {

            return;

        }

        const response =
            await fetch(
                "http://localhost:8080/api/jokers/use",
                {
                    method: "POST",
                    headers: {
                        "Content-Type":
                            "application/json"
                    },
                    body: JSON.stringify({

                        captainName:
                            username,

                        jokerType:
                            joker.jokerType

                    })
                });

        alert(
            await response.text()
        );

        loadJoker();

    };

    return (

        <div className="app-container">

            <div
                className="form-card"
                style={{
                    maxWidth: "700px",
                    margin: "40px auto",
                    textAlign: "center"
                }}
            >

                <h1>

                    🃏 Your Secret Joker

                </h1>

                <h2
                    style={{
                        fontSize: "42px",
                        color: "#2563eb"
                    }}
                >

                    {joker.jokerType.replaceAll("_", " ")}

                </h2>

                <p
                    style={{
                        fontSize: "18px",
                        marginBottom: "25px"
                    }}
                >

                    {descriptions[joker.jokerType]}

                </p>

                <h3>

                    Status:
                    {" "}

                    {joker.used
                        ? "❌ Used"
                        : "✅ Ready"}

                </h3>

                {!joker.used && (

                    <button
                        className="button"
                        style={{
                            marginTop: "30px",
                            padding: "15px 40px",
                            fontSize: "20px"
                        }}
                        onClick={activateJoker}
                    >

                        🚀 Activate Joker

                    </button>

                )}

                {joker.used && (

                    <div
                        style={{
                            marginTop: "30px",
                            fontSize: "22px",
                            color: "green",
                            fontWeight: "bold"
                        }}
                    >

                        ✔ Joker Already Used

                    </div>

                )}

            </div>

        </div>

    );

}

export default MyJoker;