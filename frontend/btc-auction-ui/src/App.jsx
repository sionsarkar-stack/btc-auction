import { useState } from "react";

import Dashboard from "./pages/Dashboard";
import AuctionManager from "./pages/AuctionManager";
import NominatePlayer from "./pages/NominatePlayer";
import AuctionScreen from "./pages/AuctionScreen";

function App() {

  const [screen, setScreen] = useState("dashboard");

  return (
    <div className="app-shell">

      <div className="app-container">

        <div className="button-group">

          <button
            className="button"
            onClick={() => setScreen("dashboard")}
          >
            Dashboard
          </button>

          <button
            className="button-secondary"
            onClick={() => setScreen("nominate")}
          >
            Nominate
          </button>

          <button
            className="button-secondary"
            onClick={() => setScreen("auction")}
          >
            Sell Player
          </button>

          <button
            className="button-secondary"
            onClick={() => setScreen("live")}
          >
            Live Screen
          </button>

        </div>

        {screen === "dashboard" && <Dashboard />}

        {screen === "nominate" && <NominatePlayer />}

        {screen === "auction" && <AuctionManager />}

        {screen === "live" && <AuctionScreen />}

      </div>

    </div>
  );
}

export default App;