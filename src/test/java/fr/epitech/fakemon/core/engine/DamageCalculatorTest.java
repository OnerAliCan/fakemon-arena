package fr.epitech.fakemon.core.engine;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import fr.epitech.fakemon.core.model.BaseStats;
import fr.epitech.fakemon.core.model.Fakemon;
import fr.epitech.fakemon.core.model.Move;
import fr.epitech.fakemon.core.model.MoveCategory;
import fr.epitech.fakemon.core.model.Species;
import fr.epitech.fakemon.core.model.Type;
import java.util.List;
import java.util.Random;
import org.junit.jupiter.api.Test;

/** Vérifie la précision et la formule de dégâts, avec des graines fixes pour rester reproductible. */
class DamageCalculatorTest {

    /** Avec cette graine, le premier tirage de calculate ne donne pas de critique. */
    private static final long NO_CRITICAL_SEED = 50;

    /** Avec cette graine, le premier tirage de calculate donne un critique. */
    private static final long CRITICAL_SEED = 4608;

    private static final BaseStats NEUTRAL_STATS = new BaseStats(
        100,
        100,
        100,
        100,
        100,
        100
    );

    @Test
    void accuracyZeroAlwaysMisses() {
        TypeChart typeChart = new TypeChart();
        Random random = new Random(50);
        Move move = createMove(Type.FIRE, MoveCategory.PHYSICAL, 65, 0);

        DamageCalculator damageCalculator = new DamageCalculator(
            random,
            typeChart
        );
        assertFalse(damageCalculator.rollAccuracy(move));
    }

    @Test
    void accuracyHundredAlwaysHits() {
        DamageCalculator damageCalculator = createCalculator(NO_CRITICAL_SEED);
        Move move = createMove(Type.FIRE, MoveCategory.PHYSICAL, 65, 100);

        assertTrue(damageCalculator.rollAccuracy(move));
    }

    @Test
    void sameTypeBonusAddsDamage() {
        Fakemon sameType = createFakemon(List.of(Type.FORCE), NEUTRAL_STATS);
        Fakemon otherType = createFakemon(List.of(Type.WATER), NEUTRAL_STATS);
        Fakemon target = createFakemon(List.of(Type.FIRE), NEUTRAL_STATS);
        Move move = createMove(Type.FORCE, MoveCategory.PHYSICAL, 65, 100);

        int withBonus = createCalculator(NO_CRITICAL_SEED)
            .calculate(sameType, target, move)
            .getDamage();
        int withoutBonus = createCalculator(NO_CRITICAL_SEED)
            .calculate(otherType, target, move)
            .getDamage();

        assertTrue(withBonus > withoutBonus);
    }

    @Test
    void superEffectiveDoublesDamage() {
        Fakemon attacker = createFakemon(List.of(Type.ROCK), NEUTRAL_STATS);
        Fakemon weakTarget = createFakemon(List.of(Type.FIRE), NEUTRAL_STATS);
        Fakemon neutralTarget = createFakemon(List.of(Type.FORCE), NEUTRAL_STATS);
        Move move = createMove(Type.WATER, MoveCategory.PHYSICAL, 65, 100);

        DamageResult superEffective = createCalculator(NO_CRITICAL_SEED).calculate(
            attacker,
            weakTarget,
            move
        );
        DamageResult neutral = createCalculator(NO_CRITICAL_SEED).calculate(
            attacker,
            neutralTarget,
            move
        );

        assertEquals(2, superEffective.getMultiplier());
        assertTrue(superEffective.getDamage() >= 2 * neutral.getDamage());
        assertTrue(superEffective.getDamage() <= 2 * neutral.getDamage() + 1);
    }

    @Test
    void immuneTargetTakesNoDamage() {
        Fakemon attacker = createFakemon(List.of(Type.LIGHTNING), NEUTRAL_STATS);
        Fakemon target = createFakemon(List.of(Type.ROCK), NEUTRAL_STATS);
        Move move = createMove(Type.LIGHTNING, MoveCategory.SPECIAL, 65, 100);

        DamageResult result = createCalculator(NO_CRITICAL_SEED).calculate(
            attacker,
            target,
            move
        );

        assertEquals(0, result.getDamage());
        assertEquals(0, result.getMultiplier());
    }

