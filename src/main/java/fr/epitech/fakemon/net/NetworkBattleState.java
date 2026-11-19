package fr.epitech.fakemon.net;

import fr.epitech.fakemon.core.api.BattleEvent;
import fr.epitech.fakemon.core.api.BattleState;
import fr.epitech.fakemon.core.api.FakemonEnteredEvent;
import fr.epitech.fakemon.core.api.FakemonFaintedEvent;
import fr.epitech.fakemon.core.api.HpLostEvent;
import fr.epitech.fakemon.core.api.HpRestoredEvent;
import fr.epitech.fakemon.core.api.Side;
import fr.epitech.fakemon.core.api.TurnStartedEvent;
import fr.epitech.fakemon.core.api.StatusAppliedEvent;
import fr.epitech.fakemon.core.api.StatusRemovedEvent;
import fr.epitech.fakemon.core.api.BattleEndedEvent;
import fr.epitech.fakemon.core.model.Fakemon;
import fr.epitech.fakemon.core.model.Team;
import java.util.Objects;

/**
 * Conserve la copie du combat affichée par l'invité.
 * Les événements décodés s'appliquent dans leur ordre de réception, sur le thread JavaFX.
 */
public final class NetworkBattleState implements BattleState {
    private final Team playerTeam;
    private final Team opponentTeam;
    private Fakemon activePlayer;
    private Fakemon activeOpponent;
    private int turnNumber;

    /**
     * Prépare l'état avant les premiers événements du combat.
     *
     * @param playerTeam l'équipe du joueur local
     * @param opponentTeam l'équipe de son adversaire
     */
    public NetworkBattleState(Team playerTeam, Team opponentTeam) {
        this.playerTeam = Objects.requireNonNull(playerTeam);
        this.opponentTeam = Objects.requireNonNull(opponentTeam);
    }

    @Override
    public Team getTeam(Side side) {
        Objects.requireNonNull(side);
        return side == Side.PLAYER ? playerTeam : opponentTeam;
    }

    @Override
    public Fakemon getActiveFakemon(Side side) {
        Objects.requireNonNull(side);
        return side == Side.PLAYER ? activePlayer : activeOpponent;
    }

    @Override
    public int getTurnNumber() {
        return turnNumber;
    }

    /**
     * Applique les données annoncées par l'hôte avant de prévenir l'écran.
     *
     * @param event l'événement déjà adapté aux camps du joueur local
     * @throws IllegalArgumentException si le camp, le Fakemon, les PV ou le tour sont incohérents
     */
    public void apply(BattleEvent event) {
        Objects.requireNonNull(event);
        if (event instanceof FakemonEnteredEvent entered) {
            checkMember(entered.getSide(), entered.getFakemon());
            if (entered.getSide() == Side.PLAYER) {
                activePlayer = entered.getFakemon();
            } else {
                activeOpponent = entered.getFakemon();
            }
        } else if (event instanceof HpLostEvent lost) {
            updateHealth(lost.getSide(), lost.getFakemon(), lost.getRemainingHp());
        } else if (event instanceof HpRestoredEvent restored) {
            updateHealth(restored.getSide(), restored.getFakemon(), restored.getRemainingHp());
        } else if (event instanceof FakemonFaintedEvent fainted) {
            updateHealth(fainted.getSide(), fainted.getFakemon(), 0);
        } else if (event instanceof TurnStartedEvent started) {
            if (started.getTurnNumber() < 1 || started.getTurnNumber() < turnNumber) {
                throw new IllegalArgumentException("Le numéro du tour reçu est incohérent.");
            }
            turnNumber = started.getTurnNumber();
        } else if (event instanceof StatusAppliedEvent applied) {
            checkMember(applied.getSide(), applied.getFakemon());
            if (!applied.getFakemon().applyStatus(applied.getStatus().create())) {
                throw new IllegalArgumentException("Le statut reçu ne peut pas être appliqué au Fakemon.");
            }
        } else if (event instanceof StatusRemovedEvent removed) {
            checkMember(removed.getSide(), removed.getFakemon());
            var status = removed.getFakemon().getStatus();
            if (status == null || status.getType() != removed.getStatus()) {
                throw new IllegalArgumentException("La fin de statut reçue ne correspond pas au Fakemon.");
            }
            removed.getFakemon().clearStatus();
        } else if (event instanceof BattleEndedEvent) {
            playerTeam.getFakemons().forEach(Fakemon::clearStatus);
            opponentTeam.getFakemons().forEach(Fakemon::clearStatus);
        }
    }

    private void checkMember(Side side, Fakemon mon) {
        if (side == null || mon == null || !getTeam(side).getFakemons().contains(mon)) {
            throw new IllegalArgumentException("Le Fakemon reçu n'appartient pas au camp indiqué.");
        }
    }

    private void updateHealth(Side side, Fakemon mon, int remainingHp) {
        checkMember(side, mon);
        if (remainingHp < 0 || remainingHp > mon.getMaxHp()) {
            throw new IllegalArgumentException("Les PV reçus sont hors limites.");
        }
        int difference = remainingHp - mon.getCurrentHp();
        if (difference < 0) {
            mon.takeDamage(-difference);
        } else {
            mon.heal(difference);
        }
    }
}
