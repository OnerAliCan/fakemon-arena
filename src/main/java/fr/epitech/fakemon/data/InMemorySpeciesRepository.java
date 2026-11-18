package fr.epitech.fakemon.data;

import fr.epitech.fakemon.core.api.InvalidSpeciesException;
import fr.epitech.fakemon.core.api.SpeciesRepository;
import fr.epitech.fakemon.core.model.BaseStats;
import fr.epitech.fakemon.core.model.Move;
import fr.epitech.fakemon.core.model.MoveCategory;
import fr.epitech.fakemon.core.model.Species;
import fr.epitech.fakemon.core.model.Talent;
import fr.epitech.fakemon.core.model.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Catalogue provisoire, gardé en mémoire, avec les 12 premières espèces du roster et leurs vraies attaques.
 * 12 espèces suffisent pour tirer deux équipes de 6 sans doublon.
 * Il sert en attendant le catalogue JSON (#22).
 */
public class InMemorySpeciesRepository implements SpeciesRepository {

    private final List<Species> speciesList;

    @Override
    public List<Species> findAll() {
        return new ArrayList<>(this.speciesList);
    }

    @Override
    public Optional<Species> findByOriginalName(String originalName) {
        for (Species species : speciesList) {
            if (species.getOriginalName().equals(originalName)) {
                return Optional.of(species);
            }
        }
        return Optional.empty();
    }

    @Override
    public void add(Species species) {
        if (findByOriginalName(species.getOriginalName()).isPresent()) {
            throw new InvalidSpeciesException(
                "Ce Fakemon existe déjà : " + species.getOriginalName()
            );
        }
        speciesList.add(species);
    }

    /**
     * Initialise le catalogue à partir d'espèces déjà chargées.
     * @param species les espèces à copier dans le catalogue en mémoire
     */
    public InMemorySpeciesRepository(List<Species> species) {
        this.speciesList = new ArrayList<>(species);
    }

    /** Crée le catalogue de démonstration avec les douze premières espèces et leurs attaques. */
    public InMemorySpeciesRepository() {
        Move aiguillon = new Move("Aiguillon", Type.VENOM, MoveCategory.PHYSICAL, 40, 100, 1);
        Move crachatAcide = new Move("Crachat acide", Type.VENOM, MoveCategory.SPECIAL, 65, 100, 0);
        Move nueeVenimeuse = new Move("Nuée venimeuse", Type.VENOM, MoveCategory.PHYSICAL, 95, 80, 0);
        Move gravillon = new Move("Gravillon", Type.ROCK, MoveCategory.PHYSICAL, 40, 100, 1);
        Move coupDeMoellon = new Move("Coup de moellon", Type.ROCK, MoveCategory.PHYSICAL, 65, 100, 0);
        Move failleTellurique = new Move("Faille tellurique", Type.ROCK, MoveCategory.PHYSICAL, 95, 80, 0);
        Move coupSec = new Move("Coup sec", Type.FORCE, MoveCategory.PHYSICAL, 40, 100, 1);
        Move crochet = new Move("Crochet", Type.FORCE, MoveCategory.PHYSICAL, 65, 100, 0);
        Move coupDeMassue = new Move("Coup de massue", Type.FORCE, MoveCategory.PHYSICAL, 95, 80, 0);
        Move murmure = new Move("Murmure", Type.SHADOW, MoveCategory.SPECIAL, 40, 100, 1);
        Move voileNoir = new Move("Voile noir", Type.SHADOW, MoveCategory.SPECIAL, 65, 100, 0);
        Move eclipse = new Move("Éclipse", Type.SHADOW, MoveCategory.SPECIAL, 95, 80, 0);
        Move lueur = new Move("Lueur", Type.ETHER, MoveCategory.SPECIAL, 40, 100, 1);
        Move ondeMentale = new Move("Onde mentale", Type.ETHER, MoveCategory.SPECIAL, 65, 100, 0);
        Move comete = new Move("Comète", Type.ETHER, MoveCategory.PHYSICAL, 95, 80, 0);
        Move embrun = new Move("Embrun", Type.WATER, MoveCategory.SPECIAL, 40, 100, 1);
        Move ressac = new Move("Ressac", Type.WATER, MoveCategory.PHYSICAL, 65, 100, 0);
        Move lameDeFond = new Move("Lame de fond", Type.WATER, MoveCategory.SPECIAL, 95, 80, 0);
        Move tison = new Move("Tison", Type.FIRE, MoveCategory.PHYSICAL, 40, 100, 1);
        Move couleeArdente = new Move("Coulée ardente", Type.FIRE, MoveCategory.SPECIAL, 65, 100, 0);
        Move fournaise = new Move("Fournaise", Type.FIRE, MoveCategory.SPECIAL, 95, 80, 0);
        Move gresillement = new Move("Grésillement", Type.LIGHTNING, MoveCategory.SPECIAL, 40, 100, 1);
        Move arcElectrique = new Move("Arc électrique", Type.LIGHTNING, MoveCategory.SPECIAL, 65, 100, 0);
        Move orageSec = new Move("Orage sec", Type.LIGHTNING, MoveCategory.SPECIAL, 95, 80, 0);
        Move courantDAir = new Move("Courant d'air", Type.WIND, MoveCategory.SPECIAL, 40, 100, 1);
        Move bourrasque = new Move("Bourrasque", Type.WIND, MoveCategory.SPECIAL, 65, 100, 0);
        Move cyclone = new Move("Cyclone", Type.WIND, MoveCategory.PHYSICAL, 95, 80, 0);
        Move epine = new Move("Épine", Type.NATURE, MoveCategory.PHYSICAL, 40, 100, 1);
        Move fouetDeRonces = new Move("Fouet de ronces", Type.NATURE, MoveCategory.PHYSICAL, 65, 100, 0);
        Move grandeFloraison = new Move("Grande floraison", Type.NATURE, MoveCategory.SPECIAL, 95, 80, 0);

        this.speciesList = new ArrayList<>();
        this.speciesList.add(new Species.Builder()
            .originalName("Trantima")
            .frenchName("Tempsrentule")
            .types(List.of(Type.VENOM, Type.ROCK))
            .stats(new BaseStats(85, 150, 110, 55, 105, 95))
            .possibleMoves(List.of(aiguillon, crachatAcide, nueeVenimeuse, gravillon, coupDeMoellon, failleTellurique))
            .dexNumber(1)
            .talent(Talent.CARAPACE)
            .concept("Tarentule-cadran solaire, gardienne du temps")
            .frontSprite("/sprites/front/trantima.png")
            .backSprite("/sprites/back/trantima.png")
            .build());
        this.speciesList.add(new Species.Builder()
            .originalName("Webruiser")
            .frenchName("Stoillone")
            .types(List.of(Type.VENOM, Type.FORCE))
            .stats(new BaseStats(110, 150, 115, 55, 90, 80))
            .possibleMoves(List.of(aiguillon, crachatAcide, nueeVenimeuse, coupSec, crochet, coupDeMassue))
            .dexNumber(2)
            .talent(Talent.DERNIER_SOUFFLE)
            .concept("Araignée boxeuse, inspirée de Rocky")
            .frontSprite("/sprites/front/webruiser.png")
            .backSprite("/sprites/back/webruiser.png")
            .build());
        this.speciesList.add(new Species.Builder()
            .originalName("Anu")
            .frenchName("Nubis")
            .types(List.of(Type.SHADOW))
            .stats(new BaseStats(100, 50, 80, 145, 115, 110))
            .possibleMoves(List.of(murmure, voileNoir, eclipse, lueur, ondeMentale, comete))
            .dexNumber(3)
            .talent(Talent.RECUPERATION)
            .coverageType(Type.ETHER)
            .concept("Renard des rêves, qui rôde la nuit")
            .frontSprite("/sprites/front/anu.png")
            .backSprite("/sprites/back/anu.png")
            .build());
        this.speciesList.add(new Species.Builder()
            .originalName("Lizquent")
            .frenchName("Zarlou")
            .types(List.of(Type.SHADOW, Type.FORCE))
            .stats(new BaseStats(85, 120, 85, 95, 85, 130))
            .possibleMoves(List.of(murmure, voileNoir, eclipse, coupSec, crochet, coupDeMassue))
            .dexNumber(4)
            .talent(Talent.FRACAS)
            .concept("Lézard voyou au gros poing")
            .frontSprite("/sprites/front/lizquent.png")
            .backSprite("/sprites/back/lizquent.png")
            .build());
        this.speciesList.add(new Species.Builder()
            .originalName("Nemesusa")
            .frenchName("Némésuse")
            .types(List.of(Type.SHADOW, Type.WATER))
            .stats(new BaseStats(90, 60, 85, 150, 125, 90))
            .possibleMoves(List.of(murmure, voileNoir, eclipse, embrun, ressac, lameDeFond))
            .dexNumber(5)
            .talent(Talent.DERNIER_SOUFFLE)
            .concept("Sorcière des mers, entre Némésis et la méduse")
            .frontSprite("/sprites/front/nemesusa.png")
            .backSprite("/sprites/back/nemesusa.png")
            .build());
        this.speciesList.add(new Species.Builder()
            .originalName("Firomenis")
            .frenchName("Pyrale")
            .types(List.of(Type.FIRE, Type.VENOM))
            .stats(new BaseStats(75, 125, 65, 85, 100, 150))
            .possibleMoves(List.of(tison, couleeArdente, fournaise, aiguillon, crachatAcide, nueeVenimeuse))
            .dexNumber(6)
            .talent(Talent.DERNIER_SOUFFLE)
            .concept("Papillon de nuit qu'on croyait né des flammes")
            .frontSprite("/sprites/front/firomenis.png")
            .backSprite("/sprites/back/firomenis.png")
            .build());
        this.speciesList.add(new Species.Builder()
            .originalName("Dynabit")
            .frenchName("Dynacell")
            .types(List.of(Type.LIGHTNING))
            .stats(new BaseStats(135, 95, 120, 70, 115, 65))
            .possibleMoves(List.of(gresillement, arcElectrique, orageSec, coupSec, crochet, coupDeMassue))
            .dexNumber(7)
            .talent(Talent.RECUPERATION)
            .coverageType(Type.FORCE)
            .concept("Lapin-dynamo rose, toujours chargé")
            .frontSprite("/sprites/front/dynabit.png")
            .backSprite("/sprites/back/dynabit.png")
            .build());
        this.speciesList.add(new Species.Builder()
            .originalName("Chimbat")
            .frenchName("Chauve-Suie")
            .types(List.of(Type.FIRE, Type.WIND))
            .stats(new BaseStats(75, 75, 80, 150, 110, 110))
            .possibleMoves(List.of(tison, couleeArdente, fournaise, courantDAir, bourrasque, cyclone))
            .dexNumber(8)
            .talent(Talent.FRACAS)
            .concept("Chauve-souris de cheminée, couverte de suie")
            .frontSprite("/sprites/front/chimbat.png")
            .backSprite("/sprites/back/chimbat.png")
            .build());
        this.speciesList.add(new Species.Builder()
            .originalName("Varanitor")
            .frenchName("Varaniteur")
            .types(List.of(Type.NATURE, Type.ROCK))
            .stats(new BaseStats(95, 150, 110, 65, 80, 100))
            .possibleMoves(List.of(epine, fouetDeRonces, grandeFloraison, gravillon, coupDeMoellon, failleTellurique))
            .dexNumber(9)
            .talent(Talent.RECUPERATION)
            .concept("Varan couvert de mousse, qui surveille son territoire")
            .frontSprite("/sprites/front/varanitor.png")
            .backSprite("/sprites/back/varanitor.png")
            .build());
        this.speciesList.add(new Species.Builder()
            .originalName("Forbiddron")
            .frenchName("Mormite")
            .types(List.of(Type.VENOM, Type.SHADOW))
            .stats(new BaseStats(100, 55, 85, 140, 120, 100))
            .possibleMoves(List.of(aiguillon, crachatAcide, nueeVenimeuse, murmure, voileNoir, eclipse))
            .dexNumber(10)
            .talent(Talent.RECUPERATION)
            .concept("Bonbon gluant sorti d'une marmite maudite")
            .frontSprite("/sprites/front/forbiddron.png")
            .backSprite("/sprites/back/forbiddron.png")
            .build());
        this.speciesList.add(new Species.Builder()
            .originalName("Tuffettry")
            .frenchName("Poufêtonne")
            .types(List.of(Type.ETHER))
            .stats(new BaseStats(75, 60, 95, 150, 115, 105))
            .possibleMoves(List.of(lueur, ondeMentale, comete, courantDAir, bourrasque, cyclone))
            .dexNumber(11)
            .talent(Talent.CARAPACE)
            .coverageType(Type.WIND)
            .concept("Pouf capitonné extravagant, toujours en fête")
            .frontSprite("/sprites/front/tuffettry.png")
            .backSprite("/sprites/back/tuffettry.png")
            .build());
        this.speciesList.add(new Species.Builder()
            .originalName("Deember")
            .frenchName("Décendre")
            .types(List.of(Type.FIRE, Type.LIGHTNING))
            .stats(new BaseStats(85, 60, 80, 150, 130, 95))
            .possibleMoves(List.of(tison, couleeArdente, fournaise, gresillement, arcElectrique, orageSec))
            .dexNumber(12)
            .talent(Talent.DERNIER_SOUFFLE)
            .concept("Braise de décembre, incandescente, qui crache des cendres")
            .frontSprite("/sprites/front/deember.png")
            .backSprite("/sprites/back/deember.png")
            .build());
    }
}
