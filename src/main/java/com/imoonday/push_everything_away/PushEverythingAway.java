package com.imoonday.push_everything_away;

import com.imoonday.push_everything_away.entities.PushedBlockEntity;
import com.imoonday.push_everything_away.init.ModItems;
import com.imoonday.push_everything_away.utils.HammerMaterial;
import com.imoonday.push_everything_away.utils.Pushable;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class PushEverythingAway implements ModInitializer {

    public static final String MOD_ID = "push_everything_away";
    public static final EntityType<PushedBlockEntity> PUSHED_BLOCK = Registry.register(Registries.ENTITY_TYPE, new Identifier(MOD_ID, "pushed_block"), EntityType.Builder.<PushedBlockEntity>create(PushedBlockEntity::new, SpawnGroup.MISC).setDimensions(0.98f, 0.98f).maxTrackingRange(10).trackingTickInterval(5).build("pushed_block"));
    public static final RegistryKey<ItemGroup> ITEM_GROUP_REGISTRY_KEY = RegistryKey.of(RegistryKeys.ITEM_GROUP, new Identifier(MOD_ID, "push_everything_away"));
    public static final ItemGroup ITEM_GROUP = Registry.register(Registries.ITEM_GROUP, ITEM_GROUP_REGISTRY_KEY.getValue(), FabricItemGroup.builder().displayName(Text.translatable("group.push_everything_away")).icon(() -> new ItemStack(ModItems.HAMMERS.get(HammerMaterial.BEDROCK))).entries((displayContext, entries) -> ModItems.HAMMERS.values().forEach(entries::add)).build());

    @Override
    public void onInitialize() {
        ModItems.init();
        PlayerBlockBreakEvents.BEFORE.register((world, player, pos, state, blockEntity) -> beforeBlockBreak(world, player, pos));
    }

    private static boolean beforeBlockBreak(World world, PlayerEntity player, BlockPos pos) {
        ActionResult pushed = Pushable.tryPushBlock(player, world, Hand.MAIN_HAND, pos, 1);
        boolean accepted = pushed.isAccepted();
        if (accepted && !world.isClient) {
            ItemStack stack = player.getMainHandStack();
            stack.damage(1, player, e -> e.sendEquipmentBreakStatus(EquipmentSlot.MAINHAND));
        }
        return !accepted;
    }
}
