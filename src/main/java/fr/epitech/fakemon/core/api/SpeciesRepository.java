package fr.epitech.fakemon.core.api;

import fr.epitech.fakemon.core.model.Species;
import java.util.List;
import java.util.Optional;

/**
 * Le catalogue des mons, utilisé par le Mondex, le tireur d'équipes, « Choisissez vos fakemon » et Ajouter un Mon.
 */
public interface SpeciesRepository {
    /**
     * Donne tout le catalogue.
     *
     * @return toutes les espèces, dans l'ordre du Mondex
     */
    List<Species> findAll();

    /**
     * Cherche une espèce par son nom d'origine.
     *
     * @param originalName le nom d'origine de l'espèce
     * @return l'espèce trouvée, ou un Optional vide si elle n'existe pas
     */
    Optional<Species> findByOriginalName(String originalName);

    /**
     * Ajoute une espèce au catalogue.
     *
     * @param species l'espèce à ajouter
     * @throws InvalidSpeciesException si l'espèce est refusée
     */
    void add(Species species);
}
