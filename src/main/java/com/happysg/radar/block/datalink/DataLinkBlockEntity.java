package com.happysg.radar.block.datalink;

import com.happysg.radar.block.behavior.networks.WeaponNetworkRuntime;
import com.happysg.radar.compat.Mods;
import com.happysg.radar.compat.vs2.DataLinkVSHelper;
import com.happysg.radar.registry.AllDataBehaviors;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nullable;
import java.util.List;

public class DataLinkBlockEntity extends SmartBlockEntity {

    public enum WeaponEndpointType {
        NONE,
        YAW,
        PITCH,
        FIRING
    }

    protected BlockPos targetOffset = BlockPos.ZERO;
    @Nullable
    private Long linkedShipId = null;
    private BlockPos targetOffsetShip = BlockPos.ZERO;
    private WeaponEndpointType weaponEndpointType = WeaponEndpointType.NONE;

    public DataPeripheral activeSource;
    public DataController activeTarget;

    private CompoundTag sourceConfig;
    boolean ledState = false;

    private BlockPos linkedMonitorPos;

    public DataLinkBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {

    }

    @Override
    public void tick() {
        super.tick();
        registerWeaponLink();
        updateGatheredData();
    }

    public void updateGatheredData() {
        BlockPos sourcePosition = getSourcePosition();
        BlockPos targetPosition = getTargetPosition();

        if (!level.isLoaded(targetPosition) || !level.isLoaded(sourcePosition))
            return;

        DataController target = AllDataBehaviors.targetOf(level, targetPosition);
        DataPeripheral source = AllDataBehaviors.sourcesOf(level, sourcePosition);
        boolean notify = false;

        if (activeTarget != target) {
            activeTarget = target;
            notify = true;
        }

        if (activeSource != source) {
            activeSource = source;
            sourceConfig = new CompoundTag();
            notify = true;
        }

        if (notify)
            notifyUpdate();
        if (activeSource == null || activeTarget == null) {
            ledState = false;
            return;
        }

        ledState = true;
        activeSource.transferData(new DataLinkContext(level, this), activeTarget);
        sendData();
        //TODO implement advancement
    }

    @Override
    public void writeSafe(CompoundTag tag) {
        super.writeSafe(tag);
        writeGatheredData(tag);
    }

    @Override
    protected void write(CompoundTag tag, boolean clientPacket) {
        super.write(tag, clientPacket);
        writeGatheredData(tag);
        if (clientPacket && activeTarget != null)
            tag.putString("TargetType", activeTarget.id.toString());
        tag.putBoolean("LedState", ledState);
    }

    private void writeGatheredData(CompoundTag tag) {
        tag.put("TargetOffset", NbtUtils.writeBlockPos(targetOffset));
        tag.putString("WeaponEndpointType", weaponEndpointType.name());

        if (linkedShipId != null)
            tag.putLong("LinkedShipId", linkedShipId);
        tag.put("TargetOffsetShip", NbtUtils.writeBlockPos(targetOffsetShip));

        if (activeSource != null) {
            CompoundTag data = sourceConfig.copy();
            data.putString("Id", activeSource.id.toString());
            tag.put("Source", data);
        }
    }

    @Override
    protected void read(CompoundTag tag, boolean clientPacket) {
        super.read(tag, clientPacket);

        targetOffset = NbtUtils.readBlockPos(tag.getCompound("TargetOffset"));
        ledState = tag.getBoolean("LedState");
        weaponEndpointType = readWeaponEndpointType(tag);

        linkedShipId = tag.contains("LinkedShipId") ? tag.getLong("LinkedShipId") : null;
        targetOffsetShip = tag.contains("TargetOffsetShip")
                ? NbtUtils.readBlockPos(tag.getCompound("TargetOffsetShip"))
                : BlockPos.ZERO;

        if (clientPacket && tag.contains("TargetType"))
            activeTarget = AllDataBehaviors.getTarget(new ResourceLocation(tag.getString("TargetType")));

        if (!tag.contains("Source"))
            return;

        CompoundTag data = tag.getCompound("Source");
        activeSource = AllDataBehaviors.getSource(new ResourceLocation(data.getString("Id")));
        sourceConfig = new CompoundTag();
        if (activeSource != null)
            sourceConfig = data.copy();
    }

