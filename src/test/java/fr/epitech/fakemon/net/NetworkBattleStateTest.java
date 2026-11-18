package fr.epitech.fakemon.net;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import fr.epitech.fakemon.core.api.FakemonEnteredEvent;
import fr.epitech.fakemon.core.api.FakemonFaintedEvent;
import fr.epitech.fakemon.core.api.HpLostEvent;
import fr.epitech.fakemon.core.api.HpRestoredEvent;
import fr.epitech.fakemon.core.api.Side;
import fr.epitech.fakemon.core.api.TurnStartedEvent;
import fr.epitech.fakemon.core.engine.TeamDrafter;
import fr.epitech.fakemon.core.model.Fakemon;
import fr.epitech.fakemon.core.model.Team;
import fr.epitech.fakemon.data.InMemorySpeciesRepository;
import java.util.List;
import java.util.Random;
import org.junit.jupiter.api.Test;

/** Vérifie la mise à jour de la copie du combat utilisée par l'invité. */
class NetworkBattleStateTest {
    private final TeamDrafter drafter = new TeamDrafter(new InMemorySpeciesRepository(), new Random(50));
    private final Team player = drafter.randomTeam(List.of());
    private final Team opponent = drafter.randomTeam(
            player.getFakemons().stream().map(Fakemon::getSpecies).toList());
    private final NetworkBattleState state = new NetworkBattleState(player, opponent);

    @Test
    void startsWithTheReceivedTeamsAndNoActiveFakemon() {
        assertSame(player, state.getTeam(Side.PLAYER));
        assertSame(opponent, state.getTeam(Side.OPPONENT));
        assertNull(state.getActiveFakemon(Side.PLAYER));
        assertNull(state.getActiveFakemon(Side.OPPONENT));
        assertEquals(0, state.getTurnNumber());
    }

    @Test
    void tracksEntriesAndPreservesBenchHealthAfterSwitching() {
        Fakemon first = player.getFakemons().get(0);
        Fakemon second = player.getFakemons().get(1);
        state.apply(new FakemonEnteredEvent(Side.PLAYER, first));
        state.apply(new HpLostEvent(Side.PLAYER, first, 20, first.getMaxHp() - 20));
        state.apply(new FakemonEnteredEvent(Side.PLAYER, second));
        assertSame(second, state.getActiveFakemon(Side.PLAYER));
        assertEquals(first.getMaxHp() - 20, first.getCurrentHp());
        state.apply(new FakemonEnteredEvent(Side.PLAYER, first));
        assertSame(first, state.getActiveFakemon(Side.PLAYER));
        assertEquals(first.getMaxHp() - 20, first.getCurrentHp());
    }

    @Test
    void appliesLossHealingAndFaintingToTheExistingTeamMember() {
        Fakemon mon = opponent.getFakemons().get(0);
        state.apply(new FakemonEnteredEvent(Side.OPPONENT, mon));
        state.apply(new HpLostEvent(Side.OPPONENT, mon, 30, mon.getMaxHp() - 30));
        state.apply(new HpRestoredEvent(Side.OPPONENT, mon, 10, mon.getMaxHp() - 20));
        assertEquals(mon.getMaxHp() - 20, state.getActiveFakemon(Side.OPPONENT).getCurrentHp());
        state.apply(new FakemonFaintedEvent(Side.OPPONENT, mon));
        assertEquals(0, mon.getCurrentHp());
        assertEquals(5, opponent.getStandingFakemons().size());
    }

    @Test
    void rejectsInvalidUpdatesWithoutChangingTheState() {
        Fakemon mon = player.getFakemons().get(0);
        assertThrows(IllegalArgumentException.class,
                () -> state.apply(new FakemonEnteredEvent(Side.OPPONENT, mon)));
        assertNull(state.getActiveFakemon(Side.OPPONENT));
        for (int invalid : List.of(-1, mon.getMaxHp() + 1)) {
            assertThrows(IllegalArgumentException.class,
                    () -> state.apply(new HpLostEvent(Side.PLAYER, mon, 0, invalid)));
        }
        assertEquals(mon.getMaxHp(), mon.getCurrentHp());
    }

    @Test
    void tracksTurnsAndRejectsGoingBackwards() {
        state.apply(new TurnStartedEvent(1));
        state.apply(new TurnStartedEvent(2));
        assertThrows(IllegalArgumentException.class, () -> state.apply(new TurnStartedEvent(1)));
        assertThrows(IllegalArgumentException.class, () -> state.apply(new TurnStartedEvent(0)));
        assertEquals(2, state.getTurnNumber());
    }

    @Test
    void updatesTheGuestTeamWhenTheHostAnnouncesAnOpponentEntry() {
        NetworkEventCodec hostCodec = new NetworkEventCodec(opponent, player, Side.PLAYER);
        NetworkEventCodec guestCodec = new NetworkEventCodec(opponent, player, Side.OPPONENT);
        Fakemon guestMon = player.getFakemons().get(0);
        guestCodec.decode(hostCodec.encode(new FakemonEnteredEvent(Side.OPPONENT, guestMon)))
                .ifPresent(state::apply);
        guestCodec.decode(hostCodec.encode(
                new HpLostEvent(Side.OPPONENT, guestMon, 10, guestMon.getMaxHp() - 10)))
                .ifPresent(state::apply);
        assertSame(guestMon, state.getActiveFakemon(Side.PLAYER));
        assertEquals(guestMon.getMaxHp() - 10, state.getActiveFakemon(Side.PLAYER).getCurrentHp());
    }
}
