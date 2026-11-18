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

    /**
     * Donne le nom d'origine utilisé dans le catalogue.
     * @return le nom d'origine utilisé dans le catalogue
     */
    public String getOriginalName() {
        return this.originalName;
    }

    /**
     * Donne le nom français affiché au joueur.
     * @return le nom français affiché au joueur
     */
    public String getFrenchName() {
        return this.frenchName;
    }

    /**
     * Donne les types du Fakemon.
     * @return les types du Fakemon
     */
    public List<Type> getTypes() {
        return this.types;
    }

    /**
     * Donne les statistiques de base.
     * @return les statistiques de base
     */
    public Stats getStats() {
        return this.stats;
    }

    /**
     * Donne les attaques que le Fakemon peut apprendre.
     * @return les attaques que le Fakemon peut apprendre
     */
    public List<Move> getPossibleMoves() {
        return this.possibleMoves;
    }

    /**
     * Donne le numéro du Fakemon dans le Mondex.
     * @return le numéro du Fakemon dans le Mondex
     */
    public int getDexNumber() {
        return this.dexNumber;
    }

    /**
     * Donne le talent du Fakemon.
     * @return le talent du Fakemon
     */
    public Talent getTalent() {
        return this.talent;
    }

    /**
     * Donne le type d'attaque de couverture.
     * @return le type de couverture, ou {@code null} s'il n'y en a pas
     */
    public Type getCoverageType() {
        return this.coverageType;
    }

    /**
     * Donne le concept du Fakemon.
     * @return le concept, ou {@code null} s'il n'est pas renseigné
     */
    public String getConcept() {
        return this.concept;
    }

    /**
     * Donne le designer du Fakemon.
     * @return le designer, ou {@code null} s'il n'est pas renseigné
     */
    public String getDesigner() {
        return this.designer;
    }

    /**
     * Donne le chemin du sprite de face.
     * @return le chemin du sprite de face
     */
    public String getFrontSprite() {
        return this.frontSprite;
    }

    /**
     * Donne le chemin du sprite de dos.
     * @return le chemin du sprite de dos
     */
    public String getBackSprite() {
        return this.backSprite;
    }

    /**
     * Donne le chemin du cri.
     * @return le chemin du fichier audio, ou {@code null} s'il n'y en a pas encore
     */
    public String getCry() {
        return this.cry;
    }

    /**
     * Remplit une fiche information par information, puis la crée avec
     * {@link #build()}.
     * Chaque méthode renvoie le Builder lui-même, pour enchaîner les appels.
     */
    public static class Builder {
        /** Prépare une fiche dont les informations seront vérifiées à la construction. */
        public Builder() {}


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

        /**
         * Renseigne le nom d'origine.
         *
         * @param originalName le nom d'origine
         * @return ce constructeur
         */
        public Builder originalName(String originalName) {
            this.originalName = originalName;
            return this;
        }

        /**
         * Renseigne le nom français.
         *
         * @param frenchName le nom français
         * @return ce constructeur
         */
        public Builder frenchName(String frenchName) {
            this.frenchName = frenchName;
            return this;
        }

        /**
         * Renseigne les types du Fakemon.
         *
         * @param types les types du Fakemon
         * @return ce constructeur
         */
        public Builder types(List<Type> types) {
            this.types = types;
            return this;
        }

        /**
         * Renseigne les statistiques de base.
         *
         * @param stats les statistiques de base
         * @return ce constructeur
         */
        public Builder stats(Stats stats) {
            this.stats = stats;
            return this;
        }

        /**
         * Renseigne les attaques possibles.
         *
         * @param possibleMoves les attaques possibles
         * @return ce constructeur
         */
        public Builder possibleMoves(List<Move> possibleMoves) {
            this.possibleMoves = possibleMoves;
            return this;
        }

        /**
         * Renseigne le numéro dans le Mondex.
         *
         * @param dexNumber le numéro dans le Mondex
         * @return ce constructeur
         */
        public Builder dexNumber(int dexNumber) {
            this.dexNumber = dexNumber;
            return this;
        }

        /**
         * Renseigne le talent du Fakemon.
         *
         * @param talent le talent du Fakemon
         * @return ce constructeur
         */
        public Builder talent(Talent talent) {
            this.talent = talent;
            return this;
        }

        /**
         * Renseigne le type de couverture facultatif.
         *
         * @param coverageType le type de couverture facultatif
         * @return ce constructeur
         */
        public Builder coverageType(Type coverageType) {
            this.coverageType = coverageType;
            return this;
        }

        /**
         * Renseigne le concept facultatif.
         *
         * @param concept le concept facultatif
         * @return ce constructeur
         */
        public Builder concept(String concept) {
            this.concept = concept;
            return this;
        }

        /**
         * Renseigne le designer, s'il est renseigné.
         *
         * @param designer le designer, s'il est renseigné
         * @return ce constructeur
         */
        public Builder designer(String designer) {
            this.designer = designer;
            return this;
        }

        /**
         * Renseigne le chemin du sprite de face.
         *
         * @param frontSprite le chemin du sprite de face
         * @return ce constructeur
         */
        public Builder frontSprite(String frontSprite) {
            this.frontSprite = frontSprite;
            return this;
        }

        /**
         * Renseigne le chemin du sprite de dos.
         *
         * @param backSprite le chemin du sprite de dos
         * @return ce constructeur
         */
        public Builder backSprite(String backSprite) {
            this.backSprite = backSprite;
            return this;
        }

        /**
         * Renseigne le chemin du cri, s'il est renseigné.
         *
         * @param cry le chemin du cri, s'il est renseigné
         * @return ce constructeur
         */
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
