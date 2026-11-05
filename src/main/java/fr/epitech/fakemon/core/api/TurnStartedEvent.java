package fr.epitech.fakemon.core.api;

/**
 * Représente le début d'un tour dans un combat.
 *
 * Cet événement contient le numéro du tour qui commence.
 */

public class TurnStartedEvent extends BattleEvent {

    private final int turnNumber;

    /**
     * Construit un événement de début de tour.
     *
     * @param turnNumber le numéro du tour qui commence
     */
    public TurnStartedEvent(int turnNumber) {
        this.turnNumber = turnNumber;
    }

    /**
     * Retourne le numéro du tour qui commence.
     *
     * @return le numéro du tour
     */
    public int getTurnNumber() {
        return turnNumber;
    }

}
