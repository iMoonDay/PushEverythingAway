package com.imoonday.push_everything_away.init;

import com.imoonday.push_everything_away.PushEverythingAway;
import com.imoonday.push_everything_away.items.GroundHammerItem;
import com.imoonday.push_everything_away.utils.HammerMaterial;
import net.fabricmc.fabric.api.item.v1.FabricItemSettings;
import net.minecraft.item.Items;
import net.minecraft.util.Identifier;

import java.util.LinkedHashMap;
import java.util.Map;

public class ModItems {

    public static final Map<HammerMaterial, GroundHammerItem> HAMMERS = new LinkedHashMap<>();

    public static void init() {
        for (HammerMaterial material : HammerMaterial.values()) {
            HAMMERS.put(material, registerHammer(material));
        }
    }

    private static GroundHammerItem registerHammer(HammerMaterial material) {
        return (GroundHammerItem) Items.register(new Identifier(PushEverythingAway.MOD_ID, material.asString() + "_ground_hammer"), new GroundHammerItem(material, new FabricItemSettings()));
    }
}
