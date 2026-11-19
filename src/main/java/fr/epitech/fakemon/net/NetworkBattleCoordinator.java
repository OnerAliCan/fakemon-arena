package fr.epitech.fakemon.net;

import com.google.gson.JsonObject;
import fr.epitech.fakemon.core.api.BattleEndReason;
import fr.epitech.fakemon.core.api.BattleEndedEvent;
import fr.epitech.fakemon.core.api.BattleEvent;
import fr.epitech.fakemon.core.api.BattleMode;
import fr.epitech.fakemon.core.api.FakemonSelectionRequiredEvent;
import fr.epitech.fakemon.core.api.Forfeit;
import fr.epitech.fakemon.core.api.PlayerAction;
import fr.epitech.fakemon.core.api.Opponent;
import fr.epitech.fakemon.core.api.SendFakemon;
import fr.epitech.fakemon.core.api.SelectionReason;
import fr.epitech.fakemon.core.api.Side;
import fr.epitech.fakemon.core.engine.DefaultBattleEngine;
import fr.epitech.fakemon.core.model.Team;
import fr.epitech.fakemon.core.model.Fakemon;
import fr.epitech.fakemon.core.model.Move;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Objects;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

/**
 * Exécute le combat LAN sur l'hôte avec un thread moteur et un lecteur réseau.
 * Les messages destinés à l'écran doivent être transférés sur le thread JavaFX par l'appelant.
 */
public final class NetworkBattleCoordinator implements AutoCloseable {
    private final NetworkConnection connection;
    private final Team hostTeam;
    private final Team guestTeam;
    private final Random random;
    private final BattleMode mode;
    private final boolean keepConnection;
    private final CompletableFuture<NetworkConnection> released = new CompletableFuture<>();
    private final Consumer<NetworkMessage> onHostMessage;
    private final Consumer<Throwable> onFailure;
    private final DefaultBattleEngine engine = new DefaultBattleEngine();
    private final NetworkOpponentChannel opponent;
    private final NetworkEventCodec events;
    private final NetworkActionCodec actions = new NetworkActionCodec();
    private final ExecutorService battleThread = Executors.newSingleThreadExecutor(
            Thread.ofPlatform().daemon().name("lan-battle").factory());
    private final ExecutorService readerThread = Executors.newSingleThreadExecutor(
            Thread.ofPlatform().daemon().name("lan-reader").factory());
    private final AtomicBoolean started = new AtomicBoolean();
    private final AtomicBoolean closed = new AtomicBoolean();
    private final AtomicBoolean finished = new AtomicBoolean();
    private final Object publishing = new Object();
    private final AtomicLong requestNumber = new AtomicLong();
    private long hostRequest;
    private long guestRequest;
    private PlayerAction hostChoice;
    private PlayerAction guestChoice;
    private PlayerAction resolvedGuestAction;
    private boolean hostNeedsReplacement;
    private volatile boolean guestNeedsReplacement;

    /**
     * Prépare un combat aléatoire avec les deux équipes déjà tirées par l'hôte.
     *
     * @param connection la connexion avec l'invité, prise en charge par ce coordinateur
     * @param hostTeam l'équipe de l'hôte
     * @param guestTeam l'équipe de l'invité
     * @param random la source de hasard du moteur
     * @param onHostMessage reçoit les messages à afficher côté hôte depuis un thread de travail
     * @param onFailure reçoit une erreur terminale depuis un thread de travail
     */
    public NetworkBattleCoordinator(NetworkConnection connection, Team hostTeam, Team guestTeam, Random random,
            Consumer<NetworkMessage> onHostMessage, Consumer<Throwable> onFailure) {
        this(connection, hostTeam, guestTeam, BattleMode.RANDOM, random, onHostMessage, onFailure);
    }

