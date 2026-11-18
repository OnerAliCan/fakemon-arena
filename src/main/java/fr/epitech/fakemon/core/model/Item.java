package fr.epitech.fakemon.core.model;

import fr.epitech.fakemon.core.effects.ItemStats;

/** Objet tenu qui bonifie certaines statistiques du porteur. */
public enum Item {
    /** Augmente de 20 % l'attaque physique et l'attaque spéciale. */
    GANTELET("Gantelet", 1.2, 1.0, 1.0),
    /** Augmente de 20 % la défense physique et la défense spéciale. */
    ECU("Écu", 1.0, 1.2, 1.0),
    /** Augmente de 20 % la vitesse. */
    PLUME("Plume", 1.0, 1.0, 1.2);

    private final String displayName;
    private final double attack;
    private final double defense;
    private final double speed;

    Item(String displayName, double attack, double defense, double speed) {
        this.displayName = displayName;
        this.attack = attack;
        this.defense = defense;
        this.speed = speed;
    }

    /**
     * Donne le nom affiché au joueur.
     * @return le nom affiché au joueur
     */
    public String getDisplayName() {
        return displayName;
    }

    /**
     * Applique les bonus à une vue des statistiques d'origine.
     *
     * @param base les statistiques de base
     * @return les statistiques avec les bonus de l'objet
     */
    public Stats decorate(Stats base) {
        return new ItemStats(base, attack, defense, speed);
    }
}
