package fr.epitech.fakemon.net;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import com.google.gson.JsonObject;

import fr.epitech.fakemon.core.api.BattleEndReason;
import fr.epitech.fakemon.core.api.BattleEndedEvent;
import fr.epitech.fakemon.core.api.BattleMode;
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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

/** Exerce le moteur hébergé avec un invité relié par un socket TCP local. */
class NetworkBattleCoordinatorTest {
    @ParameterizedTest
    @EnumSource(BattleMode.class)
    void aFullBattleCanBeFollowedByPreparationAndAnotherBattleOnTheSameConnection(BattleMode mode) throws Exception {
        try (Session session = new Session(mode, true);
                NetworkGuestSession firstGuest = automaticGuest(session.remote, session.failures, true)) {
            firstGuest.start();
            session.coordinator.start();
            View first = playToEnd(session.coordinator, session.hostMessages, session.failures);
            assertEquals(BattleEndReason.ALL_FAINTED, first.ended.getReason());
            NetworkConnection hostConnection = session.coordinator.releaseConnection().get(2, TimeUnit.SECONDS);
            NetworkConnection guestConnection = firstGuest.releaseConnection().get(2, TimeUnit.SECONDS);
            assertSame(session.remote, guestConnection);

            var hostPreparation = new NetworkPreparationSession(hostConnection, true);
            var guestPreparation = new NetworkPreparationSession(guestConnection, false);
            guestPreparation.ready();
            hostPreparation.awaitReady();
            hostPreparation.start(mode);
            assertEquals(mode, guestPreparation.awaitStart());
            if (mode == BattleMode.CONSTRUCTED) {
                var names = new InMemorySpeciesRepository().findAll().subList(0, 6).stream()
                        .map(species -> species.getOriginalName()).toList();
                guestPreparation.sendSelection(names);
                assertEquals(names, hostPreparation.receiveSelection());
            }

            var drafter = new TeamDrafter(new InMemorySpeciesRepository(), new Random(71));
            Team hostTeam = drafter.randomTeam(List.of());
            Team guestTeam = drafter.randomTeam(hostTeam.getFakemons().stream().map(Fakemon::getSpecies).toList());
            try (var second = new NetworkBattleCoordinator(hostConnection, hostTeam, guestTeam, mode, new Random(71),
                        session.hostMessages::add, session.failures::add);
                    var secondGuest = automaticGuest(guestConnection, session.failures, false)) {
                secondGuest.start();
                second.start();
                View replay = playToEnd(second, session.hostMessages, session.failures);
                assertEquals(BattleEndReason.ALL_FAINTED, replay.ended.getReason());
                assertTrue(session.failures.isEmpty(), session.failures.toString());
            }
        }
    }

    @ParameterizedTest
    @EnumSource(Side.class)
    void eitherPlayerCanForfeitAndStillReleaseTheConnectionForRematch(Side forfeiting) throws Exception {
        try (Session session = new Session(BattleMode.RANDOM, true)) {
            var messages = new LinkedBlockingQueue<NetworkMessage>();
            try (var guest = new NetworkGuestSession(session.remote, messages::add, session.failures::add, true)) {
                guest.start();
                session.coordinator.start();
                View hostView = new View(Side.PLAYER);
                View guestView = new View(Side.OPPONENT);
                NetworkMessage hostRequest = session.nextRequest(hostView);
                NetworkMessage guestRequest = nextMessage(messages, guestView, "ACTION_REQUIRED", null);
                if (forfeiting == Side.PLAYER) {
                    session.coordinator.submitHostAction(hostView.forfeit(hostRequest)).get(2, TimeUnit.SECONDS);
                } else {
                    guest.submitAction(guestView.forfeit(guestRequest)).get(2, TimeUnit.SECONDS);
                }
                NetworkConnection hostConnection = session.coordinator.releaseConnection().get(2, TimeUnit.SECONDS);
                NetworkConnection guestConnection = guest.releaseConnection().get(2, TimeUnit.SECONDS);
                assertThrows(ExecutionException.class,
                        () -> guest.submitAction(guestView.forfeit(guestRequest)).get(2, TimeUnit.SECONDS));
                var hostPreparation = new NetworkPreparationSession(hostConnection, true);
                var guestPreparation = new NetworkPreparationSession(guestConnection, false);
                guestPreparation.ready();
                hostPreparation.awaitReady();
                assertTrue(session.failures.isEmpty(), session.failures.toString());
            }
        }
    }

