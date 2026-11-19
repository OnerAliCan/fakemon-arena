package fr.epitech.fakemon.net;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import fr.epitech.fakemon.core.api.BattleEndedEvent;
import fr.epitech.fakemon.core.api.BattleEvent;
import fr.epitech.fakemon.core.api.CriticalHitEvent;
import fr.epitech.fakemon.core.api.EffectivenessEvent;
import fr.epitech.fakemon.core.api.FakemonEnteredEvent;
import fr.epitech.fakemon.core.api.FakemonFaintedEvent;
import fr.epitech.fakemon.core.api.FakemonSelectionRequiredEvent;
import fr.epitech.fakemon.core.api.HpLostEvent;
import fr.epitech.fakemon.core.api.HpRestoredEvent;
import fr.epitech.fakemon.core.api.ItemActivatedEvent;
import fr.epitech.fakemon.core.api.MoveMissedEvent;
import fr.epitech.fakemon.core.api.MoveUsedEvent;
import fr.epitech.fakemon.core.api.BattleEndReason;
import fr.epitech.fakemon.core.api.Effectiveness;
import fr.epitech.fakemon.core.api.SelectionReason;
import fr.epitech.fakemon.core.api.Side;
import fr.epitech.fakemon.core.api.TalentActivatedEvent;
import fr.epitech.fakemon.core.api.TurnStartedEvent;
import fr.epitech.fakemon.core.api.StatusAppliedEvent;
import fr.epitech.fakemon.core.api.StatusActivatedEvent;
import fr.epitech.fakemon.core.api.AttackPreventedEvent;
import fr.epitech.fakemon.core.api.StatusRemovedEvent;
import fr.epitech.fakemon.core.model.StatusType;
import fr.epitech.fakemon.core.model.Fakemon;
import fr.epitech.fakemon.core.model.Team;
import fr.epitech.fakemon.core.model.Item;
import fr.epitech.fakemon.core.model.Move;
import fr.epitech.fakemon.core.model.Talent;
import java.lang.reflect.Type;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/** Transporte les événements du moteur en conservant les références aux équipes. */
public final class NetworkEventCodec {
    private static final Map<String, Class<? extends BattleEvent>> EVENT_TYPES = Map.ofEntries(
            Map.entry("TURN_STARTED", TurnStartedEvent.class),
            Map.entry("FAKEMON_ENTERED", FakemonEnteredEvent.class),
            Map.entry("MOVE_USED", MoveUsedEvent.class),
            Map.entry("MOVE_MISSED", MoveMissedEvent.class),
            Map.entry("HP_LOST", HpLostEvent.class),
            Map.entry("HP_RESTORED", HpRestoredEvent.class),
            Map.entry("CRITICAL_HIT", CriticalHitEvent.class),
            Map.entry("EFFECTIVENESS", EffectivenessEvent.class),
            Map.entry("FAKEMON_FAINTED", FakemonFaintedEvent.class),
            Map.entry("SELECTION_REQUIRED", FakemonSelectionRequiredEvent.class),
            Map.entry("ITEM_ACTIVATED", ItemActivatedEvent.class),
            Map.entry("TALENT_ACTIVATED", TalentActivatedEvent.class),
            Map.entry("STATUS_APPLIED", StatusAppliedEvent.class),
            Map.entry("STATUS_ACTIVATED", StatusActivatedEvent.class),
            Map.entry("ATTACK_PREVENTED", AttackPreventedEvent.class),
            Map.entry("STATUS_REMOVED", StatusRemovedEvent.class),
            Map.entry("BATTLE_ENDED", BattleEndedEvent.class));

    private final Gson gson;
    private final Team hostTeam;
    private final Team guestTeam;
    private final Side localSide;

    /**
     * Prépare la conversion avec les équipes dans l'ordre utilisé par l'hôte.
     *
     * @param hostTeam l'équipe de l'hôte
     * @param guestTeam l'équipe de l'invité
     * @param localSide PLAYER pour l'hôte, OPPONENT pour l'invité
     */
    public NetworkEventCodec(Team hostTeam, Team guestTeam, Side localSide) {
        this.hostTeam = Objects.requireNonNull(hostTeam);
        this.guestTeam = Objects.requireNonNull(guestTeam);
        this.localSide = Objects.requireNonNull(localSide);
        gson = new GsonBuilder()
                .registerTypeAdapter(Fakemon.class, new FakemonReferenceAdapter(hostTeam, guestTeam))
                .create();
    }

