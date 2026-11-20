package fr.epitech.fakemon.ui;

import fr.epitech.fakemon.core.api.BattleEvent;
import fr.epitech.fakemon.core.api.FakemonFaintedEvent;
import fr.epitech.fakemon.core.api.HpLostEvent;
import fr.epitech.fakemon.core.api.HpRestoredEvent;
import fr.epitech.fakemon.core.api.ItemActivatedEvent;
import fr.epitech.fakemon.core.api.MoveUsedEvent;
import fr.epitech.fakemon.core.api.StatusAppliedEvent;
import fr.epitech.fakemon.core.api.StatusRemovedEvent;
import fr.epitech.fakemon.core.api.TalentActivatedEvent;
import fr.epitech.fakemon.core.engine.TypeChart;
import fr.epitech.fakemon.core.model.Fakemon;
import fr.epitech.fakemon.core.model.Item;
import fr.epitech.fakemon.core.model.Move;
import fr.epitech.fakemon.core.model.MoveCategory;
import fr.epitech.fakemon.core.model.Stats;
import fr.epitech.fakemon.core.model.StatusType;
import fr.epitech.fakemon.core.model.Talent;
import fr.epitech.fakemon.core.model.Type;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.IdentityHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import javafx.scene.Node;
import javafx.scene.control.Control;
import javafx.scene.control.Tooltip;
import javafx.util.Duration;

/**
 * Présente les informations connues au moment où les événements sont affichés.
 * Les observations du combat avancent sur le thread JavaFX avec le journal.
 */
final class BattleTooltips {
    private final Map<Fakemon, Seen> observations = new IdentityHashMap<>();
    private final TypeChart types = new TypeChart();

    /** Mémorise les PV et le statut capturés à l'entrée, sans révéler l'équipement adverse. */
    void entered(Fakemon mon, int hp, StatusType status) {
        Seen seen = seen(mon);
        seen.hp = hp;
        seen.status = status;
    }

    /** Enregistre un événement seulement lorsque le journal et les cartouches l'affichent. */
    void observe(BattleEvent event) {
        if (event instanceof HpLostEvent lost) {
            seen(lost.getFakemon()).hp = lost.getRemainingHp();
        } else if (event instanceof HpRestoredEvent restored) {
            seen(restored.getFakemon()).hp = restored.getRemainingHp();
        } else if (event instanceof FakemonFaintedEvent fainted) {
            seen(fainted.getFakemon()).hp = 0;
        } else if (event instanceof StatusAppliedEvent applied) {
            seen(applied.getFakemon()).status = applied.getStatus();
        } else if (event instanceof StatusRemovedEvent removed) {
            seen(removed.getFakemon()).status = null;
        } else if (event instanceof ItemActivatedEvent activated) {
            Seen seen = seen(activated.getFakemon());
            seen.item = activated.getItem();
            seen.itemKnown = true;
        } else if (event instanceof TalentActivatedEvent activated) {
            seen(activated.getFakemon()).talent = activated.getTalent();
        } else if (event instanceof MoveUsedEvent used) {
            seen(used.getFakemon()).moves.add(used.getMove());
        }
    }

    boolean itemKnown(Fakemon mon) {
        return seen(mon).itemKnown;
    }

    Item revealedItem(Fakemon mon) {
        return seen(mon).item;
    }

    /** Compose la fiche alliée complète ou la fiche adverse limitée aux révélations. */
    String fakemon(Fakemon mon, boolean allied) {
        if (mon == null) {
            return "En attente d'un Fakemon.";
        }
        Seen seen = seen(mon);
        String percent = number(100.0 * seen.hp / mon.getMaxHp()) + " %";
        StringBuilder text = new StringBuilder(mon.getSpecies().getFrenchName())
                .append("\nTypes : ").append(mon.getSpecies().getTypes().stream()
                        .map(BattleTooltips::typeName).collect(Collectors.joining(" / ")))
                .append("\nPV : ").append(allied ? seen.hp + " / " + mon.getMaxHp() + " (" + percent + ")" : percent);
        if (seen.status != null) {
            text.append("\nStatut : ").append(seen.status.getDisplayName());
        }
        Talent talent = allied ? mon.getSpecies().getTalent() : seen.talent;
        text.append("\nTalent : ").append(talent == null ? "inconnu" : talentDescription(talent));
        Item item = allied ? mon.getItem() : seen.item;
        text.append("\nObjet : ").append(!allied && !seen.itemKnown ? "inconnu" : itemDescription(item));
        if (allied) {
            Stats stats = item == null ? mon.getSpecies().getStats() : item.decorate(mon.getSpecies().getStats());
            if (seen.status != null) {
                stats = seen.status.create().decorate(stats);
            }
            text.append("\nAttaque ").append(number(stats.getAttack()))
                    .append(" / Défense ").append(number(stats.getDefense()))
                    .append("\nAtt. spé. ").append(number(stats.getSpecialAttack()))
                    .append(" / Déf. spé. ").append(number(stats.getSpecialDefense()))
                    .append("\nVitesse : ").append(number(stats.getSpeed()));
        } else {
            text.append("\nVitesse : ").append(number(mon.getSpecies().getStats().getSpeed()))
                    .append(" (avant modificateurs)");
        }
        List<Move> moves = allied ? mon.getMoves() : List.copyOf(seen.moves);
        text.append(allied ? "\nAttaques :" : "\nAttaques révélées :");
        if (moves.isEmpty()) {
            text.append(" aucune");
        } else {
            moves.forEach(move -> text.append("\n• ").append(move.getName()));
        }
        return text.toString();
    }

