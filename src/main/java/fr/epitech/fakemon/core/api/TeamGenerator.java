package fr.epitech.fakemon.core.api;

import fr.epitech.fakemon.core.model.Species;
import fr.epitech.fakemon.core.model.Team;
import java.util.List;

/**
 * Prépare les équipes avant le combat, en tirant les 4 attaques et l'objet de chaque mon.
 */
public interface TeamGenerator {
    /**
     * Tire une équipe de 6 mons au hasard.
     *
     * @param excludedSpecies les espèces à ne pas tirer, déjà prises par l'autre équipe
     * @return l'équipe tirée
     */
    Team randomTeam(List<Species> excludedSpecies);

    /**
     * Construit l'équipe du joueur en combat construit.
     *
     * @param chosenSpecies les 6 espèces choisies par le joueur
     * @return l'équipe construite
     */
    Team buildTeam(List<Species> chosenSpecies);
}
