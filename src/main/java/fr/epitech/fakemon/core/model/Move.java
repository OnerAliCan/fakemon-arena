package fr.epitech.fakemon.core.model;

/**
 * Une attaque : nom, type, catégorie, puissance, précision en pourcentage et priorité.
 * Elle ne change jamais pendant un combat.
 */
public class Move {

    private final String name;
    private final Type type;
    private final MoveCategory category;
    private final int power;
    private final int accuracy;
    private final int priority;

    /**
     * Crée une attaque avec ses caractéristiques de combat.
     *
     * @param name le nom de l'attaque
     * @param type le type utilisé dans la table d'efficacité
     * @param category la catégorie physique ou spéciale
     * @param power la puissance de base
     * @param accuracy la précision en pourcentage
     * @param priority la priorité, comparée avant la vitesse des Fakemon
     */
    public Move(
        String name,
        Type type,
        MoveCategory category,
        int power,
        int accuracy,
        int priority
    ) {
        this.name = name;
        this.type = type;
        this.category = category;
        this.power = power;
        this.accuracy = accuracy;
        this.priority = priority;
    }

    /**
     * Donne le nom de l'attaque.
     * @return le nom affiché au joueur
     */
    public String getName() {
        return this.name;
    }

    /**
     * Donne le type de l'attaque.
     * @return le type utilisé pour calculer l'efficacité
     */
    public Type getType() {
        return this.type;
    }

    /**
     * Donne la catégorie de l'attaque.
     * @return la catégorie qui détermine les statistiques utilisées pour les dégâts
     */
    public MoveCategory getCategory() {
        return this.category;
    }

    /**
     * Donne la puissance de base.
     * @return la puissance de l'attaque
     */
    public int getPower() {
        return this.power;
    }

    /**
     * Donne la précision de l'attaque.
     * @return la précision en pourcentage
     */
    public int getAccuracy() {
        return this.accuracy;
    }

    /**
     * Donne la priorité de l'attaque.
     * @return la priorité, les valeurs les plus grandes passant d'abord
     */
    public int getPriority() {
        return this.priority;
    }
}
