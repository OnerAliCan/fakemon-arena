package fr.epitech.fakemon.core.api;

import fr.epitech.fakemon.core.model.Fakemon;
import fr.epitech.fakemon.core.model.StatusType;

/** Annonce l'activation d'un statut avant ses conséquences sur les PV. */
public final class StatusActivatedEvent extends BattleEvent {
    private final Side side;
    private final Fakemon fakemon;
    private final StatusType status;

    /**
     * @param side camp du Fakemon concerné
     * @param fakemon Fakemon qui subit son statut
     * @param status statut qui s'active
     */
    public StatusActivatedEvent(Side side, Fakemon fakemon, StatusType status) {
        this.side = side;
        this.fakemon = fakemon;
        this.status = status;
    }

    /** @return le camp du Fakemon */
    public Side getSide() { return side; }

    /** @return le Fakemon concerné */
    public Fakemon getFakemon() { return fakemon; }

    /** @return le statut qui s'active */
    public StatusType getStatus() { return status; }
}
