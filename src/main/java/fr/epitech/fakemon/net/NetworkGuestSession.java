package fr.epitech.fakemon.net;

import com.google.gson.JsonObject;
import java.io.IOException;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

/**
 * Reçoit le combat de l'hôte et envoie les choix de l'invité sur des threads dédiés.
 * L'appelant transfère les messages reçus sur le thread JavaFX pour les afficher.
 */
public final class NetworkGuestSession implements AutoCloseable {
    private final NetworkConnection connection;
    private final Consumer<NetworkMessage> onMessage;
    private final Consumer<Throwable> onFailure;
    private final boolean keepConnection;
    private final CompletableFuture<NetworkConnection> released = new CompletableFuture<>();
    private final ExecutorService reader = Executors.newSingleThreadExecutor(
            Thread.ofPlatform().daemon().name("lan-guest-reader").factory());
    private final ExecutorService sender = Executors.newSingleThreadExecutor(
            Thread.ofPlatform().daemon().name("lan-guest-sender").factory());
    private final AtomicBoolean started = new AtomicBoolean();
    private final AtomicBoolean closed = new AtomicBoolean();
    private final AtomicLong pendingRequest = new AtomicLong();
    private final AtomicLong submittedRequest = new AtomicLong();
    private volatile boolean battleStarted;
    private volatile boolean battleEnded;
    private volatile boolean replacementRequired;
    private long lastRequest;

    /**
     * Prend en charge la connexion ouverte depuis le lobby.
     *
     * @param connection la connexion avec l'hôte
     * @param onMessage reçoit les messages depuis le thread de lecture
     * @param onFailure reçoit une erreur terminale depuis un thread de travail
     */
    public NetworkGuestSession(NetworkConnection connection, Consumer<NetworkMessage> onMessage,
            Consumer<Throwable> onFailure) {
        this(connection, onMessage, onFailure, false);
    }

    /**
     * Prépare une session pouvant rendre la connexion après le résultat.
     * @param connection connexion avec l'hôte
     * @param onMessage reçoit les messages depuis le thread de lecture
     * @param onFailure reçoit les erreurs terminales
     * @param keepConnection conserve la connexion pour une revanche après confirmation de fin
     */
    public NetworkGuestSession(NetworkConnection connection, Consumer<NetworkMessage> onMessage,
            Consumer<Throwable> onFailure, boolean keepConnection) {
        this.connection = Objects.requireNonNull(connection);
        this.onMessage = Objects.requireNonNull(onMessage);
        this.onFailure = Objects.requireNonNull(onFailure);
        this.keepConnection = keepConnection;
    }

    /** @return la connexion une fois les derniers envois terminés et le lecteur arrêté, ou une erreur */
    public CompletableFuture<NetworkConnection> releaseConnection() {
        return released.copy();
    }

    /** Lance la réception des équipes, des événements et des demandes de choix. */
    public void start() {
        if (closed.get() || !started.compareAndSet(false, true)) {
            throw new IllegalStateException("La session invitée est déjà démarrée ou fermée.");
        }
        reader.execute(this::readMessages);
    }

    /**
     * Envoie un choix numéroté sans bloquer l'écran.
     * L'abandon peut être envoyé dès le début du combat, même sans demande de choix.
     *
     * @param message le message ACTION, avec le requestId reçu pour une attaque ou un changement
     * @return la confirmation de l'envoi, ou l'erreur rencontrée
     */
    public CompletableFuture<Void> submitAction(NetworkMessage message) {
        if (closed.get() || !battleStarted || battleEnded) {
            return CompletableFuture.failedFuture(new IllegalStateException("Le combat est fermé ou pas encore démarré."));
        }
        if (message == null || !"ACTION".equals(message.getType()) || message.getPayload() == null) {
            return CompletableFuture.failedFuture(new IllegalArgumentException("Un message ACTION est attendu."));
        }
        NetworkMessage copy = new NetworkMessage("ACTION", message.getPayload().deepCopy());
        var action = copy.getPayload().get("action");
        if (action == null || !action.isJsonPrimitive() || !action.getAsJsonPrimitive().isString()
                || !("USE_MOVE".equals(action.getAsString()) || "SEND_FAKEMON".equals(action.getAsString())
                || "FORFEIT".equals(action.getAsString()))) {
            return CompletableFuture.failedFuture(new IllegalArgumentException("Action réseau inconnue."));
        }
        boolean forfeit = "FORFEIT".equals(action.getAsString());
        if (!forfeit) {
            long id;
            try {
                id = readRequestId(copy.getPayload());
            } catch (IOException error) {
                return CompletableFuture.failedFuture(error);
            }
            boolean valid = replacementRequired ? pendingRequest.compareAndSet(id, 0)
                    : pendingRequest.get() == id && submittedRequest.compareAndSet(0, id);
            if (!valid) {
                return CompletableFuture.failedFuture(new IllegalStateException("La demande est périmée ou déjà traitée."));
            }
        }
        return sendMessage(copy);
    }

