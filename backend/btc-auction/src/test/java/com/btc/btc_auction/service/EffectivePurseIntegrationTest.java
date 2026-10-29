package com.btc.btc_auction.service;

import com.btc.btc_auction.entity.BlindOpeningBidEntity;
import com.btc.btc_auction.entity.ClubbedPlayerPairEntity;
import com.btc.btc_auction.entity.CursePlayerEntity;
import com.btc.btc_auction.entity.PlayerEntity;
import com.btc.btc_auction.entity.TeamEntity;
import com.btc.btc_auction.enums.AuctionPhase;
import com.btc.btc_auction.model.Auction;
import com.btc.btc_auction.repository.BlindOpeningBidRepository;
import com.btc.btc_auction.repository.ClubbedPlayerPairRepository;
import com.btc.btc_auction.repository.CursePlayerRepository;
import com.btc.btc_auction.repository.PlayerRepository;
import com.btc.btc_auction.repository.TeamRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:effective-purse;DB_CLOSE_DELAY=-1",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@Transactional
class EffectivePurseIntegrationTest {

    @Autowired private AuctionService auctionService;
    @Autowired private EffectivePurseService effectivePurseService;
    @Autowired private EffectiveSalePriceService effectiveSalePriceService;
    @Autowired private AuctionConfigService auctionConfigService;
    @Autowired private CurrentAuctionService currentAuctionService;
    @Autowired private TeamRepository teamRepository;
    @Autowired private PlayerRepository playerRepository;
    @Autowired private BlindOpeningBidRepository blindOpeningBidRepository;
    @Autowired private CursePlayerRepository cursePlayerRepository;
    @Autowired private ClubbedPlayerPairRepository pairRepository;
    @Autowired private ReAuctionSnapshotService snapshotService;
    @Autowired private jakarta.persistence.EntityManager entityManager;
    @Autowired private BidAffordabilityService affordabilityService;
    @Autowired private BlindOpeningBidService blindBidService;
    @Autowired private SilentBidService silentBidService;
    @Autowired private RtmService rtmService;

    private TeamEntity buyer;

    @BeforeEach
    void setUp() {
        buyer = team("Buyer", 4000);
        currentAuctionService.setCurrentAuction(new Auction("", "", 0, "None", 0));
        auctionService.restorePersistedRuntimeState();
    }

    @Test
    void saleDeductsEffectivePriceAndUndoRestoresTheWholeBudget() {
        PlayerEntity player = player("Monty", 450, false);
        bonus(player.getName(), false, 0);

        assertTrue(sell(player).contains("final purse impact ₹150"));
        assertEquals(3850, buyer.getPurse());
        assertEquals(0, buyer.getTournamentPoints());
        assertEquals(450, player.getSoldPrice());
        assertEquals(300, buyer.getEffectivePriceCredit());

        assertEquals("Last sale undone", auctionService.undoLastSale());
        assertEquals(4000, buyer.getPurse());
        assertEquals(0, buyer.getTournamentPoints());
        assertEquals(0, buyer.getEffectivePriceCredit());
    }

    @Test
    void existingSalesAreReconciledOnceEvenAcrossRepeatedStartupReconciliation() {
        player("Arijit Lal", 700, true);
        player("Monty", 450, true);
        player("Balaram", 650, true);
        buyer.setPurse(2200);
        buyer.setTournamentPoints(600);
        bonus("Arijit Lal", true, 300);
        bonus("Monty", true, 300);

        effectivePurseService.reconcilePurses();
        assertEquals(2800, buyer.getPurse());
        assertEquals(600, buyer.getEffectivePriceCredit());

        effectivePurseService.reconcilePurses();
        assertEquals(2800, buyer.getPurse());
        assertEquals(600, buyer.getTournamentPoints());
    }

