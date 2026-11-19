package fr.epitech.fakemon.core.effects;

import fr.epitech.fakemon.core.model.Fakemon;
import fr.epitech.fakemon.core.model.StatusType;
import java.util.Random;

/** Pendant trois tentatives, le Fakemon risque une fois sur trois de se blesser au lieu d'attaquer. */
public final class Confusion extends Status {
    private int remaining = 3;

    /** Crée une confusion valable pour trois tentatives d'attaque. */
    public Confusion() { }

    @Override
    public StatusType getType() {
        return StatusType.CONFUSION;
    }

    @Override
    public boolean canApply(Fakemon target) {
        return true;
    }

    @Override
    public boolean preventsAttack(Random random) {
        if (remaining == 0) {
            return false;
        }
        remaining--;
        return random.nextInt(3) == 0;
    }

    @Override
    public int preventedAttackDamage(Fakemon holder) {
        return Math.min(holder.getCurrentHp(), Math.max(1, holder.getMaxHp() / 8));
    }

    @Override
    public boolean hasEnded() {
        return remaining == 0;
    }

    @Override
    public int endOfTurnDamage(Fakemon holder) {
        return 0;
    }
}
