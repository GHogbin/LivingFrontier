package dev.livingfrontier.encounter;

import dev.livingfrontier.FrontierConfig;
import dev.livingfrontier.FrontierEntities;
import dev.livingfrontier.FrontierSpawnRules;
import dev.livingfrontier.LivingFrontier;
import dev.livingfrontier.entity.ProwlerEntity;
import dev.livingfrontier.entity.RaiderEntity;
import dev.livingfrontier.entity.SkyWraithEntity;
import dev.livingfrontier.entity.VillageGuardEntity;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraftforge.event.ForgeEventFactory;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = LivingFrontier.MOD_ID)
public final class EncounterDirector {
    public static final String ENCOUNTER_TAG = "livingfrontier_encounter";

    private EncounterDirector() {
    }

    @SubscribeEvent
    public static void serverTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        ServerLevel level = event.getServer().overworld();
        long now = level.getGameTime();
        if (now % 600 != 0 || !FrontierConfig.ENCOUNTERS_ENABLED.get()) {
            return;
        }
        EncounterState state = state(level);
        for (ServerPlayer player : level.players()) {
            if (!state.contains(player.getUUID())) {
                state.schedule(player.getUUID(), now + nextDelay(level.getRandom()), player.blockPosition());
            } else if (state.isDue(player.getUUID(), now) && state.hasTravelled(player.getUUID(), player.blockPosition())
                    && canStart(level, player)) {
                List<Kind> kinds = availableKinds(level, player.blockPosition());
                if (!kinds.isEmpty()) {
                    attempt(level, player, kinds.get(level.random.nextInt(kinds.size())));
                }
                state.schedule(player.getUUID(), now + nextDelay(level.getRandom()), player.blockPosition());
            }
        }
    }

    public static EncounterState state(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(EncounterState::load, EncounterState::new, "livingfrontier_encounters");
    }

    public static long nextDelay(RandomSource random) {
        int min = FrontierConfig.ENCOUNTER_MIN_MINUTES.get();
        int max = Math.max(min, FrontierConfig.ENCOUNTER_MAX_MINUTES.get());
        return (min + random.nextInt(max - min + 1)) * 1200L;
    }

    public static boolean canStart(ServerLevel level, ServerPlayer player) {
        if (!FrontierConfig.ENCOUNTERS_ENABLED.get() || !level.dimension().equals(Level.OVERWORLD)
                || level.getDifficulty() == Difficulty.PEACEFUL || !level.getGameRules().getBoolean(GameRules.RULE_DOMOBSPAWNING)
                || !player.isAlive() || player.isCreative() || player.isSpectator() || player.isSleeping()
                || player instanceof net.minecraftforge.common.util.FakePlayer) {
            return false;
        }
        BlockPos position = player.blockPosition();
        int surface = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, position.getX(), position.getZ());
        return Math.abs(position.getY() - surface) <= 4 && FrontierSpawnRules.isOutdoors(level, position)
                && level.getFluidState(position).isEmpty()
                && level.getBrightness(LightLayer.BLOCK, position) <= 7 && !level.isCloseToVillage(position, 2)
                && level.getEntitiesOfClass(RaiderEntity.class, player.getBoundingBox().inflate(96), RaiderEntity::isGuard).isEmpty()
                && level.getEntitiesOfClass(VillageGuardEntity.class, player.getBoundingBox().inflate(64),
                        VillageGuardEntity::isVillageResident).isEmpty()
                && nearbyHostiles(level, player) < FrontierConfig.ENCOUNTER_LOCAL_CAP.get();
    }

    public static int attempt(ServerLevel level, ServerPlayer player, Kind kind) {
        if (!canStart(level, player) || !availableKinds(level, player.blockPosition()).contains(kind)) {
            return 0;
        }
        int encounterCount = 0;
        int monsters = 0;
        for (var entity : level.getAllEntities()) {
            if (entity.isAlive() && entity.getTags().contains(ENCOUNTER_TAG)) {
                encounterCount++;
            }
            if (entity instanceof Mob mob && mob.isAlive() && mob.getType().getCategory() == MobCategory.MONSTER) {
                monsters++;
            }
        }
        int budget = Math.min(FrontierConfig.ENCOUNTER_GLOBAL_CAP.get() - encounterCount,
                FrontierConfig.ENCOUNTER_LOCAL_CAP.get() - nearbyHostiles(level, player));
        budget = Math.min(budget, MobCategory.MONSTER.getMaxInstancesPerChunk() - monsters);
        if (budget <= 0) {
            return 0;
        }
        RandomSource random = level.getRandom();
        int wanted = Math.min(budget, kind.min + random.nextInt(kind.max - kind.min + 1));
        double angle = random.nextDouble() * Math.PI * 2;
        int radius = 30 + random.nextInt(15);
        int centerX = player.blockPosition().getX() + (int) (Math.cos(angle) * radius);
        int centerZ = player.blockPosition().getZ() + (int) (Math.sin(angle) * radius);
        int spawned = 0;
        for (int tries = 0; tries < 40 && spawned < wanted; tries++) {
            int x = centerX + random.nextInt(9) - 4;
            int z = centerZ + random.nextInt(9) - 4;
            BlockPos probe = new BlockPos(x, player.blockPosition().getY(), z);
            if (!level.hasChunkAt(probe)) {
                continue;
            }
            int surface = level.getHeight(kind == Kind.SKY_WRAITH ? Heightmap.Types.MOTION_BLOCKING
                    : Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
            BlockPos pos = new BlockPos(x, surface + (kind == Kind.SKY_WRAITH ? 6 + random.nextInt(5) : 0), z);
            if (!safePosition(level, pos, player) || !level.getBiome(pos).is(
                    TagKey.create(Registries.BIOME, FrontierEntities.id(kind.id + "_habitat")))) {
                continue;
            }
            EntityType<? extends Mob> type = kind.type.get();
            if (!SpawnPlacements.checkSpawnRules(type, level, MobSpawnType.EVENT, pos, random)) {
                continue;
            }
            Mob mob = type.create(level);
            if (mob == null) {
                throw new IllegalStateException("Cannot create registered encounter entity " + kind.id);
            }
            mob.moveTo(x + 0.5, pos.getY(), z + 0.5, random.nextFloat() * 360, 0);
            if (!level.noCollision(mob) || !ForgeEventFactory.checkSpawnPosition(mob, level, MobSpawnType.EVENT)) {
                continue;
            }
            ForgeEventFactory.onFinalizeSpawn(mob, level, level.getCurrentDifficultyAt(pos), MobSpawnType.EVENT, null, null);
            if (mob.isSpawnCancelled()) {
                continue;
            }
            if (mob instanceof RaiderEntity raider) {
                raider.setArcher(spawned % 2 == 1);
            }
            mob.addTag(ENCOUNTER_TAG);
            mob.setTarget(player);
            if (level.addFreshEntity(mob)) {
                spawned++;
            } else {
                LivingFrontier.LOGGER.warn("Encounter entity {} could not be added at {}", kind.id, pos);
            }
        }
        if (spawned > 0) {
            player.displayClientMessage(Component.translatable("message.livingfrontier.encounter." + kind.id), true);
        }
        return spawned;
    }

    public static boolean safePosition(ServerLevel level, BlockPos pos, ServerPlayer player) {
        return level.hasChunkAt(pos) && level.getWorldBorder().isWithinBounds(pos) && FrontierSpawnRules.isOutdoors(level, pos)
                && level.getFluidState(pos).isEmpty() && level.getBrightness(LightLayer.BLOCK, pos) <= 7
                && !level.isCloseToVillage(pos, 2) && player.distanceToSqr(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5) >= 24 * 24
                && level.players().stream().noneMatch(other -> other.isAlive()
                        && other.distanceToSqr(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5) < 24 * 24);
    }

    private static int nearbyHostiles(ServerLevel level, ServerPlayer player) {
        return level.getEntitiesOfClass(Mob.class, player.getBoundingBox().inflate(96),
                mob -> mob.isAlive() && (mob instanceof ProwlerEntity || mob instanceof SkyWraithEntity
                        || mob instanceof RaiderEntity raider && !raider.isGuard())).size();
    }

    private static List<Kind> availableKinds(ServerLevel level, BlockPos position) {
        List<Kind> result = new ArrayList<>();
        for (Kind kind : Kind.values()) {
            if (kind.weight.get() > 0 && (kind == Kind.RAIDER || level.isNight())
                    && level.getBiome(position).is(TagKey.create(Registries.BIOME, FrontierEntities.id(kind.id + "_habitat")))) {
                result.add(kind);
            }
        }
        return result;
    }

    public enum Kind {
        RAIDER("raider", FrontierEntities.RAIDER, FrontierConfig.RAIDER_WEIGHT, 2, 4),
        PROWLER("prowler", FrontierEntities.PROWLER, FrontierConfig.PROWLER_WEIGHT, 2, 3),
        SKY_WRAITH("sky_wraith", FrontierEntities.SKY_WRAITH, FrontierConfig.SKY_WRAITH_WEIGHT, 1, 2);

        private final String id;
        private final Supplier<? extends EntityType<? extends Mob>> type;
        private final net.minecraftforge.common.ForgeConfigSpec.IntValue weight;
        private final int min;
        private final int max;

        Kind(String id, Supplier<? extends EntityType<? extends Mob>> type,
                net.minecraftforge.common.ForgeConfigSpec.IntValue weight, int min, int max) {
            this.id = id;
            this.type = type;
            this.weight = weight;
            this.min = min;
            this.max = max;
        }
    }
}