    private static WeaponEndpointType readWeaponEndpointType(CompoundTag tag) {
        if (!tag.contains("WeaponEndpointType"))
            return WeaponEndpointType.NONE;

        try {
            return WeaponEndpointType.valueOf(tag.getString("WeaponEndpointType"));
        } catch (IllegalArgumentException ignored) {
            return WeaponEndpointType.NONE;
        }
    }

    @Override
    public void onLoad() {
        super.onLoad();
        registerWeaponLink();
    }

    private void registerWeaponLink() {
        if (level instanceof ServerLevel sl)
            WeaponNetworkRuntime.register(sl, this);
    }

    private void unregisterWeaponLink() {
        if (level instanceof ServerLevel sl)
            WeaponNetworkRuntime.unregister(sl, worldPosition);
    }

    @Override
    public void onChunkUnloaded() {
        unregisterWeaponLink();
        super.onChunkUnloaded();
    }

    public WeaponEndpointType getWeaponEndpointType() {
        return weaponEndpointType;
    }

    public void setWeaponEndpointType(WeaponEndpointType weaponEndpointType) {
        this.weaponEndpointType = weaponEndpointType == null ? WeaponEndpointType.NONE : weaponEndpointType;
        registerWeaponLink();
        notifyUpdate();
        setChanged();
    }



    public void target(BlockPos targetPosition) {
        if (!(level instanceof ServerLevel sl)) {
            this.targetOffset = targetPosition.subtract(worldPosition);
            return;
        }

        if (Mods.VALKYRIENSKIES.isLoaded()) {
            DataLinkVSHelper.SameShipTarget sameShipTarget = DataLinkVSHelper.getSameShipTarget(sl, worldPosition, targetPosition);
            if (sameShipTarget != null) {
                linkedShipId = sameShipTarget.shipId();
                targetOffsetShip = sameShipTarget.targetOffsetShip();

                targetOffset = targetPosition.subtract(worldPosition);
                registerWeaponLink();
                setChanged();
                return;
            }
        }

        linkedShipId = null;
        targetOffsetShip = BlockPos.ZERO;
        this.targetOffset = targetPosition.subtract(worldPosition);
        registerWeaponLink();
        setChanged();
    }

    public BlockPos getSourcePosition() {
        if (!(level instanceof ServerLevel sl) || linkedShipId == null || !Mods.VALKYRIENSKIES.isLoaded())
            return worldPosition.relative(getDirection());

        BlockPos sourcePos = DataLinkVSHelper.getSourcePosition(sl, linkedShipId, worldPosition, getDirection());
        if (sourcePos == null) {
            linkedShipId = null;
            return worldPosition.relative(getDirection());
        }

        return sourcePos;
    }

    public CompoundTag getSourceConfig() {
        return sourceConfig;
    }

    public void setSourceConfig(CompoundTag sourceConfig) {
        this.sourceConfig = sourceConfig;
    }

    public Direction getDirection() {
        return getBlockState().getOptionalValue(DataLinkBlock.FACING)
                .orElse(Direction.UP)
                .getOpposite();
    }

    public BlockPos getTargetPosition() {
        if (!(level instanceof ServerLevel sl) || !Mods.VALKYRIENSKIES.isLoaded()) {
            return worldPosition.offset(targetOffset);
        }

        if (linkedShipId == null) {
            tryRelinkSameShip(sl);
        }

        if (linkedShipId == null) {
            return worldPosition.offset(targetOffset);
        }

        BlockPos targetPos = DataLinkVSHelper.getTargetPosition(sl, linkedShipId, worldPosition, targetOffsetShip);
        if (targetPos == null) {
            linkedShipId = null;
            tryRelinkSameShip(sl);
            if (linkedShipId != null) {
                BlockPos relinkedTarget = DataLinkVSHelper.getTargetPosition(sl, linkedShipId, worldPosition, targetOffsetShip);
                if (relinkedTarget != null) {
                    return relinkedTarget;
                }
            }
            return worldPosition.offset(targetOffset);
        }

        return targetPos;
    }

    private void tryRelinkSameShip(ServerLevel level) {
        BlockPos fallbackTarget = worldPosition.offset(targetOffset);
        DataLinkVSHelper.SameShipTarget sameShipTarget = DataLinkVSHelper.getSameShipTarget(level, worldPosition, fallbackTarget);
        if (sameShipTarget == null) {
            return;
        }

        linkedShipId = sameShipTarget.shipId();
        targetOffsetShip = sameShipTarget.targetOffsetShip();
        setChanged();
    }
}
