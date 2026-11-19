package fr.epitech.fakemon.core.api;

import fr.epitech.fakemon.core.model.Fakemon;
import fr.epitech.fakemon.core.model.StatusType;

/** Annonce qu'un statut empêche le Fakemon d'exécuter son attaque. */
public final class AttackPreventedEvent extends BattleEvent {
    private final Side side;
    private final Fakemon fakemon;
    private final StatusType status;

    /**
     * @param side camp de l'attaquant
     * @param fakemon Fakemon qui ne peut pas attaquer
     * @param status statut responsable du blocage
     */
    public AttackPreventedEvent(Side side, Fakemon fakemon, StatusType status) {
        this.side = side;
        this.fakemon = fakemon;
        this.status = status;
    }

    /** @return le camp du Fakemon */
    public Side getSide() { return side; }

    /** @return le Fakemon concerné */
    public Fakemon getFakemon() { return fakemon; }

    /** @return le statut qui bloque l'attaque */
    public StatusType getStatus() { return status; }
}