    /**
     * Prépare le combat avec les premiers combattants déjà choisis en mode construit.
     * @param connection connexion avec l'invité
     * @param hostTeam équipe de l'hôte, premier combattant en tête
     * @param guestTeam équipe de l'invité, premier combattant en tête
     * @param mode mode choisi dans le lobby
     * @param random source de hasard du moteur
     * @param onHostMessage reçoit les messages depuis un thread de travail
     * @param onFailure reçoit une erreur terminale depuis un thread de travail
     */
    public NetworkBattleCoordinator(NetworkConnection connection, Team hostTeam, Team guestTeam, BattleMode mode,
            Random random, Consumer<NetworkMessage> onHostMessage, Consumer<Throwable> onFailure) {
        this(connection, hostTeam, guestTeam, mode, random, onHostMessage, onFailure, false);
    }

    /**
     * Prépare un combat pouvant céder sa connexion après le résultat.
     * @param connection connexion avec l'invité
     * @param hostTeam équipe de l'hôte, premier combattant en tête
     * @param guestTeam équipe de l'invité, premier combattant en tête
     * @param mode mode choisi dans le lobby
     * @param random source de hasard du moteur
     * @param onHostMessage reçoit les messages depuis un thread de travail
     * @param onFailure reçoit les erreurs terminales
     * @param keepConnection conserve la connexion pour une revanche après confirmation de fin
     */
    public NetworkBattleCoordinator(NetworkConnection connection, Team hostTeam, Team guestTeam, BattleMode mode,
            Random random, Consumer<NetworkMessage> onHostMessage, Consumer<Throwable> onFailure,
            boolean keepConnection) {
        this.connection = Objects.requireNonNull(connection);
        this.hostTeam = Objects.requireNonNull(hostTeam);
        this.guestTeam = Objects.requireNonNull(guestTeam);
        this.random = Objects.requireNonNull(random);
        this.mode = Objects.requireNonNull(mode);
        this.keepConnection = keepConnection;
        this.onHostMessage = Objects.requireNonNull(onHostMessage);
        this.onFailure = Objects.requireNonNull(onFailure);
        events = new NetworkEventCodec(hostTeam, guestTeam, Side.PLAYER);
        opponent = new NetworkOpponentChannel(connection, guestTeam,
                () -> engine.getActiveFakemon(Side.OPPONENT), () -> finish(Side.PLAYER),
                requestNumber::incrementAndGet);
        engine.addListener(this::publishEvent);
    }

    /** @return la connexion après confirmation du résultat par l'invité et arrêt de l'ancien lecteur */
    public CompletableFuture<NetworkConnection> releaseConnection() {
        return released.copy();
    }

    /** Envoie les équipes initiales, démarre le moteur et lance la lecture des réponses. */
    public void start() {
        if (closed.get() || !started.compareAndSet(false, true)) {
            throw new IllegalStateException("Le combat est déjà démarré ou fermé.");
        }
        readerThread.execute(this::readGuestActions);
        battleThread.execute(() -> {
            try {
                NetworkTeamCodec teams = new NetworkTeamCodec();
                JsonObject payload = new JsonObject();
                payload.add("hostTeam", teams.encode(hostTeam).getPayload());
                payload.add("guestTeam", teams.encode(guestTeam).getPayload());
                payload.addProperty("mode", mode.name());
                synchronized (publishing) {
                    if (closed.get()) {
                        return;
                    }
                    broadcast(new NetworkMessage("BATTLE_START", payload));
                }
                engine.start(hostTeam, guestTeam, mode, simultaneousOpponent(), random);
                if (mode == BattleMode.CONSTRUCTED) {
                    engine.submitAction(new SendFakemon(hostTeam.getFakemons().getFirst()));
                }
                requestChoices();
            } catch (RuntimeException error) {
                fail(error);
            }
        });
    }

