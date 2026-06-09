import { useState } from "react";

import Dashboard from "./pages/Dashboard";
import AuctionManager from "./pages/AuctionManager";
import NominatePlayer from "./pages/NominatePlayer";
import AuctionScreen from "./pages/AuctionScreen";
import AddPlayer from "./pages/AddPlayer";
import ManualSale from "./pages/ManualSale";
import AdminLogs from "./pages/AdminLogs";
import Login from "./pages/Login";

function App() {

  const [screen, setScreen] =
    useState("dashboard");

  const [role, setRole] =
    useState(
      localStorage.getItem("role")
    );

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
          {localStorage.getItem("username")}
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

          {role !== "VIEWER" && (

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

              window.location.reload();

            }}
          >
            Logout
          </button>

        </div>

        {screen === "dashboard" &&
          <Dashboard />}

        {screen === "nominate" &&
          role !== "VIEWER" &&
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

      </div>

    </div>
  );
}

export default App;