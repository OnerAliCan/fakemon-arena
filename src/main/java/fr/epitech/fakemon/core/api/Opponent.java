package fr.epitech.fakemon.core.api;

import fr.epitech.fakemon.core.model.Fakemon;
import fr.epitech.fakemon.core.model.Move;
import java.util.List;

/**
 * L'autre camp du combat : l'IA, ou l'adversaire en ligne.
 * L'arbitre lui pose les mêmes questions dans les deux cas, sans savoir lequel il a en face.
 */
public interface Opponent {
    /**
     * Demande à l'adversaire son action pour le tour.
     *
     * @param moveList les attaques de son mon actif
     * @param standingFakemonList ses mons encore debout
     * @return l'action choisie
     */
    PlayerAction chooseAction(
        List<Move> moveList,
        List<Fakemon> standingFakemonList
    );

    /**
     * Demande à l'adversaire quel Fakemon envoyer pour remplacer son Fakemon KO.
     * Son premier Fakemon n'est pas demandé : c'est toujours le premier de son équipe.
     *
     * @param standingFakemonList ses mons encore debout
     * @return l'action choisie
     */
    PlayerAction chooseFakemon(List<Fakemon> standingFakemonList);
}
