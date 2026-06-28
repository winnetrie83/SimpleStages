package be.winnetrie.mod.simplestages.event;

import be.winnetrie.mod.simplestages.stage.StageManager;
import be.winnetrie.mod.simplestages.stage.data.StageDefinitionManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@EventBusSubscriber
public class StructureEvents {

    private static final int SAFE_SEARCH_START_RADIUS = 8;
    private static final int SAFE_SEARCH_MAX_RADIUS = 512;
    private static final int SAFE_SEARCH_STEP = 4;

    private static final Map<UUID, SafePosition> LAST_SAFE_POSITIONS = new HashMap<>();

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        if (player.tickCount % 20 != 0) {
            return;
        }

        ServerLevel level = (ServerLevel) player.level();
        BlockPos playerPos = player.blockPosition();

        Identifier lockedStructure = getLockedStructureAt(level, playerPos);

        if (lockedStructure == null) {
            saveSafePosition(player);
            return;
        }

        StageDefinitionManager.StructureLockEntry lock =
                StageDefinitionManager.getStructureLock(lockedStructure);

        if (lock == null) {
            saveSafePosition(player);
            return;
        }

        if (StageManager.hasStage(player, lock.stage())) {
            saveSafePosition(player);
            return;
        }

        denyStructureAccess(player, lock.stage());
    }

    private static Identifier getLockedStructureAt(ServerLevel level, BlockPos pos) {
        Map<Identifier, StageDefinitionManager.StructureLockEntry> structureLocks =
                StageDefinitionManager.getStructureLocks();

        if (structureLocks.isEmpty()) {
            return null;
        }

        Registry<Structure> structureRegistry = level.registryAccess().lookupOrThrow(Registries.STRUCTURE);

        ChunkPos playerChunk = new ChunkPos(pos.getX() >> 4, pos.getZ() >> 4);

        int playerChunkX = playerChunk.x();
        int playerChunkZ = playerChunk.z();

        for (Identifier structureId : structureLocks.keySet()) {
            Structure structure = structureRegistry.getValue(structureId);

            if (structure == null) {
                continue;
            }

            int buffer = structureLocks.get(structureId).buffer();

            for (int chunkX = playerChunkX - 1; chunkX <= playerChunkX + 1; chunkX++) {
                for (int chunkZ = playerChunkZ - 1; chunkZ <= playerChunkZ + 1; chunkZ++) {
                    ChunkPos checkChunk = new ChunkPos(chunkX, chunkZ);

                    for (StructureStart start : level.structureManager().startsForStructure(checkChunk, foundStructure -> foundStructure == structure)) {
                        if (start == null || !start.isValid()) {
                            continue;
                        }

                        BoundingBox box = start.getBoundingBox();

                        BoundingBox expandedBox = new BoundingBox(
                                box.minX() - buffer,
                                box.minY(),
                                box.minZ() - buffer,
                                box.maxX() + buffer,
                                box.maxY(),
                                box.maxZ() + buffer
                        );

                        if (expandedBox.isInside(pos)) {
                            return structureId;
                        }
                    }
                }
            }
        }

        return null;
    }

    private static void saveSafePosition(ServerPlayer player) {
        LAST_SAFE_POSITIONS.put(
                player.getUUID(),
                new SafePosition(
                        (ServerLevel) player.level(),
                        player.getX(),
                        player.getY(),
                        player.getZ(),
                        player.getYRot(),
                        player.getXRot()
                )
        );
    }

    private static void denyStructureAccess(ServerPlayer player, String requiredStage) {
        String displayName = StageDefinitionManager.getDisplayName(requiredStage);

        player.sendSystemMessage(
                Component.literal("You need " + displayName + " to enter this structure.")
        );

        SafePosition safePosition = LAST_SAFE_POSITIONS.get(player.getUUID());

        if (safePosition != null && isStoredSafePositionStillValid(safePosition)) {
            teleportToSafePosition(player, safePosition);
            return;
        }

        SafePosition newSafePosition = findSafePositionOutsideLockedStructure(player);

        if (newSafePosition != null) {
            LAST_SAFE_POSITIONS.put(player.getUUID(), newSafePosition);
            teleportToSafePosition(player, newSafePosition);
            return;
        }

        player.teleportTo(
                (ServerLevel) player.level(),
                player.getX(),
                player.getY() + 2.0D,
                player.getZ(),
                Set.of(),
                player.getYRot(),
                player.getXRot(),
                false
        );
    }

    private static boolean isStoredSafePositionStillValid(SafePosition safePosition) {
        BlockPos pos = BlockPos.containing(safePosition.x(), safePosition.y(), safePosition.z());

        if (getLockedStructureAt(safePosition.level(), pos) != null) {
            return false;
        }

        return isSafeStandingPosition(safePosition.level(), pos);
    }

    private static SafePosition findSafePositionOutsideLockedStructure(ServerPlayer player) {
        ServerLevel level = (ServerLevel) player.level();
        BlockPos origin = player.blockPosition();

        for (int radius = SAFE_SEARCH_START_RADIUS; radius <= SAFE_SEARCH_MAX_RADIUS; radius += SAFE_SEARCH_STEP) {
            for (int dx = -radius; dx <= radius; dx += SAFE_SEARCH_STEP) {
                for (int dz = -radius; dz <= radius; dz += SAFE_SEARCH_STEP) {
                    if (Math.abs(dx) != radius && Math.abs(dz) != radius) {
                        continue;
                    }

                    int x = origin.getX() + dx;
                    int z = origin.getZ() + dz;
                    int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);

                    BlockPos candidate = new BlockPos(x, y, z);

                    if (getLockedStructureAt(level, candidate) != null) {
                        continue;
                    }

                    if (!isSafeStandingPosition(level, candidate)) {
                        continue;
                    }

                    return new SafePosition(
                            level,
                            candidate.getX() + 0.5D,
                            candidate.getY(),
                            candidate.getZ() + 0.5D,
                            player.getYRot(),
                            player.getXRot()
                    );
                }
            }
        }

        return null;
    }

    private static boolean isSafeStandingPosition(ServerLevel level, BlockPos pos) {
        BlockPos below = pos.below();
        BlockPos above = pos.above();

        if (!level.getBlockState(below).isSolidRender()) {
            return false;
        }

        if (!level.getBlockState(pos).isAir()) {
            return false;
        }

        if (!level.getBlockState(above).isAir()) {
            return false;
        }

        if (!level.getFluidState(pos).isEmpty()) {
            return false;
        }

        if (!level.getFluidState(above).isEmpty()) {
            return false;
        }

        return true;
    }

    private static void teleportToSafePosition(ServerPlayer player, SafePosition safePosition) {
        player.teleportTo(
                safePosition.level(),
                safePosition.x(),
                safePosition.y(),
                safePosition.z(),
                Set.of(),
                safePosition.yRot(),
                safePosition.xRot(),
                false
        );
    }

    private record SafePosition(
            ServerLevel level,
            double x,
            double y,
            double z,
            float yRot,
            float xRot
    ) {
    }
}