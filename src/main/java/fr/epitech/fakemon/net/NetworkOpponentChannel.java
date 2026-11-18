package fr.epitech.fakemon.net;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import fr.epitech.fakemon.core.api.Forfeit;
import fr.epitech.fakemon.core.api.Opponent;
import fr.epitech.fakemon.core.api.PlayerAction;
import fr.epitech.fakemon.core.api.SendFakemon;
import fr.epitech.fakemon.core.model.Fakemon;
import fr.epitech.fakemon.core.model.Move;
import fr.epitech.fakemon.core.model.Team;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;
import java.util.function.LongSupplier;
import java.util.concurrent.atomic.AtomicLong;

/** Relie les demandes du moteur aux réponses reçues par le lecteur réseau de l'hôte. */
public final class NetworkOpponentChannel implements Opponent, AutoCloseable {
    private final NetworkConnection connection;
    private final Team guestTeam;
    private final Supplier<Fakemon> activeGuest;
    private final Runnable onForfeit;
    private final NetworkOpponent opponent;
    private final NetworkActionCodec actions = new NetworkActionCodec();
    private final LongSupplier nextRequest;
    private long pendingRequest;
    private Fakemon requestedActive;
    private boolean closed;

    /**
     * Prépare l'adversaire distant. Le lecteur réseau appelle receiveAction pour chaque réponse.
     *
     * @param connection la connexion de l'invité, fermée avec ce canal
     * @param guestTeam l'équipe de l'invité dans le moteur de l'hôte
     * @param activeGuest fournit son Fakemon actif depuis le thread du moteur
     * @param onForfeit signale l'abandon au coordinateur, depuis le thread réseau
     */
    public NetworkOpponentChannel(NetworkConnection connection, Team guestTeam,
            Supplier<Fakemon> activeGuest, Runnable onForfeit) {
        this(connection, guestTeam, activeGuest, onForfeit, new AtomicLong()::incrementAndGet);
    }

    /**
     * Prépare le canal avec la numérotation commune de la session.
     *
     * @param connection la connexion avec l'invité
     * @param guestTeam l'équipe de l'invité
     * @param activeGuest fournit le Fakemon actif sur le thread du moteur
     * @param onForfeit signale un abandon
     * @param nextRequest fournit un nouvel identifiant de demande
     */
    public NetworkOpponentChannel(NetworkConnection connection, Team guestTeam,
            Supplier<Fakemon> activeGuest, Runnable onForfeit, LongSupplier nextRequest) {
        this.connection = Objects.requireNonNull(connection);
        this.guestTeam = Objects.requireNonNull(guestTeam);
        this.activeGuest = Objects.requireNonNull(activeGuest);
        this.onForfeit = Objects.requireNonNull(onForfeit);
        this.nextRequest = Objects.requireNonNull(nextRequest);
        opponent = new NetworkOpponent(() -> request("TURN"), () -> request("REPLACEMENT"));
    }

    @Override
    public PlayerAction chooseAction(List<Move> moveList, List<Fakemon> standingFakemonList) {
        return opponent.chooseAction(moveList, standingFakemonList);
    }

    @Override
    public PlayerAction chooseFakemon(List<Fakemon> standingFakemonList) {
        return opponent.chooseFakemon(standingFakemonList);
    }

    private void request(String reason) {
        JsonObject payload = new JsonObject();
        synchronized (this) {
            if (closed) {
                throw new IllegalStateException("Le canal de l'invité est fermé.");
            }
            pendingRequest = nextRequest.getAsLong();
            requestedActive = activeGuest.get();
            payload.addProperty("requestId", pendingRequest);
            payload.addProperty("reason", reason);
        }
        try {
            connection.send(new NetworkMessage("ACTION_REQUIRED", payload));
        } catch (IOException error) {
            opponent.close();
            throw new UncheckedIOException("Impossible de demander son choix à l'invité.", error);
        }
    }

    /**
     * Transmet une réponse au moteur, après vérification du numéro de demande.
     * Un abandon débloque le moteur et prévient le coordinateur pour terminer la session.
     *
     * @param message le message ACTION lu par le thread réseau
     * @throws IOException si la réponse est invalide, périmée ou inattendue
     */
    public void receiveAction(NetworkMessage message) throws IOException {
        boolean forfeited;
        synchronized (this) {
            if (closed || pendingRequest == 0) {
                throw new IOException("Aucune réponse de l'invité n'est attendue.");
            }
            if (message == null || !"ACTION".equals(message.getType()) || message.getPayload() == null) {
                throw new IOException("Un message ACTION est attendu.");
            }
            if (readRequestId(message.getPayload()) != pendingRequest) {
                throw new IOException("La réponse ne correspond pas à la demande en cours.");
            }
            PlayerAction action;
            try {
                action = actions.decode(message, guestTeam, requestedActive);
                if (action instanceof SendFakemon send && send.getFakemon() == requestedActive) {
                    throw new IllegalArgumentException("Le Fakemon est déjà sur le terrain.");
                }
                forfeited = action instanceof Forfeit;
                if (!forfeited) {
                    opponent.submitAction(action);
                }
            } catch (IllegalArgumentException | IllegalStateException error) {
                throw new IOException("Le choix de l'invité est invalide.", error);
            }
            pendingRequest = 0;
            if (forfeited) {
                closed = true;
            }
        }
        if (forfeited) {
            try {
                onForfeit.run();
            } finally {
                opponent.close();
            }
        }
    }

    private long readRequestId(JsonObject payload) throws IOException {
        JsonElement value = payload.get("requestId");
        if (value == null || !value.isJsonPrimitive() || !value.getAsJsonPrimitive().isNumber()) {
            throw new IOException("Le numéro de demande doit être un entier.");
        }
        try {
            return value.getAsBigDecimal().longValueExact();
        } catch (ArithmeticException | NumberFormatException error) {
            throw new IOException("Le numéro de demande doit être un entier.", error);
        }
    }

    /**
     * Débloque le moteur et ferme la connexion de l'invité.
     *
     * @throws IOException si la fermeture de la connexion échoue
     */
    @Override
    public void close() throws IOException {
        synchronized (this) {
            closed = true;
            pendingRequest = 0;
        }
        opponent.close();
        connection.close();
    }
}
