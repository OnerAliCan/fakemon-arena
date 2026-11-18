package fr.epitech.fakemon.net;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonObject;
import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;

/** Vérifie l'échange UTF-8 et la fermeture pendant une lecture bloquante. */
class NetworkConnectionTest {
    @Test
    void closingTheSocketReleasesAWaitingReaderAndCanBeRepeated() throws Exception {
        try (ServerSocket server = new ServerSocket(0, 1, InetAddress.getLoopbackAddress());
                Socket client = new Socket(InetAddress.getLoopbackAddress(), server.getLocalPort());
                Socket accepted = server.accept();
                NetworkConnection connection = new NetworkConnection(accepted);
                var executor = Executors.newSingleThreadExecutor()) {
            try {
                CountDownLatch reading = new CountDownLatch(1);
                var response = executor.submit(() -> {
                    reading.countDown();
                    return connection.receive();
                });
                assertTrue(reading.await(2, TimeUnit.SECONDS));
                connection.close();
                ExecutionException error = assertThrows(ExecutionException.class,
                        () -> response.get(2, TimeUnit.SECONDS));
                assertInstanceOf(IOException.class, error.getCause());
                assertTrue(accepted.isClosed());
                connection.close();
            } finally {
                connection.close();
                executor.shutdownNow();
            }
        }
    }

    @Test
    void exchangesAccentsAndEmbeddedLineBreaksBeforeClosingBothConnections() throws Exception {
        try (ServerSocket server = new ServerSocket(0, 1, InetAddress.getLoopbackAddress());
                Socket clientSocket = new Socket(InetAddress.getLoopbackAddress(), server.getLocalPort());
                Socket hostSocket = server.accept();
                NetworkConnection client = new NetworkConnection(clientSocket);
                NetworkConnection host = new NetworkConnection(hostSocket)) {
            clientSocket.setSoTimeout(2000);
            hostSocket.setSoTimeout(2000);
            JsonObject payload = new JsonObject();
            payload.addProperty("texte", "Éclair\nDeuxième ligne");
            host.send(new NetworkMessage("TEST", payload));
            NetworkMessage received = client.receive();
            assertEquals("TEST", received.getType());
            assertEquals(payload, received.getPayload());
            client.send(received);
            assertEquals(payload, host.receive().getPayload());
            client.close();
            host.close();
            assertTrue(clientSocket.isClosed());
            assertTrue(hostSocket.isClosed());
        }
    }
}