    @Test
    void pairSaleAndCancellationApplyOneBonusEvenWhenCancellingTheSecondPlayer() {
        PlayerEntity first = player("A", 450, false);
        player("C", 450, false);
        ClubbedPlayerPairEntity pair = new ClubbedPlayerPairEntity();
        pair.setPlayerOne("A");
        pair.setPlayerTwo("C");
        pair.setActive(true);
        pairRepository.save(pair);
        bonus("C", false, 0);

        sell(first);
        assertEquals(3850, buyer.getPurse());
        assertEquals(300, buyer.getEffectivePriceCredit());
        assertEquals(2, buyer.getPlayersBought());

        assertEquals("Sale cancelled for C", auctionService.cancelSale("C"));
        assertEquals(4000, buyer.getPurse());
        assertEquals(0, buyer.getEffectivePriceCredit());
        assertEquals(0, buyer.getTournamentPoints());
    }

    @Test
    void cursePenaltyAndSetterPurseRewardApplyWithoutAnyTournamentBalance() {
        PlayerEntity player = player("Cursed", 450, false);
        TeamEntity setter = team("Setter", 4000);
        CursePlayerEntity curse = new CursePlayerEntity();
        curse.setPlayerName(player.getName());
        curse.setCaptainName(setter.getCaptainName());
        cursePlayerRepository.save(curse);

        sell(player);
        assertEquals(3450, buyer.getPurse());
        assertEquals(0, buyer.getTournamentPoints());
        assertEquals(-100, buyer.getEffectivePriceCredit());
        assertEquals(4100, setter.getPurse());
        assertEquals(100, setter.getCurseRewardCredit());
        effectivePurseService.reconcilePurses();
        assertEquals(4100, setter.getPurse());
        assertEquals(3450, buyer.getPurse());

        auctionService.cancelSale(player.getName());
        assertEquals(4000, buyer.getPurse());
        assertEquals(0, buyer.getTournamentPoints());
        assertEquals(0, buyer.getEffectivePriceCredit());
        assertEquals(4000, setter.getPurse());
        assertEquals(0, setter.getCurseRewardCredit());
    }

    @Test
    void discountCannotCreditMoreThanThePriceOfThePlayer() {
        PlayerEntity player = player("Cheap", 100, false);
        bonus(player.getName(), false, 0);

        sell(player);
        assertEquals(4000, buyer.getPurse());
        assertEquals(100, buyer.getEffectivePriceCredit());
        assertEquals(0, buyer.getTournamentPoints());

        auctionService.cancelSale(player.getName());
        assertEquals(4000, buyer.getPurse());
    }

    @Test
    void manualTransferRemovesOldBuyersDiscountAndChargesTheNewBuyer() {
        PlayerEntity player = player("Monty", 450, false);
        bonus(player.getName(), false, 0);
        TeamEntity newBuyer = team("New Buyer", 4000);
        sell(player);

        assertEquals("Manual Sale Updated",
                auctionService.manualSale(player.getName(), newBuyer.getCaptainName(), 650, "Correction"));
        assertEquals(4000, buyer.getPurse());
        assertEquals(0, buyer.getEffectivePriceCredit());
        assertEquals(3350, newBuyer.getPurse());
        assertEquals(650, player.getSoldPrice());
    }

    @Test
    void snapshotRollbackRestoresPurseAndAppliedCreditWithoutDoubleCrediting() {
        PlayerEntity first = player("First", 450, false);
        PlayerEntity second = player("Second", 700, false);
        bonus(first.getName(), false, 0);
        sell(first);
        snapshotService.capture(0);

        bonus(second.getName(), false, 0);
        sell(second);
        assertEquals(3450, buyer.getPurse());
        assertEquals(600, buyer.getEffectivePriceCredit());

        snapshotService.restore();
        effectivePurseService.reconcilePurses();
        assertEquals(3850, buyer.getPurse());
        assertEquals(300, buyer.getEffectivePriceCredit());
        assertEquals(150, effectiveSalePriceService.applyToPlayers(List.of(first))
                .getFirst().getEffectivePrice());
    }

