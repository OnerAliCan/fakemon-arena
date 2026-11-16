package fr.epitech.fakemon.core.effects;

import fr.epitech.fakemon.core.model.Fakemon;

/** Effet d'un talent pendant un combat. */
public abstract class Ability {
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
     * @param entrant le Fakemon qui entre
     * @param target le Fakemon en face
     * @return les dégâts à appliquer à l'entrée
     */
    public int entryDamage(Fakemon entrant, Fakemon target) {
        return 0;
    }

    /**
     * @param holder le porteur du talent
     * @return les PV à rendre en fin de tour
     */
    public int endOfTurnHeal(Fakemon holder) {
        return 0;
    }
}
