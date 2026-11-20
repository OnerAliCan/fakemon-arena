# Ajouter un fakemon — issue #20

Depuis le menu, « Ajouter un Mon » ouvre `AddSpeciesScreen`. Le formulaire
reste utilisable au clavier et défile dans la fenêtre ; les actions de retour,
d'enregistrement et les messages restent en bas de l'écran.

## Champs et médias

- Nom d'origine et nom français obligatoires ; les espaces extérieurs sont retirés.
- Sprite avant obligatoire, choisi avec `FileChooser` (PNG, JPEG ou GIF).
- Sprite arrière facultatif. L'aperçu reprend la face en miroir jusqu'à ce
  qu'un fichier arrière soit choisi. Le bouton « Utiliser la face retournée »
  permet de retirer ce fichier.
- Cri WAV ou MP3 facultatif, choisi avec `FileChooser`, avec écoute et retrait.
  L'écoute respecte le volume des effets de la session. Le lecteur est libéré
  au remplacement du fichier, en fin de lecture et quand on quitte l'écran.
  Un fichier illisible affiche un message sans interrompre le formulaire.
- Un ou deux types distincts parmi les dix types du jeu.
- Six curseurs entiers, de 50 à 150, avec les valeurs et le total en direct.
  Le dépôt limite le total à 600 ; un dépassement est signalé à l'enregistrement.
- Talent obligatoire, parmi Carapace, Dernier souffle, Fracas et Récupération.
- Liste à cocher des 38 attaques du catalogue. La recherche porte sur le nom
  ou le type français et ignore les accents. Les choix cachés par le filtre
  restent cochés. Il faut au moins quatre attaques pour composer les combats.
- Designer et concept facultatifs. Le numéro de Mondex suit le plus grand
  numéro du catalogue ; il n'est pas demandé à l'utilisateur.

## Enregistrement

`SpeciesDraft` construit la fiche à partir des contrôles. `SpeciesRepository.add`
reste la frontière de validation : ses `InvalidSpeciesException` sont affichées
sans vider le formulaire. Le succès confirme le nom du fakemon ; une nouvelle
saisie s'ouvre avec « Ajouter un autre fakemon ».

L'issue #22 est déjà livrée : on utilise donc `JsonSpeciesRepository`, plutôt
qu'un stockage uniquement en mémoire. Il copie les médias et enregistre la fiche
sous `~/.fakemon-arena`. Sans sprite arrière, il écrit une image PNG miroir de la
face dans les médias sauvegardés. Le dos miroir reste disponible même après
suppression du fichier avant d'origine. Une erreur de copie ou de génération
annule l'ajout et nettoie les fichiers temporaires.

Au retour dans le menu, le Mondex et la sélection d'équipe rechargent ce même
dépôt : le nouveau fakemon est disponible immédiatement et après redémarrage.
Les attaques cochées sont les instances du dépôt, pour respecter les références
utilisées par la sauvegarde JSON.

## Vérification

- `./mvnw verify` : construction de fiche, contraintes du dépôt, filtrage,
  sauvegarde, dos miroir pixel par pixel et rechargement sans les fichiers source.
- `./mvnw verify -Dfakemon.ui.tests=true` : ajoute le formulaire JavaFX,
  les limites des curseurs, la limite de deux types, la conservation des choix
  filtrés, les messages d'erreur, les aperçus et l'écoute d'un cri invalide.
- Les tests du formulaire sont également exécutés en CI Linux sous Xvfb.

Démonstration : ouvrir Ajouter un Mon, renseigner les noms et la face,
choisir un type, quatre attaques et un talent. Laisser les stats à 100,
le dos et le cri vides, puis enregistrer. Ouvrir le Mondex et composer une
équipe avec le nouveau fakemon. Fermer et relancer le jeu : sa fiche reste présente.
Essayer aussi un nom existant, moins de quatre attaques et un total supérieur
à 600 ; les messages doivent expliquer le refus sans perdre la saisie.
