package fr.epitech.fakemon.core.api;

import fr.epitech.fakemon.core.model.Fakemon;

/**
 * Représente la mise K.O. d'un Fakemon pendant un combat.
 *
 * Cet événement contient le camp du Fakemon concerné
 * ainsi que le Fakemon qui a été mis K.O.
 */
public class FakemonFaintedEvent extends BattleEvent {

    private final Side side;
    private final Fakemon fakemon;

    /**
     * Construit un événement de Fakemon mis K.O.
     *
     * @param side    le camp du Fakemon mis K.O.
     * @param fakemon le Fakemon qui a été mis K.O.
     */
    public FakemonFaintedEvent(Side side, Fakemon fakemon) {
        this.side = side;
        this.fakemon = fakemon;
    }

    /**
     * Retourne le camp du Fakemon mis K.O.
     *
     * @return le camp du Fakemon mis K.O.
     */
    public Side getSide() {
        return side;
    }

    /**
     * Retourne le Fakemon qui a été mis K.O.
     *
     * @return le Fakemon mis K.O.
     */
    public Fakemon getFakemon() {
        return fakemon;
    }

}
