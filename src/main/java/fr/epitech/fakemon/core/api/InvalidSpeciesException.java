package fr.epitech.fakemon.core.api;

/**
 * Signale qu'un mon ne peut pas être ajouté au catalogue, par exemple parce qu'il y est déjà.
 */
public class InvalidSpeciesException extends RuntimeException {

    public InvalidSpeciesException(String message) {
        super(message);
    }
}
