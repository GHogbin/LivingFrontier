package dev.livingfrontier.test;

import com.mojang.authlib.GameProfile;
import dev.livingfrontier.FrontierConfig;
import dev.livingfrontier.FrontierEntities;
import dev.livingfrontier.entity.TravellerEntity;
import dev.livingfrontier.entity.TraderEntity;
import dev.livingfrontier.entity.VillageGuardEntity;
import dev.livingfrontier.village.VillageLife;
import dev.livingfrontier.village.VillageLifeState;
import dev.livingfrontier.village.VillagerConversations;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("livingfrontier")
@PrefixGameTestTemplate(false)
public final class VillageLifeGameTests {
    @GameTest(template = "test/empty")
    public static void villageStateSurvivesSave(GameTestHelper helper) {
        VillageLifeState state = new VillageLifeState();
        BlockPos village = new BlockPos(120, 70, 120);
        UUID player = UUID.randomUUID();
        UUID villager = UUID.randomUUID();
        state.recordVillage(village);
        state.queueTrader(village);
        state.setErrand(player, new VillageLifeState.Errand(villager, "WHEAT", 3, true));
        VillageLifeState loaded = VillageLifeState.load(state.save(new CompoundTag()));
        helper.assertTrue(loaded.hasVillage(village.offset(40, 0, 0)), "Moved/extra bells do not duplicate residents");
        helper.assertTrue(!loaded.hasVillage(village.offset(200, 0, 0)), "Separate villages may improve");
        helper.assertTrue(loaded.getErrand(player).completed() && loaded.getErrand(player).day() == 3
                && loaded.getErrand(player).villager().equals(villager), "Daily rewards remain claimed after save");
        helper.assertTrue(loaded.pendingTradersNear(village, 1).contains(village),
                "Pending village traders survive save/load for later retry");
        helper.succeed();
    }

    @GameTest(template = "test/arena", batch = "villageLife", timeoutTicks = 80)
    public static void inhabitedVillageUpgradesOnce(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos center = helper.absolutePos(new BlockPos(14, 1, 14));
        int floor = level.getMaxBuildHeight() - 50;
        for (int x = -30; x <= 30; x++) {
            for (int z = -30; z <= 30; z++) {
                level.setBlock(new BlockPos(center.getX() + x, floor, center.getZ() + z),
                        Blocks.GRASS_BLOCK.defaultBlockState(), 2);
            }
        }
        BlockPos bell = new BlockPos(center.getX(), floor + 1, center.getZ());
        level.setBlock(bell, Blocks.BELL.defaultBlockState(), 2);
        helper.assertTrue(!VillageLife.improve(level, bell), "Empty bell is not an inhabited village");
        Villager first = EntityType.VILLAGER.create(level);
        Villager second = EntityType.VILLAGER.create(level);
        helper.assertTrue(first != null && second != null, "Vanilla villagers created");
        first.moveTo(bell.offset(0, 0, 2), 0, 0);
        second.moveTo(bell.offset(0, 0, -2), 0, 0);
        level.addFreshEntity(first);
        level.addFreshEntity(second);
        helper.assertTrue(VillageLife.improve(level, bell), "Occupied village improved");
        var area = new AABB(bell).inflate(40);
        var guards = level.getEntitiesOfClass(VillageGuardEntity.class, area);
        var travellers = level.getEntitiesOfClass(TravellerEntity.class, area);
        var traders = level.getEntitiesOfClass(TraderEntity.class, area);
        helper.assertTrue(guards.size() == 2 && travellers.size() == 1 && traders.size() == 1,
                "Two defenders, one visitor and one cargo trader added");
        helper.assertTrue(guards.stream().filter(VillageGuardEntity::isArcher).count() == 1, "Village has one archer and one melee defender");
        helper.assertTrue(guards.stream().allMatch(guard -> guard.isVillageResident() && guard.isPersistenceRequired())
                && travellers.get(0).isVillageResident(), "Village residents retain a persistent home");
        CompoundTag saved = new CompoundTag();
        guards.get(0).save(saved);
        var restored = EntityType.create(saved, level).orElseThrow();
        helper.assertTrue(restored instanceof VillageGuardEntity guard && guard.isVillageResident()
                && guard.isPersistenceRequired(), "Guard home survives save");
        helper.assertTrue(first.isAlive() && second.isAlive(), "No vanilla villagers replaced");
        guards.forEach(VillageGuardEntity::discard);
        travellers.forEach(TravellerEntity::discard);
        traders.forEach(TraderEntity::discard);
        helper.assertTrue(!VillageLife.improve(level, bell), "Missing residents are not respawned");
        helper.assertTrue(level.getEntitiesOfClass(VillageGuardEntity.class, area).isEmpty(), "Defeated guards stay absent");
        helper.succeed();
    }

