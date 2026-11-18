package fr.epitech.fakemon.core.model;

import java.util.List;

/**
 * Un mon pendant un combat : son espèce, ses 4 attaques et ses PV actuels.
 */
public class Fakemon {

    private final Species species;
    private final List<Move> moves;
    private int currentHp;
    private Item item;
    private Stats battleStats;

    /**
     * Crée un Fakemon avec tous ses PV.
     *
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

    public Species getSpecies() {
        return this.species;
    }

    public List<Move> getMoves() {
        return this.moves;
    }

    public int getCurrentHp() {
        return this.currentHp;
    }

    public Stats getStats() {
        return battleStats == null ? species.getStats() : battleStats;
    }

    /** @return l'objet tenu, ou null si le Fakemon n'en a pas */
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

    public int getMaxHp() {
        return species.getStats().getHp();
    }

    public boolean isFainted() {
        if (this.currentHp > 0) {
            return false;
        }
        return true;
    }

    /** Retire des PV, sans descendre sous 0. */
    public void takeDamage(int damageTaken) {
        currentHp = Math.max(currentHp - damageTaken, 0);
    }

    /** Rend des PV, sans dépasser le maximum. */
    public void heal(int healedHp) {
        currentHp = Math.min(currentHp + healedHp, getMaxHp());
    }
}
