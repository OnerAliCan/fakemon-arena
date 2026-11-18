package fr.epitech.fakemon.net;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import fr.epitech.fakemon.core.api.Forfeit;
import fr.epitech.fakemon.core.api.PlayerAction;
import fr.epitech.fakemon.core.api.SendFakemon;
import fr.epitech.fakemon.core.api.UseMove;
import fr.epitech.fakemon.core.engine.TeamDrafter;
import fr.epitech.fakemon.core.model.Fakemon;
import fr.epitech.fakemon.core.model.Team;
import fr.epitech.fakemon.data.InMemorySpeciesRepository;
import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.Test;

/** Vérifie les demandes numérotées sur une connexion TCP locale. */
class NetworkOpponentChannelTest {
    private final Team guestTeam = new TeamDrafter(new InMemorySpeciesRepository(), new Random(50))
            .randomTeam(List.of());
    private final Fakemon active = guestTeam.getFakemons().get(0);
    private final NetworkActionCodec actions = new NetworkActionCodec();

    @Test
    void exchangesChoicesAndRejectsRepliesFromPreviousRequests() throws Exception {
        try (Link link = new Link(() -> {}); var executor = Executors.newSingleThreadExecutor()) {
            try {
                var first = executor.submit(() -> link.channel.chooseAction(active.getMoves(), guestTeam.getStandingFakemons()));
                NetworkMessage request = link.remote.receive();
                assertEquals("ACTION_REQUIRED", request.getType());
                assertEquals("TURN", request.getPayload().get("reason").getAsString());
                NetworkMessage response = reply(request, new UseMove(active.getMoves().get(0)));
                link.remote.send(response);
                link.channel.receiveAction(link.host.receive());
                assertInstanceOf(UseMove.class, first.get(2, TimeUnit.SECONDS));
                var second = executor.submit(() -> link.channel.chooseFakemon(guestTeam.getStandingFakemons()));
                NetworkMessage replacement = link.remote.receive();
                assertEquals("REPLACEMENT", replacement.getPayload().get("reason").getAsString());
                assertTrue(replacement.getPayload().get("requestId").getAsLong()
                        > request.getPayload().get("requestId").getAsLong());
                assertThrows(IOException.class, () -> link.channel.receiveAction(response));
                NetworkMessage alreadyActive = reply(replacement, new SendFakemon(active));
                assertThrows(IOException.class, () -> link.channel.receiveAction(alreadyActive));
                NetworkMessage wrongKind = reply(replacement, new UseMove(active.getMoves().get(0)));
                assertThrows(IOException.class, () -> link.channel.receiveAction(wrongKind));
                Fakemon substitute = guestTeam.getFakemons().get(1);
                NetworkMessage valid = reply(replacement, new SendFakemon(substitute));
                link.remote.send(valid);
                link.channel.receiveAction(link.host.receive());
                assertEquals(substitute, assertInstanceOf(SendFakemon.class,
                        second.get(2, TimeUnit.SECONDS)).getFakemon());
            } finally {
                link.channel.close();
                executor.shutdownNow();
            }
        }
    }

    @Test
    void forfeitNotifiesTheSessionAndReleasesTheEngine() throws Exception {
        AtomicBoolean forfeited = new AtomicBoolean();
        try (Link link = new Link(() -> forfeited.set(true));
                var executor = Executors.newSingleThreadExecutor()) {
            try {
                var result = executor.submit(() -> link.channel.chooseAction(active.getMoves(), guestTeam.getStandingFakemons()));
                NetworkMessage request = link.remote.receive();
                NetworkMessage response = reply(request, new Forfeit());
                link.remote.send(response);
                link.channel.receiveAction(link.host.receive());
                assertTrue(forfeited.get());
                ExecutionException error = assertThrows(ExecutionException.class,
                        () -> result.get(2, TimeUnit.SECONDS));
                assertInstanceOf(IllegalStateException.class, error.getCause());
                assertThrows(IOException.class, () -> link.channel.receiveAction(response));
            } finally {
                link.channel.close();
                executor.shutdownNow();
            }
        }
    }

    @Test
    void failingForfeitCallbackStillReleasesTheEngine() throws Exception {
        try (Link link = new Link(() -> { throw new IllegalStateException("Session fermée."); });
                var executor = Executors.newSingleThreadExecutor()) {
            try {
                var result = executor.submit(() -> link.channel.chooseAction(active.getMoves(), guestTeam.getStandingFakemons()));
                NetworkMessage request = link.remote.receive();
                NetworkMessage response = reply(request, new Forfeit());
                assertThrows(IllegalStateException.class, () -> link.channel.receiveAction(response));
                assertThrows(ExecutionException.class, () -> result.get(2, TimeUnit.SECONDS));
            } finally {
                link.channel.close();
                executor.shutdownNow();
            }
        }
    }

    private NetworkMessage reply(NetworkMessage request, PlayerAction action) {
        NetworkMessage message = actions.encode(action, guestTeam, active);
        message.getPayload().add("requestId", request.getPayload().get("requestId"));
        return message;
    }

    private final class Link implements AutoCloseable {
        private final NetworkConnection remote;
        private final NetworkConnection host;
        private final NetworkOpponentChannel channel;

        private Link(Runnable onForfeit) throws IOException {
            try (ServerSocket server = new ServerSocket(0, 1, InetAddress.getLoopbackAddress())) {
                Socket client = new Socket(InetAddress.getLoopbackAddress(), server.getLocalPort());
                client.setSoTimeout(2000);
                remote = new NetworkConnection(client);
                Socket accepted = server.accept();
                accepted.setSoTimeout(2000);
                host = new NetworkConnection(accepted);
                channel = new NetworkOpponentChannel(host, guestTeam, () -> active, onForfeit);
            }
        }

        @Override
        public void close() throws IOException {
            try {
                channel.close();
            } finally {
                remote.close();
            }
        }
    }
}
