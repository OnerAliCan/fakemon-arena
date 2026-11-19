package fr.epitech.fakemon.core.effects;

import fr.epitech.fakemon.core.model.Stats;

/** Décore les statistiques avec des coefficients d'attaque, de défense et de vitesse. */
public final class ItemStats implements Stats {
    private final Stats base;
    private final double attack;
    private final double defense;
    private final double speed;

    /**
     * Prépare les coefficients à appliquer sans modifier les statistiques reçues.
     *
     * @param base les statistiques à décorer
     * @param attack le coefficient des deux statistiques d'attaque
     * @param defense le coefficient des deux statistiques de défense
     * @param speed le coefficient de vitesse
     */
    public ItemStats(Stats base, double attack, double defense, double speed) {
        this.base = base;
        this.attack = attack;
        this.defense = defense;
        this.speed = speed;
    }

    @Override
    public int getHp() {
        return base.getHp();
    }

    @Override
    public double getAttack() {
        return base.getAttack() * attack;
    }

    @Override
    public double getSpecialAttack() {
        return base.getSpecialAttack() * attack;
    }

    @Override
    public double getDefense() {
        return base.getDefense() * defense;
    }

    @Override
    public double getSpecialDefense() {
        return base.getSpecialDefense() * defense;
    }

    @Override
    public double getSpeed() {
        return base.getSpeed() * speed;
    }
}
