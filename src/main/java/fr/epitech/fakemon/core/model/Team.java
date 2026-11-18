package fr.epitech.fakemon.core.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Une équipe de 6 mons au plus.
 */
public class Team {

    private final List<Fakemon> fakemons;

    /**
     * Crée une équipe.
     *
     * @param fakemons les membres de l'équipe, dans leur ordre de sélection
     * @throws IllegalArgumentException si la liste contient plus de 6 Fakemon
     */
    public Team(List<Fakemon> fakemons) {
        if (fakemons.size() > 6) {
            throw new IllegalArgumentException(
                "Une équipe doit être composée de 6 Fakemons maximum."
            );
        } else {
            this.fakemons = fakemons;
        }
    }

    /**
     * Donne les membres dans leur ordre de sélection.
     * @return la liste des Fakemon de l'équipe
     */
    public List<Fakemon> getFakemons() {
        return this.fakemons;
    }

    /**
     * Donne une nouvelle liste des membres encore debout.
     * @return les Fakemon dont les PV sont strictement positifs
     */
    public List<Fakemon> getStandingFakemons() {
        List<Fakemon> standingFakemons = new ArrayList<>();

        for (Fakemon fakemon : fakemons) {
            if (fakemon.getCurrentHp() > 0) {
                standingFakemons.add(fakemon);
            }
        }
        return standingFakemons;
    }

    /**
     * Indique si l'équipe peut poursuivre le combat.
     * @return true si au moins un membre a encore des PV
     */
    public boolean hasStandingFakemon() {
        if (this.getStandingFakemons().size() == 0) {
            return false;
        }
        return true;
    }
}
