package fr.epitech.fakemon.core.model;

/**
 * Choisit les stats du calcul de dégâts : Attaque et Défense pour une attaque physique,
 * Attaque spéciale et Défense spéciale pour une attaque spéciale. La catégorie STATUS ne fait pas de dégâts directs.
 */
public enum MoveCategory {
    /** Utilise l'attaque et la défense physiques. */
    PHYSICAL,
    /** Utilise l'attaque spéciale et la défense spéciale. */
    SPECIAL,
    /** Applique un effet sans infliger de dégâts directs. */
    STATUS,
}
