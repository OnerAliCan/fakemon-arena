package fr.epitech.fakemon.net;

import com.google.gson.JsonObject;

/** Un message réseau, identifié par son type et accompagné de ses données JSON. */
public class NetworkMessage {
    private final String type;
    private final JsonObject payload;

    /**
     * Crée un message à transmettre.
     *
     * @param type le type de message
     * @param payload les données du message
     */
    public NetworkMessage(String type, JsonObject payload) {
        this.type = type;
        this.payload = payload;
    }

    /** @return le type de message */
    public String getType() {
        return type;
    }

    /** @return les données du message */
    public JsonObject getPayload() {
        return payload;
    }
}
