import { useEffect, useState } from "react";
import axios from "axios";

function AuctionScreen() {

    const [auction, setAuction] = useState(null);

    useEffect(() => {

        const loadAuction = () => {
            axios
                .get("http://localhost:8080/api/auction/current")
                .then((response) => {
                    setAuction(response.data);
                });
        };

        loadAuction();

        const interval = setInterval(
            loadAuction,
            3000
        );

        return () => clearInterval(interval);

    }, []);

    if (!auction) {
        return (
            <div className="app-container">
                Loading...
            </div>
        );
    }

    return (
        <div className="app-container">

            <div className="page-header">
                <h1 className="page-title">
                    🏏 LIVE AUCTION
                </h1>
            </div>

            <div className="current-auction-card">

                <div className="current-player-name">
                    {auction.currentPlayer || "No Player Nominated"}
                </div>

                <div className="current-player-seed">
                    {auction.seed || "-"}
                </div>

                <p>
                    Current Bid: ₹{auction.currentBid}
                </p>

                <p>
                    Leader: {auction.leader}
                </p>

            </div>

        </div>
    );
}

export default AuctionScreen;