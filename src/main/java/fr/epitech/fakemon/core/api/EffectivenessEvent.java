package fr.epitech.fakemon.core.api;

import fr.epitech.fakemon.core.model.Fakemon;

/**
 * Représente le résultat d'efficacité d'une attaque
 * sur un Fakemon pendant un combat.
 *
 * Cet événement contient le camp ciblé, le Fakemon
 * visé et le résultat d'efficacité de l'attaque.
 */
public class EffectivenessEvent extends BattleEvent {

    private final Side side;
    private final Fakemon fakemon;
    private final Effectiveness effectiveness;

    /**
     * Construit un événement d'efficacité d'attaque.
     *
     * @param side          le camp du Fakemon ciblé
     * @param fakemon       le Fakemon qui reçoit l'attaque
     * @param effectiveness le résultat d'efficacité de l'attaque
     */
    public EffectivenessEvent(Side side, Fakemon fakemon, Effectiveness effectiveness) {
        this.side = side;
        this.fakemon = fakemon;
        this.effectiveness = effectiveness;
    }

    /**
     * Retourne le camp du Fakemon ciblé.
     *
     * @return le camp du Fakemon ciblé
     */
    public Side getSide() {
        return side;
    }

    /**
     * Retourne le Fakemon qui reçoit l'attaque.
     *
     * @return le Fakemon ciblé
     */
    public Fakemon getFakemon() {
        return fakemon;
    }

    /**
     * Retourne le résultat d'efficacité de l'attaque.
     *
     * @return le résultat d'efficacité
     */
    public Effectiveness getEffectiveness() {
        return effectiveness;
    }
}