package fr.epitech.fakemon.core.effects;

import fr.epitech.fakemon.core.model.Fakemon;
import fr.epitech.fakemon.core.model.StatusType;
import fr.epitech.fakemon.core.model.Type;

/** Retire un huitième des PV maximum en fin de tour ; les types Venin sont immunisés. */
public final class Poison extends Status {
    /** Crée un empoisonnement sans limite de durée pendant le combat. */
    public Poison() { }

    @Override
    public StatusType getType() {
        return StatusType.POISON;
    }

    @Override
    public boolean canApply(Fakemon target) {
        return !target.getSpecies().getTypes().contains(Type.VENOM);
    }

    @Override
    public int endOfTurnDamage(Fakemon holder) {
        return Math.min(holder.getCurrentHp(), Math.max(1, holder.getMaxHp() / 8));
    }
}
