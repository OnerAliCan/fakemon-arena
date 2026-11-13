package fr.epitech.fakemon.core.model;

/**
 * Le talent d'un Fakemon, choisi parmi les 4 talents du GDD.
 * Chaque talent agit à un moment différent du combat ; ses effets arrivent avec
 * #24.
 */
public enum Talent {
    /**
     * Dégâts reçus : les attaques super efficaces reçues font 25 % de dégâts en
     * moins.
     */
    CARAPACE,

    /**
     * Dégâts infligés : sous un tiers de ses PV max, ses attaques font 50 % de
     * dégâts en plus.
     */
    DERNIER_SOUFFLE,

    /**
     * Entrée en jeu : inflige au Fakemon d'en face 10 % de ses PV max, sans pouvoir
     * le mettre KO.
     */
    FRACAS,

    /** Fin de tour : récupère 10 % de ses PV max, sans dépasser le maximum. */
    RECUPERATION
}
