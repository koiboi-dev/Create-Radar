package com.happysg.radar.block.radar.behavior;

import com.happysg.radar.block.radar.track.RadarTrack;
import net.minecraft.core.BlockPos;

import java.util.Collection;

public interface IRadar {
    Collection<RadarTrack> getTracks();

    float getRange();

    boolean isRunning();

    BlockPos getBlockPos();

    float getGlobalAngle();
}