    @Test
    void historicZeroBalanceCurseStillAddsFullPenaltyAndCreditsSetterExactlyOnceAfterReload() {
        PlayerEntity player = player("Arnab Nandy", 550, true);
        buyer.setPurse(3450);
        TeamEntity setter = team("Setter", 4000);
        CursePlayerEntity curse = new CursePlayerEntity();
        curse.setPlayerName(player.getName());
        curse.setCaptainName(setter.getCaptainName());
        curse.setTriggered(true);
        curse.setSettledBuyer(buyer.getCaptainName());
        curse.setSettledAmount(100);
        curse.setBuyerDeduction(0);
        cursePlayerRepository.save(curse);

        effectivePurseService.reconcilePurses();
        assertEquals(3350, buyer.getPurse());
        assertEquals(4100, setter.getPurse());
        assertEquals(650, player.getEffectivePrice());
        entityManager.flush();
        entityManager.clear();

        effectivePurseService.reconcilePurses();
        assertEquals(3350, teamRepository.findByCaptainName("Buyer").orElseThrow().getPurse());
        assertEquals(4100, teamRepository.findByCaptainName("Setter").orElseThrow().getPurse());
    }

    @Test
    void packageCursesChargeBuyerOnceAndCreditEachDistinctSetterOnce() {
        PlayerEntity first = player("A", 1000, false);
        player("C", 1000, false);
        TeamEntity setter = team("Setter", 4000);
        TeamEntity otherSetter = team("Other Setter", 4000);
        ClubbedPlayerPairEntity pair = new ClubbedPlayerPairEntity();
        pair.setPlayerOne("A");
        pair.setPlayerTwo("C");
        pair.setActive(true);
        pairRepository.save(pair);
        for (String name : List.of("A", "C")) {
            CursePlayerEntity curse = new CursePlayerEntity();
            curse.setPlayerName(name);
            curse.setCaptainName(setter.getCaptainName());
            cursePlayerRepository.save(curse);
        }
        CursePlayerEntity otherCurse = new CursePlayerEntity();
        otherCurse.setPlayerName("C");
        otherCurse.setCaptainName(otherSetter.getCaptainName());
        cursePlayerRepository.save(otherCurse);

        sell(first);
        assertEquals(2600, buyer.getPurse());
        assertEquals(4400, setter.getPurse());
        assertEquals(4400, otherSetter.getPurse());
        assertEquals(1400, first.getEffectivePrice());

        auctionService.cancelSale("C");
        assertEquals(4000, buyer.getPurse());
        assertEquals(4000, setter.getPurse());
        assertEquals(4000, otherSetter.getPurse());
    }

    @Test
    void snapshotRollbackRemovesNewCurseRewardsAndPenalty() {
        PlayerEntity player = player("Target", 550, false);
        TeamEntity setter = team("Setter", 4000);
        CursePlayerEntity curse = new CursePlayerEntity();
        curse.setPlayerName(player.getName());
        curse.setCaptainName(setter.getCaptainName());
        cursePlayerRepository.save(curse);
        snapshotService.capture(0);

        sell(player);
        assertEquals(3350, buyer.getPurse());
        assertEquals(4100, setter.getPurse());

        snapshotService.restore();
        effectivePurseService.reconcilePurses();
        assertEquals(4000, buyer.getPurse());
        assertEquals(4000, setter.getPurse());
        assertEquals(0, setter.getCurseRewardCredit());
    }

    private TeamEntity team(String name, int purse) {
        TeamEntity team = new TeamEntity();
        team.setCaptainName(name);
        team.setPurse(purse);
        team.setPlayersLeft(9);
        return teamRepository.save(team);
    }

    @Test
    void openingBidCapIs450With150PurseAnd350WithMarketBoom() {
        PlayerEntity player = player("Target", 50, false);
        buyer.setPurse(150);
        blindBidService.startRound(player.getName());
        assertEquals(450, affordabilityService.maxBid(player.getName(), "Buyer", true));
        assertEquals("Blind opening bid submitted.", blindBidService.submitBid(player.getName(), "Buyer", 450));
        assertTrue(blindBidService.submitBid(player.getName(), "Buyer", 500).contains("Maximum affordable bid is ₹450"));
        assertEquals(450, blindOpeningBidRepository.findByPlayerName(player.getName()).getFirst().getBidAmount());

        market(player.getName(), com.btc.btc_auction.enums.RandomEventType.MARKET_BOOM);
        assertEquals(350, affordabilityService.maxBid(player.getName(), "Buyer", true));
        assertEquals("Blind opening bid submitted.", blindBidService.submitBid(player.getName(), "Buyer", 350));
        assertTrue(blindBidService.submitBid(player.getName(), "Buyer", 400).contains("Maximum affordable bid is ₹350"));
    }

