package fr.epitech.fakemon.core.api;

/**
 * Le mode d'un combat, qui dit à l'arbitre qui choisit le premier mon.
 */
public enum BattleMode {
    /** Tout est tiré au hasard, y compris le premier mon. */
    RANDOM,

    /** Le joueur choisit ses 6 mons et son premier mon. */
    CONSTRUCTED,
}
