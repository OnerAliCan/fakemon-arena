package fr.epitech.fakemon.ui;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.function.Consumer;
import java.util.regex.Pattern;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

/** Présente l'équipe du projet et les attributions des ressources embarquées. */
final class CreditsScreen extends BorderPane {
    private static final Pattern DESIGNER = Pattern.compile("- Designer : \\[([^]]+)]\\((.+)\\)");
    private static final String LIGHT = "-fx-text-fill: #fff5dd;";
    private static final String MUTED = "-fx-text-fill: #d6dcf5;";

    /** Construit les crédits et relie les actions de retour et d'ouverture des sources. */
    CreditsScreen(Runnable back, Consumer<String> openLink) {
        setFocusTraversable(true);
        setPadding(new Insets(22));
        setStyle("-fx-background-color: #19233a;");
        Label title = label("Crédits", LIGHT + "-fx-font-size: 28px; -fx-font-weight: bold;");
        Button returnButton = new Button("Retour au menu");
        MenuSounds.action(returnButton, MenuSounds.Sound.CLOSE);
        returnButton.setStyle("-fx-base: #f6c443;");
        returnButton.setOnAction(event -> back.run());
        HBox header = new HBox(16, title, returnButton);
        header.setAlignment(Pos.CENTER_LEFT);
        setTop(header);

        VBox content = new VBox(10,
                section("Fakemon Arena"),
                label("Projet étudiant de jeu 2D · Epitech", MUTED),
                label("Ali Can Öner (@OnerAliCan) · Scrum Master : moteur, IA, données et tests.", LIGHT),
                label("Matteo Bleu-Vialon (@matteobleuvialon) · Product Owner : interface, animations et son.", LIGHT),
                section("Créatures et sprites"),
                link("Collection Mongratis sur Pokengine", "https://pokengine.org/collections/107s7x9x/Mongratis", openLink),
                label("Utilisation non commerciale avec attribution.", MUTED));
        appendSpriteCredits(content, openLink);
        content.getChildren().addAll(
                section("Musiques"),
                link("Musique de combat : PanicAce! — Soundtrack Pokemon battle theme",
                        "https://panicace.itch.io/soundtrack-pokemon-theme-battle", openLink),
                label("Composition originale proposée en utilisation gratuite par son auteur.", MUTED),
                section("Effets sonores"),
                link("JRPG Style UI Sounds — KillaMaaki",
                        "https://opengameart.org/content/jrpg-style-ui-sounds", openLink),
                link("Licence Creative Commons Attribution 3.0",
                        "https://creativecommons.org/licenses/by/3.0/", openLink),
                section("Outils et remerciements"),
                label("Java, JavaFX et Maven. Merci aux artistes de la communauté Mongratis.", LIGHT));
        content.setPadding(new Insets(10, 14, 16, 14));
        content.setStyle("-fx-background-color: #262b45;");
        ScrollPane scroll = new ScrollPane(content);
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background: #262b45; -fx-background-color: #3a4170;");
        BorderPane.setMargin(scroll, new Insets(14, 0, 0, 0));
        setCenter(scroll);

        addEventFilter(KeyEvent.KEY_PRESSED, event -> {
            if (event.getCode() == KeyCode.ESCAPE) {
                event.consume();
                MenuSounds.play(this, MenuSounds.Sound.CLOSE);
                back.run();
            }
        });
    }

    private void appendSpriteCredits(VBox content, Consumer<String> openLink) {
        var stream = getClass().getResourceAsStream("/sprites/CREDITS.md");
        if (stream == null) {
            content.getChildren().add(label("Les crédits détaillés des sprites sont indisponibles.", MUTED));
            return;
        }
        try (var reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
            String creature = "";
            String source = "";
            VBox detail = null;
            for (String line; (line = reader.readLine()) != null;) {
                if (line.startsWith("## ")) {
                    creature = line.substring(3);
                    source = "";
                    detail = null;
                } else if (line.startsWith("- Page : ")) {
                    source = line.substring(9).strip();
                } else if (!creature.isEmpty() && line.startsWith("- Designer : ")) {
                    var designer = DESIGNER.matcher(line);
                    Label name = label(creature, LIGHT + "-fx-font-weight: bold;");
                    name.setMinWidth(145);
                    var credit = designer.matches() ? link(designer.group(1), designer.group(2), openLink)
                            : label(line.substring(13), LIGHT);
                    HBox row = new HBox(12, name, credit);
                    HBox.setHgrow(credit, Priority.ALWAYS);
                    row.setAlignment(Pos.CENTER_LEFT);
                    if (!source.isEmpty()) {
                        row.getChildren().add(link("Fiche source", source, openLink));
                    }
                    detail = new VBox(2, row);
                    detail.setPadding(new Insets(5, 0, 5, 0));
                    detail.setStyle("-fx-border-color: #3a4170 transparent transparent transparent;");
                    content.getChildren().add(detail);
                } else if (detail != null && line.startsWith("- Notes : ")) {
                    String notes = line.substring(10).replaceAll("\\[([^]]+)]\\([^)]+\\)", "$1");
                    detail.getChildren().add(label(notes, MUTED));
                }
            }
        } catch (IOException error) {
            content.getChildren().add(label("Impossible de lire tous les crédits des sprites.", MUTED));
        }
    }

    private Label section(String title) {
        return label(title, LIGHT + "-fx-font-size: 17px; -fx-font-weight: bold;");
    }

    private Label label(String text, String style) {
        Label label = new Label(text);
        label.setWrapText(true);
        label.setStyle(style);
        return label;
    }

    private Hyperlink link(String text, String url, Consumer<String> openLink) {
        Hyperlink link = new Hyperlink(text);
        link.setWrapText(true);
        link.setStyle("-fx-text-fill: #f6c443;");
        link.setOnAction(event -> openLink.accept(url));
        return link;
    }
}
