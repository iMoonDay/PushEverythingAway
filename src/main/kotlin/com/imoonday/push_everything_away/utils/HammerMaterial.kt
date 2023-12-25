package com.imoonday.push_everything_away.utils

import com.imoonday.push_everything_away.config.Config
import com.imoonday.push_everything_away.init.ModItems
import com.imoonday.push_everything_away.items.GroundHammerItem
import net.fabricmc.yarn.constants.MiningLevels
import net.minecraft.item.Items
import net.minecraft.item.ToolMaterial
import net.minecraft.item.ToolMaterials
import net.minecraft.recipe.Ingredient
import net.minecraft.registry.tag.BlockTags
import net.minecraft.registry.tag.ItemTags
import net.minecraft.util.StringIdentifiable
import net.minecraft.util.math.BlockPos
import net.minecraft.world.World
import java.util.*
import java.util.stream.Collectors

enum class HammerMaterial : ToolMaterial, Pushable, StringIdentifiable {

    WOOD(
        name = "wooden",
        material = ToolMaterials.WOOD,
        baseDistance = 2.0,
        maxPushStrength = 1,
        maxRange = 1,
        repairIngredient = Ingredient.fromTag(ItemTags.LOGS)
    ),
    STONE(
        name = "stone",
        material = ToolMaterials.STONE,
        baseDistance = 2.5,
        maxPushStrength = 2,
        maxRange = 1,
        repairIngredient = Ingredient.ofItems(Items.STONE)
    ),
    COPPER(
        materialName = "copper",
        material = createToolMaterial(
            durability = 190,
            miningSpeedMultiplier = 4.0f,
            attackDamage = 1.5f,
            miningLevel = MiningLevels.STONE,
            enchantability = 8,
            repairIngredient = Ingredient.ofItems(Items.COPPER_INGOT)
        ),
        baseDistance = 3.0,
        maxPushStrength = 3,
        maxRange = 2,
        pushStrengthIntervalMultiplier = 1.5f,
        pushRangeIntervalMultiplier = 1.5f,
        repairIngredient = Ingredient.ofItems(
            Items.COPPER_BLOCK
        )
    ),
    IRON(
        name = "iron",
        material = ToolMaterials.IRON,
        baseDistance = 4.0,
        maxPushStrength = 3,
        maxRange = 2,
        repairIngredient = Ingredient.ofItems(Items.IRON_BLOCK)
    ),
    GOLD(
        materialName = "gold",
        material = ToolMaterials.GOLD,
        baseDistance = 3.5,
        maxPushStrength = 3,
        maxRange = 2,
        pushStrengthIntervalMultiplier = 0.5f,
        pushRangeIntervalMultiplier = 0.5f,
        repairIngredient = Ingredient.ofItems(Items.GOLD_BLOCK)
    ),
    DIAMOND(
        name = "diamond",
        material = ToolMaterials.DIAMOND,
        baseDistance = 5.0,
        maxPushStrength = 4,
        maxRange = 3,
        repairIngredient = Ingredient.ofItems(Items.DIAMOND_BLOCK)
    ),
    EMERALD(
        materialName = "emerald",
        material = createToolMaterial(
            durability = 1796,
            miningSpeedMultiplier = 8.5f,
            attackDamage = 3.5f,
            miningLevel = MiningLevels.DIAMOND,
            enchantability = 13,
            repairIngredient = Ingredient.ofItems(Items.EMERALD)
        ),
        baseDistance = 5.0,
        maxPushStrength = 4,
        maxRange = 3,
        pushStrengthIntervalMultiplier = 0.45f,
        pushRangeIntervalMultiplier = 0.5f,
        repairIngredient = Ingredient.ofItems(
            Items.EMERALD_BLOCK
        )
    ),
    NETHERITE(
        name = "netherite",
        material = ToolMaterials.NETHERITE,
        baseDistance = 6.0,
        maxPushStrength = 5,
        maxRange = 4,
        repairIngredient = Ingredient.ofItems(Items.NETHERITE_INGOT)
    ),
    BEDROCK(
        materialName = "bedrock",
        material = createToolMaterial(
            durability = 0,
            miningSpeedMultiplier = 15.0f,
            attackDamage = 90.0f,
            miningLevel = MiningLevels.NETHERITE,
            enchantability = Int.MAX_VALUE,
            repairIngredient = Ingredient.EMPTY
        ),
        baseDistance = 512.0,
        maxPushStrength = 10,
        maxRange = 5,
        pushStrengthIntervalMultiplier = 0.05f,
        pushRangeIntervalMultiplier = 0.1f,
        repairIngredient = Ingredient.EMPTY
    );

