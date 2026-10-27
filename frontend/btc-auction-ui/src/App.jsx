import { useEffect, useState } from "react";
import Dashboard from "./pages/Dashboard";
import AuctionManager from "./pages/AuctionManager";
import AuctionScreen from "./pages/AuctionScreen";
import ManualSale from "./pages/ManualSale";
import AdminLogs from "./pages/AdminLogs";
import Login from "./pages/Login";
import Settings from "./pages/Settings";
import CaptainManagement from "./pages/CaptainManagement";
import PlayersImport from "./pages/PlayersImport";
import SilentBid from "./pages/SilentBid";
import MarketPreview from "./pages/MarketPreview";
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
  const marketPreview = import.meta.env.DEV
    && new URLSearchParams(window.location.search).get("marketPreview") === "1";

  const [sessionChecked, setSessionChecked] = useState(!role);

  useEffect(() => {
    if (!role || marketPreview) {
      return undefined;
    }

    let active = true;
    fetch(`${API_URL}/api/session`, { credentials: "include" })
      .then(response => {
        if (!active) {
          return;
        }
        if (!response.ok) {
          localStorage.removeItem("role");
          localStorage.removeItem("username");
          setRole(null);
        }
      })
      .catch(() => {
        if (active) {
          setRole(null);
        }
      })
      .finally(() => {
        if (active) {
          setSessionChecked(true);
        }
      });

    return () => {
      active = false;
    };
  }, [role, marketPreview]);

  useEffect(() => {
    if (marketPreview) {
      return undefined;
    }

    fetch(`${API_URL}/api/config`)
      .then(response => response.json())
      .then(config => setSeasonName(config.seasonName || ""))
      .catch(() => { });
  }, [marketPreview]);

  useEffect(() => {
    if (role !== "CAPTAIN" || marketPreview) {
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
  }, [role, screen, marketPreview]);

  useEffect(() => {
    const handleToast = (event) => {
      setToast(event.detail);
      window.setTimeout(() => setToast(null), 4000);
    };

    window.addEventListener("auction-toast", handleToast);
    return () => window.removeEventListener("auction-toast", handleToast);
  }, []);

  useEffect(() => {
    const handleSessionExpired = () => {
      localStorage.removeItem("role");
      localStorage.removeItem("username");
      setRole(null);
      setScreen("dashboard");
      setSessionChecked(true);
    };

    window.addEventListener("auction-session-expired", handleSessionExpired);
    return () => window.removeEventListener("auction-session-expired", handleSessionExpired);
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

  const navigateToMenuScreen = (nextScreen, event) => {
    setScreen(nextScreen);
    event.currentTarget.closest("details")?.removeAttribute("open");
  };

  if (marketPreview) {
    return <MarketPreview />;
  }

  if (!sessionChecked) {
    return <div className="app-container">Checking session...</div>;
  }

  if (!role) {

    return (
      <Login
        onLogin={(loggedInRole) => {
          setSessionChecked(false);
          setRole(loggedInRole);
        }}
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

          {role === "CAPTAIN" && (
            <button
              className={screen === "live" ? "button button-active" : "button-secondary"}
              onClick={() =>
                setScreen("live")}
            >
              Live Screen
            </button>
          )}

          {role === "ADMIN" && (
            <details name="admin-navigation" className={`app-nav-menu app-nav-settings-menu${["settings", "import-players", "captain-management"].includes(screen) ? " is-current" : ""}`}>
              <summary className="app-nav-menu-trigger">
                <span>Settings</span>
                <span className="app-nav-menu-chevron" aria-hidden="true">⌄</span>
              </summary>
              <div className="app-nav-menu-panel">
                <div className="app-nav-menu-group" role="group" aria-label="Auction setup">
                  <h2>Auction Setup</h2>
                  <button type="button" className={screen === "settings" ? "is-current" : ""}
                    aria-current={screen === "settings" ? "page" : undefined}
                    onClick={event => navigateToMenuScreen("settings", event)}>
                    Auction Settings
                  </button>
                  <button type="button" className={screen === "import-players" ? "is-current" : ""}
                    aria-current={screen === "import-players" ? "page" : undefined}
                    onClick={event => navigateToMenuScreen("import-players", event)}>
                    Import Players
                  </button>
                </div>
                <div className="app-nav-menu-group" role="group" aria-label="Teams">
                  <h2>Teams</h2>
                  <button type="button" className={screen === "captain-management" ? "is-current" : ""}
                    aria-current={screen === "captain-management" ? "page" : undefined}
                    onClick={event => navigateToMenuScreen("captain-management", event)}>
                    Captain Management
                  </button>
                </div>
              </div>
            </details>
          )}

          {role === "ADMIN" && (
            <details name="admin-navigation" className={`app-nav-menu app-nav-manage-menu${["manual-sale", "admin-logs"].includes(screen) ? " is-current" : ""}`}>
              <summary className="app-nav-menu-trigger">
                <span>Manage</span>
                <span className="app-nav-menu-chevron" aria-hidden="true">⌄</span>
              </summary>
              <div className="app-nav-menu-panel app-nav-manage-panel">
                <div className="app-nav-menu-group" role="group" aria-label="Auction management">
                  <h2>Auction</h2>
                  <button type="button" className={screen === "manual-sale" ? "is-current" : ""}
                    aria-current={screen === "manual-sale" ? "page" : undefined}
                    onClick={event => navigateToMenuScreen("manual-sale", event)}>
                    Manual Sale
                  </button>
                </div>
                <div className="app-nav-menu-group" role="group" aria-label="Administration">
                  <h2>Administration</h2>
                  <button type="button" className={screen === "admin-logs" ? "is-current" : ""}
                    aria-current={screen === "admin-logs" ? "page" : undefined}
                    onClick={event => navigateToMenuScreen("admin-logs", event)}>
                    Admin Logs
                  </button>
                </div>
              </div>
            </details>
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

          {screen === "live" && role !== "ADMIN" &&
            <AuctionScreen />}

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
