package fr.epitech.fakemon.core.model;

import java.util.List;

/**
 * La fiche d'un Fakemon dans le catalogue, qui ne change pas d'un combat à
 * l'autre.
 * Elle se crée avec un {@link Builder}, qui vérifie que les informations
 * obligatoires sont présentes.
 */
public class Species {

    private final String originalName;
    private final String frenchName;
    private final List<Type> types;
    private final Stats stats;
    private final List<Move> possibleMoves;
    private final int dexNumber;
    private final Talent talent;
    private final Type coverageType;
    private final String concept;
    private final String designer;
    private final String frontSprite;
    private final String backSprite;
    private final String cry;

    private Species(Builder builder) {
        this.originalName = builder.originalName;
        this.frenchName = builder.frenchName;
        this.types = builder.types;
        this.stats = builder.stats;
        this.possibleMoves = builder.possibleMoves;
        this.dexNumber = builder.dexNumber;
        this.talent = builder.talent;
        this.coverageType = builder.coverageType;
        this.concept = builder.concept;
        this.designer = builder.designer;
        this.frontSprite = builder.frontSprite;
        this.backSprite = builder.backSprite;
        this.cry = builder.cry;
    }

    public String getOriginalName() {
        return this.originalName;
    }

    public String getFrenchName() {
        return this.frenchName;
    }

    public List<Type> getTypes() {
        return this.types;
    }

    public Stats getStats() {
        return this.stats;
    }

    public List<Move> getPossibleMoves() {
        return this.possibleMoves;
    }

    public int getDexNumber() {
        return this.dexNumber;
    }

    public Talent getTalent() {
        return this.talent;
    }

    /** Le type d'attaque de couverture, ou {@code null} s'il n'y en a pas. */
    public Type getCoverageType() {
        return this.coverageType;
    }

    /** Le concept du Fakemon, ou {@code null} s'il n'est pas renseigné. */
    public String getConcept() {
        return this.concept;
    }

    /** Le designer du Fakemon, ou {@code null} s'il n'est pas renseigné. */
    public String getDesigner() {
        return this.designer;
    }

    public String getFrontSprite() {
        return this.frontSprite;
    }

    public String getBackSprite() {
        return this.backSprite;
    }

    /** Le chemin du cri, ou {@code null} s'il n'y en a pas encore. */
    public String getCry() {
        return this.cry;
    }

    /**
     * Remplit une fiche information par information, puis la crée avec
     * {@link #build()}.
     * Chaque méthode renvoie le Builder lui-même, pour enchaîner les appels.
     */
    public static class Builder {

        private String originalName;
        private String frenchName;
        private List<Type> types;
        private Stats stats;
        private List<Move> possibleMoves;
        private int dexNumber;
        private Talent talent;
        private Type coverageType;
        private String concept;
        private String designer;
        private String frontSprite;
        private String backSprite;
        private String cry;

        public Builder originalName(String originalName) {
            this.originalName = originalName;
            return this;
        }

        public Builder frenchName(String frenchName) {
            this.frenchName = frenchName;
            return this;
        }

        public Builder types(List<Type> types) {
            this.types = types;
            return this;
        }

        public Builder stats(Stats stats) {
            this.stats = stats;
            return this;
        }

        public Builder possibleMoves(List<Move> possibleMoves) {
            this.possibleMoves = possibleMoves;
            return this;
        }

        public Builder dexNumber(int dexNumber) {
            this.dexNumber = dexNumber;
            return this;
        }

        public Builder talent(Talent talent) {
            this.talent = talent;
            return this;
        }

        public Builder coverageType(Type coverageType) {
            this.coverageType = coverageType;
            return this;
        }

        public Builder concept(String concept) {
            this.concept = concept;
            return this;
        }

        public Builder designer(String designer) {
            this.designer = designer;
            return this;
        }

        public Builder frontSprite(String frontSprite) {
            this.frontSprite = frontSprite;
            return this;
        }

        public Builder backSprite(String backSprite) {
            this.backSprite = backSprite;
            return this;
        }

        public Builder cry(String cry) {
            this.cry = cry;
            return this;
        }

        /**
         * Crée la fiche.
         *
         * @return la fiche remplie
         * @throws IllegalStateException s'il manque une information obligatoire :
         *                               noms, types, statistiques, attaques possibles,
         *                               talent ou sprites
         */
        public Species build() {
            if (originalName == null
                    || frenchName == null
                    || types == null
                    || stats == null
                    || possibleMoves == null
                    || talent == null
                    || frontSprite == null
                    || backSprite == null) {
                throw new IllegalStateException(
                        "Il manque une information obligatoire pour créer l'espèce " + originalName + ".");
            }
            return new Species(this);
        }
    }
}