    @Test
    void aPrematureEndAcknowledgementFailsInsteadOfReleasingTheConnection() throws Exception {
        try (Session session = new Session(BattleMode.RANDOM, true)) {
            session.coordinator.start();
            session.remote.send(new NetworkMessage("BATTLE_FINISHED", new JsonObject()));
            assertThrows(ExecutionException.class,
                    () -> session.coordinator.releaseConnection().get(2, TimeUnit.SECONDS));
        }
    }

    @Test
    void leavingBeforeEndAcknowledgementCancelsRematch() throws Exception {
        try (Session session = new Session(BattleMode.RANDOM, true)) {
            session.coordinator.start();
            View host = new View(Side.PLAYER);
            NetworkMessage request = session.nextRequest(host);
            session.coordinator.submitHostAction(host.forfeit(request)).get(2, TimeUnit.SECONDS);
            session.remote.close();
            assertThrows(ExecutionException.class,
                    () -> session.coordinator.releaseConnection().get(2, TimeUnit.SECONDS));
        }
    }

    private static NetworkGuestSession automaticGuest(NetworkConnection connection,
            BlockingQueue<Throwable> failures, boolean keepConnection) {
        View view = new View(Side.OPPONENT);
        NetworkGuestSession[] session = new NetworkGuestSession[1];
        session[0] = new NetworkGuestSession(connection, message -> {
            view.accept(message);
            if ("ACTION_REQUIRED".equals(message.getType())) {
                session[0].submitAction(view.reply(message)).whenComplete((ignored, error) -> {
                    if (error != null) {
                        failures.add(error);
                    }
                });
            }
        }, failures::add, keepConnection);
        return session[0];
    }

    private static View playToEnd(NetworkBattleCoordinator coordinator, BlockingQueue<NetworkMessage> messages,
            BlockingQueue<Throwable> failures) throws Exception {
        View host = new View(Side.PLAYER);
        while (host.ended == null) {
            NetworkMessage message = messages.poll(5, TimeUnit.SECONDS);
            assertNotNull(message, "Combat interrompu : " + failures);
            host.accept(message);
            if ("ACTION_REQUIRED".equals(message.getType())) {
                coordinator.submitHostAction(host.reply(message)).get(2, TimeUnit.SECONDS);
            }
        }
        return host;
    }

    @Test
    void constructedBattleStartsWithBothChosenStartersAndAcceptsSimultaneousActions() throws Exception {
        try (Session session = new Session(BattleMode.CONSTRUCTED)) {
            session.coordinator.start();
            View host = new View(Side.PLAYER);
            View guest = new View(Side.OPPONENT);
            NetworkMessage hostRequest = session.nextRequest(host);
            NetworkMessage guestRequest = session.nextRemoteRequest(guest);
            assertEquals("TURN", hostRequest.getPayload().get("reason").getAsString());
            assertEquals("TURN", guestRequest.getPayload().get("reason").getAsString());
            assertSame(host.state.getTeam(Side.PLAYER).getFakemons().getFirst(),
                    host.state.getActiveFakemon(Side.PLAYER));
            assertSame(guest.state.getTeam(Side.PLAYER).getFakemons().getFirst(),
                    guest.state.getActiveFakemon(Side.PLAYER));
            session.coordinator.submitHostAction(attack(host, hostRequest)).get(2, TimeUnit.SECONDS);
            session.remote.send(attack(guest, guestRequest));
            session.nextRequest(host);
            session.nextRemoteRequest(guest);
            assertEquals(2, host.state.getTurnNumber());
            assertEquals(host.state.getTurnNumber(), guest.state.getTurnNumber());
            assertTrue(session.failures.isEmpty(), session.failures.toString());
        }
    }

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

