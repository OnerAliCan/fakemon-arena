package fr.epitech.fakemon.core.model;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class ItemTest {
    private final Stats base = new BaseStats(100, 75, 80, 95, 100, 105);

    @Test
    void ganteletIncreasesBothAttackStats() {
        Stats stats = Item.GANTELET.decorate(base);
        assertEquals(90, stats.getAttack(), 0.001);
        assertEquals(114, stats.getSpecialAttack(), 0.001);
        assertEquals(base.getDefense(), stats.getDefense());
        assertEquals(base.getSpecialDefense(), stats.getSpecialDefense());
        assertEquals(base.getSpeed(), stats.getSpeed());
        assertEquals(base.getHp(), stats.getHp());
    }

    @Test
    void ecuIncreasesBothDefenseStats() {
        Stats stats = Item.ECU.decorate(base);
        assertEquals(96, stats.getDefense(), 0.001);
        assertEquals(120, stats.getSpecialDefense(), 0.001);
        assertEquals(base.getAttack(), stats.getAttack());
        assertEquals(base.getSpecialAttack(), stats.getSpecialAttack());
        assertEquals(base.getSpeed(), stats.getSpeed());
        assertEquals(base.getHp(), stats.getHp());
    }

    @Test
    void plumeOnlyIncreasesSpeed() {
        Stats stats = Item.PLUME.decorate(base);
        assertEquals(126, stats.getSpeed(), 0.001);
        assertEquals(base.getAttack(), stats.getAttack());
        assertEquals(base.getSpecialAttack(), stats.getSpecialAttack());
        assertEquals(base.getDefense(), stats.getDefense());
        assertEquals(base.getSpecialDefense(), stats.getSpecialDefense());
        assertEquals(base.getHp(), stats.getHp());
    }
}
