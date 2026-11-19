package fr.epitech.fakemon.net;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import fr.epitech.fakemon.core.api.BattleMode;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** Échange la disponibilité, le mode et les choix d'équipe avant de céder la connexion au combat. */
public final class NetworkPreparationSession {
    private final NetworkConnection connection;
    private final boolean hosting;
    private boolean ready;
    private BattleMode mode;

    /**
     * @param connection connexion déjà établie, lue par un seul thread à la fois
     * @param hosting true pour l'hôte, false pour l'invité
     */
    public NetworkPreparationSession(NetworkConnection connection, boolean hosting) {
        this.connection = Objects.requireNonNull(connection);
        this.hosting = hosting;
    }

    /** Attend le signal Prêt côté hôte, sur un thread de travail.
     * @throws IOException si l'invité se déconnecte ou envoie un message inattendu
     */
    public void awaitReady() throws IOException {
        require(hosting && !ready && mode == null);
        receive("READY");
        ready = true;
    }

    /** Signale que l'invité est prêt.
     * @throws IOException si l'envoi échoue
     */
    public void ready() throws IOException {
        require(!hosting && !ready && mode == null);
        connection.send(new NetworkMessage("READY", new JsonObject()));
        ready = true;
    }

    /** Lance la préparation du mode choisi par l'hôte, après le signal Prêt.
     * @param selected mode choisi
     * @throws IOException si l'envoi échoue
     */
    public void start(BattleMode selected) throws IOException {
        require(hosting && ready && mode == null);
        Objects.requireNonNull(selected);
        JsonObject payload = new JsonObject();
        payload.addProperty("mode", selected.name());
        connection.send(new NetworkMessage("PREPARE", payload));
        mode = selected;
    }

    /** Attend le mode choisi côté invité, sur un thread de travail.
     * @return le mode annoncé par l'hôte
     * @throws IOException si le message est invalide ou la connexion fermée
     */
    public BattleMode awaitStart() throws IOException {
        require(!hosting && ready && mode == null);
        JsonElement value = receive("PREPARE").getPayload().get("mode");
        try {
            if (value == null || !value.isJsonPrimitive() || !value.getAsJsonPrimitive().isString()) {
                throw new IllegalArgumentException("Mode manquant.");
            }
            mode = BattleMode.valueOf(value.getAsString());
            return mode;
        } catch (IllegalArgumentException error) {
            throw new IOException("Mode de combat invalide.", error);
        }
    }

    /** Transmet les six espèces de l'invité ; la première est son combattant initial.
     * @param names noms originaux des espèces, sans doublon
     * @throws IOException si l'envoi échoue
     */
    public void sendSelection(List<String> names) throws IOException {
        require(!hosting && mode == BattleMode.CONSTRUCTED);
        validateNames(names);
        JsonArray array = new JsonArray();
        names.forEach(array::add);
        JsonObject payload = new JsonObject();
        payload.add("species", array);
        connection.send(new NetworkMessage("TEAM_SELECTION", payload));
    }

    /** Lit les noms choisis par l'invité ; l'hôte doit ensuite les chercher dans son catalogue.
     * @return six noms distincts, premier combattant en tête
     * @throws IOException si la sélection est invalide ou la connexion fermée
     */
    public List<String> receiveSelection() throws IOException {
        require(hosting && mode == BattleMode.CONSTRUCTED);
        try {
            JsonArray array = receive("TEAM_SELECTION").getPayload().getAsJsonArray("species");
            if (array == null || array.size() != 6) {
                throw new IllegalArgumentException("Six espèces sont nécessaires.");
            }
            List<String> names = new ArrayList<>();
            for (JsonElement value : array) {
                if (!value.isJsonPrimitive() || !value.getAsJsonPrimitive().isString()) {
                    throw new IllegalArgumentException("Un nom d'espèce est attendu.");
                }
                names.add(value.getAsString());
            }
            validateNames(names);
            return List.copyOf(names);
        } catch (IllegalArgumentException | IllegalStateException | ClassCastException error) {
            throw new IOException("Sélection d'équipe invalide.", error);
        }
    }

    private NetworkMessage receive(String expected) throws IOException {
        NetworkMessage message = connection.receive();
        if (!expected.equals(message.getType()) || message.getPayload() == null) {
            throw new IOException("Message de préparation inattendu : " + message.getType());
        }
        return message;
    }

    private static void validateNames(List<String> names) {
        if (names == null || names.size() != 6 || names.stream().anyMatch(name -> name == null || name.isBlank())
                || names.stream().distinct().count() != 6) {
            throw new IllegalArgumentException("Choisis six espèces différentes.");
        }
    }

    private static void require(boolean allowed) {
        if (!allowed) {
            throw new IllegalStateException("Cette action n'est pas attendue pendant la préparation.");
        }
    }
}
