package fr.epitech.fakemon.net;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;

/** Ouvre la connexion de l'invité vers l'hôte du combat. */
public final class NetworkClient {
    private static final int CONNECT_TIMEOUT_MILLIS = 5000;

    private NetworkClient() {}

    /**
     * Rejoint l'hôte depuis le thread réseau.
     *
     * @param address l'adresse IP ou le nom de l'hôte
     * @param port le port d'écoute de l'hôte
     * @return la connexion ouverte
     * @throws IOException si la connexion échoue ou dépasse cinq secondes après résolution de l'adresse
     * @throws IllegalArgumentException si l'adresse est vide ou le port invalide
     */
    public static NetworkConnection connect(String address, int port) throws IOException {
        if (address == null || address.isBlank()) {
            throw new IllegalArgumentException("L'adresse de l'hôte est obligatoire.");
        }
        InetSocketAddress target = new InetSocketAddress(address.strip(), port);
        Socket socket = new Socket();
        try {
            socket.connect(target, CONNECT_TIMEOUT_MILLIS);
            return new NetworkConnection(socket);
        } catch (IOException | RuntimeException error) {
            try {
                socket.close();
            } catch (IOException closeError) {
                error.addSuppressed(closeError);
            }
            throw error;
        }
    }
}