    @Test
    void criticalHitAddsDamage() {
        Fakemon attacker = createFakemon(List.of(Type.ROCK), NEUTRAL_STATS);
        Fakemon target = createFakemon(List.of(Type.FORCE), NEUTRAL_STATS);
        Move move = createMove(Type.WATER, MoveCategory.PHYSICAL, 65, 100);

        DamageResult critical = createCalculator(CRITICAL_SEED).calculate(
            attacker,
            target,
            move
        );
        DamageResult normal = createCalculator(NO_CRITICAL_SEED).calculate(
            attacker,
            target,
            move
        );

        assertTrue(critical.isCritical());
        assertFalse(normal.isCritical());
        assertTrue(critical.getDamage() > normal.getDamage());
    }

    @Test
    void weakHitDealsAtLeastOneDamage() {
        Fakemon attacker = createFakemon(
            List.of(Type.ROCK),
            new BaseStats(100, 1, 100, 100, 100, 100)
        );
        Fakemon target = createFakemon(
            List.of(Type.WATER, Type.FIRE),
            new BaseStats(100, 100, 255, 100, 100, 100)
        );
        Move move = createMove(Type.FIRE, MoveCategory.PHYSICAL, 1, 100);

        DamageResult result = createCalculator(NO_CRITICAL_SEED).calculate(
            attacker,
            target,
            move
        );

        assertEquals(1, result.getDamage());
    }

    @Test
    void physicalMoveMatchesGddExample() {
        Fakemon attacker = createFakemon(
            List.of(Type.WATER),
            new BaseStats(100, 90, 100, 100, 100, 100)
        );
        Fakemon target = createFakemon(
            List.of(Type.FIRE, Type.VENOM),
            new BaseStats(75, 100, 65, 100, 100, 100)
        );
        Move move = createMove(Type.WATER, MoveCategory.PHYSICAL, 65, 100);

        int damage = createCalculator(NO_CRITICAL_SEED)
            .calculate(attacker, target, move)
            .getDamage();

        assertTrue(damage >= 52 && damage <= 61);
    }

    @Test
    void specialMoveUsesSpecialStats() {
        Fakemon attacker = createFakemon(
            List.of(Type.FIRE, Type.VENOM),
            new BaseStats(75, 10, 10, 85, 10, 10)
        );
        Fakemon target = createFakemon(
            List.of(Type.WATER),
            new BaseStats(100, 200, 200, 10, 70, 10)
        );
        Move move = createMove(Type.FIRE, MoveCategory.SPECIAL, 65, 100);

        int damage = createCalculator(NO_CRITICAL_SEED)
            .calculate(attacker, target, move)
            .getDamage();

        assertTrue(damage >= 11 && damage <= 13);
    }

    @Test
    void sameSeedGivesSameDamage() {
        Fakemon attacker = createFakemon(List.of(Type.ROCK), NEUTRAL_STATS);
        Fakemon target = createFakemon(List.of(Type.FORCE), NEUTRAL_STATS);
        Move move = createMove(Type.WATER, MoveCategory.PHYSICAL, 65, 100);

        DamageResult first = createCalculator(NO_CRITICAL_SEED).calculate(
            attacker,
            target,
            move
        );
        DamageResult second = createCalculator(NO_CRITICAL_SEED).calculate(
            attacker,
            target,
            move
        );

        assertEquals(first.getDamage(), second.getDamage());
        assertEquals(first.isCritical(), second.isCritical());
    }

    private DamageCalculator createCalculator(long seed) {
        return new DamageCalculator(new Random(seed), new TypeChart());
    }

    private Fakemon createFakemon(List<Type> types, BaseStats stats) {
        Species species = new Species("Test", "Test", types, stats, List.of());
        Move move = createMove(Type.FIRE, MoveCategory.PHYSICAL, 65, 100);
        return new Fakemon(species, List.of(move, move, move, move));
    }

    private Move createMove(
        Type type,
        MoveCategory category,
        int power,
        int accuracy
    ) {
        return new Move("Test", type, category, power, accuracy, 0);
    }
}
