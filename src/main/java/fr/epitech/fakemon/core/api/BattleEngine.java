package fr.epitech.fakemon.core.api;

import fr.epitech.fakemon.core.model.Team;
import java.util.Random;

/**
 * L'arbitre du combat, tel que l'écran le voit : il applique les règles et envoie des faits aux écouteurs.
 */
public interface BattleEngine {
    /**
     * Inscrit un écouteur, qui recevra tous les événements du combat.
     *
     * @param battleListener l'écouteur à inscrire
     */
    void addListener(BattleListener battleListener);

    /**
     * Démarre le combat.
     *
     * @param playerTeam l'équipe du joueur
     * @param opponentTeam l'équipe de l'adversaire
     * @param battleMode le mode, qui dit qui choisit le premier mon
     * @param opponent l'adversaire, à qui l'arbitre demande ses actions
     * @param random la source de hasard de tous les tirages du combat
     */
    void start(
        Team playerTeam,
        Team opponentTeam,
        BattleMode battleMode,
        Opponent opponent,
        Random random
    );

    /**
     * Transmet l'action du joueur. L'arbitre demande lui-même celle de l'adversaire.
     *
     * @param playerAction l'action choisie par le joueur
     * @throws IllegalStateException si l'arbitre n'attend pas cette action à ce moment-là
     */
    void submitAction(PlayerAction playerAction);

    /**
     * Donne l'état actuel du combat.
     *
     * @return une photo du combat, en lecture seule
     */
    BattleState getState();
}
