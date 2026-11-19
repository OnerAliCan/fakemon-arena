package fr.epitech.fakemon.core.api;

import fr.epitech.fakemon.core.model.Fakemon;
import fr.epitech.fakemon.core.model.StatusType;

/** Annonce qu'un Fakemon a reçu un nouveau statut. */
public final class StatusAppliedEvent extends BattleEvent {
    private final Side side;
    private final Fakemon fakemon;
    private final StatusType status;

    /**
     * @param side camp du Fakemon touché
     * @param fakemon Fakemon qui reçoit le statut
     * @param status statut appliqué
     */
    public StatusAppliedEvent(Side side, Fakemon fakemon, StatusType status) {
        this.side = side;
        this.fakemon = fakemon;
        this.status = status;
    }

    /** @return le camp du Fakemon */
    public Side getSide() { return side; }

    /** @return le Fakemon concerné */
    public Fakemon getFakemon() { return fakemon; }

    /** @return le statut appliqué */
    public StatusType getStatus() { return status; }
}
