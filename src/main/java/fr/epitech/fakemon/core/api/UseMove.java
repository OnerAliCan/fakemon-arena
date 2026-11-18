package fr.epitech.fakemon.core.api;

import fr.epitech.fakemon.core.model.Move;

/**
 * Le joueur utilise une attaque de son mon actif.
 */
public class UseMove extends PlayerAction {

    private final Move move;

    /**
     * Prépare l'utilisation d'une attaque.
     * @param move l'attaque choisie parmi celles du Fakemon actif
     */
    public UseMove(Move move) {
        this.move = move;
    }

    /**
     * Donne l'attaque choisie.
     * @return l'attaque à utiliser
     */
    public Move getMove() {
        return this.move;
    }
}