    private val materialName: String
    val material: ToolMaterial
    override val baseDistance: Double
        get() = Config.instance?.groundHammerAttributesOverride?.get(materialName)?.get("baseDistance")?.toDouble()
            ?: field
    override val maxPushStrength: Int
        get() = Config.instance?.groundHammerAttributesOverride?.get(materialName)?.get("maxPushStrength")?.toInt()
            ?: field
    override val maxRange: Int
        get() = Config.instance?.groundHammerAttributesOverride?.get(materialName)?.get("maxRange")?.toInt()
            ?: field
    val pushStrengthIntervalMultiplier: Float
        get() = Config.instance?.groundHammerAttributesOverride?.get(materialName)
            ?.get("pushStrengthIntervalMultiplier")?.toFloat()
            ?: field
    val pushRangeIntervalMultiplier: Float
        get() = Config.instance?.groundHammerAttributesOverride?.get(materialName)?.get("pushRangeIntervalMultiplier")
            ?.toFloat()
            ?: field
    private val repairIngredient: Ingredient
    override val pushStrengthInterval: Double
        get() = super.pushStrengthInterval * pushStrengthIntervalMultiplier
    override val pushRangeInterval: Int
        get() = (super.pushRangeInterval * pushRangeIntervalMultiplier).toInt()
    val isSpecial: Boolean
        get() = this == BEDROCK
    val item: GroundHammerItem?
        get() = ModItems.HAMMERS[this]

    constructor(
        materialName: String,
        material: ToolMaterial,
        baseDistance: Double,
        maxPushStrength: Int,
        maxRange: Int,
        pushStrengthIntervalMultiplier: Float,
        pushRangeIntervalMultiplier: Float,
        repairIngredient: Ingredient,
    ) {
        this.materialName = materialName
        this.material = material
        this.baseDistance = baseDistance
        this.maxPushStrength = maxPushStrength
        this.maxRange = maxRange
        this.pushStrengthIntervalMultiplier = pushStrengthIntervalMultiplier
        this.pushRangeIntervalMultiplier = pushRangeIntervalMultiplier
        this.repairIngredient = repairIngredient
    }

    constructor(
        name: String,
        material: ToolMaterial,
        baseDistance: Double,
        maxPushStrength: Int,
        maxRange: Int,
        repairIngredient: Ingredient,
    ) : this(
        materialName = name,
        material = material,
        baseDistance = baseDistance,
        maxPushStrength = maxPushStrength,
        maxRange = maxRange,
        pushStrengthIntervalMultiplier = 1.0f,
        pushRangeIntervalMultiplier = 1.0f,
        repairIngredient = repairIngredient
    )

    override fun getDurability() = material.durability
    override fun getMiningSpeedMultiplier() = material.miningSpeedMultiplier
    override fun getAttackDamage() = material.attackDamage
    override fun getMiningLevel() = material.miningLevel
    override fun getEnchantability() = material.enchantability
    override fun getRepairIngredient() = repairIngredient
    override fun canPushAway(world: World, pos: BlockPos): Boolean {
        val i = material.miningLevel
        val state = world.getBlockState(pos)
        if (i < MiningLevels.DIAMOND && state.isIn(BlockTags.NEEDS_DIAMOND_TOOL)) {
            return false
        }
        if (i < MiningLevels.IRON && state.isIn(BlockTags.NEEDS_IRON_TOOL)) {
            return false
        }
        return if (i < MiningLevels.STONE && state.isIn(BlockTags.NEEDS_STONE_TOOL)) {
            false
        } else super.canPushAway(world, pos) || isSpecial && state.getHardness(
            world,
            pos
        ) < 0
    }

    override fun toString() = materialName
    override fun asString() = materialName

    companion object {
        fun fromToolMaterial(material: ToolMaterial): List<HammerMaterial> =
            Arrays.stream(entries.toTypedArray())
                .filter { it.material == material }
                .collect(Collectors.toList())
    }
}

fun createToolMaterial(
    durability: Int,
    miningSpeedMultiplier: Float,
    attackDamage: Float,
    miningLevel: Int,
    enchantability: Int,
    repairIngredient: Ingredient,
): ToolMaterial {
    return object : ToolMaterial {
        override fun getDurability() = durability

        override fun getMiningSpeedMultiplier() = miningSpeedMultiplier

        override fun getAttackDamage() = attackDamage

        override fun getMiningLevel() = miningLevel

        override fun getEnchantability() = enchantability

        override fun getRepairIngredient() = repairIngredient
    }
}