    private Opponent simultaneousOpponent() {
        return new Opponent() {
            @Override
            public PlayerAction chooseAction(List<Move> moves, List<Fakemon> standing) {
                return Objects.requireNonNull(resolvedGuestAction, "Le choix de l'invité est nécessaire.");
            }

            @Override
            public PlayerAction chooseFakemon(List<Fakemon> standing) {
                guestNeedsReplacement = true;
                try {
                    return opponent.chooseFakemon(standing);
                } finally {
                    guestNeedsReplacement = false;
                }
            }
        };
    }

    /**
     * Enregistre le choix de l'hôte sur le thread moteur.
     * Le tour est résolu quand les deux choix sont présents.
     *
     * @param message le message ACTION avec son requestId
     * @return la confirmation d'enregistrement, ou une erreur si le choix a été refusé
     */
    public CompletableFuture<Void> submitHostAction(NetworkMessage message) {
        CompletableFuture<Void> result = new CompletableFuture<>();
        try {
            battleThread.execute(() -> playHostAction(message, result));
        } catch (RejectedExecutionException error) {
            result.completeExceptionally(new IllegalStateException("Le combat est fermé.", error));
        }
        return result;
    }

    private void playHostAction(NetworkMessage message, CompletableFuture<Void> result) {
        PlayerAction action;
        try {
            action = validateHostAction(message);
        } catch (RuntimeException error) {
            result.completeExceptionally(error);
            return;
        }
        try {
            if (action instanceof Forfeit) {
                finish(Side.OPPONENT);
            } else if (hostNeedsReplacement) {
                choiceState(Side.PLAYER, hostRequest, "LOCKED");
                hostRequest = 0;
                hostNeedsReplacement = false;
                result.complete(null);
                engine.submitAction(action);
                requestChoices();
            } else {
                hostChoice = action;
                choiceState(Side.PLAYER, hostRequest, "WAITING");
                result.complete(null);
                resolveChoices();
            }
            result.complete(null);
        } catch (RuntimeException error) {
            if (finished.get() && closed.get()) {
                result.complete(null);
            } else {
                fail(error);
                result.completeExceptionally(error);
            }
        }
    }

    /**
     * Annule le choix de l'hôte tant que le choix adverse n'est pas reçu.
     *
     * @param requestId l'identifiant du tour à annuler
     * @return true si le choix a été retiré, false si le tour est déjà verrouillé
     */
    public CompletableFuture<Boolean> cancelHostAction(long requestId) {
        CompletableFuture<Boolean> result = new CompletableFuture<>();
        try {
            battleThread.execute(() -> {
                try {
                    result.complete(cancelChoice(Side.PLAYER, requestId));
                } catch (RuntimeException error) {
                    result.completeExceptionally(error);
                    fail(error);
                }
            });
        } catch (RejectedExecutionException error) {
            result.complete(false);
        }
        return result;
    }

    private boolean cancelChoice(Side side, long requestId) {
        long expected = side == Side.PLAYER ? hostRequest : guestRequest;
        PlayerAction choice = side == Side.PLAYER ? hostChoice : guestChoice;
        PlayerAction other = side == Side.PLAYER ? guestChoice : hostChoice;
        boolean accepted = !closed.get() && !finished.get() && requestId == expected
                && (choice == null || other == null) && !hostNeedsReplacement;
        if (accepted) {
            if (side == Side.PLAYER) {
                hostChoice = null;
            } else {
                guestChoice = null;
            }
        }
        if (!closed.get()) {
            choiceState(side, requestId, accepted ? "CANCELLED" : "LOCKED");
        }
        return accepted;
    }

    private void receiveGuestChoice(NetworkMessage message) {
        long id = readRequestId(message);
        if ("CANCEL_ACTION".equals(message.getType())) {
            cancelChoice(Side.OPPONENT, id);
            return;
        }
        if (closed.get() || finished.get() || guestRequest == 0 || id != guestRequest || guestChoice != null) {
            throw new IllegalStateException("Aucun choix de l'invité n'est attendu pour cette demande.");
        }
        PlayerAction action = actions.decode(message, guestTeam, engine.getActiveFakemon(Side.OPPONENT));
        validateSend(action, Side.OPPONENT);
        guestChoice = action;
        choiceState(Side.OPPONENT, guestRequest, "WAITING");
        resolveChoices();
    }

