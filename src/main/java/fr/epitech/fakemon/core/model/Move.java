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
    private final StatusType status;
    private final int statusChance;

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
        this(name, type, category, power, accuracy, priority, null, 0);
    }

    /**
     * Crée une attaque pouvant appliquer un statut après avoir touché.
     * @param name nom de l'attaque
     * @param type type de l'attaque
     * @param category catégorie physique, spéciale ou sans dégâts
     * @param power puissance de base, nulle pour une attaque sans dégâts
     * @param accuracy précision en pourcentage
     * @param priority priorité de l'attaque
     * @param status statut possible, ou null pour une attaque sans effet secondaire
     * @param statusChance chance d'application entre 1 et 100, ou 0 en l'absence de statut
     */
    public Move(String name, Type type, MoveCategory category, int power, int accuracy, int priority,
            StatusType status, int statusChance) {
        if (statusChance < 0 || statusChance > 100 || (status == null) != (statusChance == 0)) {
            throw new IllegalArgumentException("Le statut et sa chance d'application sont incohérents.");
        }
        if (category == MoveCategory.STATUS && (power != 0 || status == null)) {
            throw new IllegalArgumentException("Une attaque de statut doit avoir une puissance nulle et un effet.");
        }
        this.name = name;
        this.type = type;
        this.category = category;
        this.power = power;
        this.accuracy = accuracy;
        this.priority = priority;
        this.status = status;
        this.statusChance = statusChance;
    }

    /** @return le statut possible, ou null si l'attaque n'en applique pas */
    public StatusType getStatus() {
        return status;
    }

    /** @return la chance d'appliquer le statut après une attaque réussie, en pourcentage */
    public int getStatusChance() {
        return statusChance;
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
