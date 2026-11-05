
package fr.epitech.fakemon.core.api;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/**
 * Vérifie le fonctionnement de l'événement
 * de début de tour.
 */
class TurnStartedEventTest {

    /**
     * Vérifie que le numéro de tour transmis
     * au constructeur est correctement retourné.
     */
    @Test
    void shouldReturnCorrectTurnNumber() {
        TurnStartedEvent event = new TurnStartedEvent(3);

        assertEquals(3, event.getTurnNumber());
    }
}
