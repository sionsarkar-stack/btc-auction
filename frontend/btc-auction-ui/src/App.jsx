import { useEffect, useState } from "react";

import { API_URL } from "./config";
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
import CaptainTribunal from "./pages/CaptainTribunal";
import MyJoker from "./pages/MyJoker";
import SilentBidManager from "./pages/SilentBidManager";
import SilentBid from "./pages/SilentBid";
import ReverseTarget from "./pages/ReverseTarget";

function App() {

  const [screen, setScreen] =
    useState("dashboard");

  const [role, setRole] =
    useState(
      localStorage.getItem("role")
    );

  const [config, setConfig] =
    useState(null);

  useEffect(() => {
    fetch(`${API_URL}/api/config`)
      .then(res => res.json())
      .then(data => setConfig(data));
  }, []);

  const showSpecialFeatures =
    config?.showSpecialFeatures ?? true;

  if (!role) {

    return (
      <Login
        onLogin={setRole}
      />
    );
  }

  return (

    <div className="app-shell">

      <div className="app-container">

        <div
          style={{
            marginBottom: "15px",
            fontWeight: "bold"
          }}
        >
          Logged in as:
          {" "}
          {localStorage.getItem("username")}
          {" "}
          (
          {role}
          )
        </div>

        <div className="button-group">

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

          {role === "ADMIN" && (

            <button
              className="button-secondary"
              onClick={() =>
                setScreen("auction")}
            >
              Sell Player
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



          {role !== "VIEWER" && showSpecialFeatures && (

            <button
              className={
                screen === "tribunal"
                  ? "button button-active"
                  : "button-secondary"
              }
              onClick={() =>
                setScreen("tribunal")}
            >
              Captain Tribunal
            </button>

          )}

          {role === "CAPTAIN" && (

            <button
              className="button-secondary"
              onClick={() =>
                setScreen("silent-bid")}
            >

              🔒 Silent Bid

            </button>

          )}

          {showSpecialFeatures && (
            <button
              className="button-secondary"
              onClick={() =>
                setScreen("joker")}
            >

              My Joker

            </button>
          )}
          {role !== "VIEWER" && (

            <button
              className="button-secondary"
              onClick={() =>
                setScreen("reverse-target")}
            >

              🎯 BlackList

            </button>

          )}

          {role === "ADMIN" && (

            <button
              className="button-secondary"
              onClick={() =>
                setScreen("silent-bid")}
            >
              🔒 Silent Bid
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

        </div>

        {screen === "dashboard" &&
          <Dashboard />}

        {screen === "nominate" &&
          role === "CAPTAIN" &&
          <NominatePlayer />}

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



        {screen === "tribunal" &&
          role !== "VIEWER" &&
          showSpecialFeatures &&
          <CaptainTribunal />}
        {screen === "joker" &&

          role !== "VIEWER" &&
          showSpecialFeatures &&

          <MyJoker />}

        {screen === "settings" &&
          role === "ADMIN" &&
          <Settings />}
        {screen === "import-players" &&
          role === "ADMIN" &&
          <PlayersImport />}

        {screen === "silent-bid" &&
          role === "ADMIN" &&
          <SilentBidManager />}

        {screen === "silent-bid" &&
          role === "CAPTAIN" &&
          <SilentBid />}
        {screen === "reverse-target" &&
          role !== "VIEWER" &&
          <ReverseTarget />}

      </div>

    </div>
  );
}

export default App;