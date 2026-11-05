package fr.epitech.fakemon.core.api;

import fr.epitech.fakemon.core.model.Fakemon;

/**
 * Représente la récupération de points de vie d'un Fakemon
 * pendant un combat.
 *
 * Cet événement contient le camp du Fakemon soigné,
 * le Fakemon concerné, les PV récupérés et les PV restants.
 */
public class HpRestoredEvent extends BattleEvent {

    private final Side side;
    private final Fakemon fakemon;
    private final int hpRestored;
    private final int remainingHp;

    /**
     * Construit un événement de récupération de points de vie.
     *
     * @param side        le camp du Fakemon soigné
     * @param fakemon     le Fakemon qui récupère des points de vie
     * @param hpRestored  le nombre de points de vie récupérés
     * @param remainingHp le nombre de points de vie après le soin
     */
    public HpRestoredEvent(Side side, Fakemon fakemon, int hpRestored, int remainingHp) {
        this.side = side;
        this.fakemon = fakemon;
        this.hpRestored = hpRestored;
        this.remainingHp = remainingHp;
    }

    /**
     * Retourne le camp du Fakemon soigné.
     *
     * @return le camp du Fakemon soigné
     */
    public Side getSide() {
        return side;
    }

    /**
     * Retourne le Fakemon qui récupère des points de vie.
     *
     * @return le Fakemon soigné
     */
    public Fakemon getFakemon() {
        return fakemon;
    }

    /**
     * Retourne le nombre de points de vie récupérés.
     *
     * @return le nombre de PV récupérés
     */
    public int getHpRestored() {
        return hpRestored;
    }

    /**
     * Retourne le nombre de points de vie après le soin.
     *
     * @return le nombre de PV restants
     */
    public int getRemainingHp() {
        return remainingHp;
    }
}