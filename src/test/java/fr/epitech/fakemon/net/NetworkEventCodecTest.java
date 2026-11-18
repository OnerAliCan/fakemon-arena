package fr.epitech.fakemon.net;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import fr.epitech.fakemon.core.api.BattleEndReason;
import fr.epitech.fakemon.core.api.BattleEndedEvent;
import fr.epitech.fakemon.core.api.BattleEvent;
import fr.epitech.fakemon.core.api.CriticalHitEvent;
import fr.epitech.fakemon.core.api.Effectiveness;
import fr.epitech.fakemon.core.api.EffectivenessEvent;
import fr.epitech.fakemon.core.api.FakemonEnteredEvent;
import fr.epitech.fakemon.core.api.FakemonFaintedEvent;
import fr.epitech.fakemon.core.api.FakemonSelectionRequiredEvent;
import fr.epitech.fakemon.core.api.HpLostEvent;
import fr.epitech.fakemon.core.api.HpRestoredEvent;
import fr.epitech.fakemon.core.api.ItemActivatedEvent;
import fr.epitech.fakemon.core.api.MoveMissedEvent;
import fr.epitech.fakemon.core.api.MoveUsedEvent;
import fr.epitech.fakemon.core.api.SelectionReason;
import fr.epitech.fakemon.core.api.Side;
import fr.epitech.fakemon.core.api.TalentActivatedEvent;
import fr.epitech.fakemon.core.api.TurnStartedEvent;
import fr.epitech.fakemon.core.model.BaseStats;
import fr.epitech.fakemon.core.model.Fakemon;
import fr.epitech.fakemon.core.model.Item;
import fr.epitech.fakemon.core.model.Move;
import fr.epitech.fakemon.core.model.MoveCategory;
import fr.epitech.fakemon.core.model.Species;
import fr.epitech.fakemon.core.model.Talent;
import fr.epitech.fakemon.core.model.Team;
import fr.epitech.fakemon.core.model.Type;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Vérifie les événements reçus et l'adressage des demandes de sélection. */
class NetworkEventCodecTest {
    private final Move move = new Move("Frappe", Type.FORCE, MoveCategory.PHYSICAL, 50, 100, 0);
    private final Fakemon hostMon = createFakemon("Hote");
    private final Fakemon guestMon = createFakemon("Invite");
    private final Team host = new Team(List.of(hostMon));
    private final Team guest = new Team(List.of(guestMon));
    private final NetworkEventCodec hostCodec = new NetworkEventCodec(host, guest, Side.PLAYER);
    private final NetworkEventCodec guestCodec = new NetworkEventCodec(host, guest, Side.OPPONENT);

    @Test
    void rejectsMissingAndNullFieldsForEveryEventType() {
        for (BattleEvent event : events()) {
            NetworkMessage message = hostCodec.encode(event);
            JsonObject data = message.getPayload().getAsJsonObject("data");
            for (String field : List.copyOf(data.keySet())) {
                JsonObject missing = message.getPayload().deepCopy();
                missing.getAsJsonObject("data").remove(field);
                assertThrows(JsonParseException.class,
                        () -> guestCodec.decode(new NetworkMessage("EVENT", missing)),
                        event.getClass().getSimpleName() + " sans " + field);
                JsonObject nullValue = message.getPayload().deepCopy();
                nullValue.getAsJsonObject("data").add(field, JsonNull.INSTANCE);
                assertThrows(JsonParseException.class,
                        () -> guestCodec.decode(new NetworkMessage("EVENT", nullValue)),
                        event.getClass().getSimpleName() + " avec " + field + " null");
            }
        }
    }

    @Test
    void reconstructsEverySupportedEventAfterJsonTransport() {
        NetworkMessageCodec messages = new NetworkMessageCodec();
        for (BattleEvent event : events()) {
            NetworkMessage received = messages.decode(messages.encode(hostCodec.encode(event)));
            assertEquals(event.getClass(), guestCodec.decode(received).orElseThrow().getClass());
        }
    }

    @Test
    void rejectsNegativeFractionalAndExcessiveHealth() {
        for (String field : List.of("hpLost", "remainingHp")) {
            for (Number invalid : List.<Number>of(-1, 0.5, 101, 2147483648L)) {
                NetworkMessage message = hostCodec.encode(new HpLostEvent(Side.PLAYER, hostMon, 35, 65));
                message.getPayload().getAsJsonObject("data").addProperty(field, invalid);
                assertThrows(JsonParseException.class, () -> guestCodec.decode(message));
            }
        }
        NetworkMessage impossible = hostCodec.encode(new HpLostEvent(Side.PLAYER, hostMon, 40, 65));
        assertThrows(JsonParseException.class, () -> guestCodec.decode(impossible));
    }

    @Test
    void rejectsRestorationLargerThanRemainingHealth() {
        NetworkMessage message = hostCodec.encode(new HpRestoredEvent(Side.PLAYER, hostMon, 30, 20));
        assertThrows(JsonParseException.class, () -> guestCodec.decode(message));
    }

    @Test
    void preservesValidHealthAndUsesTheLocalFakemonReference() {
        HpLostEvent event = assertInstanceOf(HpLostEvent.class, guestCodec.decode(
                hostCodec.encode(new HpLostEvent(Side.PLAYER, hostMon, 35, 65))).orElseThrow());
        assertEquals(Side.OPPONENT, event.getSide());
        assertSame(hostMon, event.getFakemon());
        assertEquals(35, event.getHpLost());
        assertEquals(65, event.getRemainingHp());
    }

