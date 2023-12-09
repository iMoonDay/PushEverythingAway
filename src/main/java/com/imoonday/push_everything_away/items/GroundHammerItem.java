package com.imoonday.push_everything_away.items;

import com.imoonday.push_everything_away.client.PushEverythingAwayClient;
import com.imoonday.push_everything_away.utils.HammerMaterial;
import com.imoonday.push_everything_away.utils.Pushable;
import me.x150.renderer.render.Renderer3d;
import me.x150.renderer.util.RendererUtils;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.fabricmc.yarn.constants.MiningLevels;
import net.minecraft.block.BlockState;
import net.minecraft.block.ShapeContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.SwordItem;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Hand;
import net.minecraft.util.Rarity;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.UseAction;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.*;
import net.minecraft.world.World;

import java.awt.*;
import java.util.List;
import java.util.*;

public class GroundHammerItem extends SwordItem implements Pushable {

    private final HammerMaterial hammerMaterial;

    public GroundHammerItem(HammerMaterial material, Item.Settings settings) {
        super(material, 9, material.isSpecial() ? 1.0f : -3.5f, settings.rarity(material.isSpecial() ? Rarity.EPIC : Rarity.COMMON));
        this.hammerMaterial = material;
    }

    @Override
    public boolean canMine(BlockState state, World world, BlockPos pos, PlayerEntity miner) {
        return true;
    }

    @Override
    public float getMiningSpeedMultiplier(ItemStack stack, BlockState state) {
        return this.isSuitableFor(state) ? this.hammerMaterial.getMiningSpeedMultiplier() : 1.0f;
    }

    @Override
    public boolean isSuitableFor(BlockState state) {
        int i = this.getMaterial().getMiningLevel();
        if (i < MiningLevels.DIAMOND && state.isIn(BlockTags.NEEDS_DIAMOND_TOOL)) {
            return false;
        }
        if (i < MiningLevels.IRON && state.isIn(BlockTags.NEEDS_IRON_TOOL)) {
            return false;
        }
        return i >= MiningLevels.STONE || !state.isIn(BlockTags.NEEDS_STONE_TOOL);
    }

    @Override
    public int getMaxUseTime(ItemStack stack) {
        return 72000;
    }

    @Override
    public UseAction getUseAction(ItemStack stack) {
        return UseAction.BOW;
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        ItemStack itemStack = user.getStackInHand(hand);
        user.setCurrentHand(hand);
        return TypedActionResult.consume(itemStack);
    }

    @Override
    public void onStoppedUsing(ItemStack stack, World world, LivingEntity user, int remainingUseTicks) {
        super.onStoppedUsing(stack, world, user, remainingUseTicks);
        if (!world.isClient && user instanceof PlayerEntity player) {
            int usedTime = this.getMaxUseTime(stack) - remainingUseTicks;
            HitResult raycast = player.raycast(getDistance(usedTime), 0.0f, false);
            BlockPos center = raycast instanceof BlockHitResult hitResult && hitResult.getType() == HitResult.Type.BLOCK ? hitResult.getBlockPos() : BlockPos.ofFloored(raycast.getPos());
            int range = getPushRange(usedTime);
            int pushStrength = getPushStrength(usedTime);
            Vec3d vector = player.getRotationVector();
            Box box = this.isSpecial() ? new Box(center).expand(range) : Pushable.createBox(center, vector, range);
            List<BlockPos> posList = new ArrayList<>();
            BlockPos.stream(box).forEach(blockPos -> posList.add(new BlockPos(blockPos)));
            posList.sort(Comparator.comparingInt(Vec3i::getY).reversed().thenComparingDouble(value -> value.getSquaredDistance(center)));
            for (BlockPos pos : posList) {
                Pushable.tryPushBlock(player, world, player.getActiveHand(), pos, pushStrength);
            }
            if (!isSpecial()) {
                player.getItemCooldownManager().set(this, 10 * (range + 1));
                stack.damage((range + 1) * pushStrength, player, livingEntity -> livingEntity.sendToolBreakStatus(player.getActiveHand()));
            }
            world.playSound(null, player.getBlockPos(), SoundEvents.BLOCK_ANVIL_PLACE, SoundCategory.PLAYERS);
        }
    }

    public boolean isSpecial() {
        return this.hammerMaterial.isSpecial();
    }

    @Override
    public boolean hasGlint(ItemStack stack) {
        return isSpecial() || super.hasGlint(stack);
    }

    @Override
    public boolean canPushAway(World world, BlockPos pos) {
        int i = this.getMaterial().getMiningLevel();
        BlockState state = world.getBlockState(pos);
        if (i < MiningLevels.DIAMOND && state.isIn(BlockTags.NEEDS_DIAMOND_TOOL)) {
            return false;
        }
        if (i < MiningLevels.IRON && state.isIn(BlockTags.NEEDS_IRON_TOOL)) {
            return false;
        }
        if (i < MiningLevels.STONE && state.isIn(BlockTags.NEEDS_STONE_TOOL)) {
            return false;
        }
        return Pushable.super.canPushAway(world, pos) || this.isSpecial() && state.getHardness(world, pos) < 0;
    }

