import { useEffect, useState } from "react";
import TeamCard from "../components/TeamCard";

function Dashboard() {
    const [dashboardData, setDashboardData] = useState(null);
    const [logs, setLogs] = useState([]);
    const [error, setError] = useState(null);
    const [loading, setLoading] = useState(true);

    useEffect(() => {
        const loadDashboard = () => {
            fetch("http://localhost:8080/api/dashboard")
                .then((response) => {
                    if (!response.ok) {
                        throw new Error("Failed to fetch dashboard");
                    }
                    return response.json();
                })
                .then((data) => {
                    setDashboardData(data);
                    setError(null);
                })
                .catch((err) => {
                    console.error(err);
                    setError("Unable to load dashboard data.");
                });

            fetch("http://localhost:8080/api/logs")
                .then((response) => response.json())
                .then((data) => {
                    setLogs(data);
                })
                .catch((err) => {
                    console.error(err);
                })
                .finally(() => {
                    setLoading(false);
                });
        };

        loadDashboard();

        const interval = setInterval(loadDashboard, 3000);

        return () => clearInterval(interval);
    }, []);

    if (loading) {
        return (
            <div className="app-container">
                Loading dashboard...
            </div>
        );
    }

    if (error) {
        return (
            <div className="app-container">
                <div className="message-error">
                    {error}
                </div>
            </div>
        );
    }

    return (
        <div className="app-container">

            <div className="page-header">
                <h1 className="page-title">
                    🏏 BTC SEASON 8 DASHBOARD
                </h1>
            </div>

            {/* Current Auction */}

            <div className="current-auction-card">

                <h2>Current Auction</h2>

                <div className="current-player-name">
                    {dashboardData?.currentAuction?.currentPlayer ||
                        "No Player Nominated"}
                </div>

                <div className="current-player-seed">
                    {dashboardData?.currentAuction?.seed || "-"}
                </div>

                <p>
                    Current Bid: ₹
                    {dashboardData?.currentAuction?.currentBid || 0}
                </p>

                <p>
                    Leader:{" "}
                    {dashboardData?.currentAuction?.leader || "None"}
                </p>

            </div>

            <hr className="divider" />

            {/* Teams */}

            <div>

                <h2>Teams</h2>

                <div className="team-grid">

                    {dashboardData?.teams?.map((team) => (
                        <TeamCard
                            key={team.captainName}
                            team={team}
                        />
                    ))}

                </div>

            </div>

            <hr className="divider" />

            {/* Recent Sales */}

            <div className="notification-card">

                <h2>
                    Recent Sales ({logs.length})
                </h2>

                {logs.length === 0 ? (

                    <p>No sales yet</p>

                ) : (

                    <div className="sales-list">

                        {logs
                            .slice()
                            .reverse()
                            .slice(0, 10)
                            .map((log, index) => (

                                <div
                                    key={index}
                                    className="sale-item"
                                >

                                    <strong>
                                        {log.playerName}
                                    </strong>

                                    <span>
                                        {log.captainName}
                                    </span>

                                    <span>
                                        ₹{log.soldPrice}
                                    </span>

                                </div>

                            ))}

                    </div>

                )}

            </div>

        </div>
    );
}

export default Dashboard;