package fr.epitech.fakemon.core.engine;

/**
 * Le résultat d'une attaque qui a touché : les dégâts, le critique et le multiplicateur d'efficacité.
 * Ses valeurs ne changent plus une fois l'objet créé.
 */
public class DamageResult {

    private final int damage;
    private final boolean critical;
    private final double multiplier;

    public DamageResult(int damage, boolean critical, double multiplier) {
        this.damage = damage;
        this.critical = critical;
        this.multiplier = multiplier;
    }

    public int getDamage() {
        return this.damage;
    }

    public boolean isCritical() {
        return this.critical;
    }

    public double getMultiplier() {
        return this.multiplier;
    }
}