    private void resolveChoices() {
        if (hostChoice == null || guestChoice == null) {
            return;
        }
        choiceState(Side.PLAYER, hostRequest, "LOCKED");
        choiceState(Side.OPPONENT, guestRequest, "LOCKED");
        PlayerAction hostAction = hostChoice;
        resolvedGuestAction = guestChoice;
        hostRequest = 0;
        guestRequest = 0;
        hostChoice = null;
        guestChoice = null;
        try {
            engine.submitAction(hostAction);
        } finally {
            resolvedGuestAction = null;
        }
        requestChoices();
        if (finished.get()) {
            stopAfterBattle();
        }
    }

    private PlayerAction validateHostAction(NetworkMessage message) {
        if (closed.get() || finished.get() || hostRequest == 0) {
            throw new IllegalStateException("Aucun choix de l'hôte n'est attendu.");
        }
        if (message == null || message.getPayload() == null) {
            throw new IllegalArgumentException("Une réponse de l'hôte est attendue.");
        }
        if (readRequestId(message) != hostRequest) {
            throw new IllegalArgumentException("La réponse de l'hôte est périmée ou invalide.");
        }
        if (hostChoice != null) {
            throw new IllegalStateException("L'hôte a déjà choisi son action.");
        }
        PlayerAction action = actions.decode(message, hostTeam, engine.getActiveFakemon(Side.PLAYER));
        if (hostNeedsReplacement && !(action instanceof SendFakemon || action instanceof Forfeit)) {
            throw new IllegalArgumentException("Un remplaçant est attendu.");
        }
        validateSend(action, Side.PLAYER);
        return action;
    }

    private void validateSend(PlayerAction action, Side side) {
        if (action instanceof SendFakemon send
                && (send.getFakemon().isFainted() || send.getFakemon() == engine.getActiveFakemon(side))) {
            throw new IllegalArgumentException("Ce Fakemon ne peut pas entrer en jeu.");
        }
    }

    private long readRequestId(NetworkMessage message) {
        if (message == null || message.getPayload() == null) {
            throw new IllegalArgumentException("Un message numéroté est attendu.");
        }
        var value = message.getPayload().get("requestId");
        if (value == null || !value.isJsonPrimitive() || !value.getAsJsonPrimitive().isNumber()) {
            throw new IllegalArgumentException("L'identifiant de demande doit être un entier.");
        }
        long id = value.getAsBigDecimal().longValueExact();
        if (id < 1) {
            throw new IllegalArgumentException("L'identifiant de demande doit être positif.");
        }
        return id;
    }

    private void requestChoices() {
        if (closed.get() || finished.get()) {
            return;
        }
        hostRequest = requestNumber.incrementAndGet();
        JsonObject payload = new JsonObject();
        payload.addProperty("requestId", hostRequest);
        payload.addProperty("reason", hostNeedsReplacement ? "REPLACEMENT" : "TURN");
        onHostMessage.accept(new NetworkMessage("ACTION_REQUIRED", payload));
        if (!hostNeedsReplacement) {
            guestRequest = requestNumber.incrementAndGet();
            JsonObject guestPayload = new JsonObject();
            guestPayload.addProperty("requestId", guestRequest);
            guestPayload.addProperty("reason", "TURN");
            deliver(Side.OPPONENT, new NetworkMessage("ACTION_REQUIRED", guestPayload));
        }
    }

    private void choiceState(Side side, long requestId, String state) {
        JsonObject payload = new JsonObject();
        payload.addProperty("requestId", requestId);
        payload.addProperty("state", state);
        deliver(side, new NetworkMessage("CHOICE_STATE", payload));
    }

