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

    public List<Fakemon> getFakemons() {
        return this.fakemons;
    }

    /** Renvoie une nouvelle liste des Fakemon qui ne sont pas K.O. ; l'équipe ne change pas. */
    public List<Fakemon> getStandingFakemons() {
        List<Fakemon> standingFakemons = new ArrayList<>();

        for (Fakemon fakemon : fakemons) {
            if (fakemon.getCurrentHp() > 0) {
                standingFakemons.add(fakemon);
            }
        }
        return standingFakemons;
    }

    public boolean hasStandingFakemon() {
        if (this.getStandingFakemons().size() == 0) {
            return false;
        }
        return true;
    }
}