    @GameTest(template = "test/arena", batch = "villageMarkets", timeoutTicks = 40)
    public static void marketsNeverOverwriteOccupiedPlots(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos origin = helper.absolutePos(new BlockPos(10, 2, 10));
        for (int x = -2; x <= 2; x++) {
            for (int z = -2; z <= 2; z++) {
                level.setBlock(origin.offset(x, -1, z), Blocks.GRASS_BLOCK.defaultBlockState(), 2);
                for (int y = 0; y <= 3; y++) {
                    level.setBlock(origin.offset(x, y, z), Blocks.AIR.defaultBlockState(), 2);
                }
            }
        }
        BlockPos existing = origin.offset(1, 0, 1);
        level.setBlock(existing, Blocks.CHEST.defaultBlockState(), 2);
        helper.assertTrue(!VillageLife.canBuildMarket(level, origin) && !VillageLife.buildMarket(level, origin),
                "Player/village buildings reject upgrades");
        helper.assertTrue(level.getBlockState(existing).is(Blocks.CHEST), "Existing chest preserved");
        level.setBlock(existing, Blocks.AIR.defaultBlockState(), 2);
        helper.assertTrue(VillageLife.buildMarket(level, origin), "Empty natural plot accepts a stall");
        helper.assertTrue(level.getBlockState(origin.offset(0, 3, 0)).is(Blocks.WHITE_WOOL), "Market canopy placed");
        helper.assertTrue(level.getBlockState(origin.offset(0, 0, -1)).is(Blocks.COMPOSTER)
                && level.getBlockState(origin.offset(1, 0, -1)).is(Blocks.FLETCHING_TABLE), "Useful vanilla job sites");
        helper.runAfterDelay(5, () -> {
            helper.assertTrue(level.getBlockState(origin.offset(0, 2, -1)).is(Blocks.LANTERN), "Hanging market lantern survives updates");
            helper.succeed();
        });
    }

    @GameTest(template = "test/empty", batch = "conversations")
    public static void errandsRewardOnceAndPreserveTrading(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        Villager villager = helper.spawn(EntityType.VILLAGER, 2, 2, 2);
        villager.setVillagerData(villager.getVillagerData().setProfession(VillagerProfession.FARMER));
        TestPlayer player = new TestPlayer(level);
        player.moveTo(villager.blockPosition().offset(1, 0, 0), 0, 0);
        var normalClick = new PlayerInteractEvent.EntityInteract(player, InteractionHand.MAIN_HAND, villager);
        VillagerConversations.interact(normalClick);
        helper.assertTrue(!normalClick.isCanceled(), "Ordinary right-click retains vanilla trading");
        player.setShiftKeyDown(true);
        var conversationClick = new PlayerInteractEvent.EntityInteract(player, InteractionHand.MAIN_HAND, villager);
        player.creative = true;
        VillagerConversations.interact(conversationClick);
        helper.assertTrue(conversationClick.isCanceled(), "Sneak main-hand click routes to conversation");
        var offhand = new PlayerInteractEvent.EntityInteract(player, InteractionHand.OFF_HAND, villager);
        VillagerConversations.interact(offhand);
        helper.assertTrue(!offhand.isCanceled(), "Conversation never processes an offhand duplicate");
        player.setShiftKeyDown(false);
        player.creative = true;
        helper.assertTrue(VillagerConversations.speak(villager, player) == VillagerConversations.Outcome.GREETING,
                "Creative conversation gives no rewards or request");
        player.creative = false;
        int reputation = villager.getPlayerReputation(player);
        helper.assertTrue(VillagerConversations.speak(villager, player) == VillagerConversations.Outcome.REQUESTED, "Farmer offers supply errand");
        helper.assertTrue(VillagerConversations.speak(villager, player) == VillagerConversations.Outcome.WAITING, "Missing supplies not rewarded");
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.WHEAT, 20));
        helper.assertTrue(VillagerConversations.speak(villager, player) == VillagerConversations.Outcome.COMPLETED, "Correct supplies accepted");
        helper.assertTrue(player.getMainHandItem().getCount() == 8 && player.getInventory().countItem(Items.EMERALD) == 2, "Exact cost and emerald reward");
        helper.assertTrue(villager.getPlayerReputation(player) > reputation, "Helping earns vanilla gossip reputation");
        helper.assertTrue(VillagerConversations.speak(villager, player) == VillagerConversations.Outcome.ALREADY_COMPLETED
                && player.getInventory().countItem(Items.EMERALD) == 2, "Repeat click cannot duplicate rewards");
        Villager other = helper.spawn(EntityType.VILLAGER, 6, 2, 2);
        player.moveTo(other.blockPosition().offset(1, 0, 0), 0, 0);
        helper.assertTrue(VillagerConversations.speak(other, player) == VillagerConversations.Outcome.ALREADY_COMPLETED,
                "Different villagers cannot bypass daily reward cap");
        boolean conversations = FrontierConfig.VILLAGER_CONVERSATIONS.get();
        FrontierConfig.VILLAGER_CONVERSATIONS.set(false);
        try {
            helper.assertTrue(VillagerConversations.speak(other, player) == VillagerConversations.Outcome.UNAVAILABLE,
                    "Conversations can be disabled");
        } finally {
            FrontierConfig.VILLAGER_CONVERSATIONS.set(conversations);
        }
        helper.succeed();
    }

    @GameTest(template = "test/arena", batch = "villageDefense", timeoutTicks = 100)
    public static void guardsDefendAgainstHostiles(GameTestHelper helper) {
        for (int x = 1; x <= 10; x++) {
            for (int z = 1; z <= 10; z++) {
                helper.setBlock(x, 1, z, Blocks.GRASS_BLOCK);
            }
        }
        VillageGuardEntity guard = helper.spawn(FrontierEntities.VILLAGE_GUARD.get(), 3, 2, 3);
        var raider = helper.spawn(FrontierEntities.RAIDER.get(), 5, 2, 3);
        raider.setNoAi(true);
        float health = raider.getHealth();
        guard.setVillageHome(guard.blockPosition());
        guard.setTarget(raider);
        helper.runAfterDelay(60, () -> {
            helper.assertTrue(raider.getHealth() < health, "Village guards actually damage hostile raiders");
            helper.succeed();
        });
    }

    private static final class TestPlayer extends ServerPlayer {
        private boolean creative;

        private TestPlayer(ServerLevel level) {
            super(level.getServer(), level, new GameProfile(UUID.randomUUID(), "village-test-player"));
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
