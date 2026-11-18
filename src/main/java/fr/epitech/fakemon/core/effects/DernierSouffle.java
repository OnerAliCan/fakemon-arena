package fr.epitech.fakemon.core.effects;

import fr.epitech.fakemon.core.model.Fakemon;
import fr.epitech.fakemon.core.model.Talent;

/** Augmente les dégâts infligés quand le porteur passe sous un tiers de ses PV max. */
public final class DernierSouffle extends Ability {
    /** Crée le talent Dernier Souffle. */
    public DernierSouffle() {}


    @Override
    public double dealtDamageMultiplier(Fakemon attacker) {
        if (attacker.getSpecies().getTalent() == Talent.DERNIER_SOUFFLE
                && 3L * attacker.getCurrentHp() < attacker.getMaxHp()) {
            return 1.5;
        }
        return 1.0;
    }
}