    @Override
    public double getBaseDistance() {
        return hammerMaterial.getBaseDistance();
    }

    @Override
    public int getMaxPushStrength() {
        return hammerMaterial.getMaxPushStrength();
    }

    @Override
    public int getMaxRange() {
        return hammerMaterial.getMaxRange();
    }

    @Override
    public int getPushRangeInterval() {
        return hammerMaterial.getPushRangeInterval();
    }

    @Override
    public double getPushStrengthInterval() {
        return hammerMaterial.getPushStrengthInterval();
    }

    @Override
    public double getDistance(int usedTime) {
        return hammerMaterial.getDistance(usedTime);
    }

    @Override
    public int getPushRange(int usedTime) {
        return hammerMaterial.getPushRange(usedTime);
    }

    @Override
    public int getPushStrength(int usedTime) {
        return hammerMaterial.getPushStrength(usedTime);
    }

    public static void renderSelectionBoxes(WorldRenderContext context) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null) {
            return;
        }
        ClientPlayerEntity player = client.player;
        if (player == null) {
            return;
        }
        if (player.isSpectator()) {
            return;
        }
        Item item = player.getMainHandStack().getItem();
        if (!(item instanceof GroundHammerItem)) {
            item = player.getOffHandStack().getItem();
        }
        if (!(item instanceof GroundHammerItem hammerItem)) {
            return;
        }
        Box box = null;
        Color color = Color.GREEN;
        Set<Box> boxes = new HashSet<>();
        boolean special = hammerItem.isSpecial();
        if (!player.isUsingItem()) {
            if (client.crosshairTarget instanceof BlockHitResult hitResult && hitResult.getType() == HitResult.Type.BLOCK) {
                ClientWorld world = client.world;
                if (world != null) {
                    BlockPos pos = hitResult.getBlockPos();
                    box = world.getBlockState(pos).getOutlineShape(world, pos, ShapeContext.of(player)).getBoundingBox().offset(pos);
                }
            }
        } else {
            ItemStack activeItem = player.getActiveItem();
            ItemStack stackInHand = player.getStackInHand(player.getActiveHand());
            if (activeItem != stackInHand) {
                return;
            }
            int usedTime = player.getItemUseTime();
            int expandRange = hammerItem.getPushRange(usedTime);
            HitResult raycast = player.raycast(hammerItem.getDistance(usedTime), 0.0f, false);
            BlockPos blockPos = raycast instanceof BlockHitResult hitResult && hitResult.getType() == HitResult.Type.BLOCK ? hitResult.getBlockPos() : BlockPos.ofFloored(raycast.getPos());
            Vec3d vector = player.getRotationVector();
            double selectionRange = MathHelper.clamp((double) usedTime / hammerItem.getPushRangeInterval(), 0.0, hammerItem.getMaxRange() + 1);
            if (selectionRange < 1) {
                selectionRange /= 2;
            } else {
                selectionRange -= 0.5;
            }
            Vec3d selectionPos = raycast instanceof BlockHitResult hitResult && hitResult.getType() == HitResult.Type.BLOCK ? hitResult.getBlockPos().toCenterPos() : raycast.getPos();
            box = special ? Box.from(selectionPos).expand(selectionRange) : Pushable.createBox(selectionPos, vector, selectionRange);
            int pushStrength = hammerItem.getPushStrength(usedTime);
            color = RendererUtils.lerp(Color.GREEN, Color.RED, (double) pushStrength / hammerItem.getMaxPushStrength());
            if (PushEverythingAwayClient.displaySelectionOutlines) {
                Box selectionBox = special ? new Box(blockPos).expand(expandRange) : Pushable.createBox(blockPos, vector, expandRange);
                BlockPos.stream(selectionBox).map(pos -> client.world.getBlockState(pos).getOutlineShape(client.world, pos, ShapeContext.of(player)).offset(pos.getX(), pos.getY(), pos.getZ())).filter(voxelShape -> !voxelShape.isEmpty()).forEach(voxelShape -> boxes.addAll(voxelShape.getBoundingBoxes()));
            }
        }
        for (Box box1 : boxes) {
            Renderer3d.renderEdged(context.matrixStack(), RendererUtils.modify(Color.WHITE, -1, -1, -1, 75), Color.WHITE, new Vec3d(box1.minX, box1.minY, box1.minZ), new Vec3d(box1.getLengthX(), box1.getLengthY(), box1.getLengthZ()));
        }
        if (box != null) {
            box = box.expand(0.02);
            Renderer3d.renderEdged(context.matrixStack(), RendererUtils.modify(color, -1, -1, -1, 50), color, new Vec3d(box.minX, box.minY, box.minZ), new Vec3d(box.getLengthX(), box.getLengthY(), box.getLengthZ()));
        }
    }
}
