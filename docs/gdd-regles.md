# GDD · Règles de combat

> Issue #3 · Dev A · à valider par Dev B à la review du mercredi 4 novembre.
> Les valeurs chiffrées sont un premier jet : elles seront ajustées en jouant.

L'expérience joueur, les écrans et la direction artistique sont dans [gdd-vision.md](gdd-vision.md).

## 1. Le combat en bref

- Deux équipes de **6 fakemon** s'affrontent : le joueur contre l'ordinateur.
- Les équipes sont **tirées au hasard** parmi les 30 fakemon du roster, sans doublon (12 fakemon différents par combat).
- Chaque fakemon a **4 attaques**, tirées dans la liste des attaques qu'il peut apprendre, un **talent** fixe et un **objet** tiré au hasard.
- Un seul fakemon par camp est en jeu à la fois. À chaque tour, chaque camp choisit **une action** : attaquer ou changer de fakemon.
- Un camp **perd** quand ses 6 fakemon sont KO.

Pas de niveaux, pas d'expérience, pas de PP : chaque combat repart de zéro.

## 2. Statistiques

Chaque fakemon a 6 statistiques :

| Stat | Rôle |
|---|---|
| **PV** | Points de vie. À 0, le fakemon est KO. |
| **Attaque** | Augmente les dégâts de ses attaques physiques. |
| **Défense** | Réduit les dégâts des attaques physiques reçues. |
| **Attaque spéciale** | Augmente les dégâts de ses attaques spéciales. |
| **Défense spéciale** | Réduit les dégâts des attaques spéciales reçues. |
| **Vitesse** | Décide qui attaque en premier. |

**Règle d'équilibrage :** chaque stat est comprise entre **50 et 150**, et la somme des 6 stats ne dépasse pas **600**. Les 30 fakemon du roster sont tous à 600 pile : aucun n'est plus fort qu'un autre sur le papier. Le dashboard (#20) refuse une fiche qui ne respecte pas cette règle.

Quatre profils pour s'y retrouver en saisissant le roster (#23) :

| Profil | PV | Attaque | Défense | Att. spé. | Déf. spé. | Vitesse |
|---|---|---|---|---|---|---|
| Rapide | 85 | 115 | 75 | 100 | 85 | 140 |
| Frappeur physique | 100 | 150 | 110 | 60 | 90 | 90 |
| Frappeur spécial | 90 | 60 | 80 | 150 | 115 | 105 |
| Costaud | 140 | 95 | 130 | 60 | 115 | 60 |

Les stats d'un fakemon suivent ses attaques : si elles sont surtout spéciales, son Attaque spéciale est haute et son Attaque plus basse (voir la section 4).

## 3. Les 10 types

Feu · Eau · Nature · Foudre · Roche · Vent · Venin · Force · Ombre · Éther

| Type | Thème |
|---|---|
| **Feu** | flammes, cendres, magma |
| **Eau** | mer, pluie, glace |
| **Nature** | plantes, arbres, fleurs |
| **Foudre** | électricité, énergie |
| **Roche** | pierre, sol, métal |
| **Vent** | air, oiseaux, tempêtes |
| **Venin** | poisons, insectes, araignées |
| **Force** | combat, arts martiaux, armes |
| **Ombre** | nuit, spectres, ruse |
| **Éther** | esprit, lumière, féerie, temps |

Un fakemon a **1 ou 2 types**.

### Table d'efficacité

Lecture : une attaque du type de la ligne, sur un fakemon du type de la colonne, fait **2** (super efficace), **½** (peu efficace), **0** (aucun effet) ou **·** (normal, ×1).

| Attaque ↓ / Cible → | Feu | Eau | Nat | Fdr | Roc | Vent | Ven | For | Omb | Éth |
|---|---|---|---|---|---|---|---|---|---|---|
| **Feu** | ½ | ½ | **2** | · | · | · | **2** | · | · | · |
| **Eau** | **2** | ½ | ½ | · | **2** | · | · | · | · | · |
| **Nature** | ½ | **2** | ½ | · | **2** | ½ | · | · | · | · |
| **Foudre** | · | **2** | ½ | ½ | 0 | **2** | · | · | · | · |
| **Roche** | **2** | · | · | **2** | ½ | **2** | · | ½ | · | · |
| **Vent** | · | · | **2** | ½ | ½ | · | **2** | **2** | · | · |
| **Venin** | · | · | **2** | · | · | · | ½ | · | ½ | **2** |
| **Force** | · | · | · | · | **2** | ½ | · | · | **2** | ½ |
| **Ombre** | · | · | · | **2** | · | · | · | ½ | ½ | **2** |
| **Éther** | · | · | · | · | · | · | ½ | **2** | **2** | ½ |