    @Test
    void bonanzaCapOnlyUses500DiscountWhileEventIsEligible() {
        PlayerEntity player = player("Target", 50, false);
        buyer.setPurse(150);
        market(player.getName(), com.btc.btc_auction.enums.RandomEventType.BLIND_BONANZA);
        assertEquals(650, affordabilityService.maxBid(player.getName(), "Buyer", true));
        var config = auctionConfigService.getConfig();
        config.setBlindBonanzaAwardsUsed(4);
        auctionConfigService.save(config);
        assertEquals(450, affordabilityService.maxBid(player.getName(), "Buyer", true));
    }

    @Test
    void curseRoundingAndMarketAdjustmentBothReduceOpeningCap() {
        PlayerEntity player = player("Target", 50, false);
        buyer.setPurse(150);
        CursePlayerEntity curse = new CursePlayerEntity();
        curse.setPlayerName(player.getName());
        curse.setCaptainName("Setter");
        cursePlayerRepository.save(curse);
        assertEquals(350, affordabilityService.maxBid(player.getName(), "Buyer", true));
        assertNull(affordabilityService.validate(player.getName(), "Buyer", 350, true));
        assertTrue(affordabilityService.validate(player.getName(), "Buyer", 400, true).contains("Maximum"));
        market(player.getName(), com.btc.btc_auction.enums.RandomEventType.MARKET_BOOM);
        assertEquals(250, affordabilityService.maxBid(player.getName(), "Buyer", true));

        CursePlayerEntity selfCurse = new CursePlayerEntity();
        selfCurse.setPlayerName(player.getName());
        selfCurse.setCaptainName("Buyer");
        cursePlayerRepository.save(selfCurse);
        assertEquals(350, affordabilityService.maxBid(player.getName(), "Buyer", true));
    }

    @Test
    void liveBidsOnlyGiveDiscountToTheEligibleOpeningWinner() {
        PlayerEntity player = player("Target", 50, false);
        buyer.setPurse(150);
        currentAuctionService.setCurrentAuction(new Auction("Target", "A", 50, "None", 50));
        assertTrue(auctionService.updateCurrentAuction("Buyer", 200).contains("Maximum affordable bid is ₹150"));
        assertEquals("None", currentAuctionService.getCurrentAuction().getLeader());
        bonus(player.getName(), false, 0);
        assertEquals("Updated", auctionService.updateCurrentAuction("Buyer", 450));
        assertTrue(auctionService.updateCurrentAuction("Buyer", 500).contains("Maximum affordable bid is ₹450"));
        assertEquals(450, currentAuctionService.getCurrentAuction().getCurrentBid());
    }

    @Test
    void finalSaleRevalidatesAffordabilityBeforeChangingPlayersPurseOrMarketEvent() {
        PlayerEntity player = player("Target", 400, false);
        buyer.setPurse(150);
        bonus(player.getName(), false, 0);
        market(player.getName(), com.btc.btc_auction.enums.RandomEventType.MARKET_BOOM);
        assertTrue(sell(player).contains("Maximum affordable bid is ₹350"));
        assertFalse(player.isSold());
        assertEquals(150, buyer.getPurse());
        assertEquals(0, buyer.getPlayersBought());
        assertEquals(com.btc.btc_auction.enums.RandomEventType.MARKET_BOOM,
                auctionConfigService.getConfig().getPendingRandomEventType());
        assertEquals("Target", currentAuctionService.getCurrentAuction().getCurrentPlayer());
    }

