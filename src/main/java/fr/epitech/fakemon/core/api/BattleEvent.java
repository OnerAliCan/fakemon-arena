package fr.epitech.fakemon.core.api;

/**
 * Représente un événement survenant pendant un combat de Fakemon.
 * 
 * Cette classe abstraite sert de base aux différents événements
 * générés par le moteur de combat et transmis aux observateurs.
 */
public abstract class BattleEvent {

    /**
     * Construit un événement de combat.
     * Ce constructeur est réservé aux classes qui héritent
     * de BattleEvent ainsi qu'aux classes du même package.
     */
    protected BattleEvent() {
    }
}