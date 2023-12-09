package com.imoonday.push_everything_away.utils;

import net.fabricmc.yarn.constants.MiningLevels;
import net.minecraft.item.Items;
import net.minecraft.item.ToolMaterial;
import net.minecraft.item.ToolMaterials;
import net.minecraft.recipe.Ingredient;
import net.minecraft.registry.tag.ItemTags;
import net.minecraft.util.StringIdentifiable;
import org.jetbrains.annotations.Contract;

public enum HammerMaterial implements ToolMaterial, Pushable, StringIdentifiable {

    WOOD("wooden", ToolMaterials.WOOD, 2, 1, 1, Ingredient.fromTag(ItemTags.LOGS)),
    STONE("stone", ToolMaterials.STONE, 2.5, 2, 1, Ingredient.ofItems(Items.STONE)),
    COPPER("copper", createToolMaterial(190, 4.0f, 1.5f, MiningLevels.STONE, 8, Ingredient.ofItems(Items.COPPER_INGOT)), 3, 3, 2, 1.5f, 1.5f, Ingredient.ofItems(Items.COPPER_BLOCK)),
    IRON("iron", ToolMaterials.IRON, 4, 3, 2, Ingredient.ofItems(Items.IRON_BLOCK)),
    GOLD("gold", ToolMaterials.GOLD, 3.5, 3, 2, 0.5f, 0.5f, Ingredient.ofItems(Items.GOLD_BLOCK)),
    DIAMOND("diamond", ToolMaterials.DIAMOND, 5, 4, 3, Ingredient.ofItems(Items.DIAMOND_BLOCK)),
    EMERALD("emerald", createToolMaterial(1796, 8.5f, 3.5f, MiningLevels.DIAMOND, 13, Ingredient.ofItems(Items.EMERALD)), 5, 4, 3, 0.45f, 0.5f, Ingredient.ofItems(Items.EMERALD_BLOCK)),
    NETHERITE("netherite", ToolMaterials.NETHERITE, 6, 5, 4, Ingredient.ofItems(Items.NETHERITE_INGOT)),
    BEDROCK("bedrock", createToolMaterial(0, 15.0f, 90.0f, MiningLevels.NETHERITE, Integer.MAX_VALUE, Ingredient.EMPTY), 512, 10, 5, 0.05f, 0.1f, Ingredient.EMPTY);

    private final String name;
    private final ToolMaterial material;
    private final double baseDistance;
    private final int maxPushStrength;
    private final int maxRange;
    private final float pushStrengthIntervalMultiplier;
    private final float pushRangeIntervalMultiplier;
    private final Ingredient repairIngredient;

    HammerMaterial(String name, ToolMaterial material, double baseDistance, int maxPushStrength, int maxRange, Ingredient repairIngredient) {
        this(name, material, baseDistance, maxPushStrength, maxRange, 1.0f, 1.0f, repairIngredient);
    }

    HammerMaterial(String name, ToolMaterial material, double baseDistance, int maxPushStrength, int maxRange, float pushStrengthIntervalMultiplier, float pushRangeIntervalMultiplier, Ingredient repairIngredient) {
        this.name = name;
        this.material = material;
        this.baseDistance = baseDistance;
        this.maxPushStrength = maxPushStrength;
        this.maxRange = maxRange;
        this.pushStrengthIntervalMultiplier = pushStrengthIntervalMultiplier;
        this.pushRangeIntervalMultiplier = pushRangeIntervalMultiplier;
        this.repairIngredient = repairIngredient;
    }

    @Override
    public int getDurability() {
        return material.getDurability();
    }

    @Override
    public float getMiningSpeedMultiplier() {
        return material.getMiningSpeedMultiplier();
    }

    @Override
    public float getAttackDamage() {
        return material.getAttackDamage();
    }

    @Override
    public int getMiningLevel() {
        return material.getMiningLevel();
    }

    @Override
    public int getEnchantability() {
        return material.getEnchantability();
    }

    @Override
    public Ingredient getRepairIngredient() {
        return repairIngredient;
    }

    @Override
    public double getBaseDistance() {
        return baseDistance;
    }

    @Override
    public int getMaxPushStrength() {
        return maxPushStrength;
    }

    @Override
    public int getMaxRange() {
        return maxRange;
    }

    public ToolMaterial getMaterial() {
        return material;
    }

    @Override
    public double getPushStrengthInterval() {
        return Pushable.super.getPushStrengthInterval() * pushStrengthIntervalMultiplier;
    }

    @Override
    public int getPushRangeInterval() {
        return (int) (Pushable.super.getPushRangeInterval() * pushRangeIntervalMultiplier);
    }

    public boolean isSpecial() {
        return this == BEDROCK;
    }

    @Override
    public String toString() {
        return name;
    }

    @Override
    public String asString() {
        return name;
    }

    @Contract(value = "_, _, _, _, _, _ -> new", pure = true)
    public static ToolMaterial createToolMaterial(final int durability, final float miningSpeedMultiplier, final float attackDamage, final int miningLevel, final int enchantability, final Ingredient repairIngredient) {
        return new ToolMaterial() {
            @Override
            public int getDurability() {
                return durability;
            }

            @Override
            public float getMiningSpeedMultiplier() {
                return miningSpeedMultiplier;
            }

            @Override
            public float getAttackDamage() {
                return attackDamage;
            }

            @Override
            public int getMiningLevel() {
                return miningLevel;
            }

            @Override
            public int getEnchantability() {
                return enchantability;
            }

            @Override
            public Ingredient getRepairIngredient() {
                return repairIngredient;
            }
        };
    }
}
