package fr.epitech.fakemon.core.api;

/**
 * Représente la fin d'un combat de Fakemon.
 *
 * Cet événement contient le camp vainqueur ainsi que
 * la raison ayant entraîné la fin du combat.
 */
public class BattleEndedEvent extends BattleEvent {

    private final Side winner;
    private final BattleEndReason reason;

    /**
     * Construit un événement de fin de combat.
     *
     * @param winner le camp vainqueur du combat
     * @param reason la raison de la fin du combat
     */
    public BattleEndedEvent(Side winner, BattleEndReason reason) {
        this.winner = winner;
        this.reason = reason;
    }

    /**
     * Retourne le camp vainqueur du combat.
     *
     * @return le camp vainqueur
     */
    public Side getWinner() {
        return winner;
    }

    /**
     * Retourne la raison de la fin du combat.
     *
     * @return la raison de la fin du combat
     */
    public BattleEndReason getReason() {
        return reason;
    }
}