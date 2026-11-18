package fr.epitech.fakemon.net;

import com.google.gson.JsonObject;
import fr.epitech.fakemon.core.api.Forfeit;
import fr.epitech.fakemon.core.api.PlayerAction;
import fr.epitech.fakemon.core.api.SendFakemon;
import fr.epitech.fakemon.core.api.UseMove;
import fr.epitech.fakemon.core.model.Fakemon;
import fr.epitech.fakemon.core.model.Team;

/** Transmet les choix du joueur par indices et retrouve les objets du combat. */
public final class NetworkActionCodec {
    /**
     * Convertit un choix en message ACTION.
     *
     * @param action le choix à transmettre
     * @param team l'équipe du joueur
     * @param active son Fakemon actif, ou null avant sa première sélection
     * @return le message réseau
     * @throws IllegalArgumentException si l'action est inconnue ou étrangère au joueur
     */
    public NetworkMessage encode(PlayerAction action, Team team, Fakemon active) {
        JsonObject payload = new JsonObject();
        if (action instanceof UseMove useMove) {
            int index = active == null ? -1 : active.getMoves().indexOf(useMove.getMove());
            if (index < 0) {
                throw new IllegalArgumentException("L'attaque doit appartenir au Fakemon actif.");
            }
            payload.addProperty("action", "USE_MOVE");
            payload.addProperty("moveIndex", index);
        } else if (action instanceof SendFakemon send) {
            int index = team.getFakemons().indexOf(send.getFakemon());
            if (index < 0) {
                throw new IllegalArgumentException("Le Fakemon doit appartenir à l'équipe du joueur.");
            }
            payload.addProperty("action", "SEND_FAKEMON");
            payload.addProperty("teamIndex", index);
        } else if (action instanceof Forfeit) {
            payload.addProperty("action", "FORFEIT");
        } else {
            throw new IllegalArgumentException("Action réseau inconnue.");
        }
        return new NetworkMessage("ACTION", payload);
    }

    /**
     * Reconstruit le choix avec les attaques et les Fakemon du combat local.
     * Le moteur vérifie ensuite si ce choix est autorisé à ce moment du combat.
     *
     * @param message le message ACTION reçu
     * @param team l'équipe du joueur qui a envoyé l'action
     * @param active son Fakemon actif, ou null avant sa première sélection
     * @return le choix reconstruit
     * @throws IllegalArgumentException si le message ou l'indice est invalide
     */
    public PlayerAction decode(NetworkMessage message, Team team, Fakemon active) {
        if (message == null || !"ACTION".equals(message.getType()) || message.getPayload() == null) {
            throw new IllegalArgumentException("Un message ACTION est attendu.");
        }
        JsonObject payload = message.getPayload();
        var action = payload.get("action");
        if (action == null || !action.isJsonPrimitive() || !action.getAsJsonPrimitive().isString()) {
            throw new IllegalArgumentException("Le nom de l'action est obligatoire.");
        }
        return switch (action.getAsString()) {
            case "USE_MOVE" -> {
                if (active == null) {
                    throw new IllegalArgumentException("Aucun Fakemon actif pour utiliser une attaque.");
                }
                int index = readIndex(payload, "moveIndex", active.getMoves().size());
                yield new UseMove(active.getMoves().get(index));
            }
            case "SEND_FAKEMON" -> {
                int index = readIndex(payload, "teamIndex", team.getFakemons().size());
                yield new SendFakemon(team.getFakemons().get(index));
            }
            case "FORFEIT" -> new Forfeit();
            default -> throw new IllegalArgumentException("Action réseau inconnue.");
        };
    }

    private int readIndex(JsonObject payload, String field, int size) {
        var value = payload.get(field);
        if (value == null || !value.isJsonPrimitive() || !value.getAsJsonPrimitive().isNumber()) {
            throw new IllegalArgumentException("L'indice " + field + " doit être un entier.");
        }
        int index;
        try {
            index = value.getAsBigDecimal().intValueExact();
        } catch (ArithmeticException | NumberFormatException error) {
            throw new IllegalArgumentException("L'indice " + field + " doit être un entier.", error);
        }
        if (index < 0 || index >= size) {
            throw new IllegalArgumentException("L'indice " + field + " est hors limites.");
        }
        return index;
    }
}
