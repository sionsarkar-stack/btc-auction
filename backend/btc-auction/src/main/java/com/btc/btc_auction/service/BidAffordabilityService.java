package com.btc.btc_auction.service;

import com.btc.btc_auction.entity.AuctionConfigEntity;
import com.btc.btc_auction.entity.TeamEntity;
import com.btc.btc_auction.enums.RandomEventType;
import com.btc.btc_auction.repository.BlindOpeningBidRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class BidAffordabilityService {

    private final TeamService teamService;
    private final AuctionConfigService configService;
    private final ClubbedPlayerPairService pairService;
    private final CursePlayerService curseService;
    private final BlindOpeningBidRepository blindBidRepository;

    public BidAffordabilityService(TeamService teamService, AuctionConfigService configService,
            ClubbedPlayerPairService pairService, CursePlayerService curseService,
            BlindOpeningBidRepository blindBidRepository) {
        this.teamService = teamService;
        this.configService = configService;
        this.pairService = pairService;
        this.curseService = curseService;
        this.blindBidRepository = blindBidRepository;
    }

    public String validate(String playerName, String captainName, int bid, boolean openingBid) {
        TeamEntity team = teamService.getTeam(captainName);
        if (team == null) {
            return "Captain not found.";
        }
        List<String> names = saleNames(playerName);
        if (team.getPlayersLeft() < names.size()) {
            return "Not enough squad slots for this player/package.";
        }
        AuctionConfigEntity config = configService.getConfig();
        int discount = discount(names, captainName, openingBid, config);
        int adjustment = marketAdjustment(playerName, config);
        if ((long) bid + adjustment > Integer.MAX_VALUE) {
            return "Market-adjusted price exceeds the supported price range.";
        }
        if (effectiveCost(names, captainName, bid, adjustment, discount) > team.getPurse()) {
            return "Bid exceeds your available purse after market events, opening-bid discount and curses. "
                    + "Maximum affordable bid is ₹" + maxBid(playerName, captainName, openingBid) + ".";
        }
        return null;
    }

    public int maxBid(String playerName, String captainName, boolean openingBid) {
        TeamEntity team = teamService.getTeam(captainName);
        List<String> names = saleNames(playerName);
        if (team == null || team.getPurse() < 0 || team.getPlayersLeft() < names.size()) {
            return 0;
        }
        AuctionConfigEntity config = configService.getConfig();
        int discount = discount(names, captainName, openingBid, config);
        int adjustment = marketAdjustment(playerName, config);
        int low = 0;
        int high = (int) Math.min((long) Integer.MAX_VALUE - Math.max(0, adjustment),
                (long) team.getPurse() + discount + Math.max(0L, -(long) adjustment));
        int curseCount = curseService.countApplicableSetters(names, captainName);
        while (low < high) {
            int mid = (int) (((long) low + high + 1) / 2);
            if (cost(mid, adjustment, discount, curseCount) <= team.getPurse()) {
                low = mid;
            } else {
                high = mid - 1;
            }
        }
        int step = low <= 1000 ? 50 : 100;
        return low - low % step;
    }

    private long effectiveCost(List<String> names, String captainName, int bid, int adjustment, int discount) {
        return cost(bid, adjustment, discount, curseService.countApplicableSetters(names, captainName));
    }

    private long cost(int bid, int adjustment, int discount, int curseCount) {
        long payable = Math.max(0L, (long) bid + adjustment);
        long curse = curseService.calculateCurseAmount(payable, curseCount);
        return Math.max(0L, payable - discount + curse);
    }

    private List<String> saleNames(String name) {
        String partner = pairService.getPairedPlayer(name);
        return partner == null ? List.of(name) : List.of(name, partner);
    }

    private int discount(List<String> names, String captain, boolean openingBid, AuctionConfigEntity config) {
        return names.stream()
                .filter(name -> openingBid || blindBidRepository.findByPlayerName(name).stream()
                        .anyMatch(bid -> bid.isBonusEligible() && captain.equalsIgnoreCase(bid.getCaptainName())))
                .mapToInt(name -> config.getPendingRandomEventType() == RandomEventType.BLIND_BONANZA
                        && name.equalsIgnoreCase(config.getPendingRandomEventPlayer())
                        && config.getBlindBonanzaAwardsUsed() < 4 ? 500 : 300)
                .max().orElse(0);
    }

    private int marketAdjustment(String player, AuctionConfigEntity config) {
        if (player.equalsIgnoreCase(config.getMarketAdjustmentPlayer())) {
            return config.getMarketAdjustment();
        }
        if (player.equalsIgnoreCase(config.getPendingRandomEventPlayer())) {
            int amount = config.getPendingRandomEventAmount() == null ? 100
                    : Math.abs(config.getPendingRandomEventAmount());
            if (config.getPendingRandomEventType() == RandomEventType.MARKET_BOOM) {
                return amount;
            }
            if (config.getPendingRandomEventType() == RandomEventType.MARKET_CRASH) {
                return -amount;
            }
        }
        return 0;
    }
}
