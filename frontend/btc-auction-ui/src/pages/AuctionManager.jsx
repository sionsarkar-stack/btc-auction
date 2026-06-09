import { useEffect, useState } from "react";

function AuctionManager() {

    const [teams, setTeams] = useState([]);
    const [players, setPlayers] = useState([]);

    const [playerName, setPlayerName] = useState("");
    const [captainName, setCaptainName] = useState("");
    const [soldPrice, setSoldPrice] = useState("");

    const [message, setMessage] = useState("");

    const loadData = async () => {

        try {

            const teamsResponse = await fetch(
                "http://localhost:8080/api/teams"
            );

            const playersResponse = await fetch(
                "http://localhost:8080/api/players/available"
            );

            const teamsData =
                await teamsResponse.json();

            const playersData =
                await playersResponse.json();

            setTeams(teamsData);
            setPlayers(playersData);

        } catch (error) {

            console.error(error);

            setMessage(
                "Unable to load data."
            );

        }
    };

    useEffect(() => {
        loadData();
    }, []);

    const sellPlayer = async () => {

        if (
            !playerName ||
            !captainName ||
            !soldPrice
        ) {

            setMessage(
                "Please fill all fields."
            );

            return;
        }

        try {

            const response = await fetch(
                "http://localhost:8080/api/auction/sold",
                {
                    method: "POST",
                    headers: {
                        "Content-Type":
                            "application/json",
                    },
                    body: JSON.stringify({
                        playerName,
                        captainName,
                        soldPrice:
                            Number(soldPrice),
                    }),
                }
            );

            const result =
                await response.text();

            setMessage(result);

            setPlayerName("");
            setCaptainName("");
            setSoldPrice("");

            await loadData();

        } catch (error) {

            console.error(error);

            setMessage(
                "Unable to sell player."
            );

        }
    };

    const undoSale = async () => {

        try {

            const response = await fetch(
                "http://localhost:8080/api/auction/undo",
                {
                    method: "POST",
                }
            );

            const result =
                await response.text();

            setMessage(result);

            await loadData();

        } catch (error) {

            console.error(error);

            setMessage(
                "Unable to undo sale."
            );

        }
    };

    return (
        <div className="app-container">

            <div className="page-header">
                <h1 className="page-title">
                    🏏 BTC AUCTION MANAGER
                </h1>
            </div>

            <div className="form-card">

                <h2>Sell Player</h2>

                <form
                    onSubmit={(e) => {
                        e.preventDefault();
                        sellPlayer();
                    }}
                >

                    <div className="form-field">

                        <label>
                            Player
                        </label>

                        <select
                            className="select"
                            value={playerName}
                            onChange={(e) =>
                                setPlayerName(
                                    e.target.value
                                )
                            }
                        >

                            <option value="">
                                Select Player
                            </option>

                            {players.map(
                                (player) => (

                                    <option
                                        key={player.name}
                                        value={
                                            player.name
                                        }
                                    >
                                        {player.name}
                                        {" "}
                                        (
                                        {player.seed}
                                        )
                                    </option>

                                )
                            )}

                        </select>

                    </div>

                    <div className="form-field">

                        <label>
                            Winning Captain
                        </label>

                        <select
                            className="select"
                            value={captainName}
                            onChange={(e) =>
                                setCaptainName(
                                    e.target.value
                                )
                            }
                        >

                            <option value="">
                                Select Captain
                            </option>

                            {teams.map(
                                (team) => (

                                    <option
                                        key={
                                            team.captainName
                                        }
                                        value={
                                            team.captainName
                                        }
                                    >
                                        {
                                            team.captainName
                                        }
                                    </option>

                                )
                            )}

                        </select>

                    </div>

                    <div className="form-field">

                        <label>
                            Sold Price
                        </label>

                        <input
                            className="input"
                            type="number"
                            value={soldPrice}
                            onChange={(e) =>
                                setSoldPrice(
                                    e.target.value
                                )
                            }
                        />

                    </div>

                    <div className="button-group">

                        <button
                            className="button"
                            type="submit"
                        >
                            SOLD
                        </button>

                        <button
                            className="button-secondary"
                            type="button"
                            onClick={undoSale}
                        >
                            UNDO LAST SALE
                        </button>

                    </div>

                </form>

                {message && (

                    <div
                        className={
                            message
                                .includes(
                                    "Unable"
                                )
                                ? "message-error"
                                : "message-success"
                        }
                    >
                        {message}
                    </div>

                )}

            </div>

        </div>
    );
}

export default AuctionManager;