    /**
     * Convertit un événement de l'hôte en message EVENT.
     *
     * @param event le fait annoncé par le moteur de l'hôte
     * @return le message à envoyer à l'invité
     * @throws IllegalArgumentException si le type est inconnu ou si une sélection nécessite un destinataire
     */
    public NetworkMessage encode(BattleEvent event) {
        if (event == null || event instanceof FakemonSelectionRequiredEvent) {
            throw new IllegalArgumentException("Une demande de sélection nécessite un destinataire explicite.");
        }
        String eventType = EVENT_TYPES.entrySet().stream()
                .filter(entry -> entry.getValue().equals(event.getClass()))
                .map(Map.Entry::getKey)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Événement réseau inconnu."));
        JsonObject payload = new JsonObject();
        payload.addProperty("event", eventType);
        payload.add("data", gson.toJsonTree(event));
        return new NetworkMessage("EVENT", payload);
    }

    /**
     * Adresse une demande de sélection à un seul des joueurs.
     *
     * @param event la demande de sélection
     * @param recipient le destinataire dans les camps de l'hôte
     * @return le message adressé au joueur concerné
     */
    public NetworkMessage encodeSelection(FakemonSelectionRequiredEvent event, Side recipient) {
        Objects.requireNonNull(event);
        Objects.requireNonNull(recipient);
        JsonObject payload = new JsonObject();
        payload.addProperty("event", "SELECTION_REQUIRED");
        payload.addProperty("recipient", recipient.name());
        payload.add("data", gson.toJsonTree(event));
        return new NetworkMessage("EVENT", payload);
    }

    /**
     * Reconstruit un événement avec les Fakemon locaux.
     * Les camps sont adaptés au point de vue du joueur local.
     *
     * @param message le message EVENT envoyé par l'hôte
     * @return l'événement à afficher, ou vide si la sélection concerne l'autre joueur
     * @throws JsonParseException si le message ou le type d'événement est invalide
     */
    public Optional<BattleEvent> decode(NetworkMessage message) {
        if (message == null || !"EVENT".equals(message.getType()) || message.getPayload() == null) {
            throw new JsonParseException("Un message EVENT est attendu.");
        }
        JsonObject payload = message.getPayload();
        try {
            String eventType = readString(payload, "event");
            JsonObject data = readObject(payload, "data");
            if ("SELECTION_REQUIRED".equals(eventType)) {
                Side recipient = readEnum(payload, "recipient", Side.class);
                SelectionReason reason = readEnum(data, "reason", SelectionReason.class);
                return recipient == localSide
                        ? Optional.of(new FakemonSelectionRequiredEvent(reason)) : Optional.empty();
            }
            BattleEvent event = switch (eventType) {
                case "TURN_STARTED" -> new TurnStartedEvent(readInt(data, "turnNumber", 1, Integer.MAX_VALUE));
                case "BATTLE_ENDED" -> new BattleEndedEvent(
                        toLocalSide(readEnum(data, "winner", Side.class)),
                        readEnum(data, "reason", BattleEndReason.class));
                default -> decodeFakemonEvent(eventType, data);
            };
            return Optional.of(event);
        } catch (IllegalArgumentException | IllegalStateException | ArithmeticException error) {
            throw new JsonParseException("Événement réseau invalide.", error);
        }
    }

    private BattleEvent decodeFakemonEvent(String eventType, JsonObject data) {
        if (!EVENT_TYPES.containsKey(eventType)) {
            throw new JsonParseException("Événement réseau inconnu.");
        }
        Side wireSide = readEnum(data, "side", Side.class);
        Fakemon mon = gson.fromJson(readObject(data, "fakemon"), Fakemon.class);
        Team team = wireSide == Side.PLAYER ? hostTeam : guestTeam;
        if (!team.getFakemons().contains(mon)) {
            throw new JsonParseException("Le Fakemon ne correspond pas au camp de l'événement.");
        }
        Side side = toLocalSide(wireSide);
        return switch (eventType) {
            case "STATUS_APPLIED" -> new StatusAppliedEvent(side, mon, readEnum(data, "status", StatusType.class));
            case "STATUS_ACTIVATED" -> new StatusActivatedEvent(side, mon, readEnum(data, "status", StatusType.class));
            case "ATTACK_PREVENTED" -> new AttackPreventedEvent(side, mon, readEnum(data, "status", StatusType.class));
            case "STATUS_REMOVED" -> new StatusRemovedEvent(side, mon, readEnum(data, "status", StatusType.class));
            case "FAKEMON_ENTERED" -> new FakemonEnteredEvent(side, mon);
            case "FAKEMON_FAINTED" -> new FakemonFaintedEvent(side, mon);
            case "CRITICAL_HIT" -> new CriticalHitEvent(side, mon);
            case "MOVE_USED" -> new MoveUsedEvent(side, mon, readMove(data, mon));
            case "MOVE_MISSED" -> new MoveMissedEvent(side, mon, readMove(data, mon));
            case "EFFECTIVENESS" -> new EffectivenessEvent(
                    side, mon, readEnum(data, "effectiveness", Effectiveness.class));
            case "TALENT_ACTIVATED" -> new TalentActivatedEvent(
                    side, mon, readEnum(data, "talent", Talent.class));
            case "ITEM_ACTIVATED" -> new ItemActivatedEvent(side, mon, readEnum(data, "item", Item.class));
            case "HP_LOST" -> {
                int remaining = readInt(data, "remainingHp", 0, mon.getMaxHp());
                int lost = readInt(data, "hpLost", 0, mon.getMaxHp() - remaining);
                yield new HpLostEvent(side, mon, lost, remaining);
            }
            case "HP_RESTORED" -> {
                int remaining = readInt(data, "remainingHp", 0, mon.getMaxHp());
                int restored = readInt(data, "hpRestored", 0, remaining);
                yield new HpRestoredEvent(side, mon, restored, remaining);
            }
            default -> throw new JsonParseException("Événement réseau inconnu.");
        };
    }

