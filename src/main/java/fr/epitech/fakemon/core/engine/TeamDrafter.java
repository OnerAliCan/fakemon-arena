package fr.epitech.fakemon.core.engine;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import fr.epitech.fakemon.core.api.SpeciesRepository;
import fr.epitech.fakemon.core.api.TeamGenerator;
import fr.epitech.fakemon.core.model.Fakemon;
import fr.epitech.fakemon.core.model.Move;
import fr.epitech.fakemon.core.model.Species;
import fr.epitech.fakemon.core.model.Team;

/**
 * Prépare les équipes en tirant au hasard dans le catalogue des espèces.
 * Chaque mon reçoit 4 attaques tirées parmi ses attaques possibles.
 * Le générateur est reçu de l'extérieur : une graine fixe rend les équipes reproductibles dans les tests.
 */
public class TeamDrafter implements TeamGenerator {

    /** Nombre minimum d'espèces pour tirer deux équipes de 6 sans doublon. */
    private static final int MINIMUM_SPECIES = 12;

    private final Random random;
    private final SpeciesRepository speciesRepository;

    /**
     * Crée un générateur d'équipes.
     *
     * @param speciesRepository le catalogue dans lequel les espèces sont tirées
     * @param random générateur utilisé pour tous les tirages
     */
    public TeamDrafter(SpeciesRepository speciesRepository, Random random) {
        this.speciesRepository = speciesRepository;
        this.random = random;
    }

    /**
     * Tire 6 espèces différentes dans le catalogue, en évitant celles de l'autre équipe.
     *
     * @param excludedSpecies les espèces déjà prises par l'autre équipe
     * @return l'équipe tirée
     * @throws IllegalStateException si le catalogue contient moins de 12 espèces
     */
    @Override
    public Team randomTeam(List<Species> excludedSpecies) {
        if (speciesRepository.findAll().size() < MINIMUM_SPECIES) {
            throw new IllegalStateException("Le catalogue contient moins de 12 Fakemon");
        }
        List<Species> speciesRepositoryCopy = new ArrayList<Species>(speciesRepository.findAll());
        speciesRepositoryCopy.removeAll(excludedSpecies);
        List<Fakemon> fakemonList = new ArrayList<Fakemon>();

        for (int i = 0; i < 6; i++) {
            Species randomSpecies = speciesRepositoryCopy.get(random.nextInt(speciesRepositoryCopy.size()));
            speciesRepositoryCopy.remove(randomSpecies);
            Fakemon chosenFakemon = createFakemon(randomSpecies);
            fakemonList.add(chosenFakemon);
        }

        Team chosenTeam = new Team(fakemonList);
        return chosenTeam;
    }

    /**
     * Crée un mon de l'espèce donnée, avec 4 attaques différentes tirées parmi ses attaques possibles.
     *
     * @param chosenSpecies l'espèce du mon, avec au moins 4 attaques possibles
     * @return le mon créé
     */
    private Fakemon createFakemon(Species chosenSpecies) {
        List<Move> possibleMoves = new ArrayList<Move>();
        possibleMoves.addAll(chosenSpecies.getPossibleMoves());
        List<Move> chosenMoves = new ArrayList<Move>();

        for (int i = 0; i < 4; i++) {
            Move randomMove = possibleMoves.get(random.nextInt(possibleMoves.size()));
            possibleMoves.remove(randomMove);
            chosenMoves.add(randomMove);
        }
        return new Fakemon(chosenSpecies, chosenMoves);
    }

    /**
     * Construit l'équipe à partir des espèces choisies par le joueur, dans le même ordre.
     * Seules les attaques de chaque mon sont tirées au hasard.
     *
     * @param chosenSpecies les espèces choisies par le joueur
     * @return l'équipe construite
     */
    @Override
    public Team buildTeam(List<Species> chosenSpecies) {
        List<Fakemon> fakemonList = new ArrayList<Fakemon>();

        for (int i = 0; i < chosenSpecies.size(); i++) {
            Fakemon chosenFakemon = createFakemon(chosenSpecies.get(i));
            fakemonList.add(chosenFakemon);
        }
        Team chosenTeam = new Team(fakemonList);
        return chosenTeam;
    }

}
