package fr.epitech.fakemon.net;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import fr.epitech.fakemon.core.api.BattleEndReason;
import fr.epitech.fakemon.core.api.BattleEndedEvent;
import fr.epitech.fakemon.core.api.Forfeit;
import fr.epitech.fakemon.core.api.PlayerAction;
import fr.epitech.fakemon.core.api.SendFakemon;
import fr.epitech.fakemon.core.api.Side;
import fr.epitech.fakemon.core.api.UseMove;
import fr.epitech.fakemon.core.engine.TeamDrafter;
import fr.epitech.fakemon.core.model.Fakemon;
import fr.epitech.fakemon.core.model.Item;
import fr.epitech.fakemon.core.model.Team;
import fr.epitech.fakemon.data.InMemorySpeciesRepository;
import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.List;
import java.util.Random;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;

/** Exerce le moteur hébergé avec un invité relié par un socket TCP local. */
class NetworkBattleCoordinatorTest {
    @Test
    void guestSessionReceivesTheBattleAndSubmitsChoicesThroughItsSenderThread() throws Exception {
        try (Session session = new Session()) {
            View guestView = new View(Side.OPPONENT);
            CompletableFuture<View> completed = new CompletableFuture<>();
            NetworkGuestSession[] guest = new NetworkGuestSession[1];
            guest[0] = new NetworkGuestSession(session.remote, message -> {
                guestView.accept(message);
                if ("ACTION_REQUIRED".equals(message.getType())) {
                    guest[0].submitAction(guestView.reply(message)).whenComplete((ignored, error) -> {
                        if (error != null) {
                            completed.completeExceptionally(error);
                        }
                    });
                }
                if (guestView.ended != null) {
                    completed.complete(guestView);
                }
            }, completed::completeExceptionally);
            try (NetworkGuestSession client = guest[0]) {
                client.start();
                session.coordinator.start();
                View host = new View(Side.PLAYER);
                while (host.ended == null) {
                    NetworkMessage message = session.hostMessage();
                    host.accept(message);
                    if ("ACTION_REQUIRED".equals(message.getType())) {
                        session.coordinator.submitHostAction(host.reply(message)).whenComplete((ignored, error) -> {
                            if (error != null) {
                                completed.completeExceptionally(error);
                            }
                        });
                    }
                }
                View remote = completed.get(5, TimeUnit.SECONDS);
                assertTrue(host.ended.getWinner() != remote.ended.getWinner());
                assertEquals(host.state.getTurnNumber(), remote.state.getTurnNumber());
                assertTrue(session.failures.isEmpty(), session.failures.toString());
            }
        }
    }

    @Test
    void playsACompleteSixAgainstSixBattleWithSwitchesAndReplacements() throws Exception {
        try (Session session = new Session(); var executor = Executors.newSingleThreadExecutor()) {
            try {
                View host = new View(Side.PLAYER);
                var guest = executor.submit(() -> {
                    View view = new View(Side.OPPONENT);
                    try {
                        while (view.ended == null) {
                            NetworkMessage message = session.remote.receive();
                            view.accept(message);
                            if ("ACTION_REQUIRED".equals(message.getType())) {
                                session.remote.send(view.reply(message));
                            }
                        }
                    } catch (Exception error) {
                        session.failures.add(error);
                        throw error;
                    }
                    return view;
                });
                session.coordinator.start();
                while (host.ended == null) {
                    NetworkMessage message = session.hostMessage();
                    host.accept(message);
                    if ("ACTION_REQUIRED".equals(message.getType())) {
                        session.coordinator.submitHostAction(host.reply(message)).whenComplete((ignored, error) -> {
                            if (error != null) {
                                session.failures.add(error);
                            }
                        });
                    }
                }
                View remote = guest.get(5, TimeUnit.SECONDS);
                assertEquals(BattleEndReason.ALL_FAINTED, host.ended.getReason());
                assertTrue(host.ended.getWinner() != remote.ended.getWinner());
                assertTrue(host.switched && remote.switched);
                assertTrue(host.replacements + remote.replacements > 0);
                assertEquals(host.state.getTurnNumber(), remote.state.getTurnNumber());
                for (int index = 0; index < 6; index++) {
                    assertEquals(host.state.getTeam(Side.PLAYER).getFakemons().get(index).getCurrentHp(),
                            remote.state.getTeam(Side.OPPONENT).getFakemons().get(index).getCurrentHp());
                    assertEquals(host.state.getTeam(Side.OPPONENT).getFakemons().get(index).getCurrentHp(),
                            remote.state.getTeam(Side.PLAYER).getFakemons().get(index).getCurrentHp());
                }
                assertTrue(session.failures.isEmpty(), session.failures.toString());
            } finally {
                session.coordinator.close();
                executor.shutdownNow();
            }
        }
    }

    @Test
    void guestCanForfeitWhileTheHostIsStillChoosing() throws Exception {
        try (Session session = new Session()) {
            session.coordinator.start();
            View host = new View(Side.PLAYER);
            NetworkMessage request = session.nextRequest(host);
            NetworkMessage forfeit = new NetworkActionCodec().encode(new Forfeit(), null, null);
            session.remote.send(forfeit);
            while (host.ended == null) {
                host.accept(session.hostMessage());
            }
            assertEquals(Side.PLAYER, host.ended.getWinner());
            assertEquals(BattleEndReason.FORFEIT, host.ended.getReason());
            assertThrows(ExecutionException.class, () -> session.coordinator
                    .submitHostAction(host.reply(request)).get(2, TimeUnit.SECONDS));
        }
    }

