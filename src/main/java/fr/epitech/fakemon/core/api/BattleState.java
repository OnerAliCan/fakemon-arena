package fr.epitech.fakemon.core.api;

import fr.epitech.fakemon.core.model.Fakemon;
import fr.epitech.fakemon.core.model.Team;

/**
 * Une photo du combat, que l'écran lit sans pouvoir la modifier.
 */
public interface BattleState {
    /**
     * Donne l'équipe d'un camp.
     *
     * @param side le camp voulu
     * @return son équipe
     */
    Team getTeam(Side side);

    /**
     * Donne le mon actif d'un camp, celui qui est sur le terrain.
     *
     * @param side le camp voulu
     * @return son mon actif
     */
    Fakemon getActiveFakemon(Side side);

    /**
     * Donne le numéro du tour en cours.
     *
     * @return le numéro du tour
     */
    int getTurnNumber();
}
