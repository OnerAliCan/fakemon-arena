package fr.epitech.fakemon.core.api;

import fr.epitech.fakemon.core.model.Fakemon;
import fr.epitech.fakemon.core.model.Move;

/**
 * Représente l'échec d'une attaque par un Fakemon
 * pendant un combat.
 *
 * Cet événement contient le camp du Fakemon,
 * le Fakemon attaquant et l'attaque qui a échoué.
 */
public class MoveMissedEvent extends BattleEvent {

    private final Side side;
    private final Fakemon fakemon;
    private final Move move;

    /**
     * Construit un événement d'échec d'attaque.
     *
     * @param side    le camp du Fakemon attaquant
     * @param fakemon le Fakemon qui a échoué à utiliser l'attaque
     * @param move    l'attaque qui a échoué
     */
    public MoveMissedEvent(Side side, Fakemon fakemon, Move move) {
        this.side = side;
        this.fakemon = fakemon;
        this.move = move;
    }

    /**
     * Retourne le camp du Fakemon attaquant.
     *
     * @return le camp du Fakemon attaquant
     */
    public Side getSide() {
        return side;
    }

    /**
     * Retourne le Fakemon qui a échoué à utiliser l'attaque.
     *
     * @return le Fakemon attaquant
     */
    public Fakemon getFakemon() {
        return fakemon;
    }

    /**
     * Retourne l'attaque qui a échoué.
     *
     * @return l'attaque du Fakemon
     */
    public Move getMove() {
        return move;
    }

}
