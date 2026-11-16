package fr.epitech.fakemon.net;

import com.google.gson.Gson;
import fr.epitech.fakemon.core.api.TurnStartedEvent;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;

/** Envoie un événement de début de tour au serveur du spike. */
public class NetworkSpikeClient {

    /**
     * Envoie un événement JSON sur une ligne.
     *
     * @param args arguments inutilisés
     * @throws IOException si la connexion ou l'envoi échoue
     */
    public static void main(String[] args) throws IOException {
        try (Socket socket = new Socket("localhost", 5000);
                PrintWriter writer = new PrintWriter(socket.getOutputStream(), true)) {
            TurnStartedEvent event = new TurnStartedEvent(1);
            writer.println(new Gson().toJson(event));
            if (writer.checkError()) {
                throw new IOException("L'envoi de l'événement a échoué.");
            }
        }
    }
}
