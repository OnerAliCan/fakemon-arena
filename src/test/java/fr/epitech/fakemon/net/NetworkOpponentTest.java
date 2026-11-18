package fr.epitech.fakemon.net;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import fr.epitech.fakemon.core.api.Forfeit;
import fr.epitech.fakemon.core.api.SendFakemon;
import fr.epitech.fakemon.core.api.UseMove;
import fr.epitech.fakemon.core.engine.TeamDrafter;
import fr.epitech.fakemon.core.model.Fakemon;
import fr.epitech.fakemon.core.model.Move;
import fr.epitech.fakemon.core.model.MoveCategory;
import fr.epitech.fakemon.core.model.Type;
import fr.epitech.fakemon.data.InMemorySpeciesRepository;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;

/** Vérifie l'attente des choix distants et son interruption à la déconnexion. */
class NetworkOpponentTest {
    private final Fakemon mon = new TeamDrafter(new InMemorySpeciesRepository(), new Random(50))
            .randomTeam(List.of()).getFakemons().get(0);

    @Test
    void waitsForTheGuestAndDeliversTheExistingMove() throws Exception {
        CountDownLatch requested = new CountDownLatch(1);
        try (NetworkOpponent opponent = new NetworkOpponent(requested::countDown, () -> {});
                var executor = Executors.newSingleThreadExecutor()) {
            var result = executor.submit(() -> opponent.chooseAction(mon.getMoves(), List.of(mon)));
            try {
                assertTrue(requested.await(2, TimeUnit.SECONDS));
                var action = new UseMove(mon.getMoves().get(0));
                opponent.submitAction(action);
                assertSame(action, result.get(2, TimeUnit.SECONDS));
            } finally {
                opponent.close();
                executor.shutdownNow();
            }
        }
    }

    @Test
    void rejectsInvalidMovesWithoutConsumingTheRequest() throws Exception {
        CountDownLatch requested = new CountDownLatch(1);
        try (NetworkOpponent opponent = new NetworkOpponent(requested::countDown, () -> {});
                var executor = Executors.newSingleThreadExecutor()) {
            var result = executor.submit(() -> opponent.chooseAction(mon.getMoves(), List.of(mon)));
            try {
                assertTrue(requested.await(2, TimeUnit.SECONDS));
                Move foreign = new Move("Inconnue", Type.FORCE, MoveCategory.PHYSICAL, 50, 100, 0);
                assertThrows(IllegalArgumentException.class, () -> opponent.submitAction(new UseMove(foreign)));
                var valid = new UseMove(mon.getMoves().get(0));
                opponent.submitAction(valid);
                assertSame(valid, result.get(2, TimeUnit.SECONDS));
            } finally {
                opponent.close();
                executor.shutdownNow();
            }
        }
    }

    @Test
    void replacementRequestAcceptsOnlyAStandingTeamMember() throws Exception {
        CountDownLatch requested = new CountDownLatch(1);
        try (NetworkOpponent opponent = new NetworkOpponent(() -> {}, requested::countDown);
                var executor = Executors.newSingleThreadExecutor()) {
            var result = executor.submit(() -> opponent.chooseFakemon(List.of(mon)));
            try {
                assertTrue(requested.await(2, TimeUnit.SECONDS));
                assertThrows(IllegalArgumentException.class,
                        () -> opponent.submitAction(new UseMove(mon.getMoves().get(0))));
                var replacement = new SendFakemon(mon);
                opponent.submitAction(replacement);
                assertSame(replacement, result.get(2, TimeUnit.SECONDS));
            } finally {
                opponent.close();
                executor.shutdownNow();
            }
        }
    }

    @Test
    void closingReleasesTheWaitingCombatThread() throws Exception {
        CountDownLatch requested = new CountDownLatch(1);
        try (NetworkOpponent opponent = new NetworkOpponent(requested::countDown, () -> {});
                var executor = Executors.newSingleThreadExecutor()) {
            var result = executor.submit(() -> opponent.chooseAction(mon.getMoves(), List.of(mon)));
            try {
                assertTrue(requested.await(2, TimeUnit.SECONDS));
                opponent.close();
                ExecutionException error = assertThrows(ExecutionException.class,
                        () -> result.get(2, TimeUnit.SECONDS));
                assertEquals(IllegalStateException.class, error.getCause().getClass());
                assertThrows(IllegalStateException.class, () -> opponent.chooseFakemon(List.of(mon)));
            } finally {
                opponent.close();
                executor.shutdownNow();
            }
        }
    }

    @Test
    void rejectsUnsolicitedActionsAndAllowsSynchronousReplies() {
        NetworkOpponent[] holder = new NetworkOpponent[1];
        UseMove chosen = new UseMove(mon.getMoves().get(0));
        try (NetworkOpponent opponent = new NetworkOpponent(() -> holder[0].submitAction(chosen), () -> {})) {
            holder[0] = opponent;
            assertThrows(IllegalStateException.class, () -> opponent.submitAction(new Forfeit()));
            assertSame(chosen, opponent.chooseAction(mon.getMoves(), List.of(mon)));
            assertSame(chosen, opponent.chooseAction(mon.getMoves(), List.of(mon)));
            assertThrows(IllegalStateException.class, () -> opponent.submitAction(chosen));
        }
    }
}
