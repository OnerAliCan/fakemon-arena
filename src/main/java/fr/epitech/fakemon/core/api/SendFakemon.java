package fr.epitech.fakemon.core.api;

import fr.epitech.fakemon.core.model.Fakemon;

/**
 * Le joueur envoie un mon : changement, premier mon ou remplaçant d'un mon KO.
 * L'arbitre sait lequel de ces cas il attend.
 */
public class SendFakemon extends PlayerAction {

    private final Fakemon fakemon;

    /**
     * Prépare l'envoi d'un Fakemon sur le terrain.
     * @param fakemon le membre de l'équipe choisi par le joueur
     */
    public SendFakemon(Fakemon fakemon) {
        this.fakemon = fakemon;
    }

    /**
     * Donne le Fakemon choisi.
     * @return le Fakemon à envoyer
     */
    public Fakemon getFakemon() {
        return this.fakemon;
    }
}
