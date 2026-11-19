package fr.epitech.fakemon.core.model;

import fr.epitech.fakemon.core.effects.Burn;
import fr.epitech.fakemon.core.effects.Poison;
import fr.epitech.fakemon.core.effects.Paralysis;
import fr.epitech.fakemon.core.effects.Sleep;
import fr.epitech.fakemon.core.effects.Confusion;
import fr.epitech.fakemon.core.effects.Status;

/** Identifie les statuts disponibles dans les attaques et les messages de combat. */
public enum StatusType {
    /** Perte de PV en fin de tour et immunité des types Feu. */
    BURN,
    /** Perte de PV en fin de tour et immunité des types Venin. */
    POISON,
    /** Vitesse divisée par deux et risque de ne pas pouvoir attaquer. */
    PARALYSIS,
    /** Empêche une à trois tentatives d'attaque avant le réveil. */
    SLEEP,
    /** Risque de se blesser au lieu d'attaquer pendant trois tentatives. */
    CONFUSION;

    /** @return un nouvel état indépendant pour le Fakemon touché */
    public Status create() {
        return switch (this) {
            case BURN -> new Burn();
            case POISON -> new Poison();
            case PARALYSIS -> new Paralysis();
            case SLEEP -> new Sleep();
            case CONFUSION -> new Confusion();
        };
    }

    /** @return le nom du statut affiché au joueur */
    public String getDisplayName() {
        return switch (this) {
            case BURN -> "Brûlure";
            case POISON -> "Poison";
            case PARALYSIS -> "Paralysie";
            case SLEEP -> "Sommeil";
            case CONFUSION -> "Confusion";
        };
    }
}
