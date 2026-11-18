package fr.epitech.fakemon.core.effects;

import fr.epitech.fakemon.core.model.Fakemon;
import fr.epitech.fakemon.core.model.Talent;

/** Reduit de 25 % les degats super efficaces recus par son porteur. */
public final class Carapace extends Ability {
    /** Crée le talent Carapace. */
    public Carapace() {}

    @Override
    public double receivedDamageMultiplier(Fakemon defender, double typeMultiplier) {
        return defender.getSpecies().getTalent() == Talent.CARAPACE && typeMultiplier > 1.0
            ? 0.75 : 1.0;
    }
}