    /** Décrit une attaque et son efficacité contre la cible actuellement affichée. */
    String move(Move move, Fakemon target) {
        String category = switch (move.getCategory()) {
            case PHYSICAL -> "Physique";
            case SPECIAL -> "Spéciale";
            case STATUS -> "Statut";
        };
        String text = move.getName() + "\nType : " + typeName(move.getType()) + " / " + category
                + "\nPuissance : " + (move.getCategory() == MoveCategory.STATUS ? "sans dégâts" : move.getPower())
                + "\nPrécision : " + move.getAccuracy() + " %"
                + "\nPriorité : " + (move.getPriority() > 0 ? "+" : "") + move.getPriority();
        if (move.getStatus() != null) {
            text += "\nEffet : " + move.getStatus().getDisplayName() + " (" + move.getStatusChance() + " % après réussite)";
        }
        if (target == null) {
            return text + "\nAucune cible active.";
        }
        String name = target.getSpecies().getFrenchName();
        if (move.getCategory() == MoveCategory.STATUS) {
            return text + "\n" + statusResult(move, target);
        }
        double multiplier = types.multiplier(move.getType(), target.getSpecies().getTypes());
        String result = multiplier == 0 ? "Aucun effet" : multiplier > 1 ? "Super efficace"
                : multiplier < 1 ? "Peu efficace" : "Efficacité normale";
        text += "\n" + result + " contre " + name + " (×" + number(multiplier) + ")";
        if (move.getStatus() != null
                && (seen(target).status != null || !move.getStatus().create().canApply(target))) {
            text += "\n" + statusResult(move, target);
        }
        return text;
    }

    private String statusResult(Move move, Fakemon target) {
        String result = seen(target).status != null ? "possède déjà un statut."
                : !move.getStatus().create().canApply(target) ? "est immunisé à ce statut."
                : "peut recevoir ce statut.";
        return target.getSpecies().getFrenchName() + " " + result;
    }

    /** Installe une infobulle lisible, rafraîchie au survol et fermée quand son nœud disparaît. */
    static Tooltip install(Node node, Supplier<String> content) {
        Tooltip tooltip = new Tooltip(content.get());
        tooltip.setWrapText(true);
        tooltip.setMaxWidth(390);
        tooltip.setShowDelay(Duration.millis(250));
        tooltip.setHideDelay(Duration.millis(100));
        tooltip.setShowDuration(Duration.INDEFINITE);
        tooltip.setStyle("-fx-font-size: 13px; -fx-text-fill: #f4f1ea; -fx-background-color: #262b45;"
                + "-fx-border-color: #f2c230; -fx-padding: 10; -fx-background-radius: 5; -fx-border-radius: 5;");
        tooltip.setOnShowing(event -> tooltip.setText(content.get()));
        if (node instanceof Control control) {
            control.setTooltip(tooltip);
        } else {
            Tooltip.install(node, tooltip);
        }
        node.sceneProperty().addListener((property, previous, current) -> {
            if (previous != null && current == null) {
                tooltip.hide();
            }
        });
        return tooltip;
    }

    private Seen seen(Fakemon mon) {
        return observations.computeIfAbsent(mon, member -> new Seen(member.getMaxHp()));
    }

    private static String number(double value) {
        return BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP).stripTrailingZeros()
                .toPlainString().replace('.', ',');
    }

    private static String typeName(Type type) {
        return switch (type) {
            case FIRE -> "Feu";
            case WATER -> "Eau";
            case NATURE -> "Nature";
            case LIGHTNING -> "Foudre";
            case ROCK -> "Roche";
            case WIND -> "Vent";
            case VENOM -> "Venin";
            case FORCE -> "Force";
            case SHADOW -> "Ombre";
            case ETHER -> "Éther";
        };
    }

    private static String itemDescription(Item item) {
        if (item == null) {
            return "aucun";
        }
        return item.getDisplayName() + switch (item) {
            case GANTELET -> " — Attaque et Att. spé. +20 %";
            case ECU -> " — Défense et Déf. spé. +20 %";
            case PLUME -> " — vitesse +20 %";
        };
    }

    private static String talentDescription(Talent talent) {
        return BattleBadges.talentName(talent) + switch (talent) {
            case FRACAS -> " — à l'entrée, retire 10 % des PV max adverses sans mettre K.O.";
            case CARAPACE -> " — réduit de 25 % les dégâts super efficaces reçus";
            case DERNIER_SOUFFLE -> " — dégâts +50 % sous un tiers des PV max";
            case RECUPERATION -> " — rend 10 % des PV max en fin de tour";
        };
    }

    private static final class Seen {
        private int hp;
        private StatusType status;
        private boolean itemKnown;
        private Item item;
        private Talent talent;
        private final Set<Move> moves = new LinkedHashSet<>();

        private Seen(int hp) {
            this.hp = hp;
        }
    }
}
