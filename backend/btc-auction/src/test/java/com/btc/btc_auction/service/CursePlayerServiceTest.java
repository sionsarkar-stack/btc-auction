package com.btc.btc_auction.service;

import com.btc.btc_auction.entity.AuctionConfigEntity;
import com.btc.btc_auction.entity.CursePlayerEntity;
import com.btc.btc_auction.entity.TeamEntity;
import com.btc.btc_auction.repository.CursePlayerRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.never;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;

@ExtendWith(MockitoExtension.class)
class CursePlayerServiceTest {

    @Mock
    private CursePlayerRepository repository;
    @Mock
    private TeamService teamService;
    @Mock
    private PlayerService playerService;
    @Mock
    private AuctionConfigService auctionConfigService;
    @Mock
    private AuctionEventService auctionEventService;
    @Mock
    private AdminActionLogService adminActionLogService;
    @Mock
    private AuctionSocketService auctionSocketService;

    private CursePlayerService service;

    @BeforeEach
    void setUp() {
        service = new CursePlayerService(
                repository,
                teamService,
                playerService,
                auctionConfigService,
                auctionEventService,
                adminActionLogService,
                auctionSocketService);
    }

    @Test
    void roundsToNearestFiftyAndRoundsHalfwayUp() {
        assertEquals(250, service.calculateCurseAmount(1_350, 1));
        assertEquals(300, service.calculateCurseAmount(1_375, 1));
        assertEquals(550, service.calculateCurseAmount(1_350, 2));
    }

    @Test
    void differentCaptainsMayCurseTheSamePlayer() {
        AuctionConfigEntity config = new AuctionConfigEntity();
        config.setCurseSelectionEnabled(true);
        TeamEntity alpha = team("Alpha", 0, 1_000);
        TeamEntity beta = team("Beta", 0, 1_000);
        var player = new com.btc.btc_auction.entity.PlayerEntity();
        player.setName("Target");
        when(auctionConfigService.getConfig()).thenReturn(config);
        when(teamService.getTeam(anyString())).thenReturn(alpha, beta);
        when(playerService.getPlayer("Target")).thenReturn(player);
        when(repository.findByCaptainName(anyString())).thenReturn(List.of());
        when(repository.findByCaptainNameAndPlayerName(anyString(), anyString())).thenReturn(Optional.empty());

        assertEquals("Curse saved secretly.", service.saveCurse("Alpha", "Target"));
        assertEquals("Curse saved secretly.", service.saveCurse("Beta", "Target"));

        verify(repository, times(2)).save(any(CursePlayerEntity.class));
        verify(adminActionLogService).addLog(
                "CURSE_SELECTED",
                "Target",
                "Alpha",
                "Curse player selected.");
        verify(auctionSocketService, times(2)).broadcastRefresh();
    }

    @Test
    void listsPersistedCurseSelectionsForTheAdminView() {
        CursePlayerEntity olderSelection = curse("Alpha", "Player A");
        CursePlayerEntity newerSelection = curse("Beta", "Player B");
        when(repository.findAllByOrderByIdDesc()).thenReturn(List.of(newerSelection, olderSelection));

        var selections = service.getAllSelections();

        assertEquals(2, selections.size());
        assertEquals("Beta", selections.get(0).captainName());
        assertEquals("Player B", selections.get(0).playerName());
        assertEquals("Alpha", selections.get(1).captainName());
        assertEquals("Player A", selections.get(1).playerName());
    }

    @Test
    void stackedCursesMultiplyTransferWithoutTouchingThePurse() {
        CursePlayerEntity firstCurse = curse("Alpha", "Target");
        CursePlayerEntity secondCurse = curse("Beta", "Target");
        TeamEntity buyer = team("Buyer", 700, 1_300);
        TeamEntity alpha = team("Alpha", 100, 800);
        TeamEntity beta = team("Beta", 50, 600);
        when(repository.findByPlayerName("Target")).thenReturn(List.of(firstCurse, secondCurse));
        when(teamService.getTeam("Buyer")).thenReturn(buyer);
        when(teamService.getTeam("Alpha")).thenReturn(alpha);
        when(teamService.getTeam("Beta")).thenReturn(beta);

        service.settleSale("Target", "Buyer", 1_350);

        assertEquals(150, buyer.getTournamentPoints());
        assertEquals(1_300, buyer.getPurse());
        assertEquals(650, alpha.getTournamentPoints());
        assertEquals(800, alpha.getPurse());
        assertEquals(600, beta.getTournamentPoints());
        assertEquals(600, beta.getPurse());
        assertTrue(firstCurse.isTriggered());
        assertTrue(secondCurse.isTriggered());
    }

