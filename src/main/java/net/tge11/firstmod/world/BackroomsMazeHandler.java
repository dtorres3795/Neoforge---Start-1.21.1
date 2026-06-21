package net.tge11.firstmod.world;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.network.protocol.game.ClientboundStopSoundPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LadderBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.tge11.firstmod.FirstMod;
import net.tge11.firstmod.sound.ModSounds;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class BackroomsMazeHandler {
    private static final int ROOM_SPACING = 28;
    private static final int FLOOR_Y = 32;
    private static final int STORY_COUNT = 10;

    public static final ResourceKey<Level> BACKROOMS_LEVEL = ResourceKey.create(
            Registries.DIMENSION,
            ResourceLocation.fromNamespaceAndPath(FirstMod.MODID, "backrooms")
    );

    private static final Set<Long> SHAPED_CHUNKS = new HashSet<>();
    private static final Map<UUID, Long> MUSIC_NEXT_PLAY_TICK = new HashMap<>();
    private static final Map<UUID, Long> MUSIC_PLAYING_UNTIL_TICK = new HashMap<>();

    public static void teleportWithBackroomsKey(ServerPlayer player) {
        if (player.serverLevel().dimension().equals(BACKROOMS_LEVEL)) {
            teleportToOverworld(player);
        } else {
            teleportToBackrooms(player);
        }
    }

    public static void teleportToBackrooms(ServerPlayer player) {
        ServerLevel destination = player.server.getLevel(BACKROOMS_LEVEL);
        if (destination != null) {
            shapeChunk(destination, new ChunkPos(0, 0));
            player.teleportTo(destination, 8.5D, 33.0D, 8.5D, player.getYRot(), player.getXRot());
        }
    }

    private static void teleportToOverworld(ServerPlayer player) {
        ServerLevel destination = player.server.overworld();
        BlockPos spawn = destination.getSharedSpawnPos();
        player.teleportTo(destination, spawn.getX() + 0.5D, spawn.getY() + 1.0D, spawn.getZ() + 0.5D, player.getYRot(), player.getXRot());
    }

    @SubscribeEvent
    public static void shapeMazeAroundPlayer(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        ServerLevel level = player.serverLevel();
        if (!level.dimension().equals(BACKROOMS_LEVEL) || level.getGameTime() % 20 != 0) {
            return;
        }

        playBackroomsMusic(player);

        ChunkPos center = player.chunkPosition();
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                shapeChunk(level, new ChunkPos(center.x + dx, center.z + dz));
            }
        }
    }


    private static void playBackroomsMusic(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        long gameTime = level.getGameTime();
        UUID playerId = player.getUUID();
        long playingUntil = MUSIC_PLAYING_UNTIL_TICK.getOrDefault(playerId, -1L);
        if (gameTime < playingUntil) {
            return;
        }

        if (gameTime % 100L == 0L) {
            player.connection.send(new ClientboundStopSoundPacket(null, SoundSource.MUSIC));
        }

        long nextPlayTick = MUSIC_NEXT_PLAY_TICK.getOrDefault(playerId, -1L);
        if (gameTime < nextPlayTick) {
            return;
        }

        player.connection.send(new ClientboundStopSoundPacket(null, SoundSource.MUSIC));
        level.playSound(null, player.blockPosition(), ModSounds.BACKROOMS_MUSIC.get(), SoundSource.MUSIC, 0.65F, 1.0F);
        MUSIC_PLAYING_UNTIL_TICK.put(playerId, gameTime + 20L * 240L);
        MUSIC_NEXT_PLAY_TICK.put(playerId, gameTime + 20L * 600L + level.getRandom().nextInt(20 * 600));
    }
    private static void shapeChunk(ServerLevel level, ChunkPos chunkPos) {
        long key = chunkPos.toLong();
        if (!SHAPED_CHUNKS.add(key)) {
            return;
        }

        BlockState wall = firstModBlockState("liminal_block");
        BlockState floor = firstModBlockState("liminal_rug");
        BlockState floorSupport = Blocks.SMOOTH_STONE.defaultBlockState();
        BlockState stairStep = Blocks.SMOOTH_STONE_SLAB.defaultBlockState();
        BlockState ceiling = Blocks.SMOOTH_STONE.defaultBlockState();
        BlockState light = firstModBlockState("fluorescent_lights");
        BlockState planks = firstModBlockState("liminal_planks");
        BlockState plankSlab = firstModBlockState("liminal_slab");
        BlockState plankFence = firstModBlockState("liminal_fence");
        BlockState poolTiles = firstModBlockState("pool_tiles");
        BlockState poolSlab = firstModBlockState("pool_tile_slab");
        BlockState ladder = Blocks.LADDER.defaultBlockState().setValue(LadderBlock.FACING, Direction.NORTH);
        BlockState air = Blocks.AIR.defaultBlockState();

        int minX = chunkPos.getMinBlockX();
        int minZ = chunkPos.getMinBlockZ();
        for (int localX = 0; localX < 16; localX++) {
            for (int localZ = 0; localZ < 16; localZ++) {
                int worldX = minX + localX;
                int worldZ = minZ + localZ;
                MassiveRoom massiveRoom = massiveRoomAt(worldX, worldZ);
                boolean poolrooms = isPoolroomsRegion(worldX, worldZ);
                BlockState regionWall = poolrooms ? poolTiles : wall;
                BlockState regionFloor = poolrooms ? poolTiles : floor;
                BlockState regionSupport = poolrooms ? poolTiles : floorSupport;
                BlockState regionStairStep = poolrooms ? poolSlab : stairStep;
                BlockState regionDetailSlab = poolrooms ? poolSlab : plankSlab;

                for (int story = 0; story < STORY_COUNT; story++) {
                    int storyFloor = storyFloor(story);
                    int storyCeiling = storyCeiling(story);
                    boolean lowerStair = story > 0 && isStairWell(worldX, worldZ, story - 1);
                    boolean lowerLadder = story > 0 && isLadderShaft(worldX, worldZ, story - 1);
                    boolean floorHole = massiveRoom != null || lowerLadder || lowerStair && !isStairBlock(worldX, storyFloor - 1, worldZ, story - 1);
                    boolean ceilingHole = massiveRoom != null || story < STORY_COUNT - 1 && (isStairWell(worldX, worldZ, story) || isLadderShaft(worldX, worldZ, story));
                    boolean walkable = isWalkable(worldX, worldZ, story);

                    set(level, worldX, storyFloor - 2, worldZ, floorHole ? air : regionSupport);
                    set(level, worldX, storyFloor - 1, worldZ, floorHole ? air : regionFloor);
                    for (int y = storyFloor; y < storyCeiling; y++) {
                        BlockState state;
                        if (isLadderBlock(worldX, y, worldZ, story)) {
                            state = ladder;
                        } else if (isStairBlock(worldX, y, worldZ, story)) {
                            state = regionStairStep;
                        } else if (isSolidMazeBlock(worldX, y, worldZ, story)) {
                            state = regionWall;
                        } else {
                            state = interiorDetailState(worldX, y, worldZ, story, regionWall, poolrooms ? poolTiles : planks, regionDetailSlab, poolrooms ? poolTiles : plankFence, air);
                        }
                        set(level, worldX, y, worldZ, state);
                    }
                    set(level, worldX, storyCeiling, worldZ, ceilingHole ? air : walkable && isLightCell(worldX, worldZ, story) ? light : ceiling);
                }

                for (int story = 0; story < STORY_COUNT - 1; story++) {
                    repairVerticalConnector(level, worldX, worldZ, story, regionWall, regionStairStep, ladder, air);
                }

                if (massiveRoom != null) {
                    shapeMassiveRoomColumn(level, worldX, worldZ, massiveRoom, regionWall, regionFloor, regionSupport, ceiling, light, air);
                }
            }
        }
    }

    private static void repairVerticalConnector(ServerLevel level, int x, int z, int story, BlockState wall,
                                                BlockState stairStep, BlockState ladder, BlockState air) {
        boolean stairWell = isStairWell(x, z, story);
        boolean ladderShaft = isLadderShaft(x, z, story);
        if (!stairWell && !ladderShaft) {
            return;
        }

        for (int y = storyFloor(story); y < storyFloor(story + 1); y++) {
            if (isLadderBlock(x, y, z, story)) {
                set(level, x, y, z, ladder);
            } else if (isLadderSupportBlock(x, z, story)) {
                set(level, x, y, z, wall);
            } else if (isStairBlock(x, y, z, story)) {
                set(level, x, y, z, stairStep);
            } else if (stairWell || ladderShaft) {
                set(level, x, y, z, air);
            }
        }
    }


    private static BlockState interiorDetailState(int x, int y, int z, int story, BlockState wall,
                                                 BlockState planks, BlockState plankSlab,
                                                 BlockState plankFence, BlockState air) {
        if (isCorridor(x, z, story) || isStairWell(x, z, story) || isLadderShaft(x, z, story)
                || isPillar(x, z, story) || isStairBlock(x, y, z, story) || isLadderBlock(x, y, z, story)) {
            return air;
        }

        int cellX = Math.floorDiv(x, ROOM_SPACING);
        int cellZ = Math.floorDiv(z, ROOM_SPACING);
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                int roomX = cellX + dx;
                int roomZ = cellZ + dz;
                if (!isInsideRoom(x, z, roomX, roomZ, story)) {
                    continue;
                }

                BlockState state = roomDetailState(x, y, z, roomX, roomZ, story, wall, planks, plankSlab, plankFence, air);
                if (!state.isAir()) {
                    return state;
                }
            }
        }

        return air;
    }

    private static BlockState roomDetailState(int x, int y, int z, int roomX, int roomZ, int story,
                                             BlockState wall, BlockState planks, BlockState plankSlab,
                                             BlockState plankFence, BlockState air) {
        int floorY = storyFloor(story);
        int localY = y - floorY;
        if (localY < 0 || localY > Math.min(3, roomHeight(story) - 1)) {
            return air;
        }

        int centerX = roomCenter(roomX, roomZ, story, false);
        int centerZ = roomCenter(roomX, roomZ, story, true);
        int halfWidth = roomHalfWidth(roomX, roomZ, story);
        int halfLength = roomHalfLength(roomX, roomZ, story);
        int localX = x - centerX;
        int localZ = z - centerZ;
        long hash = mix(roomX + story * 149, roomZ - story * 157);

        if (isHalfWallLine(localX, localZ, halfWidth, halfLength, hash)) {
            int height = 2 + (int) Math.floorMod(hash >>> 19, 2);
            if (localY < height) {
                return wall;
            }
            if (localY == height) {
                return plankSlab;
            }
        }

        if ((hash & 15L) != 0L) {
            return air;
        }

        int feature = (int) Math.floorMod(hash >>> 8, 6);
        if (feature == 0 && localZ == -halfLength + 1 && Math.abs(localX) <= Math.max(2, halfWidth - 2)) {
            if (localY == 1 || localY == 2 && Math.floorMod(localX, 3) != 0) {
                return plankSlab;
            }
        }

        if (feature == 1 && Math.abs(localX - halfWidth / 2) <= 1 && Math.abs(localZ + halfLength / 2) <= 1) {
            if (localY <= 2) {
                return localY == 2 ? plankSlab : plankFence;
            }
        }

        if (feature == 2 && between(localX, -halfWidth + 2, -halfWidth + 5) && between(localZ, halfLength - 5, halfLength - 2)) {
            boolean edge = localX == -halfWidth + 2 || localX == -halfWidth + 5 || localZ == halfLength - 5 || localZ == halfLength - 2;
            if (edge && localY <= 2) {
                return localY == 2 ? plankSlab : plankFence;
            }
        }

        if (feature == 3 && localY == 0 && Math.abs(localX) <= 2 && Math.abs(localZ) <= 1) {
            return planks;
        }

        if (feature == 4 && localY <= 1 && localX == halfWidth - 2 && Math.abs(localZ) <= Math.max(2, halfLength - 3)) {
            return localY == 1 ? plankSlab : planks;
        }

        if (feature == 5 && localY == 1 && Math.abs(localX + halfWidth / 2) <= 2 && localZ == halfLength - 2) {
            return plankSlab;
        }

        return air;
    }

    private static boolean isHalfWallLine(int localX, int localZ, int halfWidth, int halfLength, long hash) {
        if ((hash & 3L) == 0L) {
            int wallX = -halfWidth / 2 + (int) Math.floorMod(hash >>> 10, Math.max(1, halfWidth));
            return localX == wallX && between(localZ, -halfLength + 2, halfLength - 2) && Math.abs(localZ) > 1;
        }

        if ((hash & 3L) == 1L) {
            int wallZ = -halfLength / 2 + (int) Math.floorMod(hash >>> 12, Math.max(1, halfLength));
            return localZ == wallZ && between(localX, -halfWidth + 2, halfWidth - 2) && Math.abs(localX) > 1;
        }

        return false;
    }
    private static void shapeMassiveRoomColumn(ServerLevel level, int x, int z, MassiveRoom room, BlockState wall,
                                              BlockState floor, BlockState floorSupport, BlockState ceiling,
                                              BlockState light, BlockState air) {
        int bottom = FLOOR_Y;
        int top = Math.min(250, bottom + room.height);
        boolean boundary = isMassiveRoomClosedWall(x, z, room);
        boolean entrance = isMassiveRoomEntrance(x, z, room);
        boolean pillar = isMassiveRoomPillar(x, z, room);

        set(level, x, bottom - 2, z, floorSupport);
        set(level, x, bottom - 1, z, floor);
        for (int y = bottom; y < top; y++) {
            boolean lowEntrance = y < bottom + 5 && entrance;
            set(level, x, y, z, boundary && !lowEntrance || pillar ? wall : air);
        }
        set(level, x, top, z, isLightCell(x, z, 0) && !boundary ? light : ceiling);
    }

    private static boolean isMassiveRoomClosedWall(int x, int z, MassiveRoom room) {
        boolean west = x == room.centerX - room.halfWidth;
        boolean east = x == room.centerX + room.halfWidth;
        boolean north = z == room.centerZ - room.halfLength;
        boolean south = z == room.centerZ + room.halfLength;
        if (!(west || east || north || south)) {
            return false;
        }

        if (room.openSide >= 0 && (room.openSide == 0 && west || room.openSide == 1 && east || room.openSide == 2 && north || room.openSide == 3 && south)) {
            return false;
        }

        return true;
    }

    private static boolean isMassiveRoomEntrance(int x, int z, MassiveRoom room) {
        int doorHalfWidth = 4;
        return x == room.centerX - room.halfWidth && Math.abs(z - room.centerZ) <= doorHalfWidth
                || x == room.centerX + room.halfWidth && Math.abs(z - room.centerZ) <= doorHalfWidth
                || z == room.centerZ - room.halfLength && Math.abs(x - room.centerX) <= doorHalfWidth
                || z == room.centerZ + room.halfLength && Math.abs(x - room.centerX) <= doorHalfWidth;
    }

    private static boolean isSolidMazeBlock(int x, int y, int z, int story) {
        if (isLadderSupportBlock(x, z, story)) {
            return true;
        }

        if (isPillar(x, z, story)) {
            return true;
        }

        if (isWalkable(x, z, story)) {
            return false;
        }

        return !isHighOpening(x, y, z, story);
    }

    private static boolean isWalkable(int x, int z, int story) {
        return isInsideAnyRoom(x, z, story) || isCorridor(x, z, story) || isFalseDoorPocket(x, z, story);
    }

    private static boolean isInsideAnyRoom(int x, int z, int story) {
        int cellX = Math.floorDiv(x, ROOM_SPACING);
        int cellZ = Math.floorDiv(z, ROOM_SPACING);

        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                if (isInsideRoom(x, z, cellX + dx, cellZ + dz, story)) {
                    return true;
                }
            }
        }

        return false;
    }

    private static boolean isInsideRoom(int x, int z, int roomX, int roomZ, int story) {
        int centerX = roomCenter(roomX, roomZ, story, false);
        int centerZ = roomCenter(roomX, roomZ, story, true);
        int halfWidth = roomHalfWidth(roomX, roomZ, story);
        int halfLength = roomHalfLength(roomX, roomZ, story);

        return Math.abs(x - centerX) <= halfWidth && Math.abs(z - centerZ) <= halfLength;
    }

    private static boolean isCorridor(int x, int z, int story) {
        int cellX = Math.floorDiv(x, ROOM_SPACING);
        int cellZ = Math.floorDiv(z, ROOM_SPACING);

        for (int dx = -1; dx <= 0; dx++) {
            for (int dz = -1; dz <= 0; dz++) {
                int roomX = cellX + dx;
                int roomZ = cellZ + dz;
                if (isEastCorridor(x, z, roomX, roomZ, story) || isSouthCorridor(x, z, roomX, roomZ, story)) {
                    return true;
                }
            }
        }

        return false;
    }

    private static boolean isEastCorridor(int x, int z, int roomX, int roomZ, int story) {
        int startX = roomCenter(roomX, roomZ, story, false) + roomHalfWidth(roomX, roomZ, story);
        int endX = roomCenter(roomX + 1, roomZ, story, false) - roomHalfWidth(roomX + 1, roomZ, story);
        int startZ = roomCenter(roomX, roomZ, story, true);
        int endZ = roomCenter(roomX + 1, roomZ, story, true);
        int width = corridorHalfWidth(roomX, roomZ, story, false);

        if (Math.floorMod(story + roomX + roomZ, 3) == 0) {
            int bendX = startX + (endX - startX) / 2;
            return between(x, startX, bendX) && Math.abs(z - startZ) <= width
                    || between(x, bendX, endX) && Math.abs(z - endZ) <= width
                    || Math.abs(x - bendX) <= width && between(z, startZ, endZ);
        }

        return between(x, startX, endX) && Math.abs(z - startZ) <= width
                || between(x, startX, endX) && Math.abs(z - endZ) <= width
                || Math.abs(x - endX) <= width && between(z, startZ, endZ);
    }

    private static boolean isSouthCorridor(int x, int z, int roomX, int roomZ, int story) {
        int startZ = roomCenter(roomX, roomZ, story, true) + roomHalfLength(roomX, roomZ, story);
        int endZ = roomCenter(roomX, roomZ + 1, story, true) - roomHalfLength(roomX, roomZ + 1, story);
        int startX = roomCenter(roomX, roomZ, story, false);
        int endX = roomCenter(roomX, roomZ + 1, story, false);
        int width = corridorHalfWidth(roomX, roomZ, story, true);

        if (Math.floorMod(story + roomX - roomZ, 3) == 1) {
            int bendZ = startZ + (endZ - startZ) / 2;
            return Math.abs(x - startX) <= width && between(z, startZ, bendZ)
                    || Math.abs(x - endX) <= width && between(z, bendZ, endZ)
                    || between(x, startX, endX) && Math.abs(z - bendZ) <= width;
        }

        return Math.abs(x - startX) <= width && between(z, startZ, endZ)
                || Math.abs(x - endX) <= width && between(z, startZ, endZ)
                || between(x, startX, endX) && Math.abs(z - endZ) <= width;
    }

    private static boolean isPillar(int x, int z, int story) {
        int cellX = Math.floorDiv(x, ROOM_SPACING);
        int cellZ = Math.floorDiv(z, ROOM_SPACING);

        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                int roomX = cellX + dx;
                int roomZ = cellZ + dz;
                long hash = mix(roomX + story * 31, roomZ - story * 17);
                if ((hash & 3L) == 0L && isInsideRoom(x, z, roomX, roomZ, story)) {
                    int centerX = roomCenter(roomX, roomZ, story, false);
                    int centerZ = roomCenter(roomX, roomZ, story, true);
                    int offsetX = 3 + (int) Math.floorMod(hash >>> 4, 5);
                    int offsetZ = 3 + (int) Math.floorMod(hash >>> 8, 5);
                    boolean firstPillar = Math.abs(x - (centerX + offsetX)) <= 1 && Math.abs(z - (centerZ + offsetZ)) <= 1;
                    boolean secondPillar = (hash & 64L) == 0L
                            && Math.abs(x - (centerX - offsetX)) <= 1
                            && Math.abs(z - (centerZ - offsetZ)) <= 1;
                    if (firstPillar || secondPillar) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    private static boolean isFalseDoorPocket(int x, int z, int story) {
        int cellX = Math.floorDiv(x, ROOM_SPACING);
        int cellZ = Math.floorDiv(z, ROOM_SPACING);

        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                int roomX = cellX + dx;
                int roomZ = cellZ + dz;
                long hash = mix(roomX + story * 13, roomZ - story * 19);
                if ((hash & 15L) != 5L) {
                    continue;
                }

                int centerX = roomCenter(roomX, roomZ, story, false);
                int centerZ = roomCenter(roomX, roomZ, story, true);
                int halfWidth = roomHalfWidth(roomX, roomZ, story);
                int halfLength = roomHalfLength(roomX, roomZ, story);
                int doorwayOffset = Math.floorMod((int) (hash >>> 8), Math.max(3, halfLength * 2 - 2)) - halfLength + 1;

                if ((hash & 8L) == 0L) {
                    int pocketX = centerX + halfWidth + 1;
                    return between(x, pocketX, pocketX + 2) && Math.abs(z - (centerZ + doorwayOffset)) <= 1;
                }

                int pocketZ = centerZ + halfLength + 1;
                return between(z, pocketZ, pocketZ + 2) && Math.abs(x - (centerX + doorwayOffset)) <= 1;
            }
        }

        return false;
    }

    private static boolean isHighOpening(int x, int y, int z, int story) {
        int storyFloor = storyFloor(story);
        if (y < storyFloor + 2) {
            return false;
        }

        int cellX = Math.floorDiv(x, ROOM_SPACING);
        int cellZ = Math.floorDiv(z, ROOM_SPACING);
        long hash = mix(cellX + story * 23, cellZ - story * 29);
        if ((hash & 15L) != 11L) {
            return false;
        }

        int centerX = roomCenter(cellX, cellZ, story, false);
        int centerZ = roomCenter(cellX, cellZ, story, true);
        int highX = centerX + roomHalfWidth(cellX, cellZ, story) + 1;
        int highZ = centerZ - 2 + Math.floorMod((int) (hash >>> 12), 5);

        return Math.abs(x - highX) <= 1 && Math.abs(z - highZ) <= 1;
    }

    private static boolean isLightCell(int x, int z, int story) {
        return Math.floorMod(x + story * 3, 8) == 4 && Math.floorMod(z - story * 5, 8) == 4
                || Math.floorMod(x + z + story * 7, 19) == 0 && (Math.floorMod(x, 6) == 1 || Math.floorMod(z, 6) == 1);
    }

    private static boolean isStairWell(int x, int z, int story) {
        int cellX = Math.floorDiv(x, ROOM_SPACING);
        int cellZ = Math.floorDiv(z, ROOM_SPACING);

        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                int roomX = cellX + dx;
                int roomZ = cellZ + dz;
                if (!roomHasStairs(roomX, roomZ, story)) {
                    continue;
                }

                int centerX = stairCenterX(roomX, roomZ, story);
                int centerZ = stairCenterZ(roomX, roomZ, story);
                int ring = stairRing(roomX, roomZ, story);
                if (Math.abs(x - centerX) <= ring + 1 && Math.abs(z - centerZ) <= ring + 1) {
                    return true;
                }
            }
        }

        return false;
    }

    private static boolean isStairBlock(int x, int y, int z, int story) {
        if (story >= STORY_COUNT - 1) {
            return false;
        }

        int cellX = Math.floorDiv(x, ROOM_SPACING);
        int cellZ = Math.floorDiv(z, ROOM_SPACING);
        int storyFloor = storyFloor(story);
        int span = storyFloor(story + 1) - storyFloor;

        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                int roomX = cellX + dx;
                int roomZ = cellZ + dz;
                if (!roomHasStairs(roomX, roomZ, story)) {
                    continue;
                }

                int centerX = stairCenterX(roomX, roomZ, story);
                int centerZ = stairCenterZ(roomX, roomZ, story);
                for (int step = 0; step < span; step++) {
                    BlockPos stepPos = spiralStairStep(centerX, centerZ, step, stairRing(roomX, roomZ, story));
                    if (x == stepPos.getX() && z == stepPos.getZ() && y == storyFloor + step) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    private static BlockPos spiralStairStep(int centerX, int centerZ, int step, int ring) {
        int sideLength = ring * 2;
        int pos = Math.floorMod(step, sideLength * 4);
        if (pos < sideLength) {
            return new BlockPos(centerX - ring + pos, 0, centerZ - ring);
        }
        if (pos < sideLength * 2) {
            return new BlockPos(centerX + ring, 0, centerZ - ring + (pos - sideLength));
        }
        if (pos < sideLength * 3) {
            return new BlockPos(centerX + ring - (pos - sideLength * 2), 0, centerZ + ring);
        }
        return new BlockPos(centerX - ring, 0, centerZ + ring - (pos - sideLength * 3));
    }

    private static int stairRing(int roomX, int roomZ, int story) {
        return (mix(roomX + story * 71, roomZ - story * 43) & 1L) == 0L ? 1 : 2;
    }

    private static int stairCenterX(int roomX, int roomZ, int story) {
        return roomCenter(roomX, roomZ, story, false) - 1;
    }

    private static int stairCenterZ(int roomX, int roomZ, int story) {
        return roomCenter(roomX, roomZ, story, true) + Math.max(2, roomHalfLength(roomX, roomZ, story) - 5);
    }

    private static boolean isLadderShaft(int x, int z, int story) {
        int cellX = Math.floorDiv(x, ROOM_SPACING);
        int cellZ = Math.floorDiv(z, ROOM_SPACING);

        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                int roomX = cellX + dx;
                int roomZ = cellZ + dz;
                if (!roomHasLadder(roomX, roomZ, story)) {
                    continue;
                }

                int ladderX = roomCenter(roomX, roomZ, story, false) + roomHalfWidth(roomX, roomZ, story) - 1;
                int ladderZ = roomCenter(roomX, roomZ, story, true) - roomHalfLength(roomX, roomZ, story) + 2;
                if (Math.abs(x - ladderX) <= 1 && Math.abs(z - ladderZ) <= 1) {
                    return true;
                }
            }
        }

        return false;
    }

    private static boolean isLadderBlock(int x, int y, int z, int story) {
        if (story >= STORY_COUNT - 1) {
            return false;
        }

        int cellX = Math.floorDiv(x, ROOM_SPACING);
        int cellZ = Math.floorDiv(z, ROOM_SPACING);

        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                int roomX = cellX + dx;
                int roomZ = cellZ + dz;
                if (!roomHasLadder(roomX, roomZ, story)) {
                    continue;
                }

                int ladderX = roomCenter(roomX, roomZ, story, false) + roomHalfWidth(roomX, roomZ, story) - 1;
                int ladderZ = roomCenter(roomX, roomZ, story, true) - roomHalfLength(roomX, roomZ, story) + 2;
                if (x == ladderX && z == ladderZ && between(y, storyFloor(story), storyFloor(story + 1) - 1)) {
                    return true;
                }
            }
        }

        return false;
    }

    private static boolean isLadderSupportBlock(int x, int z, int story) {
        if (story >= STORY_COUNT - 1) {
            return false;
        }

        int cellX = Math.floorDiv(x, ROOM_SPACING);
        int cellZ = Math.floorDiv(z, ROOM_SPACING);

        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                int roomX = cellX + dx;
                int roomZ = cellZ + dz;
                if (!roomHasLadder(roomX, roomZ, story)) {
                    continue;
                }

                int ladderX = roomCenter(roomX, roomZ, story, false) + roomHalfWidth(roomX, roomZ, story) - 1;
                int ladderZ = roomCenter(roomX, roomZ, story, true) - roomHalfLength(roomX, roomZ, story) + 2;
                if (x == ladderX && z == ladderZ + 1) {
                    return true;
                }
            }
        }

        return false;
    }

    private static boolean roomHasStairs(int roomX, int roomZ, int story) {
        if (story >= STORY_COUNT - 1) {
            return false;
        }

        long hash = mix(roomX + 41 + story * 53, roomZ - 19 + story * 13);
        if (Math.floorMod(hash, 12L) != 0L) {
            return false;
        }

        return stairAreaIsClear(roomX, roomZ, story);
    }

    private static boolean stairAreaIsClear(int roomX, int roomZ, int story) {
        int ring = stairRing(roomX, roomZ, story);
        int centerX = stairCenterX(roomX, roomZ, story);
        int centerZ = stairCenterZ(roomX, roomZ, story);
        int roomCenterX = roomCenter(roomX, roomZ, story, false);
        int roomCenterZ = roomCenter(roomX, roomZ, story, true);
        int halfWidth = roomHalfWidth(roomX, roomZ, story);
        int halfLength = roomHalfLength(roomX, roomZ, story);
        int margin = ring + 2;

        if (centerX - margin <= roomCenterX - halfWidth || centerX + margin >= roomCenterX + halfWidth
                || centerZ - margin <= roomCenterZ - halfLength || centerZ + margin >= roomCenterZ + halfLength) {
            return false;
        }

        for (int dx = -margin; dx <= margin; dx++) {
            for (int dz = -margin; dz <= margin; dz++) {
                int x = centerX + dx;
                int z = centerZ + dz;
                if (!isInsideRoom(x, z, roomX, roomZ, story) || isCorridor(x, z, story) || isFalseDoorPocket(x, z, story)) {
                    return false;
                }
            }
        }

        return true;
    }

    private static boolean roomHasLadder(int roomX, int roomZ, int story) {
        return story < STORY_COUNT - 1 && (mix(roomX - 29 + story * 11, roomZ + 37 + story * 5) & 7L) == 6L;
    }

    private static MassiveRoom massiveRoomAt(int x, int z) {
        int cellX = Math.floorDiv(x, ROOM_SPACING);
        int cellZ = Math.floorDiv(z, ROOM_SPACING);

        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                MassiveRoom room = massiveRoom(cellX + dx, cellZ + dz);
                if (room != null && Math.abs(x - room.centerX) <= room.halfWidth && Math.abs(z - room.centerZ) <= room.halfLength) {
                    return room;
                }
            }
        }

        return null;
    }

    private static MassiveRoom massiveRoom(int roomX, int roomZ) {
        long hash = mix(roomX + 907, roomZ - 613);
        if ((hash & 63L) != 42L) {
            return null;
        }

        int centerX = roomX * ROOM_SPACING + ROOM_SPACING / 2;
        int centerZ = roomZ * ROOM_SPACING + ROOM_SPACING / 2;
        int halfWidth = 24 + (int) Math.floorMod(hash >>> 8, 28);
        int halfLength = 24 + (int) Math.floorMod(hash >>> 14, 28);
        int height = 140 + (int) Math.floorMod(hash >>> 20, 101);
        int openSide = (hash & 1024L) == 0L ? (int) Math.floorMod(hash >>> 30, 4) : -1;
        return new MassiveRoom(centerX, centerZ, halfWidth, halfLength, height, openSide);
    }

    private static boolean isMassiveRoomPillar(int x, int z, MassiveRoom room) {
        int spacing = 9;
        int localX = Math.floorMod(x - room.centerX, spacing);
        int localZ = Math.floorMod(z - room.centerZ, spacing);
        return Math.abs(x - room.centerX) > 4
                && Math.abs(z - room.centerZ) > 4
                && (localX == 0 || localX == 1)
                && (localZ == 0 || localZ == 1)
                && (mix(Math.floorDiv(x, spacing), Math.floorDiv(z, spacing)) & 3L) != 0L;
    }


    private static boolean isPoolroomsRegion(int x, int z) {
        int regionX = Math.floorDiv(x, ROOM_SPACING * 6);
        int regionZ = Math.floorDiv(z, ROOM_SPACING * 6);
        long hash = mix(regionX + 1201, regionZ - 8849);
        return Math.floorMod(hash, 5L) == 0L;
    }
    private static int storyFloor(int story) {
        int y = FLOOR_Y;
        for (int i = 0; i < story; i++) {
            y += roomHeight(i) + 3;
        }
        return y;
    }

    private static int storyCeiling(int story) {
        return storyFloor(story) + roomHeight(story);
    }

    private static int roomHeight(int story) {
        return 4 + Math.floorMod(story * 3 + 1, 4);
    }

    private static int stairRun(int story) {
        return 5 + Math.floorMod(story, 4);
    }

    private static int roomCenter(int roomX, int roomZ, int story, boolean zAxis) {
        long hash = mix(roomX + story * 101 + (zAxis ? 17 : 0), roomZ - story * 83 - (zAxis ? 0 : 19));
        int wobble = (int) Math.floorMod(hash >>> 6, 7) - 3;
        return (zAxis ? roomZ : roomX) * ROOM_SPACING + ROOM_SPACING / 2 + wobble;
    }

    private static int roomHalfWidth(int roomX, int roomZ, int story) {
        long hash = mix(roomX + story * 37, roomZ - story * 41);
        int base = 4 + (int) Math.floorMod(hash, 9);
        if ((hash & 64L) == 0L) {
            base += 4;
        }
        return Math.min(13, base);
    }

    private static int roomHalfLength(int roomX, int roomZ, int story) {
        long hash = mix(roomX + 113 - story * 29, roomZ - 71 + story * 47);
        int base = 4 + (int) Math.floorMod(hash, 10);
        if ((hash & 128L) == 0L) {
            base += 5;
        }
        return Math.min(15, base);
    }

    private static int corridorHalfWidth(int roomX, int roomZ, int story, boolean south) {
        long hash = mix(roomX + story * 17 + (south ? 17 : 3), roomZ - story * 11 - (south ? 11 : 5));
        return (hash & 7L) == 0L ? 3 : (hash & 3L) == 0L ? 2 : 1;
    }

    private static boolean between(int value, int a, int b) {
        return value >= Math.min(a, b) && value <= Math.max(a, b);
    }

    private static long mix(int x, int z) {
        long value = x * 341873128712L + z * 132897987541L + 0x9E3779B97F4A7C15L;
        value ^= value >>> 30;
        value *= 0xBF58476D1CE4E5B9L;
        value ^= value >>> 27;
        value *= 0x94D049BB133111EBL;
        return value ^ (value >>> 31);
    }

    private record MassiveRoom(int centerX, int centerZ, int halfWidth, int halfLength, int height, int openSide) {
    }

    private static BlockState firstModBlockState(String blockName) {
        ResourceLocation id = ResourceLocation.fromNamespaceAndPath(FirstMod.MODID, blockName);
        Block block = BuiltInRegistries.BLOCK.get(id);
        if (block == Blocks.AIR) {
            throw new IllegalStateException("Missing required firstmod block: " + id);
        }
        return block.defaultBlockState();
    }

    private static void set(ServerLevel level, int x, int y, int z, BlockState state) {
        level.setBlock(new BlockPos(x, y, z), state, Block.UPDATE_CLIENTS);
    }
}