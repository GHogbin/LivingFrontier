package dev.livingfrontier.test;

import dev.livingfrontier.FrontierEntities;
import dev.livingfrontier.entity.GuardArrowEntity;
import dev.livingfrontier.entity.RaiderEntity;
import dev.livingfrontier.entity.VillageGuardEntity;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("livingfrontier")
@PrefixGameTestTemplate(false)
public final class GuardCombatGameTests {
    @GameTest(template = "test/empty")
    public static void rolesAndArrowProtectionSurviveSave(GameTestHelper helper) {
        var level = helper.getLevel();
        var raider = helper.spawn(FrontierEntities.RAIDER.get(), 2, 2, 2);
        raider.setArcher(true);
        helper.assertTrue(raider.isArcher() && raider.isHoldingBow()
                && raider.getAttributeValue(Attributes.ATTACK_DAMAGE) == 4, "Archer role does not inflate melee damage");
        CompoundTag saved = new CompoundTag();
        raider.save(saved);
        var loaded = EntityType.create(saved, level).orElseThrow();
        helper.assertTrue(loaded instanceof RaiderEntity restored && restored.isArcher() && restored.isHoldingBow(),
                "Raider role/loadout survive save");
        var guard = helper.spawn(FrontierEntities.VILLAGE_GUARD.get(), 4, 2, 2);
        guard.setArcher(true);
        CompoundTag guardSave = new CompoundTag();
        guard.save(guardSave);
        var guardLoaded = EntityType.create(guardSave, level).orElseThrow();
        helper.assertTrue(guardLoaded instanceof VillageGuardEntity restored && restored.isArcher(), "Friendly archer role saved");
        var boss = helper.spawn(FrontierEntities.WARLORD.get(), 6, 2, 2);
        boss.setArcher(true);
        helper.assertTrue(!boss.isArcher() && !boss.isHoldingBow(), "Warlord cannot become an archer");
        var villager = helper.spawn(EntityType.VILLAGER, 4, 2, 5);
        GuardArrowEntity friendly = FrontierEntities.GUARD_ARROW.get().create(level);
        helper.assertTrue(friendly != null, "Registered projectile can be created");
        friendly.configure(guard, raider);
        var firefly = helper.spawn(FrontierEntities.FIREFLY.get(), 2, 4, 5);
        helper.assertTrue(friendly.protects(villager) && friendly.protects(guard) && !friendly.protects(raider),
                "Village arrows protect villagers/allied guards but can hit hostile raiders");
        helper.assertTrue(friendly.protects(firefly), "Ambient wildlife is also protected");
        CompoundTag arrowSave = new CompoundTag();
        friendly.save(arrowSave);
        var restoredArrow = EntityType.create(arrowSave, level).orElseThrow();
        helper.assertTrue(restoredArrow instanceof GuardArrowEntity arrow && arrow.protects(villager)
                && arrow.pickup == net.minecraft.world.entity.projectile.AbstractArrow.Pickup.DISALLOWED,
                "Arrow faction safety and pickup restriction survive reload");
        GuardArrowEntity hostile = FrontierEntities.GUARD_ARROW.get().create(level);
        helper.assertTrue(hostile != null, "Hostile arrow created");
        hostile.configure(raider, guard);
        helper.assertTrue(hostile.protects(raider) && hostile.protects(boss) && !hostile.protects(guard),
                "Raider arrows never damage their own faction");
        helper.succeed();
    }

