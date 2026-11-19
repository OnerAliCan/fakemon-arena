package fr.epitech.fakemon.core.model;

import java.util.List;
import java.util.Objects;
import fr.epitech.fakemon.core.effects.Status;

/**
 * Un mon pendant un combat : son espèce, ses 4 attaques et ses PV actuels.
 */
public class Fakemon {

    private final Species species;
    private final List<Move> moves;
    private int currentHp;
    private Item item;
    private Stats battleStats;
    private Status status;

    /** @return le statut courant, ou null si le Fakemon n'en porte aucun */
    public Status getStatus() {
        return status;
    }

    /**
     * Applique un statut sans remplacer un état existant, en respectant les immunités.
     * @param effect nouvel état à appliquer
     * @return true si le statut a été appliqué
     */
    public boolean applyStatus(Status effect) {
        Objects.requireNonNull(effect);
        if (status != null || isFainted() || !effect.canApply(this)) {
            return false;
        }
        status = effect;
        return true;
    }

    /** Retire le statut lorsque le combat se termine. */
    public void clearStatus() {
        status = null;
    }

    /**
     * Crée un Fakemon avec tous ses PV.
     *
     * @param species la fiche de son espèce
     * @param moves ses quatre attaques de combat
     * @throws IllegalArgumentException si la liste ne contient pas exactement 4 attaques
     */
    public Fakemon(Species species, List<Move> moves) {
        if (moves.size() != 4) {
            throw new IllegalArgumentException(
                "Un Fakemon doit avoir 4 attaques."
            );
        }
        this.species = species;
        this.moves = moves;
        this.currentHp = species.getStats().getHp();
    }

    /**
     * Donne l'espèce du Fakemon.
     * @return sa fiche du catalogue
     */
    public Species getSpecies() {
        return this.species;
    }

    /**
     * Donne les attaques choisies pour le combat.
     * @return les quatre attaques du Fakemon
     */
    public List<Move> getMoves() {
        return this.moves;
    }

    /**
     * Donne les points de vie restants.
     * @return les PV actuels
     */
    public int getCurrentHp() {
        return this.currentHp;
    }

    /**
     * Donne les statistiques de combat avec les bonus de l'objet tenu et les effets du statut.
     * @return les statistiques de base ou leur vue décorée par l'objet et le statut
     */
    public Stats getStats() {
        Stats stats = battleStats == null ? species.getStats() : battleStats;
        return status == null ? stats : status.decorate(stats);
    }

    /**
     * Donne l'objet tenu, ou null si le Fakemon n'en a pas.
     * @return l'objet tenu, ou null si le Fakemon n'en a pas
     */
    public Item getItem() {
        return item;
    }

    /**
     * Équipe un objet et recalcule les bonus depuis les statistiques de base.
     *
     * @param item l'objet à tenir, ou null pour le retirer
     */
    public void equip(Item item) {
        this.item = item;
        battleStats = item == null ? null : item.decorate(species.getStats());
    }

    /**
     * Donne le maximum de points de vie défini par l'espèce.
     * @return les PV maximum
     */
    public int getMaxHp() {
        return species.getStats().getHp();
    }

    /**
     * Indique si le Fakemon est K.O.
     * @return true si ses PV sont nuls
     */
    public boolean isFainted() {
        if (this.currentHp > 0) {
            return false;
        }
        return true;
    }

    /**
     * Retire des PV, sans descendre sous 0.
     * @param damageTaken les dégâts à retirer
     */
    public void takeDamage(int damageTaken) {
        currentHp = Math.max(currentHp - damageTaken, 0);
    }

    /**
     * Rend des PV, sans dépasser le maximum.
     * @param healedHp les PV à rendre
     */
    public void heal(int healedHp) {
        currentHp = Math.min(currentHp + healedHp, getMaxHp());
    }
}
