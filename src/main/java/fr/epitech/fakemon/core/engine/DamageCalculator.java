package fr.epitech.fakemon.core.engine;

import fr.epitech.fakemon.core.model.Fakemon;
import fr.epitech.fakemon.core.model.Move;
import fr.epitech.fakemon.core.model.MoveCategory;
import java.util.Random;

/**
 * Décide si une attaque touche et calcule ses dégâts selon la formule du GDD.
 * Il ne modifie pas les PV : c'est au moteur de les appliquer.
 */
public class DamageCalculator {

    private final Random random;
    private final TypeChart typeChart;

    /**
     * Crée le calculateur.
     *
     * @param random le générateur de tous les tirages ; les tests fixent sa graine
     * @param typeChart la table d'efficacité des types
     */
    public DamageCalculator(Random random, TypeChart typeChart) {
        this.random = random;
        this.typeChart = typeChart;
    }

    /**
     * Tire la précision d'une attaque.
     *
     * @param move l'attaque lancée
     * @return vrai si l'attaque touche
     */
    public boolean rollAccuracy(Move move) {
        if (random.nextInt(100) < move.getAccuracy()) {
            return true;
        }
        return false;
    }

    /**
     * Calcule les dégâts d'une attaque qui a touché. Le résultat est arrondi à l'entier inférieur
     * à la toute fin, avec au moins 1 dégât, sauf si la cible est immunisée.
     *
     * @param attacker le Fakemon qui attaque
     * @param target le Fakemon visé
     * @param move l'attaque lancée
     * @return les dégâts, le critique et le multiplicateur d'efficacité
     */
    public DamageResult calculate(Fakemon attacker, Fakemon target, Move move) {
        double resultDamage;
        boolean critical;
        double multiplier = 1;
        if (move.getCategory() == MoveCategory.PHYSICAL) {
            resultDamage =
                (move.getPower() * attacker.getStats().getAttack()) /
                    target.getStats().getDefense() /
                    4 +
                2;
        } else {
            resultDamage =
                (move.getPower() * attacker.getStats().getSpecialAttack()) /
                    target.getStats().getSpecialDefense() /
                    4 +
                2;
        }
        if (random.nextInt(16) == 0) {
            critical = true;
            multiplier = multiplier * 1.5;
        } else {
            critical = false;
        }
        if (attacker.getSpecies().getTypes().contains(move.getType())) {
            multiplier = multiplier * 1.25;
        }
        double damageMultiplier = this.typeChart.multiplier(
            move.getType(),
            target.getSpecies().getTypes()
        );

        resultDamage = resultDamage * damageMultiplier;
        resultDamage = resultDamage * random.nextDouble(0.85, 1.00);
        resultDamage = resultDamage * multiplier;
        int resultDamageInt = (int) resultDamage;
        if (resultDamageInt == 0 && damageMultiplier != 0) {
            resultDamageInt = 1;
        }
        DamageResult result = new DamageResult(
            resultDamageInt,
            critical,
            damageMultiplier
        );

        return result;
    }
}
