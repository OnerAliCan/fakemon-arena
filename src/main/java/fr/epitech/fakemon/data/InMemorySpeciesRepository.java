package fr.epitech.fakemon.data;

import fr.epitech.fakemon.core.api.InvalidSpeciesException;
import fr.epitech.fakemon.core.api.SpeciesRepository;
import fr.epitech.fakemon.core.model.BaseStats;
import fr.epitech.fakemon.core.model.Species;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Catalogue provisoire, gardé en mémoire, avec 3 espèces aux valeurs de remplissage.
 * Il sert en attendant le catalogue JSON.
 */
public class InMemorySpeciesRepository implements SpeciesRepository {

    private final List<Species> speciesList;

    @Override
    public List<Species> findAll() {
        return new ArrayList<>(this.speciesList);
    }

    @Override
    public Optional<Species> findByOriginalName(String originalName) {
        for (Species species : speciesList) {
            if (species.getOriginalName().equals(originalName)) {
                return Optional.of(species);
            }
        }
        return Optional.empty();
    }

    @Override
    public void add(Species species) {
        if (findByOriginalName(species.getOriginalName()).isPresent()) {
            throw new InvalidSpeciesException(
                "Ce Fakemon existe déjà : " + species.getOriginalName()
            );
        }
        speciesList.add(species);
    }

    public InMemorySpeciesRepository() {
        this.speciesList = new ArrayList<>();
        this.speciesList.add(createSpecies("Delugar"));
        this.speciesList.add(createSpecies("Anu"));
        this.speciesList.add(createSpecies("Nemesusa"));
    }

    private Species createSpecies(String name) {
        return new Species(
            name,
            name,
            List.of(),
            new BaseStats(100, 100, 100, 100, 100, 100),
            List.of()
        );
    }
}
