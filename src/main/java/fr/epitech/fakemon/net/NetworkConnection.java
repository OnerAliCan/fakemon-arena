package fr.epitech.fakemon.net;

import com.google.gson.JsonParseException;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

/** Échange un message JSON par ligne sur une connexion TCP. */
public class NetworkConnection implements AutoCloseable {
    private final Socket socket;
    private final BufferedReader reader;
    private final BufferedWriter writer;
    private final NetworkMessageCodec codec = new NetworkMessageCodec();

    /**
     * Prend en charge une connexion déjà ouverte et sa fermeture.
     *
     * @param socket la connexion avec l'autre joueur
     * @throws IOException si les flux ne peuvent pas être ouverts
     */
    public NetworkConnection(Socket socket) throws IOException {
        this.socket = Objects.requireNonNull(socket);
        try {
            InputStreamReader input = new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8);
            reader = new BufferedReader(input);
            OutputStreamWriter output = new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8);
            writer = new BufferedWriter(output);
        } catch (IOException error) {
            try {
                socket.close();
            } catch (IOException closeError) {
                error.addSuppressed(closeError);
            }
            throw error;
        }
    }

    /**
     * Envoie immédiatement un message complet.
     * Les envois simultanés utilisent le même verrou pour conserver des lignes complètes.
     *
     * @param message le message à envoyer
     * @throws IOException si l'envoi échoue
     * @throws JsonParseException si le message est incomplet
     */
    public void send(NetworkMessage message) throws IOException {
        String json = codec.encode(message);
        synchronized (writer) {
            writer.write(json);
            writer.newLine();
            writer.flush();
        }
    }

    /**
     * Attend le prochain message. Un seul thread doit lire cette connexion.
     * Cette méthode s'appelle depuis le thread réseau, car elle attend des données.
     *
     * @return le message reçu
     * @throws EOFException si l'autre joueur ferme sa connexion
     * @throws IOException si la lecture échoue ou le message est invalide
     */
    public NetworkMessage receive() throws IOException {
        String json = reader.readLine();
        if (json == null) {
            throw new EOFException("L'autre joueur a fermé la connexion.");
        }
        try {
            return codec.decode(json);
        } catch (JsonParseException error) {
            throw new IOException("Le message réseau reçu est invalide.", error);
        }
    }

    /**
     * Ferme le socket et ses flux, et interrompt une lecture en attente.
     *
     * @throws IOException si la fermeture échoue
     */
    @Override
    public void close() throws IOException {
        try (socket; reader; writer) {
            socket.close();
        }
    }
}