    @Test
    void marketBoomSaleAtMaximumConsumesExactlyTheAvailablePurse() {
        PlayerEntity player = player("Target", 350, false);
        team("Other", 1000);
        buyer.setPurse(150);
        bonus(player.getName(), false, 0);
        market(player.getName(), com.btc.btc_auction.enums.RandomEventType.MARKET_BOOM);
        var config = auctionConfigService.getConfig();
        config.setMarketAdjustmentPlayer("Target");
        config.setMarketAdjustment(100);
        auctionConfigService.save(config);
        assertTrue(sell(player).startsWith("Target sold to Buyer"));
        assertEquals(0, buyer.getPurse());
        assertEquals(150, player.getEffectivePrice());
        assertEquals(450, player.getFinalPrice());
    }

    @Test
    void silentBidsCannotUseAnUnwonOpeningDiscountAndCanStillPass() {
        PlayerEntity player = player("Target", 50, false);
        buyer.setPurse(150);
        silentBidService.startRound(player.getName());
        assertTrue(silentBidService.submitBid("Target", "Buyer", 200).contains("Maximum affordable bid is ₹150"));
        assertFalse(silentBidService.getCaptainBid("Buyer").isSubmitted());
        assertEquals("Bid submitted.", silentBidService.submitBid("Target", "Buyer", 0));
    }

    @Test
    void rtmCannotClaimOrRaiseBeyondItsOwnSituationalBudget() {
        player("Target", 50, false);
        buyer.setPurse(150);
        market("Target", com.btc.btc_auction.enums.RandomEventType.MARKET_BOOM);
        var config = auctionConfigService.getConfig();
        config.setAuctionPhase(AuctionPhase.SOLD);
        auctionConfigService.save(config);
        currentAuctionService.setCurrentAuction(new Auction("Target", "A", 100, "Other", 50));
        assertTrue(rtmService.claimRtm("Buyer").contains("Maximum affordable bid is ₹50"));
        assertNull(rtmService.getCurrentRtm());
        config.setPendingRandomEventType(null);
        config.setPendingRandomEventPlayer(null);
        auctionConfigService.save(config);
        assertEquals("RTM claimed.", rtmService.claimRtm("Buyer"));
        assertTrue(rtmService.submitBid(200).contains("Maximum affordable bid is ₹150"));
        assertEquals("CLAIMED", rtmService.getCurrentRtm().getStatus());
        assertEquals("RTM bid submitted.", rtmService.submitBid(150));
    }

    @Test
    void pairedSaleNeedsTwoSlotsAndCountsDuplicateSetterOnlyOnceInCap() {
        player("A", 50, false);
        player("C", 50, false);
        buyer.setPurse(150);
        buyer.setPlayersLeft(1);
        ClubbedPlayerPairEntity pair = new ClubbedPlayerPairEntity();
        pair.setPlayerOne("A");
        pair.setPlayerTwo("C");
        pair.setActive(true);
        pairRepository.save(pair);
        assertEquals(0, affordabilityService.maxBid("A", "Buyer", true));
        assertTrue(affordabilityService.validate("A", "Buyer", 50, true).contains("squad slots"));
        buyer.setPlayersLeft(2);
        for (String name : List.of("A", "C")) {
            CursePlayerEntity curse = new CursePlayerEntity();
            curse.setPlayerName(name);
            curse.setCaptainName("Setter");
            cursePlayerRepository.save(curse);
        }
        assertEquals(350, affordabilityService.maxBid("A", "Buyer", true));
    }

    @Test
    void returningAnUnsoldPlayerClearsDeferralAndOldBidsWithoutChangingPurse() {
        PlayerEntity player = player("Jit da", 50, false);
        player.setDeferredToReAuction(true);
        player.setReAuctioned(true);
        bonus(player.getName(), false, 0);
        currentAuctionService.setCurrentAuction(new Auction("Other player", "B", 50, "None", 50));

        assertEquals("Jit da returned to the auction pool.", auctionService.returnPlayerToPool("Jit da"));
        assertFalse(player.isDeferredToReAuction());
        assertFalse(player.isReAuctioned());
        assertTrue(blindOpeningBidRepository.findByPlayerName("Jit da").isEmpty());
        assertEquals(4000, buyer.getPurse());
        assertEquals("Other player", currentAuctionService.getCurrentAuction().getCurrentPlayer());
    }

