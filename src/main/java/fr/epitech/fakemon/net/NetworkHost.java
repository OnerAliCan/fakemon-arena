package fr.epitech.fakemon.net;

import java.io.IOException;
import java.net.ServerSocket;

/** Attend la connexion d'un invité sur le réseau local. */
public class NetworkHost implements AutoCloseable {
    private final ServerSocket server;

    /**
     * Ouvre le port d'écoute.
     *
     * @param port le port à écouter, ou 0 pour choisir un port disponible
     * @throws IOException si le port ne peut pas être ouvert
     */
    public NetworkHost(int port) throws IOException {
        server = new ServerSocket(port);
    }

    /**
     * Attend un invité depuis le thread réseau.
     *
     * @return la connexion avec l'invité
     * @throws IOException si l'attente échoue ou si l'hôte est fermé
     */
    public NetworkConnection accept() throws IOException {
        return new NetworkConnection(server.accept());
    }

    /** @return le port sur lequel l'hôte attend un invité */
    public int getPort() {
        return server.getLocalPort();
    }

    /**
     * Arrête l'écoute et interrompt une attente de connexion.
     * Une connexion déjà acceptée se ferme avec son propre close().
     *
     * @throws IOException si la fermeture échoue
     */
    @Override
    public void close() throws IOException {
        server.close();
    }
}