    private void deliver(Side side, NetworkMessage message) {
        if (side == Side.PLAYER) {
            onHostMessage.accept(message);
        } else {
            try {
                connection.send(message);
            } catch (IOException error) {
                throw new UncheckedIOException(error);
            }
        }
    }

    private void publishEvent(BattleEvent event) {
        synchronized (publishing) {
            publishOrderedEvent(event);
        }
    }

    private void publishOrderedEvent(BattleEvent event) {
        if (closed.get() || finished.get()) {
            return;
        }
        NetworkMessage message;
        if (event instanceof FakemonSelectionRequiredEvent selection) {
            if (selection.getReason() == SelectionReason.FIRST_FAKEMON) {
                return;
            }
            hostNeedsReplacement = true;
            message = events.encodeSelection(selection, Side.PLAYER);
        } else {
            message = events.encode(event);
        }
        if (event instanceof BattleEndedEvent) {
            finished.set(true);
        }
        broadcast(message);
    }

    private void readGuestActions() {
        try {
            while (!closed.get()) {
                NetworkMessage message = connection.receive();
                if ("BATTLE_FINISHED".equals(message.getType())) {
                    if (!keepConnection || !finished.get()) {
                        throw new IOException("Confirmation de fin de combat inattendue.");
                    }
                    readerThread.shutdown();
                    battleThread.shutdown();
                    released.complete(connection);
                    return;
                }
                if (keepConnection && finished.get()) {
                    continue;
                }
                if (isGuestForfeit(message)) {
                    finish(Side.PLAYER);
                    if (!keepConnection) {
                        return;
                    }
                    continue;
                }
                if (guestNeedsReplacement && !"CANCEL_ACTION".equals(message.getType())) {
                    opponent.receiveAction(message);
                } else {
                    battleThread.execute(() -> {
                        try {
                            receiveGuestChoice(message);
                        } catch (RuntimeException error) {
                            if (!closed.get() && !finished.get()) {
                                fail(error);
                            }
                        }
                    });
                }
            }
        } catch (IOException | RuntimeException error) {
            if (!closed.get() && (!finished.get() || keepConnection)) {
                fail(error);
            }
        }
    }

    private boolean isGuestForfeit(NetworkMessage message) {
        if (!"ACTION".equals(message.getType())) {
            return false;
        }
        var action = message.getPayload().get("action");
        return action != null && action.isJsonPrimitive() && action.getAsJsonPrimitive().isString()
                && "FORFEIT".equals(action.getAsString());
    }

    private void finish(Side winner) {
        synchronized (publishing) {
            if (closed.get() || !finished.compareAndSet(false, true)) {
                return;
            }
            try {
                hostTeam.getFakemons().forEach(Fakemon::clearStatus);
                guestTeam.getFakemons().forEach(Fakemon::clearStatus);
                broadcast(events.encode(new BattleEndedEvent(winner, BattleEndReason.FORFEIT)));
            } catch (RuntimeException error) {
                onFailure.accept(error);
            } finally {
                stopAfterBattle();
            }
        }
    }

    private void broadcast(NetworkMessage message) {
        try {
            connection.send(message);
            onHostMessage.accept(message);
        } catch (IOException error) {
            throw new UncheckedIOException(error);
        }
    }

    private void fail(Throwable error) {
        if (closed.get()) {
            return;
        }
        released.completeExceptionally(error);
        stop();
        onFailure.accept(error);
    }

    private void stopAfterBattle() {
        if (!keepConnection) {
            stop();
        }
    }

    private void stop() {
        try {
            close();
        } catch (IOException error) {
            onFailure.accept(error);
        }
    }

    /** @throws IOException si la fermeture de la connexion échoue */
    @Override
    public void close() throws IOException {
        if (closed.compareAndSet(false, true)) {
            released.completeExceptionally(new IOException("Le combat est fermé."));
            try {
                opponent.close();
            } finally {
                battleThread.shutdown();
                readerThread.shutdownNow();
            }
        }
    }
}
