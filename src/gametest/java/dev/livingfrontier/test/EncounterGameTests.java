package dev.livingfrontier.test;

import dev.livingfrontier.FrontierConfig;
import dev.livingfrontier.FrontierEntities;
import dev.livingfrontier.encounter.EncounterDirector;
import dev.livingfrontier.encounter.EncounterState;
import com.mojang.authlib.GameProfile;
import java.util.UUID;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.ChunkPos;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("livingfrontier")
@PrefixGameTestTemplate(false)
public final class EncounterGameTests {
    @GameTest(template = "test/empty")
    public static void cooldownsSurviveSave(GameTestHelper helper) {
        UUID player = UUID.randomUUID();
        EncounterState state = new EncounterState();
        helper.assertTrue(!state.isDue(player, 100), "New players have no immediate encounter");
        state.schedule(player, 10000, new BlockPos(10, 70, 10));
        EncounterState loaded = EncounterState.load(state.save(new CompoundTag()));
        helper.assertTrue(loaded.contains(player) && !loaded.isDue(player, 9999) && loaded.isDue(player, 10000), "Cooldown saved");
        helper.assertTrue(!loaded.hasTravelled(player, new BlockPos(20, 70, 20)), "Standing near home is not exploring");
        helper.assertTrue(loaded.hasTravelled(player, new BlockPos(60, 70, 10)), "Travel qualifies");
        RandomSource random = RandomSource.create(31);
        for (int index = 0; index < 200; index++) {
            long delay = EncounterDirector.nextDelay(random);
            helper.assertTrue(delay >= 7200 && delay <= 14400, "Default cooldown is six to twelve minutes");
        }
        helper.succeed();
    }

