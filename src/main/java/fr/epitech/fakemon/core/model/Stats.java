package fr.epitech.fakemon.core.model;

/**
 * Les statistiques d'un Fakemon. Les 5 stats de combat sont des nombres à virgule,
 * car les objets les multiplient et le GDD n'arrondit les dégâts qu'à la fin du calcul.
 * Les PV restent entiers, car les objets ne les changent pas.
 */
public interface Stats {
    public int getHp();

    public double getAttack();
    public double getDefense();
    public double getSpecialAttack();
    public double getSpecialDefense();
    public double getSpeed();
}
