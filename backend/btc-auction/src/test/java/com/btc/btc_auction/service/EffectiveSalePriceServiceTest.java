package com.btc.btc_auction.service;

import com.btc.btc_auction.entity.BlindOpeningBidEntity;
import com.btc.btc_auction.entity.ClubbedPlayerPairEntity;
import com.btc.btc_auction.entity.CursePlayerEntity;
import com.btc.btc_auction.entity.PlayerEntity;
import com.btc.btc_auction.repository.BlindOpeningBidRepository;
import com.btc.btc_auction.repository.CursePlayerRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EffectiveSalePriceServiceTest {

    @Mock
    private BlindOpeningBidRepository blindOpeningBidRepository;

    @Mock
    private CursePlayerRepository cursePlayerRepository;

    @Mock
    private ClubbedPlayerPairService clubbedPlayerPairService;

    private EffectiveSalePriceService service;

    @BeforeEach
    void setUp() {
        service = new EffectiveSalePriceService(
                blindOpeningBidRepository,
                cursePlayerRepository,
                clubbedPlayerPairService);
    }

    @Test
    void subtractsBuyerBonusWithoutChangingRecordedSalePrice() {
        PlayerEntity player = soldPlayer("Monty", "Captain", 450);
        BlindOpeningBidEntity bonus = bonus("Monty", "Captain", 300);
        when(blindOpeningBidRepository.findByPlayerNameIn(List.of("Monty"))).thenReturn(List.of(bonus));
        when(cursePlayerRepository.findByPlayerNameIn(List.of("Monty"))).thenReturn(List.of());
        when(clubbedPlayerPairService.getActivePairs()).thenReturn(List.of());

        service.applyToPlayers(List.of(player));

        assertEquals(150, player.getEffectivePrice());
        assertEquals(450, player.getFinalPrice());
        assertEquals(450, player.getSoldPrice());
    }

    @Test
    void addsOnlyTheBuyersCurseDeductionToEffectivePrice() {
        PlayerEntity player = soldPlayer("Monty", "Captain", 450);
        CursePlayerEntity curse = curse("Monty", "Captain", 200);
        CursePlayerEntity otherBuyersCurse = curse("Monty", "Other Captain", 400);
        when(blindOpeningBidRepository.findByPlayerNameIn(List.of("Monty"))).thenReturn(List.of());
        when(cursePlayerRepository.findByPlayerNameIn(List.of("Monty")))
                .thenReturn(List.of(curse, otherBuyersCurse));
        when(clubbedPlayerPairService.getActivePairs()).thenReturn(List.of());

        service.applyToPlayers(List.of(player));

        assertEquals(650, player.getEffectivePrice());
        assertEquals(450, player.getFinalPrice());
    }

    @Test
    void countsPackageBonusAndCursePenaltyOnlyOnceAcrossEitherPlayer() {
        PlayerEntity playerOne = soldPlayer("A", "Captain", 450);
        PlayerEntity playerTwo = soldPlayer("C", "Captain", 450);
        ClubbedPlayerPairEntity pair = new ClubbedPlayerPairEntity();
        pair.setPlayerOne("A");
        pair.setPlayerTwo("C");
        pair.setActive(true);
        when(blindOpeningBidRepository.findByPlayerNameIn(List.of("A", "C")))
                .thenReturn(List.of(bonus("C", "Captain", 300)));
        when(cursePlayerRepository.findByPlayerNameIn(List.of("A", "C")))
                .thenReturn(List.of(curse("A", "Captain", 100), curse("C", "Captain", 100)));
        when(clubbedPlayerPairService.getActivePairs()).thenReturn(List.of(pair));

        service.applyToPlayers(List.of(playerOne, playerTwo));

        assertEquals(250, playerOne.getEffectivePrice());
        assertEquals(250, playerTwo.getEffectivePrice());
        assertEquals(450, playerOne.getFinalPrice());
        assertEquals(450, playerTwo.getFinalPrice());
    }

    private PlayerEntity soldPlayer(String name, String captain, int price) {
        PlayerEntity player = new PlayerEntity();
        player.setName(name);
        player.setTeam(captain);
        player.setSold(true);
        player.setSoldPrice(price);
        player.setFinalPrice(price);
        return player;
    }

    private BlindOpeningBidEntity bonus(String playerName, String captainName, int amount) {
        BlindOpeningBidEntity bonus = new BlindOpeningBidEntity();
        bonus.setPlayerName(playerName);
        bonus.setCaptainName(captainName);
        bonus.setBonusAwarded(true);
        bonus.setBonusAwardedAmount(amount);
        return bonus;
    }

    private CursePlayerEntity curse(String playerName, String buyer, int deduction) {
        CursePlayerEntity curse = new CursePlayerEntity();
        curse.setPlayerName(playerName);
        curse.setTriggered(true);
        curse.setSettledBuyer(buyer);
        curse.setBuyerDeduction(deduction);
        curse.setSettledAmount(deduction);
        return curse;
    }
}