    @ParameterizedTest
    @EnumSource(BattleMode.class)
    void playsACompleteSixAgainstSixBattleWithSwitchesAndReplacements(BattleMode mode) throws Exception {
        try (Session session = new Session(mode); var executor = Executors.newSingleThreadExecutor()) {
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
    void disconnectWhileChoosingClosesTheSession() throws Exception {
        try (Session session = new Session()) {
            session.coordinator.start();
            View host = new View(Side.PLAYER);
            NetworkMessage request = session.nextRequest(host);
            var action = session.coordinator.submitHostAction(host.reply(request));
            action.get(2, TimeUnit.SECONDS);
            NetworkMessage remoteMessage;
            do {
                remoteMessage = session.remote.receive();
            } while (!"ACTION_REQUIRED".equals(remoteMessage.getType()));
            session.remote.close();
            assertNotNull(session.failures.poll(2, TimeUnit.SECONDS));
            assertThrows(ExecutionException.class, () -> session.coordinator
                    .submitHostAction(host.reply(request)).get(2, TimeUnit.SECONDS));
        }
    }

    @Test
    void guestCanChooseFirstAndNoTurnRunsBeforeBothChoices() throws Exception {
        try (Session session = new Session()) {
            session.coordinator.start();
            View host = new View(Side.PLAYER);
            View guest = new View(Side.OPPONENT);
            NetworkMessage hostRequest = session.nextRequest(host);
            NetworkMessage guestRequest = session.nextRemoteRequest(guest);
            session.remote.send(guest.reply(guestRequest));
            session.remoteState(guest, "WAITING");
            assertEquals(1, host.state.getTurnNumber());
            assertNull(session.hostMessages.poll(100, TimeUnit.MILLISECONDS));
            session.coordinator.submitHostAction(host.reply(hostRequest)).get(2, TimeUnit.SECONDS);
            session.nextRequest(host);
            assertEquals(2, host.state.getTurnNumber());
            assertTrue(session.failures.isEmpty(), session.failures.toString());
        }
    }

    @Test
    void guestSessionCanCancelAndALateCancellationDoesNotDisconnectIt() throws Exception {
        try (Session session = new Session()) {
            BlockingQueue<NetworkMessage> messages = new LinkedBlockingQueue<>();
            try (NetworkGuestSession client = new NetworkGuestSession(session.remote, messages::add, session.failures::add)) {
                client.start();
                session.coordinator.start();
                View host = new View(Side.PLAYER);
                View guest = new View(Side.OPPONENT);
                NetworkMessage hostRequest = session.nextRequest(host);
                NetworkMessage guestRequest = nextMessage(messages, guest, "ACTION_REQUIRED", null);
                client.submitAction(guest.reply(guestRequest)).get(2, TimeUnit.SECONDS);
                assertThrows(ExecutionException.class,
                        () -> client.submitAction(attack(guest, guestRequest)).get(2, TimeUnit.SECONDS));
                nextMessage(messages, guest, "CHOICE_STATE", "WAITING");
                client.cancelAction(id(guestRequest)).get(2, TimeUnit.SECONDS);
                nextMessage(messages, guest, "CHOICE_STATE", "CANCELLED");
                client.submitAction(attack(guest, guestRequest)).get(2, TimeUnit.SECONDS);
                session.coordinator.submitHostAction(attack(host, hostRequest)).get(2, TimeUnit.SECONDS);
                session.nextRequest(host);
                nextMessage(messages, guest, "ACTION_REQUIRED", null);
                client.cancelAction(id(guestRequest)).get(2, TimeUnit.SECONDS);
                nextMessage(messages, guest, "CHOICE_STATE", "LOCKED");
                assertTrue(session.failures.isEmpty(), session.failures.toString());
                assertEquals(2, guest.state.getTurnNumber());
            }
        }
    }

    private static NetworkMessage nextMessage(BlockingQueue<NetworkMessage> messages, View view,
            String type, String state) throws InterruptedException {
        while (true) {
            NetworkMessage message = messages.poll(2, TimeUnit.SECONDS);
            assertNotNull(message, "Message attendu " + type);
            view.accept(message);
            if (type.equals(message.getType())
                    && (state == null || state.equals(message.getPayload().get("state").getAsString()))) {
                return message;
            }
        }
    }

    @Test
    void hostCanCancelASwitchAndChooseAnAttackInstead() throws Exception {
        try (Session session = new Session()) {
            session.coordinator.start();
            View host = new View(Side.PLAYER);
            View guest = new View(Side.OPPONENT);
            NetworkMessage hostRequest = session.nextRequest(host);
            NetworkMessage guestRequest = session.nextRemoteRequest(guest);
            Fakemon original = host.state.getActiveFakemon(Side.PLAYER);
            session.coordinator.submitHostAction(host.reply(hostRequest)).get(2, TimeUnit.SECONDS);
            assertTrue(session.coordinator.cancelHostAction(id(hostRequest)).get(2, TimeUnit.SECONDS));
            session.coordinator.submitHostAction(attack(host, hostRequest)).get(2, TimeUnit.SECONDS);
            session.remote.send(attack(guest, guestRequest));
            session.nextRequest(host);
            assertSame(original, host.state.getActiveFakemon(Side.PLAYER));
            assertEquals(2, host.state.getTurnNumber());
            assertFalse(session.coordinator.cancelHostAction(id(hostRequest)).get(2, TimeUnit.SECONDS));
        }
    }

    @Test
    void guestCanCancelASwitchAndChooseAnAttackInstead() throws Exception {
        try (Session session = new Session()) {
            session.coordinator.start();
            View host = new View(Side.PLAYER);
            View guest = new View(Side.OPPONENT);
            NetworkMessage hostRequest = session.nextRequest(host);
            NetworkMessage guestRequest = session.nextRemoteRequest(guest);
            Fakemon original = host.state.getActiveFakemon(Side.OPPONENT);
            session.remote.send(guest.reply(guestRequest));
            session.remoteState(guest, "WAITING");
            session.remote.send(cancel(guestRequest));
            session.remoteState(guest, "CANCELLED");
            session.remote.send(attack(guest, guestRequest));
            session.remoteState(guest, "WAITING");
            session.coordinator.submitHostAction(attack(host, hostRequest)).get(2, TimeUnit.SECONDS);
            session.nextRequest(host);
            assertSame(original, host.state.getActiveFakemon(Side.OPPONENT));
            assertEquals(2, host.state.getTurnNumber());
        }
    }

    @Test
    void cancellationAfterTheSecondChoiceCannotUndoTheResolvedTurn() throws Exception {
        try (Session session = new Session()) {
            session.coordinator.start();
            View host = new View(Side.PLAYER);
            View guest = new View(Side.OPPONENT);
            NetworkMessage hostRequest = session.nextRequest(host);
            NetworkMessage guestRequest = session.nextRemoteRequest(guest);
            session.coordinator.submitHostAction(attack(host, hostRequest)).get(2, TimeUnit.SECONDS);
            session.remote.send(attack(guest, guestRequest));
            session.remote.send(cancel(guestRequest));
            session.nextRequest(host);
            assertEquals(2, host.state.getTurnNumber());
            assertFalse(session.coordinator.cancelHostAction(id(hostRequest)).get(2, TimeUnit.SECONDS));
            session.remoteState(guest, "LOCKED");
            assertTrue(session.failures.isEmpty(), session.failures.toString());
        }
    }

    private static long id(NetworkMessage request) {
        return request.getPayload().get("requestId").getAsLong();
    }

    @Test
    void repeatingAGuestCancellationLeavesTheChoiceAvailable() throws Exception {
        try (Session session = new Session()) {
            session.coordinator.start();
            View host = new View(Side.PLAYER);
            View guest = new View(Side.OPPONENT);
            NetworkMessage hostRequest = session.nextRequest(host);
            NetworkMessage guestRequest = session.nextRemoteRequest(guest);
            session.remote.send(guest.reply(guestRequest));
            session.remoteState(guest, "WAITING");
            session.remote.send(cancel(guestRequest));
            session.remoteState(guest, "CANCELLED");
            session.remote.send(cancel(guestRequest));
            NetworkMessage repeated = session.nextRemoteChoiceState(guest);
            assertEquals("CANCELLED", repeated.getPayload().get("state").getAsString());
            session.coordinator.submitHostAction(attack(host, hostRequest)).get(2, TimeUnit.SECONDS);
            session.remote.send(attack(guest, guestRequest));
            session.nextRequest(host);
            assertEquals(2, host.state.getTurnNumber());
            assertTrue(session.failures.isEmpty(), session.failures.toString());
        }
    }

    @Test
    void duplicateHostChoiceIsRejectedWithoutClosingTheSession() throws Exception {
        try (Session session = new Session()) {
            session.coordinator.start();
            View host = new View(Side.PLAYER);
            View guest = new View(Side.OPPONENT);
            NetworkMessage hostRequest = session.nextRequest(host);
            NetworkMessage guestRequest = session.nextRemoteRequest(guest);
            session.coordinator.submitHostAction(attack(host, hostRequest)).get(2, TimeUnit.SECONDS);
            assertThrows(ExecutionException.class, () -> session.coordinator
                    .submitHostAction(attack(host, hostRequest)).get(2, TimeUnit.SECONDS));
            assertTrue(session.failures.isEmpty(), session.failures.toString());
            session.remote.send(attack(guest, guestRequest));
            session.nextRequest(host);
            assertEquals(2, host.state.getTurnNumber());
        }
    }

    private static NetworkMessage cancel(NetworkMessage request) {
        JsonObject payload = new JsonObject();
        payload.addProperty("requestId", id(request));
        return new NetworkMessage("CANCEL_ACTION", payload);
    }

    private static NetworkMessage attack(View view, NetworkMessage request) {
        Fakemon active = view.state.getActiveFakemon(Side.PLAYER);
        NetworkMessage message = new NetworkActionCodec().encode(
                new UseMove(active.getMoves().get(0)), view.state.getTeam(Side.PLAYER), active);
        message.getPayload().addProperty("requestId", id(request));
        return message;
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
            this(BattleMode.RANDOM);
        }

        private Session(BattleMode mode) throws IOException {
            this(mode, false);
        }

        private Session(BattleMode mode, boolean keepConnection) throws IOException {
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
                        host, guest, mode, new Random(50), hostMessages::add, failures::add, keepConnection);
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

        private NetworkMessage nextRemoteRequest(View view) throws IOException {
            NetworkMessage message;
            do {
                message = remote.receive();
                view.accept(message);
            } while (!"ACTION_REQUIRED".equals(message.getType()));
            return message;
        }

        private void remoteState(View view, String state) throws IOException {
            NetworkMessage message;
            do {
                message = remote.receive();
                view.accept(message);
            } while (!"CHOICE_STATE".equals(message.getType())
                    || !state.equals(message.getPayload().get("state").getAsString()));
        }

        private NetworkMessage nextRemoteChoiceState(View view) throws IOException {
            NetworkMessage message;
            do {
                message = remote.receive();
                view.accept(message);
            } while (!"CHOICE_STATE".equals(message.getType()));
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
