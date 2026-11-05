package fr.epitech.fakemon.core.api;

import fr.epitech.fakemon.core.model.Fakemon;

/**
 * Représente la perte de points de vie d'un Fakemon
 * pendant un combat.
 *
 * Cet événement contient le camp du Fakemon touché,
 * le Fakemon concerné, les PV perdus et les PV restants.
 */
public class HpLostEvent extends BattleEvent {

    private final Side side;
    private final Fakemon fakemon;
    private final int hpLost;
    private final int remainingHp;

    /**
     * Construit un événement de perte de points de vie.
     *
     * @param side        le camp du Fakemon touché
     * @param fakemon     le Fakemon qui perd des points de vie
     * @param hpLost      le nombre de points de vie perdus
     * @param remainingHp le nombre de points de vie restants
     */
    public HpLostEvent(Side side, Fakemon fakemon, int hpLost, int remainingHp) {
        this.side = side;
        this.fakemon = fakemon;
        this.hpLost = hpLost;
        this.remainingHp = remainingHp;
    }

    /**
     * Retourne le camp du Fakemon touché.
     *
     * @return le camp du Fakemon touché
     */
    public Side getSide() {
        return side;
    }

    /**
     * Retourne le Fakemon qui a perdu des points de vie.
     *
     * @return le Fakemon touché
     */
    public Fakemon getFakemon() {
        return fakemon;
    }

    /**
     * Retourne le nombre de points de vie perdus.
     *
     * @return le nombre de PV perdus
     */
    public int getHpLost() {
        return hpLost;
    }

    /**
     * Retourne le nombre de points de vie restants.
     *
     * @return le nombre de PV restants
     */
    public int getRemainingHp() {
        return remainingHp;
    }
}