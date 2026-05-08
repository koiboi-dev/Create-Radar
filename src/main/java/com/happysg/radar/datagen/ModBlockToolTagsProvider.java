package com.happysg.radar.datagen;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.tterrag.registrate.util.entry.BlockEntry;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;

public class ModBlockToolTagsProvider implements DataProvider {
    private final PackOutput output;

    public ModBlockToolTagsProvider(PackOutput output) {
        this.output = output;
    }

    @Override
    public CompletableFuture<?> run(CachedOutput cache) {
        List<ResourceLocation> blocks = ModBlockData.toolAndLootBlocks().stream()
                .map(BlockEntry::getId)
                .sorted(Comparator.comparing(ResourceLocation::toString))
                .toList();

        return CompletableFuture.allOf(
                saveMineableTag(cache, "pickaxe", blocks),
                saveMineableTag(cache, "axe", blocks)
        );
    }

    private CompletableFuture<?> saveMineableTag(CachedOutput cache, String tool, List<ResourceLocation> blocks) {
        Path path = output.getOutputFolder()
                .resolve("data")
                .resolve("minecraft")
                .resolve("tags")
                .resolve("block")
                .resolve("mineable")
                .resolve(tool + ".json");

        return DataProvider.saveStable(cache, createTag(blocks), path);
    }

    private static JsonObject createTag(List<ResourceLocation> blocks) {
        JsonObject tag = new JsonObject();
        tag.addProperty("replace", false);

        JsonArray values = new JsonArray();
        blocks.forEach(id -> values.add(id.toString()));
        tag.add("values", values);

        return tag;
    }

    @Override
    public String getName() {
        return "Create Radar Block Tool Tags";
    }
}