**Double type :** on multiplie les deux valeurs. Une attaque Eau sur Héphaïstodon (Feu / Roche) fait 2 × 2 = **×4** ; une attaque Feu sur Pyrale (Feu / Venin) fait ½ × 2 = **×1**.

Repères pour se souvenir de la table :

- le cycle **Feu → Nature → Eau → Feu** ;
- la **Roche** ne craint pas la **Foudre** (0) mais l'Eau, la Nature et la Force la cassent ;
- **Ombre** et **Éther** se font mutuellement mal (×2 dans les deux sens) ;
- chaque type est fort contre 2 ou 3 types et faible contre 2 ou 3 types.

## 4. Attaques

Chaque type a **3 attaques** : une rapide, une standard et une lourde. Soit **30 attaques**, sans effet secondaire (les statuts arrivent en bonus, #32).

| Catégorie | Puissance | Précision | Priorité | Usage |
|---|---|---|---|---|
| Rapide | 40 | 100 % | **+1** | passe avant les autres : pour achever un adversaire affaibli |
| Standard | 65 | 100 % | 0 | l'attaque fiable |
| Lourde | 95 | 80 % | 0 | plus forte en moyenne, mais rate une fois sur cinq |

Chaque attaque est aussi **physique** (P) ou **spéciale** (S) :

- une attaque **physique** utilise l'Attaque du lanceur et la Défense de la cible ;
- une attaque **spéciale** utilise l'Attaque spéciale du lanceur et la Défense spéciale de la cible.

| Type | Rapide (40, +1) | Standard (65) | Lourde (95, 80 %) |
|---|---|---|---|
| Feu | Tison (P) | Coulée ardente (S) | Fournaise (S) |
| Eau | Embrun (S) | Ressac (P) | Lame de fond (S) |
| Nature | Épine (P) | Fouet de ronces (P) | Grande floraison (S) |
| Foudre | Grésillement (S) | Arc électrique (S) | Orage sec (S) |
| Roche | Gravillon (P) | Coup de moellon (P) | Faille tellurique (P) |
| Vent | Courant d'air (S) | Bourrasque (S) | Cyclone (P) |
| Venin | Aiguillon (P) | Crachat acide (S) | Nuée venimeuse (P) |
| Force | Coup sec (P) | Crochet (P) | Coup de massue (P) |
| Ombre | Murmure (S) | Voile noir (S) | Éclipse (S) |
| Éther | Lueur (S) | Onde mentale (S) | Comète (P) |

Soit 14 attaques physiques et 16 spéciales. Roche et Force n'ont que des attaques physiques, Foudre et Ombre que des spéciales : chaque type a son style.

**Attaques qu'un fakemon peut apprendre :** les 3 attaques de chacun de ses types. Un fakemon d'un seul type ajoute les 3 attaques d'un **type de couverture**, indiqué dans le roster. Chaque fakemon a donc 6 attaques possibles, et en reçoit 4 au tirage.

## 5. Formule de dégâts

```
base    = puissance × Att ÷ Déf ÷ 4 + 2
dégâts  = base × bonus de type × efficacité × critique × aléa
```

| Facteur | Valeur |
|---|---|
| Att ÷ Déf | Attaque ÷ Défense pour une attaque physique ; Attaque spéciale ÷ Défense spéciale pour une attaque spéciale |
| Bonus de type | **×1,25** si l'attaque a l'un des types de l'attaquant, sinon ×1 |
| Efficacité | produit de la table des types : 0, ¼, ½, 1, 2 ou 4 |
| Critique | **×1,5**, avec 1 chance sur 16 |
| Aléa | un nombre entre **0,85 et 1,00** |

On arrondit à l'entier inférieur **à la fin seulement**. Une attaque qui touche fait **au moins 1** dégât, sauf si l'efficacité vaut 0.

Avant les dégâts, on tire la **précision** : si le tirage échoue, l'attaque est ratée et ne fait rien.

Tous les tirages (précision, critique, aléa, égalité de vitesse) passent par le même `Random`, donné au moteur à sa création. Les tests fixent sa graine pour être reproductibles (#9).

### Exemple chiffré

Délupard (Eau · Attaque 90) lance **Ressac** (Eau, physique, puissance 65) sur Pyrale (Feu / Venin · Défense 65 · 75 PV) :

| Étape | Calcul | Résultat |
|---|---|---|
| Base | 65 × 90 ÷ 65 ÷ 4 + 2 | 24,5 |
| Bonus de type | Délupard est de type Eau : × 1,25 | 30,625 |
| Efficacité | Eau sur Feu × 2, Eau sur Venin × 1 : × 2 | 61,25 |
| Critique | pas de critique : × 1 | 61,25 |
| Aléa | tirage 0,92 : × 0,92 | 56,35 |
| Arrondi | entier inférieur | **56 dégâts** |

Pyrale passe de 75 à 19 PV. Selon l'aléa, Ressac fait entre **52 et 61** dégâts ; un critique fait entre **78 et 91** et met Pyrale KO d'un coup.

Dans l'autre sens, la **Coulée ardente** de Pyrale (Feu, spéciale, 65, Attaque spéciale 85) sur Délupard (Défense spéciale 70) est peu efficace (Feu sur Eau × ½) : entre **11 et 13** dégâts. Pyrale a donc intérêt à changer de fakemon.

## 6. Déroulement d'un tour

1. **Choix.** Le joueur clique une attaque ou un changement ; l'ordinateur choisit en même temps (`AiController.chooseAction`). Aucun des deux ne voit le choix de l'autre.
2. **Changements d'abord.** Un changement passe toujours avant les attaques. Si les deux camps changent, le plus rapide change en premier.
3. **Attaques ensuite**, dans cet ordre :
   1. la **priorité** la plus haute (+1 pour les attaques rapides) ;
   2. à priorité égale, la **Vitesse** la plus haute, objet compris ;
   3. en cas d'égalité parfaite, **pile ou face**.
4. **Résolution de chaque attaque :** précision, puis dégâts. Si les PV tombent à 0, le fakemon est KO. Un fakemon KO avant d'avoir agi ne joue pas son action.
5. **Fin de tour :** les talents de fin de tour s'appliquent (Récupération), le plus rapide d'abord.
6. **Remplacements :** chaque camp dont le fakemon est KO en envoie un autre. Le joueur choisit dans le panneau d'équipe ; l'ordinateur prend le premier disponible. L'entrée en jeu déclenche les talents d'entrée (Fracas).
7. **Fin du combat :** dès qu'un camp n'a plus de fakemon debout, l'autre camp gagne.

Chaque étape émet un event vers l'écran (`BattleEvent`, #4) : dégâts, attaque ratée, critique, KO, entrée en jeu, talent, objet, fin de combat. L'écran affiche le journal et les animations dans cet ordre.

## 7. Talents

Chaque fakemon a **un talent**, fixé dans le roster. Quatre talents, un par moment du combat :

| Talent | Moment | Effet |
|---|---|---|
| **Fracas** | entrée en jeu | inflige à l'adversaire en face 10 % de ses PV max. Ne peut pas mettre KO : laisse au moins 1 PV. |
| **Carapace** | dégâts reçus | les attaques super efficaces reçues font 25 % de dégâts en moins. |
| **Dernier souffle** | dégâts infligés | sous un tiers de ses PV max, ses attaques font 50 % de dégâts en plus. |
| **Récupération** | fin de tour | récupère 10 % de ses PV max, sans dépasser le maximum. |

Les fakemon créés avec le dashboard n'ont pas de talent.

## 8. Objets tenus

Au tirage des équipes, chaque fakemon reçoit **un objet au hasard** parmi trois. Chaque objet augmente une ou deux stats pendant tout le combat ; dans le code, c'est un Decorator autour des stats, comme `Shield` et `FireSword` au jour 08 (#24).

| Objet | Effet |
|---|---|
| **Gantelet** | Attaque et Attaque spéciale +20 % |
| **Écu** | Défense et Défense spéciale +20 % |
| **Plume** | Vitesse +20 % |

## 9. Roster des 30 fakemon

Le jeu affiche le **nom français**. Le nom d'origine sert d'identifiant et pour les crédits : les créatures viennent de la collection libre [Mongratis](https://pokengine.org/collections/107s7x9x/Mongratis), utilisée avec crédit (voir `CREDITS.md`). Les types, talents et attaques ci-dessous sont propres à Fakemon Arena.

| # | Nom FR | Nom d'origine | Types | Talent | Couverture | Concept |
|---|---|---|---|---|---|---|
| 1 | Tarentemps | Trantima | Venin / Roche | Carapace | | Tarentule-cadran solaire, gardienne du temps |
| 2 | Stoillone | Webruiser | Venin / Force | Dernier souffle | | Araignée boxeuse, inspirée de Rocky |
| 3 | Nubis | Anu | Ombre | Récupération | Éther | Renard des rêves, qui rôde la nuit |
| 4 | Zarlou | Lizquent | Ombre / Force | Fracas | | Lézard voyou au gros poing |
| 5 | Némésuse | Nemesusa | Ombre / Eau | Dernier souffle | | Sorcière des mers, entre Némésis et la méduse |
| 6 | Pyrale | Firomenis | Feu / Venin | Dernier souffle | | Papillon de nuit qu'on croyait né des flammes |
| 7 | Dynacell | Dynabit | Foudre | Récupération | Force | Lapin-dynamo rose, toujours chargé |
| 8 | Chauve-Suie | Chimbat | Feu / Vent | Fracas | | Chauve-souris de cheminée, couverte de suie |
| 9 | Varaniteur | Varanitor | Nature / Roche | Récupération | | Varan couvert de mousse, qui surveille son territoire |
| 10 | Mormite | Forbiddron | Venin / Ombre | Récupération | | Bonbon gluant sorti d'une marmite maudite |
| 11 | Poufêtonne | Tuffettry | Éther | Carapace | Vent | Pouf capitonné extravagant, toujours en fête |
| 12 | Décendre | Deember | Feu / Foudre | Dernier souffle | | Braise de décembre, incandescente, qui crache des cendres |
| 13 | Furonclodile | Kankersaur | Roche / Venin | Carapace | | Crocodile couvert d'excroissances toxiques |
| 14 | Noisettousse | Nuenflu | Eau / Vent | Carapace | | Écureuil des neiges qui propage le rhume en hiver |
| 15 | Espadassin | Marelstorm | Eau / Force | Fracas | | Espadon bretteur, l'épée en avant |
| 16 | Primelady | Pomprim | Nature / Éther | Récupération | | Demoiselle en robe de pétales de primevère |
| 17 | Négatôme | Negagen | Ombre / Foudre | Fracas | | Spectre des rayons X, né d'une radiographie |
| 18 | Silennium | Sillennium | Roche / Éther | Carapace | | Rocher-champignon sculpté par le vent pendant mille ans |
| 19 | Avipex | Avipex | Eau / Vent | Fracas | | Oiseau marin au sommet de sa lignée |
| 20 | Porcteresse | Hogriks | Roche | Carapace | Force | Cochon-forteresse en briques, indestructible |
| 21 | Koanobi | Koaninja | Nature / Force | Dernier souffle | | Koala ninja, ceinture nouée |
| 22 | Héphaïstodon | Crator | Feu / Roche | Fracas | | Dragon-lézard de lave, forgeron des volcans |
| 23 | Nymphaline | Nymfusha | Éther / Vent | Récupération | | Nymphe espiègle aux cornes de diablesse |
| 24 | Trépassereau | Enderbird | Ombre / Vent | Dernier souffle | | Passereau gardien de l'entrée des Enfers |
| 25 | Délupard | Delugar | Eau | Carapace | Ombre | Félin du déluge, rapide sous la pluie |
| 26 | Lumax | Lumaxi | Foudre / Éther | Récupération | | Créature de lumière poussée au maximum |
| 27 | Efflutal | Efflutal | Éther / Feu | Dernier souffle | | Mentaliste aux effluves de flamme |
| 28 | Baobarafe | Baobaraffe | Nature | Récupération | Roche | Girafe-baobab, dont le tronc stocke l'eau |
| 29 | Séismodonte | Tekagon | Roche / Force | Fracas | | Colosse aveugle qui voit par les vibrations du sol |
| 30 | Criminaile | Criminalis | Vent | Fracas | Ombre | Oiseau voleur, qui chaparde tout ce qui brille |

**Répartition :** chaque type apparaît chez 4 à 7 fakemon ; chaque talent chez 7 ou 8 fakemon. Les statistiques et les attaques précises de chaque fakemon sont saisies avec le dashboard (#23), en suivant la règle des 600 points.

## 10. Questions ouvertes pour la review

- La **Roche** est le type le plus courant (7 fakemon) et le plus fort en attaque : à surveiller en jouant.
- La répartition **physique / spéciale** des attaques (section 4) : chaque type doit-il en avoir des deux ?
- Faut-il garder la **priorité +1** sur toutes les attaques rapides, ou la réserver à certains fakemon ?
- L'ordinateur remplace un fakemon KO par **le premier disponible** : suffisant pour l'IA aléatoire (#11), à revoir avec l'IA heuristique (#33).
