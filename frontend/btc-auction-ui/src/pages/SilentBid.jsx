import { useEffect, useState } from "react";

function SilentBid() {

    const username =
        localStorage.getItem("username");

    const [bid, setBid] =
        useState("");

    const [round, setRound] =
        useState(null);

    const [submitted, setSubmitted] =
        useState(false);

    const [tieBreak, setTieBreak] =
        useState(false);

    useEffect(() => {

        loadRound();

        const interval =
            setInterval(loadRound, 2000);

        return () =>
            clearInterval(interval);

    }, []);

    const loadRound = async () => {

        const response =
            await fetch(

                `${API_URL}/api/silent-bid/${username}`

            );

        const data =
            await response.json();

        if (data) {

            setRound(data);

            setSubmitted(
                data.submitted
            );

            setTieBreak(data.tieBreakRound);
            if (data.tieBreakRound && !data.submitted) {

                setBid("");

            }
        }

    };

    const submitBid = async () => {

        if (!bid) {

            alert("Enter bid amount.");

            return;

        }

        const response =
            await fetch(
                `${API_URL}/api/silent-bid/submit`,
                {

                    method: "POST",

                    headers: {

                        "Content-Type":
                            "application/json"

                    },

                    body: JSON.stringify({

                        playerName:
                            round.playerName,

                        captainName:
                            username,

                        bidAmount:
                            Number(bid)

                    })

                });

        const result = await response.text();

        alert(result);

        setSubmitted(true);

        setBid("");

        loadRound();

    };

    if (!round) {

        return (

            <div className="app-container">

                <div className="form-card">

                    No Silent Bid Round Active

                </div>

            </div>

        );

    }

    return (

        <div className="app-container">

            <div className="form-card">

                <h1>

                    🔒 Silent Bid

                </h1>

                <h2>

                    {round.playerName}

                </h2>

                {!submitted && round.eligibleForTieBreak ? (

                    <>
                        {tieBreak && (

                            <div
                                className="message-error"
                                style={{
                                    marginBottom: "20px"
                                }}
                            >

                                ⚠ Tie Break Round

                                <br /><br />

                                Multiple captains submitted the same highest bid.

                                <br />

                                Please enter a new secret bid.

                            </div>

                        )}

                        <input
                            className="input"
                            type="number"
                            placeholder="Enter Bid"
                            value={bid}
                            onChange={(e) =>
                                setBid(
                                    e.target.value
                                )}
                        />

                        <button
                            className="button"
                            style={{
                                marginTop: "20px"
                            }}
                            onClick={submitBid}
                        >

                            Submit Bid

                        </button>

                    </>

                ) : (

                    <div
                        className="message-success"
                    >

                        ✅ Bid Submitted

                        <br /><br />

                        Waiting for other captains...

                    </div>

                )}

                {tieBreak &&
                    !round.eligibleForTieBreak && (

                        <div
                            className="message-error"
                            style={{
                                marginTop: "20px"
                            }}
                        >

                            ⚠ Tie Break Round

                            <br /><br />

                            Your bid was not tied for the highest amount.

                            <br />

                            Waiting for the final winner...

                        </div>

                    )}

            </div>

        </div>

    );

}

export default SilentBid;