    @Test
    void buyerWhoSetAnyCurseCancelsAllCursesOnThatPlayer() {
        CursePlayerEntity buyerCurse = curse("Buyer", "Target");
        CursePlayerEntity otherCurse = curse("Alpha", "Target");
        TeamEntity buyer = team("Buyer", 200, 900);
        when(repository.findByPlayerName("Target")).thenReturn(List.of(buyerCurse, otherCurse));

        service.settleSale("Target", "Buyer", 1_350);

        assertEquals(200, buyer.getTournamentPoints());
        assertEquals(900, buyer.getPurse());
        assertTrue(buyerCurse.isTriggered());
        assertTrue(otherCurse.isTriggered());
        assertEquals(0, buyerCurse.getSettledAmount());
        verify(auctionEventService).logEvent(
                "CURSE_CANCELLED",
                "Target",
                "Buyer",
                0,
                "Curse cancelled: the player was bought by a captain who cursed them.");
    }

    @Test
    void buyerDeductionFloorsAtZeroButSetterReceivesFullRoundedAmount() {
        CursePlayerEntity curse = curse("Alpha", "Target");
        TeamEntity buyer = team("Buyer", 100, 1_000);
        TeamEntity setter = team("Alpha", 0, 800);
        when(repository.findByPlayerName("Target")).thenReturn(List.of(curse));
        when(teamService.getTeam("Buyer")).thenReturn(buyer);
        when(teamService.getTeam("Alpha")).thenReturn(setter);

        service.settleSale("Target", "Buyer", 1_350);

        assertEquals(0, buyer.getTournamentPoints());
        assertEquals(250, setter.getTournamentPoints());
        assertEquals(1_000, buyer.getPurse());
    }

    @Test
    void undoSettlementRestoresBuyerAndSetterTournamentPoints() {
        CursePlayerEntity curse = curse("Alpha", "Target");
        TeamEntity buyer = team("Buyer", 500, 1_000);
        TeamEntity setter = team("Alpha", 0, 800);
        when(repository.findByPlayerName("Target")).thenReturn(List.of(curse));
        when(teamService.getTeam("Buyer")).thenReturn(buyer);
        when(teamService.getTeam("Alpha")).thenReturn(setter);

        service.settleSale("Target", "Buyer", 1_350);
        service.undoSettlement("Target");

        assertEquals(500, buyer.getTournamentPoints());
        assertEquals(0, setter.getTournamentPoints());
        assertEquals(1_000, buyer.getPurse());
        assertEquals(800, setter.getPurse());
        assertEquals(false, curse.isTriggered());
    }

    @Test
    void expiresCursesOnlyForPlayersStillUnsoldAfterTheReAuction() {
        CursePlayerEntity unsoldCurse = curse("Alpha", "Unsold");
        CursePlayerEntity soldCurse = curse("Beta", "Sold");
        var unsoldPlayer = new com.btc.btc_auction.entity.PlayerEntity();
        unsoldPlayer.setName("Unsold");
        var soldPlayer = new com.btc.btc_auction.entity.PlayerEntity();
        soldPlayer.setName("Sold");
        soldPlayer.setSold(true);
        when(repository.findAll()).thenReturn(List.of(unsoldCurse, soldCurse));
        when(playerService.getPlayer("Unsold")).thenReturn(unsoldPlayer);
        when(playerService.getPlayer("Sold")).thenReturn(soldPlayer);

        service.expireForUnsoldPlayers();

        verify(repository).delete(unsoldCurse);
        verify(repository, never()).delete(soldCurse);
    }

    private CursePlayerEntity curse(String captainName, String playerName) {
        CursePlayerEntity curse = new CursePlayerEntity();
        curse.setCaptainName(captainName);
        curse.setPlayerName(playerName);
        return curse;
    }

    private TeamEntity team(String captainName, int tournamentPoints, int purse) {
        TeamEntity team = new TeamEntity();
        team.setCaptainName(captainName);
        team.setTournamentPoints(tournamentPoints);
        team.setPurse(purse);
        return team;
    }
}