package fr.epitech.fakemon.core.effects;

import fr.epitech.fakemon.core.model.Fakemon;
import fr.epitech.fakemon.core.model.StatusType;
import fr.epitech.fakemon.core.model.Type;

/** Retire un seizième des PV maximum en fin de tour ; les types Feu sont immunisés. */
public final class Burn extends Status {
    /** Crée une brûlure sans limite de durée pendant le combat. */
    public Burn() { }

    @Override
    public StatusType getType() {
        return StatusType.BURN;
    }

    @Override
    public boolean canApply(Fakemon target) {
        return !target.getSpecies().getTypes().contains(Type.FIRE);
    }

    @Override
    public int endOfTurnDamage(Fakemon holder) {
        return Math.min(holder.getCurrentHp(), Math.max(1, holder.getMaxHp() / 16));
    }
}