    @Test
    void rejectsAStaleHostResponseAndStillAcceptsItsCorrectedReply() throws Exception {
        try (Session session = new Session()) {
            session.coordinator.start();
            View host = new View(Side.PLAYER);
            NetworkMessage request = session.nextRequest(host);
            NetworkMessage stale = host.forfeit(request);
            stale.getPayload().addProperty("requestId", 0);
            assertThrows(ExecutionException.class, () -> session.coordinator
                    .submitHostAction(stale).get(2, TimeUnit.SECONDS));
            session.coordinator.submitHostAction(host.forfeit(request)).get(2, TimeUnit.SECONDS);
            while (host.ended == null) {
                host.accept(session.hostMessage());
            }
            assertEquals(Side.OPPONENT, host.ended.getWinner());
            assertTrue(session.failures.isEmpty(), session.failures.toString());
        }
    }

    @Test
    void disconnectReleasesTheEngineAndCompletesQueuedHostReplies() throws Exception {
        try (Session session = new Session()) {
            session.coordinator.start();
            View host = new View(Side.PLAYER);
            NetworkMessage request = session.nextRequest(host);
            var action = session.coordinator.submitHostAction(host.reply(request));
            NetworkMessage remoteMessage;
            do {
                remoteMessage = session.remote.receive();
            } while (!"ACTION_REQUIRED".equals(remoteMessage.getType()));
            var duplicate = session.coordinator.submitHostAction(host.reply(request));
            session.remote.close();
            assertNotNull(session.failures.poll(2, TimeUnit.SECONDS));
            assertThrows(ExecutionException.class, () -> action.get(2, TimeUnit.SECONDS));
            assertThrows(ExecutionException.class, () -> duplicate.get(2, TimeUnit.SECONDS));
        }
    }

    private static final class View {
        private final Side localSide;
        private NetworkEventCodec events;
        private NetworkBattleState state;
        private BattleEndedEvent ended;
        private boolean switched;
        private int replacements;

        private View(Side localSide) {
            this.localSide = localSide;
        }

        private void accept(NetworkMessage message) {
            if ("BATTLE_START".equals(message.getType())) {
                NetworkTeamCodec teams = new NetworkTeamCodec();
                Team host = teams.decode(new NetworkMessage("TEAM",
                        message.getPayload().getAsJsonObject("hostTeam")));
                Team guest = teams.decode(new NetworkMessage("TEAM",
                        message.getPayload().getAsJsonObject("guestTeam")));
                state = localSide == Side.PLAYER ? new NetworkBattleState(host, guest)
                        : new NetworkBattleState(guest, host);
                events = new NetworkEventCodec(host, guest, localSide);
            } else if ("EVENT".equals(message.getType())) {
                events.decode(message).ifPresent(event -> {
                    state.apply(event);
                    if (event instanceof BattleEndedEvent result) {
                        ended = result;
                    }
                });
            }
        }

        private NetworkMessage reply(NetworkMessage request) {
            Fakemon active = state.getActiveFakemon(Side.PLAYER);
            PlayerAction action;
            if ("REPLACEMENT".equals(request.getPayload().get("reason").getAsString())) {
                action = new SendFakemon(state.getTeam(Side.PLAYER).getStandingFakemons().get(0));
                replacements++;
            } else if (!switched) {
                action = new SendFakemon(state.getTeam(Side.PLAYER).getFakemons().get(1));
                switched = true;
            } else {
                action = new UseMove(active.getMoves().get(0));
            }
            NetworkMessage message = new NetworkActionCodec().encode(action, state.getTeam(Side.PLAYER), active);
            message.getPayload().add("requestId", request.getPayload().get("requestId"));
            return message;
        }

        private NetworkMessage forfeit(NetworkMessage request) {
            NetworkMessage message = new NetworkActionCodec().encode(new Forfeit(), null, null);
            message.getPayload().add("requestId", request.getPayload().get("requestId"));
            return message;
        }
    }

    private static final class Session implements AutoCloseable {
        private final BlockingQueue<NetworkMessage> hostMessages = new LinkedBlockingQueue<>();
        private final BlockingQueue<Throwable> failures = new LinkedBlockingQueue<>();
        private final NetworkConnection remote;
        private final NetworkBattleCoordinator coordinator;

        private Session() throws IOException {
            var drafter = new TeamDrafter(new InMemorySpeciesRepository(), new Random(50));
            Team host = drafter.randomTeam(List.of());
            Team guest = drafter.randomTeam(host.getFakemons().stream().map(Fakemon::getSpecies).toList());
            for (Team team : List.of(host, guest)) {
                for (int index = 1; index < team.getFakemons().size(); index++) {
                    team.getFakemons().get(index).equip(Item.values()[index % Item.values().length]);
                }
            }
            try (ServerSocket server = new ServerSocket(0, 1, InetAddress.getLoopbackAddress())) {
                Socket client = new Socket(InetAddress.getLoopbackAddress(), server.getLocalPort());
                client.setSoTimeout(5000);
                remote = new NetworkConnection(client);
                coordinator = new NetworkBattleCoordinator(new NetworkConnection(server.accept()),
                        host, guest, new Random(50), hostMessages::add, failures::add);
            }
        }

        private NetworkMessage hostMessage() throws InterruptedException {
            NetworkMessage message = hostMessages.poll(5, TimeUnit.SECONDS);
            assertNotNull(message, "Aucun message reçu. Erreurs " + failures);
            return message;
        }

        private NetworkMessage nextRequest(View view) throws InterruptedException {
            NetworkMessage message;
            do {
                message = hostMessage();
                view.accept(message);
            } while (!"ACTION_REQUIRED".equals(message.getType()));
            return message;
        }

        @Override
        public void close() throws IOException {
            try {
                coordinator.close();
            } finally {
                remote.close();
            }
        }
    }
}
