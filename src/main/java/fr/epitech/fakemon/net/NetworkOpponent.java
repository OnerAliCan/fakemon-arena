package fr.epitech.fakemon.net;

import fr.epitech.fakemon.core.api.Opponent;
import fr.epitech.fakemon.core.api.PlayerAction;
import fr.epitech.fakemon.core.api.SendFakemon;
import fr.epitech.fakemon.core.api.UseMove;
import fr.epitech.fakemon.core.model.Fakemon;
import fr.epitech.fakemon.core.model.Move;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.function.Predicate;

/**
 * Attend les choix de l'invité pour le moteur hébergé.
 * Le moteur utilise un thread de combat, et le lecteur réseau transmet les réponses.
 */
public final class NetworkOpponent implements Opponent, AutoCloseable {
    private final Runnable requestAction;
    private final Runnable requestReplacement;
    private CompletableFuture<PlayerAction> pendingAction;
    private Predicate<PlayerAction> allowedAction;
    private boolean closed;

    /**
     * Prépare les notifications à envoyer à l'invité quand son choix est attendu.
     *
     * @param requestAction annonce qu'une attaque ou un changement est attendu
     * @param requestReplacement annonce qu'un remplaçant est attendu
     */
    public NetworkOpponent(Runnable requestAction, Runnable requestReplacement) {
        this.requestAction = Objects.requireNonNull(requestAction);
        this.requestReplacement = Objects.requireNonNull(requestReplacement);
    }

    @Override
    public PlayerAction chooseAction(List<Move> moveList, List<Fakemon> standingFakemonList) {
        List<Move> moves = List.copyOf(moveList);
        List<Fakemon> standing = List.copyOf(standingFakemonList);
        return awaitAction(action ->
                (action instanceof UseMove use && moves.contains(use.getMove()))
                || (action instanceof SendFakemon send && standing.contains(send.getFakemon())),
                requestAction);
    }

    @Override
    public PlayerAction chooseFakemon(List<Fakemon> standingFakemonList) {
        List<Fakemon> standing = List.copyOf(standingFakemonList);
        return awaitAction(action -> action instanceof SendFakemon send && standing.contains(send.getFakemon()),
                requestReplacement);
    }

    /**
     * Transmet un choix reconstruit avec les objets du moteur de l'hôte.
     * Une réponse refusée laisse la demande en attente pour permettre une correction.
     *
     * @param action le choix de l'invité
     * @throws IllegalStateException si aucun choix n'est attendu ou si la connexion est fermée
     * @throws IllegalArgumentException si le choix ne correspond pas à la demande
     */
    public synchronized void submitAction(PlayerAction action) {
        if (closed || pendingAction == null || pendingAction.isDone()) {
            throw new IllegalStateException("Aucune action de l'invité n'est attendue.");
        }
        if (!allowedAction.test(action)) {
            throw new IllegalArgumentException("Le choix de l'invité ne correspond pas à la demande.");
        }
        pendingAction.complete(action);
    }

    private PlayerAction awaitAction(Predicate<PlayerAction> allowed, Runnable request) {
        CompletableFuture<PlayerAction> response = new CompletableFuture<>();
        synchronized (this) {
            if (closed || pendingAction != null) {
                throw new IllegalStateException("L'adversaire est fermé ou attend déjà une réponse.");
            }
            pendingAction = response;
            allowedAction = allowed;
        }
        try {
            request.run();
            return response.get();
        } catch (InterruptedException error) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("L'attente du choix de l'invité a été interrompue.", error);
        } catch (ExecutionException error) {
            throw new IllegalStateException("L'invité a quitté le combat.", error.getCause());
        } finally {
            synchronized (this) {
                pendingAction = null;
                allowedAction = null;
            }
        }
    }

    /** Ferme l'adversaire et débloque immédiatement une attente en cours. */
    @Override
    public synchronized void close() {
        closed = true;
        if (pendingAction != null) {
            pendingAction.completeExceptionally(new IllegalStateException("Connexion avec l'invité fermée."));
        }
    }
}
