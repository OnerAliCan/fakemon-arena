package fr.epitech.fakemon.net;

import com.google.gson.JsonObject;
import fr.epitech.fakemon.core.api.BattleEndReason;
import fr.epitech.fakemon.core.api.BattleEndedEvent;
import fr.epitech.fakemon.core.api.BattleEvent;
import fr.epitech.fakemon.core.api.BattleMode;
import fr.epitech.fakemon.core.api.FakemonSelectionRequiredEvent;
import fr.epitech.fakemon.core.api.Forfeit;
import fr.epitech.fakemon.core.api.PlayerAction;
import fr.epitech.fakemon.core.api.SendFakemon;
import fr.epitech.fakemon.core.api.Side;
import fr.epitech.fakemon.core.engine.DefaultBattleEngine;
import fr.epitech.fakemon.core.model.Team;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Objects;
import java.util.Random;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;
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
    private long requestNumber;
    private long hostRequest;
    private boolean hostNeedsReplacement;

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
        this.connection = Objects.requireNonNull(connection);
        this.hostTeam = Objects.requireNonNull(hostTeam);
        this.guestTeam = Objects.requireNonNull(guestTeam);
        this.random = Objects.requireNonNull(random);
        this.onHostMessage = Objects.requireNonNull(onHostMessage);
        this.onFailure = Objects.requireNonNull(onFailure);
        events = new NetworkEventCodec(hostTeam, guestTeam, Side.PLAYER);
        opponent = new NetworkOpponentChannel(connection, guestTeam,
                () -> engine.getActiveFakemon(Side.OPPONENT), () -> finish(Side.PLAYER));
        engine.addListener(this::publishEvent);
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
                payload.addProperty("mode", BattleMode.RANDOM.name());
                synchronized (publishing) {
                    if (closed.get()) {
                        return;
                    }
                    broadcast(new NetworkMessage("BATTLE_START", payload));
                }
                engine.start(hostTeam, guestTeam, BattleMode.RANDOM, opponent, random);
                requestHostAction();
            } catch (RuntimeException error) {
                fail(error);
            }
        });
    }

    /**
     * Soumet la réponse de l'hôte au thread moteur.
     *
     * @param message le message ACTION avec son requestId
     * @return une confirmation, ou une erreur si le choix a été refusé
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
        hostRequest = 0;
        hostNeedsReplacement = false;
        try {
            if (action instanceof Forfeit) {
                finish(Side.OPPONENT);
            } else {
                engine.submitAction(action);
                requestHostAction();
                if (finished.get()) {
                    stop();
                }
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

    private PlayerAction validateHostAction(NetworkMessage message) {
        if (closed.get() || finished.get() || hostRequest == 0) {
            throw new IllegalStateException("Aucun choix de l'hôte n'est attendu.");
        }
        if (message == null || message.getPayload() == null) {
            throw new IllegalArgumentException("Une réponse de l'hôte est attendue.");
        }
        var requestId = message.getPayload().get("requestId");
        if (requestId == null || !requestId.isJsonPrimitive() || !requestId.getAsJsonPrimitive().isNumber()
                || requestId.getAsBigDecimal().longValueExact() != hostRequest) {
            throw new IllegalArgumentException("La réponse de l'hôte est périmée ou invalide.");
        }
        PlayerAction action = actions.decode(message, hostTeam, engine.getActiveFakemon(Side.PLAYER));
        if (hostNeedsReplacement && !(action instanceof SendFakemon || action instanceof Forfeit)) {
            throw new IllegalArgumentException("Un remplaçant est attendu.");
        }
        if (action instanceof SendFakemon send
                && (send.getFakemon().isFainted() || send.getFakemon() == engine.getActiveFakemon(Side.PLAYER))) {
            throw new IllegalArgumentException("Ce Fakemon ne peut pas entrer en jeu.");
        }
        return action;
    }

    private void requestHostAction() {
        if (closed.get() || finished.get()) {
            return;
        }
        hostRequest = ++requestNumber;
        JsonObject payload = new JsonObject();
        payload.addProperty("requestId", hostRequest);
        payload.addProperty("reason", hostNeedsReplacement ? "REPLACEMENT" : "TURN");
        onHostMessage.accept(new NetworkMessage("ACTION_REQUIRED", payload));
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
                if (isGuestForfeit(message)) {
                    finish(Side.PLAYER);
                    return;
                }
                opponent.receiveAction(message);
            }
        } catch (IOException | RuntimeException error) {
            if (!closed.get() && !finished.get()) {
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
                broadcast(events.encode(new BattleEndedEvent(winner, BattleEndReason.FORFEIT)));
            } catch (RuntimeException error) {
                onFailure.accept(error);
            } finally {
                stop();
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
        stop();
        onFailure.accept(error);
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
            try {
                opponent.close();
            } finally {
                battleThread.shutdown();
                readerThread.shutdownNow();
            }
        }
    }
}
