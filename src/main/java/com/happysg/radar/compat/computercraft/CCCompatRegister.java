package com.happysg.radar.compat.computercraft;

import com.happysg.radar.CreateRadar;
import dan200.computercraft.api.ComputerCraftAPI;

public class CCCompatRegister {
    public static void registerPeripherals(){
        CreateRadar.getLogger().info("Registering Peripherals!");
        ComputerCraftAPI.registerGenericSource(new RadarBearingPeripheral());
        ComputerCraftAPI.registerGenericSource(new MonitorPeripheral());
    }
}
