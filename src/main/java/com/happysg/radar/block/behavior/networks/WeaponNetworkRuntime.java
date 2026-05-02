package com.happysg.radar.block.behavior.networks;

import com.happysg.radar.block.datalink.DataLinkBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public final class WeaponNetworkRuntime {

    private WeaponNetworkRuntime() {}

    private record DimPos(ResourceKey<Level> dim, BlockPos pos) {}

    private record LinkRef(ResourceKey<Level> dim,
                           BlockPos dataLinkPos,
                           BlockPos endpointPos,
                           BlockPos mountPos,
                           DataLinkBlockEntity.WeaponEndpointType type) {}

    public record WeaponGroupView(BlockPos mountPos,
                                  @Nullable BlockPos yawPos,
                                  @Nullable BlockPos pitchPos,
                                  @Nullable BlockPos firingPos,
                                  Set<BlockPos> dataLinks) {

        public Set<BlockPos> endpoints() {
            Set<BlockPos> out = new HashSet<>();
            if (yawPos != null) out.add(yawPos);
            if (pitchPos != null) out.add(pitchPos);
            if (firingPos != null) out.add(firingPos);
            return out;
        }

        public Set<BlockPos> otherEndpoints(BlockPos exclude) {
            Set<BlockPos> out = endpoints();
            out.remove(exclude);
            return out;
        }
    }

    private static final Map<DimPos, LinkRef> linksByDataLink = new HashMap<>();
    private static final Map<DimPos, DimPos> controllerToMount = new HashMap<>();
    private static final Map<DimPos, Set<BlockPos>> mountToDataLinks = new HashMap<>();

    public static void register(ServerLevel level, DataLinkBlockEntity link) {
        DataLinkBlockEntity.WeaponEndpointType type = link.getWeaponEndpointType();
        BlockPos dataLinkPos = link.getBlockPos();

        if (type == DataLinkBlockEntity.WeaponEndpointType.NONE) {
            unregister(level, dataLinkPos);
            return;
        }

        BlockPos mountPos = link.getTargetPosition();
        BlockPos endpointPos = link.getSourcePosition();
        ResourceKey<Level> dim = level.dimension();
        DimPos dataLinkKey = new DimPos(dim, dataLinkPos);

        LinkRef old = linksByDataLink.get(dataLinkKey);
        LinkRef next = new LinkRef(dim, dataLinkPos, endpointPos, mountPos, type);
        if (next.equals(old)) {
            return;
        }

        unregister(level, dataLinkPos);
        linksByDataLink.put(dataLinkKey, next);
        controllerToMount.put(new DimPos(dim, endpointPos), new DimPos(dim, mountPos));
        mountToDataLinks.computeIfAbsent(new DimPos(dim, mountPos), $ -> new HashSet<>()).add(dataLinkPos);
    }

    public static void unregister(ServerLevel level, BlockPos dataLinkPos) {
        ResourceKey<Level> dim = level.dimension();
        LinkRef old = linksByDataLink.remove(new DimPos(dim, dataLinkPos));
        if (old == null) {
            return;
        }

        DimPos endpointKey = new DimPos(old.dim(), old.endpointPos());
        DimPos mountKey = new DimPos(old.dim(), old.mountPos());

        DimPos indexedMount = controllerToMount.get(endpointKey);
        if (mountKey.equals(indexedMount)) {
            controllerToMount.remove(endpointKey);
        }

        Set<BlockPos> links = mountToDataLinks.get(mountKey);
        if (links != null) {
            links.remove(old.dataLinkPos());
            if (links.isEmpty()) {
                mountToDataLinks.remove(mountKey);
            }
        }
    }

    public static boolean canAttachEndpoint(ServerLevel level,
                                            BlockPos mountPos,
                                            BlockPos endpointPos,
                                            DataLinkBlockEntity.WeaponEndpointType type) {
        BlockPos existingMount = getMountForController(level, endpointPos);
        if (existingMount != null && !existingMount.equals(mountPos)) {
            return false;
        }

        WeaponGroupView view = getWeaponGroupViewFromMount(level, mountPos);
        if (view == null) {
            return true;
        }

        BlockPos existing = switch (type) {
            case YAW -> view.yawPos();
            case PITCH -> view.pitchPos();
            case FIRING -> view.firingPos();
            case NONE -> null;
        };

        return existing == null || existing.equals(endpointPos);
    }

    @Nullable
    public static BlockPos getMountForController(ServerLevel level, BlockPos controllerPos) {
        DimPos mount = controllerToMount.get(new DimPos(level.dimension(), controllerPos));
        return mount == null ? null : mount.pos();
    }

    @Nullable
    public static WeaponGroupView getWeaponGroupViewFromEndpoint(ServerLevel level, BlockPos endpointPos) {
        DimPos mount = controllerToMount.get(new DimPos(level.dimension(), endpointPos));
        return mount == null ? null : getWeaponGroupViewFromMount(level, mount.pos());
    }

    @Nullable
    public static WeaponGroupView getWeaponGroupViewFromMount(ServerLevel level, BlockPos mountPos) {
        ResourceKey<Level> dim = level.dimension();
        Set<BlockPos> dataLinks = mountToDataLinks.get(new DimPos(dim, mountPos));
        if (dataLinks == null || dataLinks.isEmpty()) {
            return null;
        }

        BlockPos yaw = null;
        BlockPos pitch = null;
        BlockPos firing = null;
        Set<BlockPos> liveLinks = new HashSet<>();

        for (BlockPos linkPos : dataLinks) {
            LinkRef ref = linksByDataLink.get(new DimPos(dim, linkPos));
            if (ref == null) {
                continue;
            }

            liveLinks.add(ref.dataLinkPos());
            switch (ref.type()) {
                case YAW -> yaw = ref.endpointPos();
                case PITCH -> pitch = ref.endpointPos();
                case FIRING -> firing = ref.endpointPos();
                case NONE -> {}
            }
        }

        if (yaw == null && pitch == null && firing == null) {
            return null;
        }

        return new WeaponGroupView(mountPos, yaw, pitch, firing, Collections.unmodifiableSet(liveLinks));
    }

    public static Collection<WeaponGroupView> getGroups(ServerLevel level) {
        ResourceKey<Level> dim = level.dimension();
        Set<BlockPos> mounts = new HashSet<>();
        for (DimPos mountKey : mountToDataLinks.keySet()) {
            if (mountKey.dim().equals(dim)) {
                mounts.add(mountKey.pos());
            }
        }

        List<WeaponGroupView> views = new ArrayList<>();
        for (BlockPos mount : mounts) {
            WeaponGroupView view = getWeaponGroupViewFromMount(level, mount);
            if (view != null) {
                views.add(view);
            }
        }
        return views;
    }
}
