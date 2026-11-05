package fr.epitech.fakemon.core.model;

import java.util.List;

/**
 * La fiche d'un mon dans le catalogue, qui ne change pas d'un combat à l'autre.
 */
public class Species {

    private final String originalName;
    private final String frenchName;
    private final List<Type> types;
    private final Stats stats;
    private final List<Move> possibleMoves;

    public Species(
        String originalName,
        String frenchName,
        List<Type> types,
        Stats stats,
        List<Move> possibleMoves
    ) {
        this.originalName = originalName;
        this.frenchName = frenchName;
        this.types = types;
        this.stats = stats;
        this.possibleMoves = possibleMoves;
    }

    public String getOriginalName() {
        return this.originalName;
    }

    public String getFrenchName() {
        return this.frenchName;
    }

    public List<Type> getTypes() {
        return this.types;
    }

    public Stats getStats() {
        return this.stats;
    }

    public List<Move> getPossibleMoves() {
        return this.possibleMoves;
    }
}
