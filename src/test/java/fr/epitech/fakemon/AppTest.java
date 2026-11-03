package fr.epitech.fakemon;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/** Premier test : vérifie que JUnit 5 tourne, en local comme dans la CI. */
class AppTest {

    @Test
    void titleIsFakemonArena() {
        assertEquals("Fakemon Arena", App.TITLE);
    }
}
