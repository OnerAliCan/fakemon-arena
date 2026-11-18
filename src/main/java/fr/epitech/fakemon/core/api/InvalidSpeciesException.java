package fr.epitech.fakemon.core.api;

/**
 * Signale qu'un mon ne peut pas être ajouté au catalogue, par exemple parce qu'il y est déjà.
 */
public class InvalidSpeciesException extends RuntimeException {

    /**
     * Signale la raison du refus d'une espèce.
     * @param message l'explication à présenter au joueur
     */
    public InvalidSpeciesException(String message) {
        super(message);
    }
}
