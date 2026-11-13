package fr.epitech.fakemon.core.engine;

/**
 * Où en est le combat : la phase dit à l'arbitre quelle action il attend du joueur.
 */
public enum BattlePhase {
    /** Combat construit : l'arbitre attend le premier mon du joueur. */
    WAITING_FIRST_FAKEMON,

    /** L'arbitre attend l'action du joueur pour le tour en cours. */
    WAITING_ACTION,

    /** Un mon sur le terrain est KO : l'arbitre attend un remplaçant. */
    WAITING_REPLACEMENT,

    /** Le combat est fini : plus aucune action n'est acceptée. */
    FINISHED
}
