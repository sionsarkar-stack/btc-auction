import { useState } from "react";
import PlayerWalkout from "../components/PlayerWalkout";

const previewPlayers = [
    { name: "Aarav Mukherjee", seed: "A", basePrice: 450 },
    { name: "Ritwik Das", seed: "B", basePrice: 350 },
    { name: "Sayan Ghosh", seed: "C", basePrice: 300 },
    { name: "Arjun Nair", seed: "A", basePrice: 500 },
    { name: "Dev Banerjee", seed: "B", basePrice: 250 },
    { name: "Kabir Sen", seed: "C", basePrice: 200 },
    { name: "Ishaan Roy", seed: "A", basePrice: 400 },
    { name: "Neel Chatterjee", seed: "B", basePrice: 275 },
];

export default function MarketPreview() {
    const [selectedPlayer, setSelectedPlayer] = useState("");
    const [spinStartedAt, setSpinStartedAt] = useState(null);
    const [spinEndsAt, setSpinEndsAt] = useState(null);
    const [isSpinning, setIsSpinning] = useState(false);

    const spinWheel = () => {
        const now = Date.now();
        const nextPlayer = previewPlayers[Math.floor(Math.random() * previewPlayers.length)];
        setSelectedPlayer(nextPlayer.name);
        setSpinStartedAt(now);
        setSpinEndsAt(now + 2600);
        setIsSpinning(true);
    };

    return (
        <main className="market-preview-page">
            <header className="market-preview-heading">
                <div>
                    <span className="walkout-preview-kicker">LOCAL PREVIEW · SAMPLE PLAYERS</span>
                    <h1>BTC Bottle Draw</h1>
                    <p>Glass bottle player reveal · No auction data is changed</p>
                </div>
                <button className="button" type="button" onClick={spinWheel} disabled={isSpinning}>
                    {isSpinning ? "Drawing a player..." : "Call Next Player"}
                </button>
            </header>

            <PlayerWalkout
                players={previewPlayers}
                selectedPlayer={selectedPlayer}
                spinStartedAt={spinStartedAt}
                spinEndsAt={spinEndsAt}
                onSpinComplete={() => setIsSpinning(false)}
            />

            <p className="market-preview-note">Sample roster is for preview only. Live auctions use the actual available-player list.</p>
        </main>
    );
}