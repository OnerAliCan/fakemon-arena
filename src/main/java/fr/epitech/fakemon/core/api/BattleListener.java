package fr.epitech.fakemon.core.api;

/**
 * Définit un observateur chargé de recevoir les événements
 * générés par le moteur de combat.
 *
 * Les événements sont transmis aux observateurs dans l'ordre
 * dans lequel ils se produisent pendant le combat.
 */
public interface BattleListener {

    /**
     * Reçoit un événement survenu pendant le combat.
     *
     * @param event l'événement de combat à traiter
     */
    void onEvent(BattleEvent event);
}