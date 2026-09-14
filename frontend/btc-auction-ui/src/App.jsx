import { useEffect, useState } from "react";
import Dashboard from "./pages/Dashboard";
import AuctionManager from "./pages/AuctionManager";
import AuctionScreen from "./pages/AuctionScreen";
import AddPlayer from "./pages/AddPlayer";
import ManualSale from "./pages/ManualSale";
import AdminLogs from "./pages/AdminLogs";
import Login from "./pages/Login";
import Settings from "./pages/Settings";
import CaptainManagement from "./pages/CaptainManagement";
import PlayersImport from "./pages/PlayersImport";
import SilentBid from "./pages/SilentBid";
import { API_URL } from "./config";

function App() {

  const [screen, setScreen] =
    useState("dashboard");

  const [role, setRole] =
    useState(
      localStorage.getItem("role")
    );

  const [toast, setToast] = useState(null);

  const [seasonName, setSeasonName] = useState("");
  const [silentBidActive, setSilentBidActive] = useState(false);

  useEffect(() => {
    fetch(`${API_URL}/api/config`)
      .then(response => response.json())
      .then(config => setSeasonName(config.seasonName || ""))
      .catch(() => { });
  }, []);

  useEffect(() => {
    if (role !== "CAPTAIN") {
      return undefined;
    }

    const loadSilentBidStatus = async () => {
      try {
        const response = await fetch(`${API_URL}/api/silent-bid/active`);
        const active = await response.json();
        setSilentBidActive(active);
        if (!active && screen === "silent-bid") {
          setScreen("dashboard");
        }
      } catch {
        setSilentBidActive(false);
      }
    };

    loadSilentBidStatus();
    const interval = window.setInterval(loadSilentBidStatus, 2000);
    return () => window.clearInterval(interval);
  }, [role, screen]);

  useEffect(() => {
    const handleToast = (event) => {
      setToast(event.detail);
      window.setTimeout(() => setToast(null), 4000);
    };

    window.addEventListener("auction-toast", handleToast);
    return () => window.removeEventListener("auction-toast", handleToast);
  }, []);

  const logout = async () => {
    try {
      await fetch(`${API_URL}/api/logout`, {
        method: "POST",
        credentials: "include",
      });
    } finally {
      localStorage.removeItem("role");
      localStorage.removeItem("username");
      window.location.reload();
    }
  };

  if (!role) {

    return (
      <Login
        onLogin={setRole}
      />
    );
  }

  if (!seasonName) {
    return <div className="app-container">Loading...</div>;
  }

  return (

    <div className="app-shell">

      {toast && (
        <div className={`toast toast-${toast.type}`} role={toast.type === "error" ? "alert" : "status"}>
          <span className="toast-message">{toast.message}</span>
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
              <h1>{seasonName.toUpperCase()} AUCTION</h1>
            </div>
          </div>

          <div className="auction-user">
            <span className="auction-live-dot" />
            <span>{localStorage.getItem("username")}</span>
            <strong>{role}</strong>
          </div>
        </header>

        <nav className="button-group app-nav" aria-label="Auction navigation">

          {role === "ADMIN" && (
            <button
              className={screen === "auction" ? "button button-active" : "button-secondary"}
              onClick={() => setScreen("auction")}
            >
              BTC Control Room
            </button>
          )}

          <button
            className={screen === "dashboard" ? "button button-active" : "button-secondary"}
            onClick={() =>
              setScreen("dashboard")}
          >
            Dashboard
          </button>

          {role === "CAPTAIN" && silentBidActive && (
            <button
              className={screen === "silent-bid" ? "button button-active" : "button-secondary"}
              onClick={() => setScreen("silent-bid")}
            >
              Silent Bid
            </button>
          )}

          {role !== "VIEWER" && (
            <button
              className={screen === "live" ? "button button-active" : "button-secondary"}
              onClick={() =>
                setScreen("live")}
            >
              Live Screen
            </button>
          )}

          {role === "ADMIN" && (

            <button
              className={screen === "add-player" ? "button button-active" : "button-secondary"}
              onClick={() =>
                setScreen("add-player")}
            >
              Add Player
            </button>

          )}
          {role === "ADMIN" && (

            <button
              className={screen === "import-players" ? "button button-active" : "button-secondary"}
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
              className={screen === "captain-management" ? "button button-active" : "button-secondary"}
              onClick={() => setScreen("captain-management")}
            >
              Captain Management
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
            onClick={logout}
          >
            Logout
          </button>

        </nav>

        <main className="app-content">
          {screen === "dashboard" &&
            <Dashboard />}

          {screen === "silent-bid" &&
            role === "CAPTAIN" &&
            <SilentBid />}

          {screen === "auction" &&
            role === "ADMIN" &&
            <AuctionManager />}

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

          {screen === "captain-management" &&
            role === "ADMIN" &&
            <CaptainManagement />}

          {screen === "import-players" &&
            role === "ADMIN" &&
            <PlayersImport />}

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