    @Test
    void returningSoldPlayerRefundsEffectiveCostOnceAndClearsActiveBidding() {
        PlayerEntity player = player("Tiyas", 450, false);
        bonus(player.getName(), false, 0);
        sell(player);
        assertEquals(3850, buyer.getPurse());
        currentAuctionService.setCurrentAuction(new Auction("Tiyas", "B", 450, "Buyer", 50));
        assertEquals("Tiyas returned to the auction pool.", auctionService.returnPlayerToPool("Tiyas"));
        assertEquals(4000, buyer.getPurse());
        assertFalse(player.isSold());
        assertTrue(currentAuctionService.getCurrentAuction().getCurrentPlayer().isBlank());
        auctionService.returnPlayerToPool("Tiyas");
        assertEquals(4000, buyer.getPurse());
    }

    @Test
    void maximumBidIsAnExactAffordableBoundaryAcrossMarketEventsCursesAndBidSteps() {
        player("Boundary", 50, false);
        for (int setters = 0; setters <= 2; setters++) {
            if (setters > 0) {
                CursePlayerEntity curse = new CursePlayerEntity();
                curse.setPlayerName("Boundary");
                curse.setCaptainName("Setter " + setters);
                cursePlayerRepository.save(curse);
            }
            for (var event : List.of(
                    com.btc.btc_auction.enums.RandomEventType.MARKET_BOOM,
                    com.btc.btc_auction.enums.RandomEventType.MARKET_CRASH,
                    com.btc.btc_auction.enums.RandomEventType.BLIND_BONANZA)) {
                market("Boundary", event);
                for (int purse : List.of(0, 50, 150, 1000, 1050, 1500)) {
                    buyer.setPurse(purse);
                    for (boolean opening : List.of(false, true)) {
                        int max = affordabilityService.maxBid("Boundary", "Buyer", opening);
                        if (max > 0) {
                            assertNull(affordabilityService.validate("Boundary", "Buyer", max, opening));
                        }
                        int next = max < 1000 ? max + 50 : max + 100;
                        assertTrue(affordabilityService.validate("Boundary", "Buyer", next, opening)
                                .contains("Maximum affordable bid"),
                                () -> "Unexpected affordable next bid: " + next);
                    }
                }
            }
        }
    }

    private void market(String name, com.btc.btc_auction.enums.RandomEventType type) {
        var config = auctionConfigService.getConfig();
        config.setPendingRandomEventPlayer(name);
        config.setPendingRandomEventType(type);
        config.setPendingRandomEventAmount(type == com.btc.btc_auction.enums.RandomEventType.BLIND_BONANZA ? 500 : 100);
        auctionConfigService.save(config);
    }

    private PlayerEntity player(String name, int price, boolean sold) {
        PlayerEntity player = new PlayerEntity();
        player.setName(name);
        player.setSeed("A");
        player.setSold(sold);
        player.setTeam(sold ? buyer.getCaptainName() : "");
        player.setSoldPrice(sold ? price : 0);
        player.setFinalPrice(sold ? price : 0);
        player.setBasePrice(price);
        return playerRepository.save(player);
    }

    private void bonus(String playerName, boolean awarded, int amount) {
        BlindOpeningBidEntity bid = new BlindOpeningBidEntity();
        bid.setPlayerName(playerName);
        bid.setCaptainName(buyer.getCaptainName());
        bid.setBonusEligible(true);
        bid.setBonusAwarded(awarded);
        bid.setBonusAwardedAmount(amount);
        blindOpeningBidRepository.save(bid);
    }

    private String sell(PlayerEntity player) {
        var config = auctionConfigService.getConfig();
        config.setAuctionPhase(AuctionPhase.SOLD);
        auctionConfigService.save(config);
        currentAuctionService.setCurrentAuction(new Auction(
                player.getName(), player.getSeed(), player.getBasePrice(), buyer.getCaptainName(),
                player.getBasePrice()));
        return auctionService.sellPlayer(player.getName(), buyer.getCaptainName(), player.getBasePrice());
    }
}
