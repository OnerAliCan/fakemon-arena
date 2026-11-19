package fr.epitech.fakemon.net;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import fr.epitech.fakemon.core.api.BattleMode;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

/** Échange la disponibilité, le mode et les choix d'équipe avant de céder la connexion au combat. */
public final class NetworkPreparationSession implements AutoCloseable {
    private final NetworkConnection connection;
    private final boolean hosting;
    private final BlockingQueue<Reception> messages = new ArrayBlockingQueue<>(8);
    private final CompletableFuture<Void> readerStopped = new CompletableFuture<>();
    private final AtomicBoolean closed = new AtomicBoolean();
    private volatile IOException readFailure;
    private boolean monitoring;
    private boolean selectionValidated;
    private boolean finished;
    private boolean ready;
    private BattleMode mode;

    /**
     * Prend en charge les échanges précédant un combat sur la connexion existante.
     *
     * @param connection connexion déjà établie, lue par un seul thread à la fois
     * @param hosting true pour l'hôte, false pour l'invité
     */
    public NetworkPreparationSession(NetworkConnection connection, boolean hosting) {
        this.connection = Objects.requireNonNull(connection);
        this.hosting = hosting;
    }

    /**
     * Lit aussi lorsque l'écran attend un clic, afin de détecter un départ immédiatement.
     * Le lecteur s'arrête au dernier message de préparation, avant celui du combat.
     *
     * @param onFailure reçoit une erreur depuis le thread réseau, sauf après une fermeture locale
     */
    public void startMonitoring(Consumer<IOException> onFailure) {
        Objects.requireNonNull(onFailure);
        require(!monitoring && !closed.get() && !ready && mode == null);
        monitoring = true;
        Thread.ofPlatform().daemon().name("lan-preparation-reader").start(() -> {
            try {
                String lastMessage = hosting ? "PREPARATION_ACK" : "PREPARATION_END";
                while (!closed.get()) {
                    NetworkMessage message = connection.receive();
                    if (!messages.offer(new Reception(message, null))) {
                        throw new IOException("Trop de messages de préparation en attente.");
                    }
                    if (lastMessage.equals(message.getType())) {
                        return;
                    }
                }
            } catch (IOException | RuntimeException error) {
                if (!closed.get()) {
                    IOException failure = error instanceof IOException io ? io
                            : new IOException("Lecture de la préparation impossible.", error);
                    interruptReception(failure);
                    onFailure.accept(failure);
                }
            } finally {
                readerStopped.complete(null);
            }
        });
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
        selectionValidated = true;
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
            selectionValidated = true;
            return List.copyOf(names);
        } catch (IllegalArgumentException | IllegalStateException | ClassCastException error) {
            throw new IOException("Sélection d'équipe invalide.", error);
        }
    }

    /**
     * Confirme la fin de préparation des deux côtés et attend l'arrêt du lecteur.
     * L'hôte doit appeler cette méthode après avoir validé les deux équipes.
     * Après son retour, le protocole de combat peut reprendre la même connexion.
     *
     * @throws IOException si l'autre joueur quitte la préparation ou si l'attente est interrompue
     */
    public void finish() throws IOException {
        require(mode != null && !finished && (mode == BattleMode.RANDOM || selectionValidated));
        if (hosting) {
            connection.send(new NetworkMessage("PREPARATION_END", new JsonObject()));
            receive("PREPARATION_ACK");
        } else {
            receive("PREPARATION_END");
            connection.send(new NetworkMessage("PREPARATION_ACK", new JsonObject()));
        }
        if (monitoring) {
            try {
                readerStopped.get();
            } catch (InterruptedException error) {
                Thread.currentThread().interrupt();
                throw new IOException("Transfert au combat interrompu.", error);
            } catch (ExecutionException error) {
                throw new IOException("Arrêt du lecteur de préparation impossible.", error.getCause());
            }
        }
        finished = true;
    }

    private NetworkMessage receive(String expected) throws IOException {
        if (readFailure != null) {
            throw readFailure;
        }
        NetworkMessage message;
        if (monitoring) {
            try {
                Reception reception = messages.take();
                if (reception.error() != null) {
                    throw reception.error();
                }
                message = reception.message();
            } catch (InterruptedException error) {
                Thread.currentThread().interrupt();
                throw new IOException("Attente de préparation interrompue.", error);
            }
        } else {
            message = connection.receive();
        }
        if (!expected.equals(message.getType()) || message.getPayload() == null) {
            throw new IOException("Message de préparation inattendu : " + message.getType());
        }
        return message;
    }

    private void interruptReception(IOException error) {
        readFailure = error;
        messages.clear();
        messages.offer(new Reception(null, error));
    }

    /**
     * Ferme la préparation et débloque les attentes locales.
     * Ne doit plus être appelée après avoir transmis la connexion au combat.
     *
     * @throws IOException si la fermeture de la connexion échoue
     */
    @Override
    public void close() throws IOException {
        if (closed.compareAndSet(false, true)) {
            interruptReception(new IOException("La préparation est fermée."));
            connection.close();
        }
    }

    private record Reception(NetworkMessage message, IOException error) { }

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
