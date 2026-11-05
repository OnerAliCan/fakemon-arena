package fr.epitech.fakemon.core.api;

import fr.epitech.fakemon.core.model.Fakemon;

/**
 * Représente l'entrée d'un Fakemon sur le terrain pendant un combat.
 *
 * Cet événement contient le camp du Fakemon concerné
 * ainsi que le Fakemon qui entre en jeu.
 */
public class FakemonEnteredEvent extends BattleEvent {

    private final Side side;
    private final Fakemon fakemon;

    /**
     * Construit un événement d'entrée d'un Fakemon sur le terrain.
     *
     * @param side    le camp du Fakemon qui entre en jeu
     * @param fakemon le Fakemon qui entre sur le terrain
     */
    public FakemonEnteredEvent(Side side, Fakemon fakemon) {
        this.side = side;
        this.fakemon = fakemon;
    }

    /**
     * Retourne le camp du Fakemon qui entre en jeu.
     *
     * @return le camp du Fakemon entrant
     */
    public Side getSide() {
        return side;
    }

    /**
     * Retourne le Fakemon qui entre sur le terrain.
     *
     * @return le Fakemon entrant
     */
    public Fakemon getFakemon() {
        return fakemon;
    }
}