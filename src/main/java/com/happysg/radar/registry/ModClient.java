package com.happysg.radar.registry;

import com.happysg.radar.CreateRadar;
import com.happysg.radar.config.RadarConfig;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;

@Mod(value = CreateRadar.MODID, dist = Dist.CLIENT)
public final class ModClient {
    public ModClient(ModContainer container) {
        container.registerExtensionPoint(IConfigScreenFactory.class, RadarConfig::createConfigScreen);
    }
}