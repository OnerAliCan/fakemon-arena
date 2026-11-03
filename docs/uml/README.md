# Diagrammes UML

Les sources sont en PlantUML (`.puml`) ; les PNG sont générés à partir d'elles et commités à côté.

| Diagramme | Source | Issue |
|---|---|---|
| Cas d'utilisation | [cas-utilisation.puml](cas-utilisation.puml) | #5 |
| Navigation entre les écrans | [navigation.puml](navigation.puml) | #5 |
| Écran de combat : déroulement d'un tour | [combat.puml](combat.puml) | #5 |

![Cas d'utilisation](cas-utilisation.png)

![Navigation entre les écrans](navigation.png)

![Écran de combat](combat.png)

## Régénérer les PNG

Les diagrammes utilisent `!pragma layout smetana` : pas besoin d'installer Graphviz, Java suffit.

- **VS Code :** extension *PlantUML* (jebbs), clic droit dans le fichier → *Export Current Diagram* → PNG.
- **Terminal :** téléchargez `plantuml.jar` depuis [plantuml.com/download](https://plantuml.com/download), puis, depuis la racine du dépôt :

```bash
java -jar plantuml.jar -tpng -charset UTF-8 docs/uml/*.puml
```

Après chaque modification d'un `.puml`, régénérez le PNG et commitez les deux ensemble.