    /**
     * Demande le retrait d'un choix déjà envoyé. Le serveur confirme le résultat.
     *
     * @param requestId le tour concerné
     * @return la confirmation d'envoi de la demande
     */
    public CompletableFuture<Void> cancelAction(long requestId) {
        if (closed.get() || !battleStarted || battleEnded || requestId < 1) {
            return CompletableFuture.failedFuture(new IllegalStateException("Ce choix ne peut plus être annulé."));
        }
        JsonObject payload = new JsonObject();
        payload.addProperty("requestId", requestId);
        return sendMessage(new NetworkMessage("CANCEL_ACTION", payload));
    }

    private CompletableFuture<Void> sendMessage(NetworkMessage message) {
        CompletableFuture<Void> result = new CompletableFuture<>();
        try {
            sender.execute(() -> {
                try {
                    if (closed.get()) {
                        throw new IOException("La session invitée est fermée.");
                    }
                    connection.send(message);
                    result.complete(null);
                } catch (IOException | RuntimeException error) {
                    result.completeExceptionally(error);
                    fail(error);
                }
            });
        } catch (RejectedExecutionException error) {
            result.completeExceptionally(error);
        }
        return result;
    }

    private void readMessages() {
        try {
            while (!closed.get()) {
                NetworkMessage message = connection.receive();
                switch (message.getType()) {
                    case "BATTLE_START" -> {
                        if (battleStarted) {
                            throw new IOException("Le combat a déjà été initialisé.");
                        }
                        battleStarted = true;
                    }
                    case "ACTION_REQUIRED" -> {
                        checkStarted();
                        long id = readRequestId(message.getPayload());
                        var reason = message.getPayload().get("reason");
                        if (id <= lastRequest || reason == null || !reason.isJsonPrimitive()
                                || !reason.getAsJsonPrimitive().isString()
                                || !("TURN".equals(reason.getAsString()) || "REPLACEMENT".equals(reason.getAsString()))) {
                            throw new IOException("La demande de choix est invalide.");
                        }
                        lastRequest = id;
                        replacementRequired = "REPLACEMENT".equals(reason.getAsString());
                        if (!pendingRequest.compareAndSet(0, id)) {
                            throw new IOException("Une réponse de l'invité est déjà attendue.");
                        }
                    }
                    case "CHOICE_STATE" -> {
                        checkStarted();
                        long id = readRequestId(message.getPayload());
                        var state = message.getPayload().get("state");
                        if (id > lastRequest || state == null || !state.isJsonPrimitive()
                                || !state.getAsJsonPrimitive().isString()
                                || !("WAITING".equals(state.getAsString()) || "LOCKED".equals(state.getAsString())
                                || "CANCELLED".equals(state.getAsString()))) {
                            throw new IOException("La confirmation de choix est invalide.");
                        }
                        if ("LOCKED".equals(state.getAsString())) {
                            pendingRequest.compareAndSet(id, 0);
                        }
                        if (!"WAITING".equals(state.getAsString())) {
                            submittedRequest.compareAndSet(id, 0);
                        }
                    }
                    case "EVENT" -> checkStarted();
                    default -> throw new IOException("Type de message inattendu dans le combat.");
                }
                onMessage.accept(message);
                if (isBattleEnded(message)) {
                    battleEnded = true;
                    if (keepConnection) {
                        sendMessage(new NetworkMessage("BATTLE_FINISHED", new JsonObject())).join();
                        reader.shutdown();
                        sender.shutdown();
                        released.complete(connection);
                    } else {
                        close();
                    }
                    return;
                }
            }
        } catch (IOException | RuntimeException error) {
            fail(error);
        }
    }

    private void checkStarted() throws IOException {
        if (!battleStarted) {
            throw new IOException("Les équipes doivent être reçues avant les événements du combat.");
        }
    }

    private boolean isBattleEnded(NetworkMessage message) {
        var event = message.getPayload().get("event");
        return "EVENT".equals(message.getType()) && event != null && event.isJsonPrimitive()
                && event.getAsJsonPrimitive().isString() && "BATTLE_ENDED".equals(event.getAsString());
    }

    private long readRequestId(JsonObject payload) throws IOException {
        var value = payload.get("requestId");
        if (value == null || !value.isJsonPrimitive() || !value.getAsJsonPrimitive().isNumber()) {
            throw new IOException("Le numéro de demande doit être un entier positif.");
        }
        try {
            long id = value.getAsBigDecimal().longValueExact();
            if (id < 1) {
                throw new IOException("Le numéro de demande doit être positif.");
            }
            return id;
        } catch (ArithmeticException | NumberFormatException error) {
            throw new IOException("Le numéro de demande est invalide.", error);
        }
    }

    private void fail(Throwable error) {
        if (closed.get()) {
            return;
        }
        released.completeExceptionally(error);
        try {
            close();
        } catch (IOException closeError) {
            error.addSuppressed(closeError);
        }
        onFailure.accept(error);
    }

    /** @throws IOException si la fermeture de la connexion échoue */
    @Override
    public void close() throws IOException {
        if (closed.compareAndSet(false, true)) {
            released.completeExceptionally(new IOException("La session invitée est fermée."));
            pendingRequest.set(0);
            submittedRequest.set(0);
            try {
                connection.close();
            } finally {
                reader.shutdownNow();
                sender.shutdown();
            }
        }
    }
}
