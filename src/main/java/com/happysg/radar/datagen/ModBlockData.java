package com.happysg.radar.datagen;

import com.happysg.radar.compat.vs2.VS2CompatRegister;
import com.happysg.radar.registry.ModBlocks;
import com.tterrag.registrate.util.entry.BlockEntry;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.world.level.block.Block;

public final class ModBlockData {
    public static List<BlockEntry<? extends Block>> toolAndLootBlocks() {
        List<BlockEntry<? extends Block>> blocks = new ArrayList<>(List.of(
                ModBlocks.MONITOR,
                ModBlocks.RADAR_LINK,
                ModBlocks.RADAR_BEARING_BLOCK,
                ModBlocks.RADAR_RECEIVER_BLOCK,
                ModBlocks.RADAR_DISH_BLOCK,
                ModBlocks.RADAR_PLATE_BLOCK,
                ModBlocks.CREATIVE_RADAR_PLATE_BLOCK,
                ModBlocks.AUTO_YAW_CONTROLLER_BLOCK,
                ModBlocks.AUTO_PITCH_CONTROLLER_BLOCK,
                ModBlocks.FIRE_CONTROLLER_BLOCK,
                ModBlocks.NETWORK_FILTERER_BLOCK
        ));

        if (VS2CompatRegister.STATIONARY_RADAR != null) {
            blocks.add(VS2CompatRegister.STATIONARY_RADAR);
        }
        if (VS2CompatRegister.ID_BLOCK != null) {
            blocks.add(VS2CompatRegister.ID_BLOCK);
        }
        if (VS2CompatRegister.RWR_BLOCK != null) {
            blocks.add(VS2CompatRegister.RWR_BLOCK);
        }

        return blocks;
    }

    private ModBlockData() {
    }
}
