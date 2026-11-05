
package fr.epitech.fakemon.core.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

/**
 * Vérifie le fonctionnement de l'événement
 * de perte de points de vie d'un Fakemon.
 */
class HpLostEventTest {

    /**
     * Vérifie que les informations de perte de PV
     * sont correctement conservées par l'événement.
     */
    @Test
    void shouldReturnCorrectHpLossInformation() {

        // Création d'un événement : 35 PV perdus, 65 PV restants.
        HpLostEvent event = new HpLostEvent(
                Side.PLAYER,
                null,
                35,
                65);

        // Vérification des attributs.
        assertEquals(Side.PLAYER, event.getSide());
        assertNull(event.getFakemon());
        assertEquals(35, event.getHpLost());
        assertEquals(65, event.getRemainingHp());
    }
}