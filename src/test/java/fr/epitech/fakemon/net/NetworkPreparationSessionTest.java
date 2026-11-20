package fr.epitech.fakemon.net;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import fr.epitech.fakemon.core.api.BattleMode;
import java.io.EOFException;
import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;

/** Vérifie la préparation sur de vrais sockets, avant le transfert au protocole de combat. */
class NetworkPreparationSessionTest {
    private static final List<String> NAMES = List.of("a", "b", "c", "d", "e", "f");

    @Test
    void bothModesRequireReadinessAndLeaveTheNextMessageForTheBattle() throws Exception {
        for (BattleMode mode : BattleMode.values()) {
            try (Link link = new Link()) {
                assertThrows(IllegalStateException.class, () -> link.host.start(mode));
                assertThrows(IllegalStateException.class, link.guest::awaitStart);
                assertThrows(IllegalStateException.class, link.guest::awaitReady);
                assertThrows(IllegalStateException.class, link.host::ready);
                link.prepare(mode);
                assertThrows(IllegalStateException.class, link.guest::ready);
                assertThrows(IllegalStateException.class, () -> link.host.start(mode));
                link.hostConnection.send(new NetworkMessage("BATTLE_START", new JsonObject()));
                assertEquals("BATTLE_START", link.guestConnection.receive().getType());
            }
        }
    }

    @Test
    void constructedSelectionPreservesTheChosenStarterAndRejectsDuplicates() throws Exception {
        try (Link link = new Link()) {
            link.prepare(BattleMode.CONSTRUCTED);
            assertThrows(IllegalStateException.class, () -> link.host.sendSelection(NAMES));
            assertThrows(IllegalArgumentException.class,
                    () -> link.guest.sendSelection(Collections.nCopies(6, "same")));
            assertThrows(IllegalArgumentException.class, () -> link.guest.sendSelection(List.of("a")));
            List<String> ordered = List.of("f", "a", "b", "c", "d", "e");
            link.guest.sendSelection(ordered);
            assertEquals(ordered, link.host.receiveSelection());
        }
    }

    @Test
    void randomModeDoesNotAcceptTeamSelections() throws Exception {
        try (Link link = new Link()) {
            link.prepare(BattleMode.RANDOM);
            assertThrows(IllegalStateException.class, () -> link.guest.sendSelection(NAMES));
            assertThrows(IllegalStateException.class, link.host::receiveSelection);
        }
    }

    @Test
    void malformedSelectionsFromThePeerAreRejected() throws Exception {
        for (JsonArray names : List.of(array("a", "a", "c", "d", "e", "f"),
                array("a"), array("a", "b", "c", "d", "e", ""))) {
            try (Link link = new Link()) {
                link.prepare(BattleMode.CONSTRUCTED);
                JsonObject payload = new JsonObject();
                payload.add("species", names);
                link.guestConnection.send(new NetworkMessage("TEAM_SELECTION", payload));
                assertThrows(IOException.class, link.host::receiveSelection);
            }
        }
        try (Link link = new Link()) {
            link.prepare(BattleMode.CONSTRUCTED);
            JsonArray values = array("a", "b", "c", "d", "e");
            values.add(12);
            JsonObject payload = new JsonObject();
            payload.add("species", values);
            link.guestConnection.send(new NetworkMessage("TEAM_SELECTION", payload));
            assertThrows(IOException.class, link.host::receiveSelection);
        }
    }

    @Test
    void invalidModesAndUnexpectedMessagesAreRejected() throws Exception {
        for (String mode : List.of("UNKNOWN", "")) {
            try (Link link = new Link()) {
                link.guest.ready();
                link.host.awaitReady();
                JsonObject payload = new JsonObject();
                payload.addProperty("mode", mode);
                link.hostConnection.send(new NetworkMessage("PREPARE", payload));
                assertThrows(IOException.class, link.guest::awaitStart);
            }
        }
        try (Link link = new Link()) {
            link.guestConnection.send(new NetworkMessage("BATTLE_START", new JsonObject()));
            assertThrows(IOException.class, link.host::awaitReady);
        }
    }

    @Test
    void disconnectInterruptsPreparation() throws Exception {
        try (Link link = new Link()) {
            link.guestConnection.close();
            assertThrows(IOException.class, link.host::awaitReady);
        }
    }

    @Test
    void hostDepartureIsDetectedBeforeTheGuestClicksReady() throws Exception {
        try (Link link = new Link()) {
            var failure = new CompletableFuture<IOException>();
            link.guest.startMonitoring(failure::complete);
            link.hostConnection.close();
            assertInstanceOf(EOFException.class, failure.get(2, TimeUnit.SECONDS));
        }
    }

