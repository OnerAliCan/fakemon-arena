package fr.epitech.fakemon.core.effects;

import fr.epitech.fakemon.core.model.Fakemon;
import fr.epitech.fakemon.core.model.StatusType;
import java.util.Random;

/** Bloque une à trois tentatives d'attaque, puis laisse le Fakemon se réveiller et agir. */
public final class Sleep extends Status {
    private int remaining = -1;
    private boolean ended;

    /** Crée un sommeil dont la durée sera tirée à la première tentative d'attaque. */
    public Sleep() { }

    @Override
    public StatusType getType() {
        return StatusType.SLEEP;
    }

    @Override
    public boolean canApply(Fakemon target) {
        return true;
    }

    @Override
    public boolean preventsAttack(Random random) {
        if (remaining < 0) {
            remaining = 1 + random.nextInt(3);
        }
        if (remaining == 0) {
            ended = true;
            return false;
        }
        remaining--;
        return true;
    }

    @Override
    public boolean hasEnded() {
        return ended;
    }

    @Override
    public int endOfTurnDamage(Fakemon holder) {
        return 0;
    }
}
