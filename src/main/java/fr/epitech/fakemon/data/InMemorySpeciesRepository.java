package fr.epitech.fakemon.data;

import fr.epitech.fakemon.core.api.InvalidSpeciesException;
import fr.epitech.fakemon.core.api.SpeciesRepository;
import fr.epitech.fakemon.core.model.BaseStats;
import fr.epitech.fakemon.core.model.Move;
import fr.epitech.fakemon.core.model.MoveCategory;
import fr.epitech.fakemon.core.model.Species;
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

    /** Initialise le catalogue à partir d'espèces déjà chargées. */
    public InMemorySpeciesRepository(List<Species> species) {
        this.speciesList = new ArrayList<>(species);
    }

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
        this.speciesList.add(new Species(
            "Trantima",
            "Tempsrentule",
            List.of(Type.VENOM, Type.ROCK),
            new BaseStats(85, 150, 110, 55, 105, 95),
            List.of(aiguillon, crachatAcide, nueeVenimeuse, gravillon, coupDeMoellon, failleTellurique)
        ));
        this.speciesList.add(new Species(
            "Webruiser",
            "Stoillone",
            List.of(Type.VENOM, Type.FORCE),
            new BaseStats(110, 150, 115, 55, 90, 80),
            List.of(aiguillon, crachatAcide, nueeVenimeuse, coupSec, crochet, coupDeMassue)
        ));
        this.speciesList.add(new Species(
            "Anu",
            "Nubis",
            List.of(Type.SHADOW),
            new BaseStats(100, 50, 80, 145, 115, 110),
            List.of(murmure, voileNoir, eclipse, lueur, ondeMentale, comete)
        ));
        this.speciesList.add(new Species(
            "Lizquent",
            "Zarlou",
            List.of(Type.SHADOW, Type.FORCE),
            new BaseStats(85, 120, 85, 95, 85, 130),
            List.of(murmure, voileNoir, eclipse, coupSec, crochet, coupDeMassue)
        ));
        this.speciesList.add(new Species(
            "Nemesusa",
            "Némésuse",
            List.of(Type.SHADOW, Type.WATER),
            new BaseStats(90, 60, 85, 150, 125, 90),
            List.of(murmure, voileNoir, eclipse, embrun, ressac, lameDeFond)
        ));
        this.speciesList.add(new Species(
            "Firomenis",
            "Pyrale",
            List.of(Type.FIRE, Type.VENOM),
            new BaseStats(75, 125, 65, 85, 100, 150),
            List.of(tison, couleeArdente, fournaise, aiguillon, crachatAcide, nueeVenimeuse)
        ));
        this.speciesList.add(new Species(
            "Dynabit",
            "Dynacell",
            List.of(Type.LIGHTNING),
            new BaseStats(135, 95, 120, 70, 115, 65),
            List.of(gresillement, arcElectrique, orageSec, coupSec, crochet, coupDeMassue)
        ));
        this.speciesList.add(new Species(
            "Chimbat",
            "Chauve-Suie",
            List.of(Type.FIRE, Type.WIND),
            new BaseStats(75, 75, 80, 150, 110, 110),
            List.of(tison, couleeArdente, fournaise, courantDAir, bourrasque, cyclone)
        ));
        this.speciesList.add(new Species(
            "Varanitor",
            "Varaniteur",
            List.of(Type.NATURE, Type.ROCK),
            new BaseStats(95, 150, 110, 65, 80, 100),
            List.of(epine, fouetDeRonces, grandeFloraison, gravillon, coupDeMoellon, failleTellurique)
        ));
        this.speciesList.add(new Species(
            "Forbiddron",
            "Mormite",
            List.of(Type.VENOM, Type.SHADOW),
            new BaseStats(100, 55, 85, 140, 120, 100),
            List.of(aiguillon, crachatAcide, nueeVenimeuse, murmure, voileNoir, eclipse)
        ));
        this.speciesList.add(new Species(
            "Tuffettry",
            "Poufêtonne",
            List.of(Type.ETHER),
            new BaseStats(75, 60, 95, 150, 115, 105),
            List.of(lueur, ondeMentale, comete, courantDAir, bourrasque, cyclone)
        ));
        this.speciesList.add(new Species(
            "Deember",
            "Décendre",
            List.of(Type.FIRE, Type.LIGHTNING),
            new BaseStats(85, 60, 80, 150, 130, 95),
            List.of(tison, couleeArdente, fournaise, gresillement, arcElectrique, orageSec)
        ));
    }
}
