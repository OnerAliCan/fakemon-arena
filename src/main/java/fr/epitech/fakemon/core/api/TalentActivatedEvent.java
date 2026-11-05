package fr.epitech.fakemon.core.api;

import fr.epitech.fakemon.core.model.Fakemon;
import fr.epitech.fakemon.core.model.Talent;

/**
 * Représente l'activation d'un talent d'un Fakemon
 * pendant un combat.
 *
 * Cet événement contient le camp du Fakemon concerné,
 * le Fakemon dont le talent s'active et le talent activé.
 */
public class TalentActivatedEvent extends BattleEvent {

    private final Side side;
    private final Fakemon fakemon;
    private final Talent talent;

    /**
     * Construit un événement d'activation de talent.
     *
     * @param side    le camp du Fakemon concerné
     * @param fakemon le Fakemon dont le talent s'active
     * @param talent  le talent qui s'active
     */
    public TalentActivatedEvent(Side side, Fakemon fakemon, Talent talent) {
        this.side = side;
        this.fakemon = fakemon;
        this.talent = talent;
    }

    /**
     * Retourne le camp du Fakemon concerné.
     *
     * @return le camp du Fakemon
     */
    public Side getSide() {
        return side;
    }

    /**
     * Retourne le Fakemon dont le talent s'active.
     *
     * @return le Fakemon concerné
     */
    public Fakemon getFakemon() {
        return fakemon;
    }

    /**
     * Retourne le talent qui s'active.
     *
     * @return le talent activé
     */
    public Talent getTalent() {
        return talent;
    }
}