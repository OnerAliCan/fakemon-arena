package fr.epitech.fakemon.core.effects;

import fr.epitech.fakemon.core.model.Fakemon;

/** Effet d'un talent pendant un combat. */
public abstract class Ability {
    /** Prépare un talent et ses effets de combat. */
    public Ability() {}

    /**
     * Donne le coefficient de degats recus.
     *
     * @param defender le Fakemon touche
     * @param typeMultiplier l'efficacite de l'attaque
     * @return le coefficient, ou 1 si le talent ne s'active pas
     */
    public double receivedDamageMultiplier(Fakemon defender, double typeMultiplier) {
        return 1.0;
    }

    /**
     * Donne le coefficient de dégâts infligés.
     *
     * @param attacker le Fakemon qui attaque
     * @return le coefficient, ou 1 si le talent ne s'active pas
     */
    public double dealtDamageMultiplier(Fakemon attacker) {
        return 1.0;
    }

    /**
     * Calcule les dégâts du talent à l'entrée en jeu.
     *
     * @param entrant le Fakemon qui entre
     * @param target le Fakemon en face
     * @return les dégâts à appliquer à l'entrée
     */
    public int entryDamage(Fakemon entrant, Fakemon target) {
        return 0;
    }

    /**
     * Calcule les PV rendus par le talent en fin de tour.
     *
     * @param holder le porteur du talent
     * @return les PV à rendre en fin de tour
     */
    public int endOfTurnHeal(Fakemon holder) {
        return 0;
    }
}
