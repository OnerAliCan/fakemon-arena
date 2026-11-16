package fr.epitech.fakemon.core.effects;

import fr.epitech.fakemon.core.model.Fakemon;
import fr.epitech.fakemon.core.model.Talent;

/** Inflige 10 % des PV max à l'adversaire à l'entrée, en lui laissant au moins un PV. */
public final class Fracas extends Ability {
    @Override
    public int entryDamage(Fakemon entrant, Fakemon target) {
        if (entrant.getSpecies().getTalent() != Talent.FRACAS || target == null || target.isFainted()) {
            return 0;
        }
        return Math.min(target.getMaxHp() / 10, target.getCurrentHp() - 1);
    }
}
