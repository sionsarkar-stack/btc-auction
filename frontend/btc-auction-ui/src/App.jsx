import { useEffect, useState } from "react";
import Dashboard from "./pages/Dashboard";
import AuctionManager from "./pages/AuctionManager";
import NominatePlayer from "./pages/NominatePlayer";
import AuctionScreen from "./pages/AuctionScreen";
import AddPlayer from "./pages/AddPlayer";
import ManualSale from "./pages/ManualSale";
import AdminLogs from "./pages/AdminLogs";
import Login from "./pages/Login";
import Settings from "./pages/Settings";
import PlayersImport from "./pages/PlayersImport";
import ReverseTarget from "./pages/ReverseTarget";
import SecretTargets from "./pages/SecretTargets";
import SilentBid from "./pages/SilentBid";
import SilentBidManager from "./pages/SilentBidManager";

function App() {

  const [screen, setScreen] =
    useState("dashboard");

  const [role, setRole] =
    useState(
      localStorage.getItem("role")
    );

  const [toast, setToast] = useState(null);

  useEffect(() => {
    const handleToast = (event) => {
      setToast(event.detail);
      window.setTimeout(() => setToast(null), 4000);
    };

    window.addEventListener("auction-toast", handleToast);
    return () => window.removeEventListener("auction-toast", handleToast);
  }, []);

  if (!role) {

    return (
      <Login
        onLogin={setRole}
      />
    );
  }

  return (

    <div className="app-shell">

      {toast && (
        <div className={`toast toast-${toast.type}`} role="status">
          <span>{toast.message}</span>
          <button type="button" onClick={() => setToast(null)} aria-label="Dismiss notification">
            ×
          </button>
        </div>
      )}

      <div className="app-container">

        <header className="auction-masthead">
          <div className="auction-brand">
            <span className="auction-brand-mark">🏏</span>
            <div>
              <p className="auction-kicker">Belgharia Turf Cricket</p>
              <h1>BTC SEASON 11 AUCTION</h1>
            </div>
          </div>

          <div className="auction-user">
            <span className="auction-live-dot" />
            <span>{localStorage.getItem("username")}</span>
            <strong>{role}</strong>
          </div>
        </header>

        <nav className="button-group app-nav" aria-label="Auction navigation">

          <button
            className="button"
            onClick={() =>
              setScreen("dashboard")}
          >
            Dashboard
          </button>

          {role === "CAPTAIN" && (

            <button
              className="button-secondary"
              onClick={() =>
                setScreen("nominate")}
            >
              Nominate
            </button>

          )}

          {role === "CAPTAIN" && (

            <button
              className={screen === "silent-bid" ? "button button-active" : "button-secondary"}
              onClick={() => setScreen("silent-bid")}
            >
              Silent Bid
            </button>

          )}

          {role === "ADMIN" && (

            <button
              className="button-secondary"
              onClick={() =>
                setScreen("auction")}
            >
              Sell Player
            </button>

          )}

          {role === "ADMIN" && (

            <button
              className={screen === "silent-manager" ? "button button-active" : "button-secondary"}
              onClick={() => setScreen("silent-manager")}
            >
              Silent Manager
            </button>

          )}

          <button
            className="button-secondary"
            onClick={() =>
              setScreen("live")}
          >
            Live Screen
          </button>

          {role === "ADMIN" && (

            <button
              className="button-secondary"
              onClick={() =>
                setScreen("add-player")}
            >
              Add Player
            </button>

          )}
          {role === "ADMIN" && (

            <button
              className="button-secondary"
              onClick={() =>
                setScreen("import-players")}
            >
              Import Players
            </button>

          )}

          {role === "ADMIN" && (

            <button
              className={
                screen === "manual-sale"
                  ? "button button-active"
                  : "button-secondary"
              }
              onClick={() =>
                setScreen("manual-sale")}
            >
              Manual Sale
            </button>

          )}

          {role === "ADMIN" && (

            <button
              className={
                screen === "settings"
                  ? "button button-active"
                  : "button-secondary"
              }
              onClick={() =>
                setScreen("settings")}
            >
              Settings
            </button>

          )}

          {role === "ADMIN" && (

            <button
              className={
                screen === "admin-logs"
                  ? "button button-active"
                  : "button-secondary"
              }
              onClick={() =>
                setScreen("admin-logs")}
            >
              Admin Logs
            </button>

          )}





          <button
            className="button-secondary"
            onClick={() => {

              localStorage.removeItem(
                "role"
              );

              localStorage.removeItem(
                "username"
              );

              window.location.reload();

            }}
          >
            Logout
          </button>

        </nav>

        <main className="app-content">
          {screen === "dashboard" &&
            <Dashboard />}

          {screen === "nominate" &&
            role === "CAPTAIN" &&
            <NominatePlayer />}

          {screen === "silent-bid" &&
            role === "CAPTAIN" &&
            <SilentBid />}

          {screen === "auction" &&
            role === "ADMIN" &&
            <AuctionManager />}

          {screen === "silent-manager" &&
            role === "ADMIN" &&
            <SilentBidManager />}

          {screen === "live" &&
            <AuctionScreen />}

          {screen === "add-player" &&
            role === "ADMIN" &&
            <AddPlayer />}

          {screen === "manual-sale" &&
            role === "ADMIN" &&
            <ManualSale />}

          {screen === "admin-logs" &&
            role === "ADMIN" &&
            <AdminLogs />}



          {screen === "settings" &&
            role === "ADMIN" &&
            <Settings />}
          {screen === "import-players" &&
            role === "ADMIN" &&
            <PlayersImport />}

          {screen === "reverse-target" &&
            role !== "VIEWER" &&
            <ReverseTarget />}
          {screen === "secret-targets" &&
            role === "CAPTAIN" &&
            <SecretTargets />}
        </main>

        <div className="dinda-watermark" aria-label="Designed by Dinda">
          <span className="dinda-watermark-line" />
          <span>DESIGNED BY</span>
          <strong>DINDA</strong>
        </div>

      </div>

    </div>
  );
}

export default App;