    @GameTest(template = "test/arena", batch = "guardCombat", timeoutTicks = 200)
    public static void villageArchersFireAndFightUpClose(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        List<ChunkPos> tickets = arena(helper);
        var guard = helper.spawn(FrontierEntities.VILLAGE_GUARD.get(), 3, 2, 3);
        var enemy = helper.spawn(FrontierEntities.RAIDER.get(), 17, 2, 3);
        guard.setArcher(true);
        enemy.setNoAi(true);
        enemy.setArcher(false);
        guard.setVillageHome(guard.blockPosition());
        guard.setTarget(enemy);
        float enemyHealth = enemy.getHealth();
        helper.runAfterDelay(85, () -> {
            boolean waitingForMelee = false;
            try {
                helper.assertTrue(enemy.getHealth() < enemyHealth, "Village archer arrows actually damage distant hostiles");
                helper.assertTrue(guard.isHoldingBow(), "Archer holds bow while fighting at range");
                enemy.moveTo(guard.getX() + 1, guard.getY(), guard.getZ(), 0, 0);
                float closeHealth = enemy.getHealth();
                helper.runAfterDelay(35, () -> {
                    try {
                        helper.assertTrue(enemy.getHealth() < closeHealth && !guard.isHoldingBow(), "Archer switches to melee when rushed");
                        helper.succeed();
                    } finally {
                        release(level, tickets);
                    }
                });
                waitingForMelee = true;
            } finally {
                if (!waitingForMelee) {
                    release(level, tickets);
                }
            }
        });
    }

    @GameTest(template = "test/arena", batch = "raiderArchery", timeoutTicks = 120)
    public static void hostileArchersFireActualProjectiles(GameTestHelper helper) {
        var level = helper.getLevel();
        List<ChunkPos> tickets = arena(helper);
        var raider = helper.spawn(FrontierEntities.RAIDER.get(), 3, 2, 3);
        var target = helper.spawn(EntityType.COW, 17, 2, 3);
        raider.setArcher(true);
        target.setNoAi(true);
        raider.setTarget(target);
        float health = target.getHealth();
        helper.runAfterDelay(85, () -> {
            try {
                helper.assertTrue(target.getHealth() < health, "Hostile archer arrows damage distant targets");
                helper.succeed();
            } finally {
                release(level, tickets);
            }
        });
    }

    @GameTest(template = "test/arena", batch = "guardFriendlyFire", timeoutTicks = 120)
    public static void villageArcherHoldsFireBehindVillagers(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        List<ChunkPos> tickets = arena(helper);
        var guard = helper.spawn(FrontierEntities.VILLAGE_GUARD.get(), 3, 2, 3);
        var enemy = helper.spawn(FrontierEntities.RAIDER.get(), 17, 2, 3);
        var villager = helper.spawn(EntityType.VILLAGER, 10, 2, 3);
        guard.setArcher(true);
        guard.setTarget(enemy);
        enemy.setNoAi(true);
        villager.setNoAi(true);
        float villagerHealth = villager.getHealth();
        float enemyHealth = enemy.getHealth();
        helper.runAfterDelay(75, () -> {
            try {
                helper.assertTrue(villager.getHealth() == villagerHealth && enemy.getHealth() == enemyHealth,
                        "Archer holds fire when an allied villager blocks the line");
                helper.assertTrue(level.getEntitiesOfClass(GuardArrowEntity.class, new AABB(guard.blockPosition()).inflate(24)).isEmpty(),
                        "Blocked firing lines create no arrows");
                helper.succeed();
            } finally {
                release(level, tickets);
            }
        });
    }

    private static List<ChunkPos> arena(GameTestHelper helper) {
        var level = helper.getLevel();
        List<ChunkPos> tickets = new ArrayList<>();
        BlockPos low = helper.absolutePos(new BlockPos(1, 1, 1));
        BlockPos high = helper.absolutePos(new BlockPos(22, 5, 15));
        for (int x = low.getX() >> 4; x <= high.getX() >> 4; x++) {
            for (int z = low.getZ() >> 4; z <= high.getZ() >> 4; z++) {
                if (!level.getForcedChunks().contains(ChunkPos.asLong(x, z))) {
                    level.setChunkForced(x, z, true);
                    tickets.add(new ChunkPos(x, z));
                }
            }
        }
        for (int x = 1; x <= 22; x++) {
            for (int z = 1; z <= 15; z++) {
                helper.setBlock(x, 1, z, Blocks.GRASS_BLOCK);
                for (int y = 2; y <= 5; y++) {
                    helper.setBlock(x, y, z, Blocks.AIR);
                }
            }
        }
        return tickets;
    }

    private static void release(ServerLevel level, List<ChunkPos> tickets) {
        tickets.forEach(chunk -> level.setChunkForced(chunk.x, chunk.z, false));
    }
}
