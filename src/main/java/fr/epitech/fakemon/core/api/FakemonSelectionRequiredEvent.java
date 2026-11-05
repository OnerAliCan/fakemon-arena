package fr.epitech.fakemon.core.api;

/**
 * Représente un événement demandant au joueur
 * de sélectionner un Fakemon pendant un combat.
 *
 * Cet événement contient la raison de cette sélection,
 * comme le choix du premier Fakemon ou d'un remplaçant.
 */
public class FakemonSelectionRequiredEvent extends BattleEvent {

    private final SelectionReason reason;

    /**
     * Construit un événement de sélection de Fakemon.
     *
     * @param reason la raison nécessitant la sélection d'un Fakemon
     */
    public FakemonSelectionRequiredEvent(SelectionReason reason) {
        this.reason = reason;
    }

    /**
     * Retourne la raison nécessitant la sélection d'un Fakemon.
     *
     * @return la raison de la sélection
     */
    public SelectionReason getReason() {
        return reason;
    }
}