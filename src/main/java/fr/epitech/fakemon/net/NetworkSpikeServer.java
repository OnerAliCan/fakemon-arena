package fr.epitech.fakemon.net;

import com.google.gson.Gson;
import fr.epitech.fakemon.core.api.TurnStartedEvent;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;

/** Reçoit un événement de début de tour pour le spike réseau. */
public class NetworkSpikeServer {

    /**
     * Attend un client et relit son événement JSON.
     *
     * @param args arguments inutilisés
     * @throws IOException si la connexion ou la lecture échoue
     */
    public static void main(String[] args) throws IOException {
        try (ServerSocket host = new ServerSocket(5000);
                Socket client = host.accept();
                BufferedReader reader = new BufferedReader(new InputStreamReader(client.getInputStream()))) {
            String message = reader.readLine();
            TurnStartedEvent event = new Gson().fromJson(message, TurnStartedEvent.class);
            if (event == null) {
                throw new IOException("Aucun événement reçu.");
            }
            System.out.println("JSON reçu : " + message);
            System.out.println("Tour reconstruit : " + event.getTurnNumber());
        }
    }
}
