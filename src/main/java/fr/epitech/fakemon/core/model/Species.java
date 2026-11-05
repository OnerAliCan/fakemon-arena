package fr.epitech.fakemon.core.model;

/**
 * La fiche d'un mon dans le catalogue, qui ne change pas d'un combat à l'autre.
 * Les données arrivent avec #8.
 */
public class Species {

    private final String originalName;

    public Species(String originalName) {
        this.originalName = originalName;
    }

    public String getOriginalName() {
        return this.originalName;
    }
}