    @GameTest(template = "test/encounters", batch = "encounters", timeoutTicks = 300)
    public static void safeBoundedRaiderEncounter(GameTestHelper helper) {
        var level = helper.getLevel();
        var player = new TestPlayer(level);
        BlockPos sceneCenter = helper.absolutePos(new BlockPos(64, 2, 64));
        int floor = level.getMaxBuildHeight() - 32;
        var forest = level.registryAccess().registryOrThrow(net.minecraft.core.registries.Registries.BIOME)
                .getHolderOrThrow(net.minecraft.world.level.biome.Biomes.FOREST);
        int chunkX = sceneCenter.getX() >> 4;
        int chunkZ = sceneCenter.getZ() >> 4;
        List<ChunkPos> fixtureTickets = new ArrayList<>();
        for (int x = chunkX - 4; x <= chunkX + 4; x++) {
            for (int z = chunkZ - 4; z <= chunkZ + 4; z++) {
                if (!level.getForcedChunks().contains(ChunkPos.asLong(x, z))) {
                    level.setChunkForced(x, z, true);
                    fixtureTickets.add(new ChunkPos(x, z));
                }
                var chunk = level.getChunk(x, z);
                chunk.fillBiomesFromNoise((qx, qy, qz, sampler) -> forest, level.getChunkSource().randomState().sampler());
            }
        }
        for (int x = sceneCenter.getX() - 52; x <= sceneCenter.getX() + 52; x++) {
            for (int z = sceneCenter.getZ() - 52; z <= sceneCenter.getZ() + 52; z++) {
                level.setBlock(new BlockPos(x, floor, z), net.minecraft.world.level.block.Blocks.GRASS_BLOCK.defaultBlockState(), 2);
            }
        }
        player.moveTo(new BlockPos(sceneCenter.getX(), floor + 1, sceneCenter.getZ()), 0, 0);
        long previousTime = level.getDayTime();
        boolean previousSpawning = level.getGameRules().getBoolean(GameRules.RULE_DOMOBSPAWNING);
        Difficulty previousDifficulty = level.getDifficulty();
        level.getServer().setDifficulty(Difficulty.NORMAL, true);
        level.setDayTime(6000);
        level.getGameRules().getRule(GameRules.RULE_DOMOBSPAWNING).set(true, level.getServer());
        boolean waitingForCombat = false;
        try {
            helper.assertTrue(EncounterDirector.canStart(level, player), "Outdoor survival player is eligible: pos="
                    + player.blockPosition() + ", height=" + level.getHeight(
                    net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                    player.blockPosition().getX(), player.blockPosition().getZ()) + ", sky=" + level.canSeeSky(player.blockPosition())
                    + ", alive=" + player.isAlive() + ", spectator=" + player.isSpectator()
                    + ", village=" + level.isCloseToVillage(player.blockPosition(), 2)
                    + ", guards=" + level.getEntitiesOfClass(dev.livingfrontier.entity.RaiderEntity.class,
                    player.getBoundingBox().inflate(96), dev.livingfrontier.entity.RaiderEntity::isGuard).size());
            helper.assertTrue(!EncounterDirector.safePosition(level, player.blockPosition(), player), "No spawns on top of players");
            int spawned = 0;
            for (int attempt = 0; attempt < 10 && spawned == 0; attempt++) {
                spawned = EncounterDirector.attempt(level, player, EncounterDirector.Kind.RAIDER);
            }
            int monsters = 0;
            int tagged = 0;
            for (var entity : level.getAllEntities()) {
                if (entity instanceof net.minecraft.world.entity.monster.Monster && entity.isAlive()) {
                    monsters++;
                }
                if (entity.getTags().contains(EncounterDirector.ENCOUNTER_TAG)) {
                    tagged++;
                }
            }
            helper.assertTrue(spawned >= 2 && spawned <= 4, "Bounded raider band created: spawned=" + spawned
                    + ", biome=" + level.getBiome(player.blockPosition()).unwrapKey().map(key -> key.location().toString())
                    .orElse("unknown") + ", monsters=" + monsters + ", tagged=" + tagged);
            var hostiles = level.getEntitiesOfClass(Mob.class, player.getBoundingBox().inflate(96),
                    mob -> mob.getTags().contains(EncounterDirector.ENCOUNTER_TAG));
            helper.assertTrue(hostiles.size() == spawned, "Every encounter mob is tracked");
            helper.assertTrue(hostiles.stream().allMatch(mob -> mob.distanceToSqr(player) >= 24 * 24), "Spawn safety radius");
            player.creative = true;
            helper.assertTrue(!EncounterDirector.canStart(level, player)
                    && EncounterDirector.attempt(level, player, EncounterDirector.Kind.RAIDER) == 0, "Creative is protected");
            player.creative = false;
            boolean enabled = FrontierConfig.ENCOUNTERS_ENABLED.get();
            FrontierConfig.ENCOUNTERS_ENABLED.set(false);
            try {
                helper.assertTrue(EncounterDirector.attempt(level, player, EncounterDirector.Kind.RAIDER) == 0, "Encounters can be disabled");
            } finally {
                FrontierConfig.ENCOUNTERS_ENABLED.set(enabled);
            }
            int oldCap = FrontierConfig.ENCOUNTER_LOCAL_CAP.get();
            FrontierConfig.ENCOUNTER_LOCAL_CAP.set(spawned);
            try {
                helper.assertTrue(EncounterDirector.attempt(level, player, EncounterDirector.Kind.RAIDER) == 0, "Local mob cap blocks another band");
            } finally {
                FrontierConfig.ENCOUNTER_LOCAL_CAP.set(oldCap);
            }
            var guard = FrontierEntities.RAIDER.get().spawn(level, player.blockPosition().offset(4, 0, 0),
                    net.minecraft.world.entity.MobSpawnType.STRUCTURE);
            helper.assertTrue(guard != null && guard.isGuard(), "Structure-spawned guard has a home");
            helper.assertTrue(!EncounterDirector.canStart(level, player), "Guarded bases suppress encounters");
            guard.discard();
            hostiles.forEach(Mob::discard);
            level.setDayTime(18000);
            level.updateSkyBrightness();
            int flyers = 0;
            for (int attempt = 0; attempt < 10 && flyers == 0; attempt++) {
                flyers = EncounterDirector.attempt(level, player, EncounterDirector.Kind.SKY_WRAITH);
            }
            helper.assertTrue(flyers >= 1 && flyers <= 2, "Night encounter creates one or two flying hostiles");
            var wraiths = level.getEntitiesOfClass(dev.livingfrontier.entity.SkyWraithEntity.class,
                    player.getBoundingBox().inflate(96));
            helper.assertTrue(wraiths.size() == flyers && wraiths.stream().allMatch(
                    mob -> mob.isNoGravity() && mob.getY() > player.getY() + 4), "Flying encounters start above ground");
            int pack = 0;
            for (int attempt = 0; attempt < 10 && pack == 0; attempt++) {
                pack = EncounterDirector.attempt(level, player, EncounterDirector.Kind.PROWLER);
            }
            helper.assertTrue(pack >= 2 && pack <= 3, "Night encounter creates a prowler pack");
            level.getEntitiesOfClass(dev.livingfrontier.entity.ProwlerEntity.class, player.getBoundingBox().inflate(96))
                    .forEach(Mob::discard);
            var target = net.minecraft.world.entity.EntityType.COW.create(level);
            helper.assertTrue(target != null, "Swoop target created");
            target.setNoAi(true);
            target.moveTo(player.getX(), player.getY(), player.getZ(), 0, 0);
            level.addFreshEntity(target);
            float initialHealth = target.getHealth();
            wraiths.forEach(mob -> mob.setTarget(target));
            helper.runAfterDelay(220, () -> {
                try {
                    helper.assertTrue(target.getHealth() < initialHealth, "Flying wraith swoops deal melee damage: "
                            + wraiths.stream().map(mob -> "ticks=" + mob.tickCount + ", alive=" + mob.isAlive()
                            + ", diving=" + mob.isDiving() + ", target=" + (mob.getTarget() == target)
                            + ", distance=" + mob.distanceTo(target) + ", motion=" + mob.getDeltaMovement()).toList());
                    helper.succeed();
                } finally {
                    wraiths.forEach(Mob::discard);
                    target.discard();
                    fixtureTickets.forEach(chunk -> level.setChunkForced(chunk.x, chunk.z, false));
                }
            });
            waitingForCombat = true;
            player.discard();
        } finally {
            if (!waitingForCombat) {
                fixtureTickets.forEach(chunk -> level.setChunkForced(chunk.x, chunk.z, false));
            }
            level.setDayTime(previousTime);
            level.updateSkyBrightness();
            level.getServer().setDifficulty(previousDifficulty, true);
            level.getGameRules().getRule(GameRules.RULE_DOMOBSPAWNING).set(previousSpawning, level.getServer());
        }
    }

    private static final class TestPlayer extends ServerPlayer {
        private boolean creative;

        private TestPlayer(ServerLevel level) {
            super(level.getServer(), level, new GameProfile(UUID.randomUUID(), "frontier-test-player"));
        }

        @Override
        public boolean isCreative() {
            return creative;
        }

        @Override
        public void displayClientMessage(Component message, boolean actionBar) {
        }
    }
}
