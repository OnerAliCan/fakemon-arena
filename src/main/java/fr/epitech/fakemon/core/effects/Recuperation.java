package fr.epitech.fakemon.core.effects;

import fr.epitech.fakemon.core.model.Fakemon;
import fr.epitech.fakemon.core.model.Talent;

/** Rend jusqu'à 10 % des PV max au porteur en fin de tour. */
public final class Recuperation extends Ability {
    @Override
    public int endOfTurnHeal(Fakemon holder) {
        if (holder.isFainted() || holder.getSpecies().getTalent() != Talent.RECUPERATION) {
            return 0;
        }
        return Math.min(holder.getMaxHp() / 10, holder.getMaxHp() - holder.getCurrentHp());
    }
}
