package fr.epitech.fakemon.core.api;

import fr.epitech.fakemon.core.model.Fakemon;
import fr.epitech.fakemon.core.model.Move;

/**
 * Représente l'utilisation d'une attaque par un Fakemon
 * pendant un combat.
 *
 * Cet événement contient le camp du Fakemon,
 * le Fakemon attaquant et l'attaque utilisée.
 */
public class MoveUsedEvent extends BattleEvent {

    private final Side side;
    private final Fakemon fakemon;
    private final Move move;

    /**
     * Construit un événement d'utilisation d'attaque.
     *
     * @param side    le camp du Fakemon attaquant
     * @param fakemon le Fakemon qui utilise l'attaque
     * @param move    l'attaque utilisée
     */
    public MoveUsedEvent(Side side, Fakemon fakemon, Move move) {
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
     * Retourne le Fakemon qui utilise l'attaque.
     *
     * @return le Fakemon attaquant
     */
    public Fakemon getFakemon() {
        return fakemon;
    }

    /**
     * Retourne l'attaque utilisée.
     *
     * @return l'attaque du Fakemon
     */
    public Move getMove() {
        return move;
    }
}