package fr.epitech.fakemon.core.model;

/** Les stats de base d'une espèce, telles que le GDD les donne, sans aucun objet. */
public class BaseStats implements Stats {

    private final int hp;
    private final int attack;
    private final int defense;
    private final int specialAttack;
    private final int specialDefense;
    private final int speed;

    /**
     * Conserve les six statistiques de base de l'espèce.
     *
     * @param hp les PV maximum
     * @param attack l'attaque physique
     * @param defense la défense physique
     * @param specialAttack l'attaque spéciale
     * @param specialDefense la défense spéciale
     * @param speed la vitesse
     */
    public BaseStats(
        int hp,
        int attack,
        int defense,
        int specialAttack,
        int specialDefense,
        int speed
    ) {
        this.hp = hp;
        this.attack = attack;
        this.defense = defense;
        this.specialAttack = specialAttack;
        this.specialDefense = specialDefense;
        this.speed = speed;
    }

    @Override
    public int getHp() {
        return this.hp;
    }

    @Override
    public double getAttack() {
        return this.attack;
    }

    @Override
    public double getDefense() {
        return this.defense;
    }

    @Override
    public double getSpecialAttack() {
        return this.specialAttack;
    }

    @Override
    public double getSpecialDefense() {
        return this.specialDefense;
    }

    @Override
    public double getSpeed() {
        return this.speed;
    }
}
