package fr.epitech.fakemon.core.ai;

import java.util.List;
import java.util.Random;

import fr.epitech.fakemon.core.api.Opponent;
import fr.epitech.fakemon.core.api.PlayerAction;
import fr.epitech.fakemon.core.api.SendFakemon;
import fr.epitech.fakemon.core.api.UseMove;
import fr.epitech.fakemon.core.model.Fakemon;
import fr.epitech.fakemon.core.model.Move;

/**
 * Adversaire contrôlé par l'ordinateur qui choisit tout au hasard.
 * Le générateur est reçu de l'extérieur : une graine fixe rend ses choix reproductibles dans les tests.
 */
public class RandomOpponent implements Opponent {
    private final Random random;

    /**
     * Crée un adversaire aléatoire.
     *
     * @param random générateur utilisé pour tous les tirages
     */
    public RandomOpponent(Random random) {
        this.random = random;
    }

    /**
     * Choisit au hasard une attaque parmi celles du mon actif.
     *
     * @param moveList les attaques du mon actif, au moins une
     * @param standingFakemonList ses mons encore debout, non utilisés ici
     * @return l'utilisation de l'attaque tirée
     */
    @Override
    public PlayerAction chooseAction(List<Move> moveList, List<Fakemon> standingFakemonList) {
        Move attackMove = moveList.get(random.nextInt(moveList.size()));
        return new UseMove(attackMove);
    }

    /**
     * Choisit au hasard le mon à envoyer parmi ceux encore debout.
     *
     * @param standingFakemonList ses mons encore debout, au moins un
     * @return l'envoi du mon tiré
     */
    @Override
    public PlayerAction chooseFakemon(List<Fakemon> standingFakemonList) {
        Fakemon chosenFakemon = standingFakemonList.get(random.nextInt(standingFakemonList.size()));
        return new SendFakemon(chosenFakemon);
    }

}