    @Test
    void guestDepartureIsDetectedAfterReadinessWhileTheHostIsIdle() throws Exception {
        try (Link link = new Link()) {
            var failure = new CompletableFuture<IOException>();
            link.host.startMonitoring(failure::complete);
            link.guest.ready();
            link.host.awaitReady();
            link.guestConnection.close();
            assertInstanceOf(EOFException.class, failure.get(2, TimeUnit.SECONDS));
        }
    }

    @Test
    void departuresAreDetectedThroughoutConstructedSelection() throws Exception {
        try (Link link = new Link()) {
            var failure = new CompletableFuture<IOException>();
            link.guest.startMonitoring(failure::complete);
            link.prepare(BattleMode.CONSTRUCTED);
            link.hostConnection.close();
            assertInstanceOf(EOFException.class, failure.get(2, TimeUnit.SECONDS));
        }
        try (Link link = new Link()) {
            var failure = new CompletableFuture<IOException>();
            link.host.startMonitoring(failure::complete);
            link.prepare(BattleMode.CONSTRUCTED);
            link.guest.sendSelection(NAMES);
            assertEquals(NAMES, link.host.receiveSelection());
            link.guestConnection.close();
            assertInstanceOf(EOFException.class, failure.get(2, TimeUnit.SECONDS));
        }
    }

    @Test
    void handoverStopsBothReadersBeforeTheFirstBattleMessages() throws Exception {
        for (BattleMode mode : BattleMode.values()) {
            try (Link link = new Link(); var worker = Executors.newSingleThreadExecutor()) {
                var hostFailure = new CompletableFuture<IOException>();
                var guestFailure = new CompletableFuture<IOException>();
                link.host.startMonitoring(hostFailure::complete);
                link.guest.startMonitoring(guestFailure::complete);
                link.prepare(mode);
                if (mode == BattleMode.CONSTRUCTED) {
                    assertThrows(IllegalStateException.class, link.host::finish);
                    link.guest.sendSelection(NAMES);
                    assertEquals(NAMES, link.host.receiveSelection());
                }
                var guestFinished = worker.submit(() -> {
                    link.guest.finish();
                    return null;
                });
                link.host.finish();
                link.hostConnection.send(new NetworkMessage("BATTLE_START", new JsonObject()));
                guestFinished.get(2, TimeUnit.SECONDS);
                assertEquals("BATTLE_START", link.guestConnection.receive().getType());
                link.guestConnection.send(new NetworkMessage("ACTION", new JsonObject()));
                assertEquals("ACTION", link.hostConnection.receive().getType());
                assertFalse(hostFailure.isDone());
                assertFalse(guestFailure.isDone());
                assertThrows(IllegalStateException.class, link.host::finish);
            }
        }
    }

    @Test
    void localCloseInterruptsAWaitingConsumerWithoutReportingAPeerDeparture() throws Exception {
        try (Link link = new Link(); var worker = Executors.newSingleThreadExecutor()) {
            var failure = new CompletableFuture<IOException>();
            link.host.startMonitoring(failure::complete);
            var waiting = worker.submit(() -> {
                link.host.awaitReady();
                return null;
            });
            link.host.close();
            ExecutionException interrupted = assertThrows(ExecutionException.class,
                    () -> waiting.get(2, TimeUnit.SECONDS));
            assertInstanceOf(IOException.class, interrupted.getCause());
            assertFalse(failure.isDone());
        }
    }

    private static JsonArray array(String... values) {
        JsonArray array = new JsonArray();
        for (String value : values) {
            array.add(value);
        }
        return array;
    }

    private static final class Link implements AutoCloseable {
        private final NetworkConnection hostConnection;
        private final NetworkConnection guestConnection;
        private final NetworkPreparationSession host;
        private final NetworkPreparationSession guest;

        private Link() throws IOException {
            try (ServerSocket server = new ServerSocket(0, 1, InetAddress.getLoopbackAddress())) {
                Socket client = new Socket(InetAddress.getLoopbackAddress(), server.getLocalPort());
                client.setSoTimeout(2000);
                Socket accepted = server.accept();
                accepted.setSoTimeout(2000);
                hostConnection = new NetworkConnection(accepted);
                guestConnection = new NetworkConnection(client);
            }
            host = new NetworkPreparationSession(hostConnection, true);
            guest = new NetworkPreparationSession(guestConnection, false);
        }

        private void prepare(BattleMode mode) throws IOException {
            guest.ready();
            host.awaitReady();
            host.start(mode);
            assertEquals(mode, guest.awaitStart());
        }

        @Override
        public void close() throws IOException {
            try (host; guest) { }
        }
    }
}
