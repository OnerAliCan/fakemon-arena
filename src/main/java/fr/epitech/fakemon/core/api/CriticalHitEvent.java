package fr.epitech.fakemon.core.api;

import fr.epitech.fakemon.core.model.Fakemon;

/**
 * Représente un coup critique subi par un Fakemon pendant un combat.
 *
 * Cet événement contient le camp et le Fakemon
 * ayant reçu le coup critique.
 */
public class CriticalHitEvent extends BattleEvent {

    private final Side side;
    private final Fakemon fakemon;

    /**
     * Construit un événement de coup critique.
     *
     * @param side    le camp du Fakemon qui reçoit le coup critique
     * @param fakemon le Fakemon qui reçoit le coup critique
     */
    public CriticalHitEvent(Side side, Fakemon fakemon) {
        this.side = side;
        this.fakemon = fakemon;
    }

    /**
     * Retourne le camp du Fakemon qui reçoit le coup critique.
     *
     * @return le camp du Fakemon ciblé
     */
    public Side getSide() {
        return side;
    }

    /**
     * Retourne le Fakemon qui reçoit le coup critique.
     *
     * @return le Fakemon ciblé
     */
    public Fakemon getFakemon() {
        return fakemon;
    }
}