package fr.epitech.fakemon.core.engine;

import fr.epitech.fakemon.core.model.Talent;

/**
 * Le résultat d'une attaque qui a touché : les dégâts, le critique et le multiplicateur d'efficacité.
 * Ses valeurs ne changent plus une fois l'objet créé.
 */
public class DamageResult {

    private final int damage;
    private final boolean critical;
    private final double multiplier;
    private final Talent attackerTalent;
    private final Talent defenderTalent;

    /**
     * Crée un résultat sans activation de talent.
     *
     * @param damage les dégâts calculés
     * @param critical indique si le coup est critique
     * @param multiplier le multiplicateur d'efficacité des types
     */
    public DamageResult(int damage, boolean critical, double multiplier) {
        this(damage, critical, multiplier, null, null);
    }

    /**
     * Cree le resultat avec les talents actives par le coup.
     *
     * @param damage les degats calcules
     * @param critical indique si le coup est critique
     * @param multiplier l'efficacite de type
     * @param attackerTalent le talent active cote attaquant, ou null
     * @param defenderTalent le talent active cote defenseur, ou null
     */
    public DamageResult(int damage, boolean critical, double multiplier, Talent attackerTalent,
            Talent defenderTalent) {
        this.attackerTalent = attackerTalent;
        this.defenderTalent = defenderTalent;
        this.damage = damage;
        this.critical = critical;
        this.multiplier = multiplier;
    }

    /**
     * Donne le talent active par l'attaquant, ou null.
     * @return le talent active par l'attaquant, ou null
     */
    public Talent getAttackerTalent() {
        return attackerTalent;
    }

    /**
     * Donne le talent active par le defenseur, ou null.
     * @return le talent active par le defenseur, ou null
     */
    public Talent getDefenderTalent() {
        return defenderTalent;
    }

    /**
     * Donne les dégâts calculés avant leur application à la cible.
     * @return les points de dégâts
     */
    public int getDamage() {
        return this.damage;
    }

    /**
     * Indique si le calcul a produit un coup critique.
     * @return true si le coup est critique
     */
    public boolean isCritical() {
        return this.critical;
    }

    /**
     * Donne l'efficacité des types de l'attaque contre la cible.
     * @return le multiplicateur de la table des types
     */
    public double getMultiplier() {
        return this.multiplier;
    }
}