    @Test
    void rejectsFakemonFromTheWrongSide() {
        NetworkMessage message = hostCodec.encode(new FakemonEnteredEvent(Side.PLAYER, hostMon));
        message.getPayload().getAsJsonObject("data").addProperty("side", "OPPONENT");
        assertThrows(JsonParseException.class, () -> guestCodec.decode(message));
    }

    @Test
    void rejectsUnknownEnumsAndInvalidTurnNumbers() {
        NetworkMessage ended = hostCodec.encode(new BattleEndedEvent(Side.PLAYER, BattleEndReason.ALL_FAINTED));
        ended.getPayload().getAsJsonObject("data").addProperty("winner", "UNKNOWN");
        assertThrows(JsonParseException.class, () -> guestCodec.decode(ended));
        for (Number invalid : List.<Number>of(0, -1, 1.5)) {
            NetworkMessage turn = hostCodec.encode(new TurnStartedEvent(1));
            turn.getPayload().getAsJsonObject("data").addProperty("turnNumber", invalid);
            assertThrows(JsonParseException.class, () -> guestCodec.decode(turn));
        }
    }

    @Test
    void resolvesAttacksFromTheExistingFakemonAndRejectsIncompleteAttacks() {
        NetworkMessage message = hostCodec.encode(new MoveUsedEvent(Side.PLAYER, hostMon, move));
        MoveUsedEvent event = assertInstanceOf(MoveUsedEvent.class, guestCodec.decode(message).orElseThrow());
        assertSame(move, event.getMove());
        message.getPayload().getAsJsonObject("data").getAsJsonObject("move").remove("power");
        assertThrows(JsonParseException.class, () -> guestCodec.decode(message));
    }

    @Test
    void selectionIsDeliveredOnlyToItsRecipient() {
        for (Side recipient : Side.values()) {
            NetworkMessage message = hostCodec.encodeSelection(
                    new FakemonSelectionRequiredEvent(SelectionReason.REPLACEMENT), recipient);
            assertEquals(recipient == Side.PLAYER, hostCodec.decode(message).isPresent());
            assertEquals(recipient == Side.OPPONENT, guestCodec.decode(message).isPresent());
            var decoder = recipient == Side.PLAYER ? hostCodec : guestCodec;
            var event = assertInstanceOf(FakemonSelectionRequiredEvent.class, decoder.decode(message).orElseThrow());
            assertEquals(SelectionReason.REPLACEMENT, event.getReason());
        }
    }

    @Test
    void rejectsSelectionWithoutARecipientAndValidatesIgnoredSelections() {
        var selection = new FakemonSelectionRequiredEvent(SelectionReason.REPLACEMENT);
        assertThrows(IllegalArgumentException.class, () -> hostCodec.encode(selection));
        NetworkMessage message = hostCodec.encodeSelection(selection, Side.PLAYER);
        message.getPayload().remove("recipient");
        assertThrows(JsonParseException.class, () -> guestCodec.decode(message));
        NetworkMessage incomplete = hostCodec.encodeSelection(selection, Side.PLAYER);
        incomplete.getPayload().getAsJsonObject("data").remove("reason");
        assertThrows(JsonParseException.class, () -> guestCodec.decode(incomplete));
    }

    @Test
    void rejectsIncompleteReferencesAndUnknownEventTypes() {
        NetworkMessage message = hostCodec.encode(new FakemonEnteredEvent(Side.PLAYER, hostMon));
        message.getPayload().getAsJsonObject("data").getAsJsonObject("fakemon").remove("teamIndex");
        assertThrows(JsonParseException.class, () -> guestCodec.decode(message));
        message.getPayload().addProperty("event", "UNKNOWN");
        assertThrows(JsonParseException.class, () -> guestCodec.decode(message));
    }

    @Test
    void invertsTheWinnerForTheGuest() {
        BattleEndedEvent result = assertInstanceOf(BattleEndedEvent.class, guestCodec.decode(
                hostCodec.encode(new BattleEndedEvent(Side.PLAYER, BattleEndReason.ALL_FAINTED))).orElseThrow());
        assertEquals(Side.OPPONENT, result.getWinner());
        assertEquals(BattleEndReason.ALL_FAINTED, result.getReason());
    }

    private List<BattleEvent> events() {
        return List.of(
                new TurnStartedEvent(1),
                new FakemonEnteredEvent(Side.PLAYER, hostMon),
                new MoveUsedEvent(Side.PLAYER, hostMon, move),
                new MoveMissedEvent(Side.PLAYER, hostMon, move),
                new HpLostEvent(Side.PLAYER, hostMon, 35, 65),
                new HpRestoredEvent(Side.OPPONENT, guestMon, 10, 80),
                new CriticalHitEvent(Side.OPPONENT, guestMon),
                new EffectivenessEvent(Side.OPPONENT, guestMon, Effectiveness.SUPER_EFFECTIVE),
                new FakemonFaintedEvent(Side.OPPONENT, guestMon),
                new ItemActivatedEvent(Side.PLAYER, hostMon, Item.PLUME),
                new TalentActivatedEvent(Side.PLAYER, hostMon, Talent.FRACAS),
                new BattleEndedEvent(Side.PLAYER, BattleEndReason.ALL_FAINTED));
    }

    private Fakemon createFakemon(String name) {
        Species species = new Species.Builder().originalName(name).frenchName(name)
                .types(List.of(Type.FORCE)).stats(new BaseStats(100, 100, 100, 100, 100, 100))
                .possibleMoves(List.of(move)).talent(Talent.FRACAS)
                .frontSprite("/sprites/front/test.png").backSprite("/sprites/back/test.png").build();
        return new Fakemon(species, List.of(move, move, move, move));
    }
}
