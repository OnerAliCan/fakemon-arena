package fr.epitech.fakemon.core.model;

/**
 * Une attaque : nom, type, catégorie, puissance, précision en pourcentage et priorité.
 * Elle ne change jamais pendant un combat.
 */
public class Move {

    private final String name;
    private final Type type;
    private final MoveCategory category;
    private final int power;
    private final int accuracy;
    private final int priority;

    public Move(
        String name,
        Type type,
        MoveCategory category,
        int power,
        int accuracy,
        int priority
    ) {
        this.name = name;
        this.type = type;
        this.category = category;
        this.power = power;
        this.accuracy = accuracy;
        this.priority = priority;
    }

    public String getName() {
        return this.name;
    }

    public Type getType() {
        return this.type;
    }

    public MoveCategory getCategory() {
        return this.category;
    }

    public int getPower() {
        return this.power;
    }

    public int getAccuracy() {
        return this.accuracy;
    }

    public int getPriority() {
        return this.priority;
    }
}
