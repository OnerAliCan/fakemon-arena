package fr.epitech.fakemon.core.engine;

import static org.junit.jupiter.api.Assertions.assertEquals;

import fr.epitech.fakemon.core.api.Effectiveness;
import fr.epitech.fakemon.core.model.Type;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/** Vérifie la lecture de la table des types et la traduction d'un multiplicateur en efficacité. */
class TypeChartTest {

    @Test
    void waterOnFireRockMultipliesToFour() {
        TypeChart typeChart = new TypeChart();

        double multiplier = typeChart.multiplier(
            Type.WATER,
            List.of(Type.FIRE, Type.ROCK)
        );

        assertEquals(4, multiplier);
    }

    @Test
    void fireOnFireVenomMultipliesToOne() {
        TypeChart typeChart = new TypeChart();

        double multiplier = typeChart.multiplier(
            Type.FIRE,
            List.of(Type.FIRE, Type.VENOM)
        );

        assertEquals(1, multiplier);
    }

    @Test
    void zeroHasNoEffect() {
        TypeChart typeChart = new TypeChart();

        Optional<Effectiveness> effectiveness = typeChart.toEffectiveness(0);

        assertEquals(Optional.of(Effectiveness.NO_EFFECT), effectiveness);
    }

    @Test
    void halfIsNotVeryEffective() {
        TypeChart typeChart = new TypeChart();

        Optional<Effectiveness> effectiveness = typeChart.toEffectiveness(0.5);

        assertEquals(
            Optional.of(Effectiveness.NOT_VERY_EFFECTIVE),
            effectiveness
        );
    }

    @Test
    void oneIsEmpty() {
        TypeChart typeChart = new TypeChart();

        Optional<Effectiveness> effectiveness = typeChart.toEffectiveness(1);

        assertEquals(Optional.empty(), effectiveness);
    }

    @Test
    void twoIsSuperEffective() {
        TypeChart typeChart = new TypeChart();

        Optional<Effectiveness> effectiveness = typeChart.toEffectiveness(2);

        assertEquals(Optional.of(Effectiveness.SUPER_EFFECTIVE), effectiveness);
    }
}
