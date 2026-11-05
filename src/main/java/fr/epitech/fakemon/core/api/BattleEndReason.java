package fr.epitech.fakemon.core.api;

/**
 * Représente les différentes raisons pouvant entraîner
 * la fin d'un combat de Fakemon.
 */
public enum BattleEndReason {

    /**
     * Indique que tous les Fakemon d'une équipe sont K.O.
     */
    ALL_FAINTED,

    /**
     * Indique que le combat se termine par un abandon.
     */
    FORFEIT
}