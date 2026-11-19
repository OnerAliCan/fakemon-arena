package fr.epitech.fakemon.core.effects;

import fr.epitech.fakemon.core.model.Fakemon;
import fr.epitech.fakemon.core.model.StatusType;
import fr.epitech.fakemon.core.model.Stats;
import java.util.Random;

/** État durable d'un Fakemon pendant un combat. */
public abstract class Status {
    /** Réserve la création d'un statut à ses sous-classes. */
    protected Status() { }

    /** @return l'identifiant du statut pour le catalogue, les événements et le réseau */
    public abstract StatusType getType();

    /**
     * Vérifie les immunités propres au statut.
     * @param target Fakemon qui pourrait recevoir le statut
     * @return true si ses types permettent l'application
     */
    public abstract boolean canApply(Fakemon target);

    /**
     * Applique les modificateurs du statut sans changer les statistiques d'origine.
     * @param stats statistiques incluant déjà l'objet tenu
     * @return les statistiques utilisées pendant le combat
     */
    public Stats decorate(Stats stats) {
        return stats;
    }

    /**
     * Vérifie si ce statut empêche l'attaque choisie, sans bloquer les changements de Fakemon.
     * @param random générateur partagé avec le moteur
     * @return true si l'attaque ne peut pas être exécutée
     */
    public boolean preventsAttack(Random random) {
        return false;
    }

    /**
     * Calcule les blessures subies à la place d'une attaque empêchée par le statut.
     * @param holder Fakemon qui ne peut pas attaquer
     * @return dégâts limités aux PV restants, ou zéro si le statut ne blesse pas
     */
    public int preventedAttackDamage(Fakemon holder) {
        return 0;
    }

    /** @return true si l'état doit être retiré après la tentative d'attaque courante */
    public boolean hasEnded() {
        return false;
    }

    /**
     * Calcule la perte de PV de fin de tour, sans modifier le Fakemon.
     * @param holder Fakemon actif qui porte le statut
     * @return les dégâts, limités aux PV restants
     */
    public abstract int endOfTurnDamage(Fakemon holder);
}
