function TeamCard({ team }) {
  return (
    <div className="team-card">

      <h2>{team.captainName}</h2>

      <div>
        <span className="stat-badge">
          Purse ₹{team.purse}
        </span>

        <span className="stat-badge">
          Bought {team.playersBought}
        </span>

        <span className="stat-badge">
          Left {team.playersLeft}
        </span>
      </div>

      <p>
        <strong>Max Z Bid:</strong> ₹{team.maxZBid}
      </p>

      <p>
        <strong>Max A/B/C Bid:</strong> ₹{team.maxABCBid}
      </p>

      <div className="squad-title">
        Squad
      </div>

      {team.squad?.length > 0 ? (
        <ul>
          {team.squad.map((player, index) => (
            <li key={index}>{player}</li>
          ))}
        </ul>
      ) : (
        <p className="empty-squad">
          No players purchased yet
        </p>
      )}

    </div>
  );
}

export default TeamCard;