import { useEffect, useState } from "react";

import TeamCard from "../components/TeamCard";
import LiveActivity from "../components/LiveActivity";

function Dashboard() {

    const [dashboard, setDashboard] =
        useState(null);

    useEffect(() => {

        loadDashboard();

        const interval =
            setInterval(loadDashboard, 5000);

        return () =>
            clearInterval(interval);

    }, []);

    const loadDashboard = () => {

        fetch("http://localhost:8080/api/dashboard")
            .then(response => response.json())
            .then(data => setDashboard(data))
            .catch(error =>
                console.error(error));
    };

    if (!dashboard) {

        return (
            <div>
                Loading...
            </div>
        );
    }

    return (
        <div>

            <div className="section-card">

                <h2>
                    Current Auction
                </h2>

                <p>
                    <strong>Player:</strong>{" "}
                    {dashboard.currentAuction.currentPlayer}
                </p>

                <p>
                    <strong>Seed:</strong>{" "}
                    {dashboard.currentAuction.seed}
                </p>

                <p>
                    <strong>Current Bid:</strong>{" "}
                    ₹{dashboard.currentAuction.currentBid}
                </p>

                <p>
                    <strong>Leading Captain:</strong>{" "}
                    {dashboard.currentAuction.leader}
                </p>

            </div>

            <div className="team-grid">

                {dashboard.teams.map(team => (

                    <TeamCard
                        key={team.captainName}
                        team={team}
                    />

                ))}

            </div>

            <LiveActivity />

        </div>
    );
}

export default Dashboard;