    private Move readMove(JsonObject data, Fakemon mon) {
        JsonObject moveData = readObject(data, "move");
        return mon.getMoves().stream()
                .filter(move -> gson.toJsonTree(move).equals(moveData))
                .findFirst()
                .orElseThrow(() -> new JsonParseException("L'attaque est absente des attaques du Fakemon."));
    }

    private static JsonObject readObject(JsonObject data, String field) {
        JsonElement value = data.get(field);
        if (value == null || !value.isJsonObject()) {
            throw new JsonParseException("Le champ " + field + " doit être un objet.");
        }
        return value.getAsJsonObject();
    }

    private static String readString(JsonObject data, String field) {
        JsonElement value = data.get(field);
        if (value == null || !value.isJsonPrimitive() || !value.getAsJsonPrimitive().isString()) {
            throw new JsonParseException("Le champ " + field + " doit être une chaîne.");
        }
        return value.getAsString();
    }

    private static int readInt(JsonObject data, String field, int minimum, int maximum) {
        JsonElement value = data.get(field);
        if (value == null || !value.isJsonPrimitive() || !value.getAsJsonPrimitive().isNumber()) {
            throw new JsonParseException("Le champ " + field + " doit être un entier.");
        }
        int number = value.getAsBigDecimal().intValueExact();
        if (number < minimum || number > maximum) {
            throw new JsonParseException("Le champ " + field + " est hors limites.");
        }
        return number;
    }

    private static <E extends Enum<E>> E readEnum(JsonObject data, String field, Class<E> enumClass) {
        return Enum.valueOf(enumClass, readString(data, field));
    }

    private Side toLocalSide(Side side) {
        return localSide == Side.PLAYER ? side : opposite(side);
    }

    private static Side opposite(Side side) {
        return side == Side.PLAYER ? Side.OPPONENT : Side.PLAYER;
    }

    private static final class FakemonReferenceAdapter
            implements JsonSerializer<Fakemon>, JsonDeserializer<Fakemon> {
        private final Team hostTeam;
        private final Team guestTeam;

        private FakemonReferenceAdapter(Team hostTeam, Team guestTeam) {
            this.hostTeam = hostTeam;
            this.guestTeam = guestTeam;
        }

        @Override
        public JsonElement serialize(Fakemon mon, Type type, JsonSerializationContext context) {
            int index = hostTeam.getFakemons().indexOf(mon);
            Side side = Side.PLAYER;
            if (index < 0) {
                index = guestTeam.getFakemons().indexOf(mon);
                side = Side.OPPONENT;
            }
            if (index < 0) {
                throw new JsonParseException("Le Fakemon est absent des équipes du combat.");
            }
            JsonObject reference = new JsonObject();
            reference.addProperty("side", side.name());
            reference.addProperty("teamIndex", index);
            return reference;
        }

        @Override
        public Fakemon deserialize(JsonElement json, Type type, JsonDeserializationContext context) {
            try {
                JsonObject reference = json.getAsJsonObject();
                Side side = readEnum(reference, "side", Side.class);
                Team team = side == Side.PLAYER ? hostTeam : guestTeam;
                int index = readInt(reference, "teamIndex", 0, team.getFakemons().size() - 1);
                return team.getFakemons().get(index);
            } catch (IllegalArgumentException | IllegalStateException | ArithmeticException error) {
                throw new JsonParseException("Référence de Fakemon invalide.", error);
            }
        }
    }
}
