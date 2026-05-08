package com.happysg.radar.datagen;

import net.neoforged.neoforge.data.event.GatherDataEvent;

public final class ModDataGenerators {
    public static void gatherData(GatherDataEvent event) {
        if (!event.includeServer()) {
            return;
        }

        event.createProvider(ModBlockLootProvider::new);
        event.createProvider(ModBlockToolTagsProvider::new);
    }

    private ModDataGenerators() {
    }
}
