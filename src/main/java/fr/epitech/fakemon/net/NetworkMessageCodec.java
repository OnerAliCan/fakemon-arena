package fr.epitech.fakemon.net;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;

/** Convertit les messages réseau en JSON et relit le JSON reçu. */
public class NetworkMessageCodec {
    private final Gson gson = new GsonBuilder().serializeNulls().create();

    /**
     * Convertit un message en une ligne JSON.
     *
     * @param message le message à envoyer
     * @return sa représentation JSON
     * @throws JsonParseException si le message est incomplet
     */
    public String encode(NetworkMessage message) {
        validate(message);
        return gson.toJson(message);
    }

    /**
     * Reconstruit un message depuis une ligne JSON.
     *
     * @param json le texte reçu
     * @return le message reconstruit
     * @throws JsonParseException si le JSON est invalide ou le message incomplet
     */
    public NetworkMessage decode(String json) {
        NetworkMessage message = gson.fromJson(json, NetworkMessage.class);
        validate(message);
        return message;
    }

    private void validate(NetworkMessage message) {
        if (message == null || message.getType() == null || message.getType().isBlank()
                || message.getPayload() == null) {
            throw new JsonParseException("Un message réseau doit contenir un type et des données.");
        }
    }
}
