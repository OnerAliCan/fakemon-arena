package fr.epitech.fakemon.core.api;

import fr.epitech.fakemon.core.model.Fakemon;
import fr.epitech.fakemon.core.model.StatusType;

/** Annonce la fin d'un statut pendant le combat, avant que le Fakemon puisse agir. */
public final class StatusRemovedEvent extends BattleEvent {
    private final Side side;
    private final Fakemon fakemon;
    private final StatusType status;

    /**
     * @param side camp du Fakemon concerné
     * @param fakemon Fakemon libéré du statut
     * @param status statut qui vient de se terminer
     */
    public StatusRemovedEvent(Side side, Fakemon fakemon, StatusType status) {
        this.side = side;
        this.fakemon = fakemon;
        this.status = status;
    }

    /** @return le camp du Fakemon */
    public Side getSide() { return side; }

    /** @return le Fakemon concerné */
    public Fakemon getFakemon() { return fakemon; }

    /** @return le statut qui s'est terminé */
    public StatusType getStatus() { return status; }
}
