package com.btc.btc_auction.service;

import com.btc.btc_auction.entity.TeamEntity;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class CaptainPresenceService {

    public static final long HEARTBEAT_GRACE_MILLIS = 15_000L;

    private record SessionPresence(String captainName, long lastSeenAt) {
    }

    private final Map<String, SessionPresence> sessions = new ConcurrentHashMap<>();
    private AuctionSocketService auctionSocketService;

    @org.springframework.beans.factory.annotation.Autowired
    public void setAuctionSocketService(AuctionSocketService auctionSocketService) {
        this.auctionSocketService = auctionSocketService;
    }

    public synchronized void checkIn(String sessionId, String username, String role) {
        if (sessionId == null || username == null || !"CAPTAIN".equalsIgnoreCase(role)) {
            return;
        }
        boolean wasOnline = isOnline(username, System.currentTimeMillis());
        sessions.put(sessionId, new SessionPresence(username, System.currentTimeMillis()));
        if (!wasOnline) {
            broadcastPresenceChange();
        }
    }

    public synchronized void signOut(String sessionId) {
        if (sessionId != null) {
            SessionPresence signedOut = sessions.remove(sessionId);
            if (signedOut != null && !isOnline(signedOut.captainName(), System.currentTimeMillis())) {
                broadcastPresenceChange();
            }
        }
    }

    public List<String> missingCaptains(List<TeamEntity> captains) {
        long cutoff = System.currentTimeMillis() - HEARTBEAT_GRACE_MILLIS;
        List<String> onlineCaptains = sessions.values().stream()
                .filter(presence -> presence.lastSeenAt() >= cutoff)
                .map(SessionPresence::captainName)
                .toList();

        return captains.stream()
                .map(TeamEntity::getCaptainName)
                .filter(captainName -> onlineCaptains.stream()
                        .noneMatch(onlineName -> onlineName.equalsIgnoreCase(captainName)))
                .toList();
    }

    private boolean isOnline(String captainName, long now) {
        long cutoff = now - HEARTBEAT_GRACE_MILLIS;
        return sessions.values().stream()
                .anyMatch(presence -> presence.lastSeenAt() >= cutoff
                        && presence.captainName().equalsIgnoreCase(captainName));
    }

    private void broadcastPresenceChange() {
        if (auctionSocketService != null) {
            auctionSocketService.broadcastRefresh();
        }
    }
}