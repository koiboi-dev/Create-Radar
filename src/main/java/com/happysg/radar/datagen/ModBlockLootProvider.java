package com.happysg.radar.datagen;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.tterrag.registrate.util.entry.BlockEntry;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;

public class ModBlockLootProvider implements DataProvider {
    private final PackOutput output;

    public ModBlockLootProvider(PackOutput output) {
        this.output = output;
    }

    @Override
    public CompletableFuture<?> run(CachedOutput cache) {
        return CompletableFuture.allOf(ModBlockData.toolAndLootBlocks().stream()
                .map(block -> saveLootTable(cache, block))
                .toArray(CompletableFuture[]::new));
    }

    private CompletableFuture<?> saveLootTable(CachedOutput cache, BlockEntry<? extends Block> block) {
        ResourceLocation id = block.getId();
        Path path = output.getOutputFolder()
                .resolve("data")
                .resolve(id.getNamespace())
                .resolve("loot_table")
                .resolve("blocks")
                .resolve(id.getPath() + ".json");

        return DataProvider.saveStable(cache, createSelfDropTable(id), path);
    }

    private static JsonObject createSelfDropTable(ResourceLocation id) {
        JsonObject table = new JsonObject();
        table.addProperty("type", "minecraft:block");

        JsonArray pools = new JsonArray();
        JsonObject pool = new JsonObject();
        pool.addProperty("rolls", 1.0);

        JsonArray entries = new JsonArray();
        JsonObject entry = new JsonObject();
        entry.addProperty("type", "minecraft:item");
        entry.addProperty("name", id.toString());
        entries.add(entry);
        pool.add("entries", entries);

        JsonArray conditions = new JsonArray();
        JsonObject condition = new JsonObject();
        condition.addProperty("condition", "minecraft:survives_explosion");
        conditions.add(condition);
        pool.add("conditions", conditions);

        pools.add(pool);
        table.add("pools", pools);
        return table;
    }

    @Override
    public String getName() {
        return "Create Radar Block Loot Tables";
    }
}
