package fr.epitech.fakemon.core.api;

import fr.epitech.fakemon.core.model.Move;

/**
 * Le joueur utilise une attaque de son mon actif.
 */
public class UseMove extends PlayerAction {

    private final Move move;

    public UseMove(Move move) {
        this.move = move;
    }

    public Move getMove() {
        return this.move;
    }
}
