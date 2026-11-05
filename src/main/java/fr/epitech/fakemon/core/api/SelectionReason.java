package fr.epitech.fakemon.core.api;

/**
 * Représente les différentes raisons nécessitant
 * la sélection d'un Fakemon pendant un combat.
 */
public enum SelectionReason {

    /**
     * Indique que le joueur doit choisir son premier Fakemon.
     */
    FIRST_FAKEMON,

    /**
     * Indique que le joueur doit choisir un Fakemon remplaçant.
     */
    REPLACEMENT
}