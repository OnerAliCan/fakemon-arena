package fr.epitech.fakemon.core.api;

import fr.epitech.fakemon.core.model.Fakemon;
import fr.epitech.fakemon.core.model.Item;

/**
 * Représente l'activation d'un objet tenu par un Fakemon
 * pendant un combat.
 *
 * Cet événement contient le camp du Fakemon concerné,
 * le Fakemon porteur et l'objet qui s'active.
 */
public class ItemActivatedEvent extends BattleEvent {

    private final Side side;
    private final Fakemon fakemon;
    private final Item item;

    /**
     * Construit un événement d'activation d'objet.
     *
     * @param side    le camp du Fakemon concerné
     * @param fakemon le Fakemon dont l'objet s'active
     * @param item    l'objet activé
     */
    public ItemActivatedEvent(Side side, Fakemon fakemon, Item item) {
        this.side = side;
        this.fakemon = fakemon;
        this.item = item;
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
     * Retourne le Fakemon dont l'objet s'active.
     *
     * @return le Fakemon concerné
     */
    public Fakemon getFakemon() {
        return fakemon;
    }

    /**
     * Retourne l'objet activé.
     *
     * @return l'objet activé
     */
    public Item getItem() {
        return item;
    }
}