package fr.epitech.fakemon.core.model;

/**
 * Les statistiques d'un Fakemon. Les 5 stats de combat sont des nombres à virgule,
 * car les objets les multiplient et le GDD n'arrondit les dégâts qu'à la fin du calcul.
 * Les PV restent entiers, car les objets ne les changent pas.
 */
public interface Stats {
    /**
     * Donne le maximum de points de vie.
     * @return les PV maximum
     */
    public int getHp();

    /**
     * Donne l'attaque physique.
     * @return l'attaque physique
     */
    public double getAttack();
    /**
     * Donne la défense physique.
     * @return la défense physique
     */
    public double getDefense();
    /**
     * Donne l'attaque spéciale.
     * @return l'attaque spéciale
     */
    public double getSpecialAttack();
    /**
     * Donne la défense spéciale.
     * @return la défense spéciale
     */
    public double getSpecialDefense();
    /**
     * Donne la vitesse utilisée pour départager les actions.
     * @return la vitesse
     */
    public double getSpeed();
}
