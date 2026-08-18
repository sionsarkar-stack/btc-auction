package com.btc.btc_auction.service;

import com.btc.btc_auction.entity.AuctionConfigEntity;
import com.btc.btc_auction.entity.PlayerEntity;
import com.btc.btc_auction.entity.SecretTargetEntity;
import com.btc.btc_auction.entity.TeamEntity;
import com.btc.btc_auction.repository.SecretTargetRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SecretTargetServiceTest {

    @Mock
    private SecretTargetRepository repository;
    @Mock
    private PlayerService playerService;
    @Mock
    private TeamService teamService;
    @Mock
    private AuctionEventService eventService;
    @Mock
    private AuctionConfigService configService;
    @Mock
    private AdminActionLogService adminActionLogService;

    private SecretTargetService secretTargetService;

    @BeforeEach
    void setUp() {
        AuctionConfigEntity config = new AuctionConfigEntity();
        config.setTargetBonus(150);
        config.setTargetMissPenalty(100);
        config.setTargetCompletionBonus(250);
        when(configService.getConfig()).thenReturn(config);

        secretTargetService = new SecretTargetService(
                repository,
                playerService,
                teamService,
                eventService,
                configService,
                adminActionLogService);
    }

    @Test
    void sharedTargetRewardsBuyerAndKeepsOtherTargetsActive() {
        SecretTargetEntity senTargets = target("Sen", "Pritam", "Harshit");
        SecretTargetEntity joyTargets = target("Joy", "Pritam", "Ujjawal");
        when(repository.findAll()).thenReturn(java.util.List.of(senTargets, joyTargets));

        TeamEntity sen = team("Sen", 5000);
        TeamEntity joy = team("Joy", 5000);
        when(teamService.getTeam("Sen")).thenReturn(sen);
        when(teamService.getTeam("Joy")).thenReturn(joy);

        PlayerEntity pritam = player("Pritam", "Sen", true);
        PlayerEntity harshit = player("Harshit", "", false);
        PlayerEntity ujjawal = player("Ujjawal", "", false);
        Map<String, PlayerEntity> players = Map.of(
                "Pritam", pritam,
                "Harshit", harshit,
                "Ujjawal", ujjawal);
        when(playerService.getPlayer(anyString())).thenAnswer(invocation -> players.get(invocation.getArgument(0)));

        int buyerAdjustment = secretTargetService.applyPurchaseReward("Pritam", "Sen");

        assertEquals(150, buyerAdjustment);
        assertEquals(5150, sen.getPurse());
        assertEquals(4900, joy.getPurse());
        assertFalse(senTargets.isSettled());
        assertFalse(joyTargets.isSettled());
        assertFalse(joyTargets.isPairSettled());
    }

    @Test
    void incompletePairAppliesOnlyTheMissedTargetPenaltyAtAuctionEnd() {
        SecretTargetEntity targets = target("Sen", "Pritam", "Harshit");
        when(repository.findAll()).thenReturn(java.util.List.of(targets));

        TeamEntity sen = team("Sen", 5000);
        when(teamService.getTeam("Sen")).thenReturn(sen);

        PlayerEntity pritam = player("Pritam", "Sen", true);
        PlayerEntity harshit = player("Harshit", "", false);
        Map<String, PlayerEntity> players = Map.of(
                "Pritam", pritam,
                "Harshit", harshit);
        when(playerService.getPlayer(anyString())).thenAnswer(invocation -> players.get(invocation.getArgument(0)));

        secretTargetService.applyPurchaseReward("Pritam", "Sen");
        secretTargetService.settleAll();

        assertEquals(5050, sen.getPurse());
        assertTrue(targets.isSettled());
        assertTrue(targets.isPlayerOneSettled());
        assertTrue(targets.isPlayerTwoSettled());
        assertTrue(targets.isPairSettled());
    }

    @Test
    void buyingBothTargetsAwardsOneHundredFiftyThenTwoHundredFifty() {
        SecretTargetEntity targets = target("Sen", "Pritam", "Harshit");
        when(repository.findAll()).thenReturn(java.util.List.of(targets));

        TeamEntity sen = team("Sen", 5000);
        when(teamService.getTeam("Sen")).thenReturn(sen);

        PlayerEntity pritam = player("Pritam", "Sen", true);
        PlayerEntity harshit = player("Harshit", "Sen", true);
        Map<String, PlayerEntity> players = Map.of(
                "Pritam", pritam,
                "Harshit", harshit);
        when(playerService.getPlayer(anyString())).thenAnswer(invocation -> players.get(invocation.getArgument(0)));

        assertEquals(150, secretTargetService.applyPurchaseReward("Pritam", "Sen"));
        assertEquals(250, secretTargetService.applyPurchaseReward("Harshit", "Sen"));
        assertEquals(5400, sen.getPurse());
        assertTrue(targets.isSettled());
    }

    private SecretTargetEntity target(String captainName, String playerOne, String playerTwo) {
        SecretTargetEntity target = new SecretTargetEntity();
        target.setCaptainName(captainName);
        target.setPlayerOne(playerOne);
        target.setPlayerTwo(playerTwo);
        return target;
    }

    private TeamEntity team(String captainName, int purse) {
        TeamEntity team = new TeamEntity();
        team.setCaptainName(captainName);
        team.setPurse(purse);
        return team;
    }

    private PlayerEntity player(String name, String teamName, boolean sold) {
        PlayerEntity player = new PlayerEntity();
        player.setName(name);
        player.setTeam(teamName);
        player.setSold(sold);
        return player;
    }
}
