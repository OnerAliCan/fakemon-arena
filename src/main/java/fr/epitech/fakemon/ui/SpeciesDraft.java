package fr.epitech.fakemon.ui;

import fr.epitech.fakemon.core.api.InvalidSpeciesException;
import fr.epitech.fakemon.core.api.SpeciesRepository;
import fr.epitech.fakemon.core.model.*;
import java.nio.file.Path;
import java.text.Normalizer;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/** Valeurs du formulaire, indépendantes des contrôles JavaFX. */
record SpeciesDraft(String name, String frenchName, List<Type> types, BaseStats stats,
        List<Move> moves, Talent talent, Path front, Path back, Path cry, String designer, String concept) {
    Species build(SpeciesRepository repository) {
        if (name == null || name.isBlank() || frenchName == null || frenchName.isBlank()) {
            throw new InvalidSpeciesException("Renseigne le nom d'origine et le nom français.");
        }
        if (front == null) {
            throw new InvalidSpeciesException("Choisis un sprite avant.");
        }
        if (talent == null) {
            throw new InvalidSpeciesException("Choisis un talent.");
        }
        int number = repository.findAll().stream().mapToInt(Species::getDexNumber).max().orElse(0) + 1;
        return new Species.Builder().originalName(name.strip()).frenchName(frenchName.strip())
                .types(List.copyOf(types)).stats(stats).possibleMoves(List.copyOf(moves)).talent(talent)
                .dexNumber(number).frontSprite(front.toString()).backSprite(back == null ? "" : back.toString())
                .cry(cry == null ? null : cry.toString()).designer(optional(designer)).concept(optional(concept)).build();
    }

    static List<Move> availableMoves(SpeciesRepository repository) {
        return repository.findAll().stream().flatMap(species -> species.getPossibleMoves().stream())
                .distinct().sorted(Comparator.comparing(move -> normalized(move.getName()))).toList();
    }

    static boolean matches(Move move, String query) {
        return normalized(move.getName() + " " + TypeBadges.name(move.getType())).contains(normalized(query));
    }

    private static String normalized(String value) {
        return Normalizer.normalize(value.strip().toLowerCase(Locale.ROOT), Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
    }

    private static String optional(String value) {
        return value == null || value.isBlank() ? null : value.strip();
    }
}
