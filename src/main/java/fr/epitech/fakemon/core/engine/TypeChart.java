package fr.epitech.fakemon.core.engine;

import fr.epitech.fakemon.core.api.Effectiveness;
import fr.epitech.fakemon.core.model.Type;
import java.util.List;
import java.util.Optional;

/**
 * La table d'efficacité des types du GDD.
 * Ses lignes (type de l'attaque) et ses colonnes (type de la cible) suivent l'ordre de l'enum {@link Type}.
 */
public class TypeChart {

    private final double[][] table = {
        { 0.5, 0.5, 2, 1, 1, 1, 2, 1, 1, 1 },
        { 2, 0.5, 0.5, 1, 2, 1, 1, 1, 1, 1 },
        { 0.5, 2, 0.5, 1, 2, 0.5, 1, 1, 1, 1 },
        { 1, 2, 0.5, 0.5, 0, 2, 1, 1, 1, 1 },
        { 2, 1, 1, 2, 0.5, 2, 1, 0.5, 1, 1 },
        { 1, 1, 2, 0.50, 0.5, 1, 2, 2, 1, 1 },
        { 1, 1, 2, 1, 1, 1, 0.5, 1, 0.5, 2 },
        { 1, 1, 1, 1, 2, 0.5, 1, 1, 2, 0.5 },
        { 1, 1, 1, 2, 1, 1, 1, 0.5, 0.5, 2 },
        { 1, 1, 1, 1, 1, 1, 0.5, 2, 2, 0.5 },
    };

    /**
     * Calcule l'efficacité d'une attaque sur une cible : les valeurs de ses types se multiplient.
     *
     * @param attack le type de l'attaque
     * @param defender les types de la cible
     * @return 0, 0,25, 0,5, 1, 2 ou 4
     */
    public double multiplier(Type attack, List<Type> defender) {
        double result = 1;
        for (Type defenderType : defender) {
            result = result * table[attack.ordinal()][defenderType.ordinal()];
        }
        return result;
    }

    /**
     * Traduit un multiplicateur en efficacité à annoncer à l'écran.
     *
     * @param multiplier le résultat de {@link #multiplier}
     * @return l'efficacité, ou un {@link Optional} vide pour un effet normal (×1)
     */
    public Optional<Effectiveness> toEffectiveness(double multiplier) {
        if (multiplier == 0) {
            return Optional.of(Effectiveness.NO_EFFECT);
        } else if (multiplier < 1) {
            return Optional.of(Effectiveness.NOT_VERY_EFFECTIVE);
        } else if (multiplier == 1) {
            return Optional.empty();
        } else {
            return Optional.of(Effectiveness.SUPER_EFFECTIVE);
        }
    }
}
