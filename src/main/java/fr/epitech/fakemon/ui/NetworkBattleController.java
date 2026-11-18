package fr.epitech.fakemon.ui;

import fr.epitech.fakemon.core.api.BattleEndedEvent;
import fr.epitech.fakemon.core.api.BattleEvent;
import fr.epitech.fakemon.core.api.BattleState;
import fr.epitech.fakemon.core.api.FakemonSelectionRequiredEvent;
import fr.epitech.fakemon.core.api.PlayerAction;
import fr.epitech.fakemon.core.api.Side;
import fr.epitech.fakemon.net.NetworkActionCodec;
import fr.epitech.fakemon.net.NetworkBattleCoordinator;
import fr.epitech.fakemon.net.NetworkBattleState;
import fr.epitech.fakemon.net.NetworkConnection;
import fr.epitech.fakemon.net.NetworkEventCodec;
import fr.epitech.fakemon.net.NetworkGuestSession;
import fr.epitech.fakemon.net.NetworkMessage;
import fr.epitech.fakemon.net.NetworkTeamCodec;
import java.io.IOException;
import java.util.Random;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import javafx.application.Platform;

/** Adapte les sessions réseau à l'écran de combat sur le thread JavaFX. */
final class NetworkBattleController implements AutoCloseable {
    private final NetworkBattleCoordinator host;
    private final NetworkGuestSession guest;
    private final Side localSide;
    private NetworkBattleState state;
    private NetworkEventCodec events;
    private Consumer<BattleEvent> onEvent;
    private Consumer<Boolean> onChoice;
    private Consumer<Throwable> onFailure;
    private long requestId;
    private boolean replacement;
    private boolean closed;
    private boolean finished;

    NetworkBattleController(NetworkConnection connection, BattleSetup setup) {
        localSide = setup == null ? Side.OPPONENT : Side.PLAYER;
        if (setup == null) {
            host = null;
            guest = new NetworkGuestSession(connection, this::dispatch, this::dispatchFailure);
        } else {
            guest = null;
            host = new NetworkBattleCoordinator(connection, setup.player, setup.opponent, new Random(),
                    this::dispatch, this::dispatchFailure);
        }
    }

    void start(Consumer<BattleEvent> onEvent, Consumer<Boolean> onChoice, Consumer<Throwable> onFailure) {
        this.onEvent = onEvent;
        this.onChoice = onChoice;
        this.onFailure = onFailure;
        if (host != null) {
            host.start();
        } else {
            guest.start();
        }
    }

    BattleState getState() {
        return state;
    }

    boolean canChoose() {
        return !closed && !finished && requestId != 0;
    }

    boolean needsReplacement() {
        return replacement;
    }

    void submitAction(PlayerAction action) {
        if (!canChoose()) {
            throw new IllegalStateException("Aucun choix n'est attendu.");
        }
        NetworkMessage message = new NetworkActionCodec().encode(
                action, state.getTeam(Side.PLAYER), state.getActiveFakemon(Side.PLAYER));
        message.getPayload().addProperty("requestId", requestId);
        requestId = 0;
        CompletableFuture<Void> sent = host != null ? host.submitHostAction(message) : guest.submitAction(message);
        sent.whenComplete((ignored, error) -> {
            if (error != null) {
                dispatchFailure(error);
            }
        });
    }

    private void dispatch(NetworkMessage message) {
        Platform.runLater(() -> {
            if (closed) {
                return;
            }
            try {
                accept(message);
            } catch (RuntimeException error) {
                fail(error);
            }
        });
    }

    private void accept(NetworkMessage message) {
        switch (message.getType()) {
            case "BATTLE_START" -> {
                if (state != null) {
                    throw new IllegalStateException("Le combat a déjà été initialisé.");
                }
                NetworkTeamCodec teams = new NetworkTeamCodec();
                var hostTeam = teams.decode(new NetworkMessage("TEAM",
                        message.getPayload().getAsJsonObject("hostTeam")));
                var guestTeam = teams.decode(new NetworkMessage("TEAM",
                        message.getPayload().getAsJsonObject("guestTeam")));
                state = localSide == Side.PLAYER ? new NetworkBattleState(hostTeam, guestTeam)
                        : new NetworkBattleState(guestTeam, hostTeam);
                events = new NetworkEventCodec(hostTeam, guestTeam, localSide);
            }
            case "EVENT" -> events.decode(message).ifPresent(event -> {
                state.apply(event);
                if (event instanceof BattleEndedEvent) {
                    finished = true;
                    requestId = 0;
                }
                if (!(event instanceof FakemonSelectionRequiredEvent)) {
                    onEvent.accept(event);
                }
            });
            case "ACTION_REQUIRED" -> {
                requestId = message.getPayload().get("requestId").getAsLong();
                replacement = "REPLACEMENT".equals(message.getPayload().get("reason").getAsString());
                onChoice.accept(replacement);
            }
            default -> throw new IllegalArgumentException("Message de combat inconnu.");
        }
    }

    private void dispatchFailure(Throwable error) {
        Platform.runLater(() -> fail(error));
    }

    private void fail(Throwable error) {
        if (!closed && !finished) {
            close();
            onFailure.accept(error);
        }
    }

    @Override
    public void close() {
        if (!closed) {
            closed = true;
            try {
                if (host != null) {
                    host.close();
                } else {
                    guest.close();
                }
            } catch (IOException error) {
                if (!finished && onFailure != null) {
                    onFailure.accept(error);
                }
            }
        }
    }
}
