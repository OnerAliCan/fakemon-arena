package fr.epitech.fakemon.core.effects;

import fr.epitech.fakemon.core.model.Fakemon;
import fr.epitech.fakemon.core.model.Stats;
import fr.epitech.fakemon.core.model.StatusType;
import fr.epitech.fakemon.core.model.Type;
import java.util.Random;

/** Divise la vitesse par deux et empêche une attaque sur quatre, sauf sur les types Foudre et Roche. */
public final class Paralysis extends Status {
    /** Crée une paralysie qui persiste jusqu'à la fin du combat. */
    public Paralysis() { }

    @Override
    public StatusType getType() {
        return StatusType.PARALYSIS;
    }

    @Override
    public boolean canApply(Fakemon target) {
        var types = target.getSpecies().getTypes();
        return !types.contains(Type.LIGHTNING) && !types.contains(Type.ROCK);
    }

    @Override
    public Stats decorate(Stats stats) {
        return new ItemStats(stats, 1, 1, 0.5);
    }

    @Override
    public boolean preventsAttack(Random random) {
        return random.nextInt(100) < 25;
    }

    @Override
    public int endOfTurnDamage(Fakemon holder) {
        return 0;
    }
}
