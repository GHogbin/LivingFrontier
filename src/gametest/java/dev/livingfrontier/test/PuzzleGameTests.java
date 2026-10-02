package dev.livingfrontier.test;

import com.mojang.authlib.GameProfile;
import dev.livingfrontier.FrontierEntities;
import dev.livingfrontier.entity.WarlordEntity;
import dev.livingfrontier.puzzle.PuzzleBlocks;
import dev.livingfrontier.puzzle.RuneBlock;
import dev.livingfrontier.puzzle.RuneStoneBlockEntity;
import dev.livingfrontier.puzzle.RuneSymbol;
import dev.livingfrontier.puzzle.SealControllerBlockEntity;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("livingfrontier")
@PrefixGameTestTemplate(false)
public final class PuzzleGameTests {
    @GameTest(template = "test/empty")
    public static void localBindingsSupportEveryRotationAndMirror(GameTestHelper helper) {
        BlockPos rune = new BlockPos(6, 1, 46);
        BlockPos gate = SiteLayouts.pos(SiteLayouts.KEEP.gateController());
        BlockPos door = SiteLayouts.pos(SiteLayouts.KEEP.gateOrigin());
        var state = PuzzleBlocks.SUN_RUNE.get().defaultBlockState();
        for (Mirror mirror : Mirror.values()) {
            for (Rotation rotation : Rotation.values()) {
                var transformedState = state.mirror(mirror).rotate(rotation);
                BlockPos transformedRune = StructureTemplate.transform(rune, mirror, rotation, BlockPos.ZERO);
                BlockPos transformedGate = StructureTemplate.transform(gate, mirror, rotation, BlockPos.ZERO);
                BlockPos actual = RuneBlock.localOffset(transformedRune, transformedState,
                        rune.getZ() - gate.getZ(), gate.getX() - rune.getX(), gate.getY() - rune.getY());
                helper.assertTrue(actual.equals(transformedGate), "Rune binding survives " + rotation + "/" + mirror);
                for (int x = 0; x < SiteLayouts.KEEP.gateWidth(); x++) {
                    BlockPos transformedDoorCell = StructureTemplate.transform(door.offset(x, 0, 0), mirror, rotation, BlockPos.ZERO);
                    BlockPos calculatedDoor = RuneBlock.localOffset(transformedGate, transformedState,
                            gate.getZ() - door.getZ(), door.getX() - gate.getX() + x, door.getY() - gate.getY());
                    helper.assertTrue(calculatedDoor.equals(transformedDoorCell), "Mirrored door width keeps its local origin");
                }
            }
        }
        helper.succeed();
    }

    @GameTest(template = "test/site_arena", batch = "puzzleKeep", timeoutTicks = 100)
    public static void wrongRunesResetAndAllSealsAwakenBoss(GameTestHelper helper) {
        verifyPuzzle(helper, Rotation.NONE, Mirror.NONE);
        helper.succeed();
    }

    @GameTest(template = "test/site_arena", batch = "mirroredPuzzleKeep", timeoutTicks = 100)
    public static void rotatedMirroredTemplatesBindAndUnlock(GameTestHelper helper) {
        verifyPuzzle(helper, Rotation.CLOCKWISE_90, Mirror.FRONT_BACK);
        helper.succeed();
    }

    private static void verifyPuzzle(GameTestHelper helper, Rotation rotation, Mirror mirror) {
        ServerLevel level = helper.getLevel();
        BlockPos origin = helper.absolutePos(new BlockPos(64, 1, 64));
        StructurePlaceSettings settings = new StructurePlaceSettings().setRotation(rotation).setMirror(mirror)
                .setIgnoreEntities(false).setFinalizeEntities(true).setKnownShape(true);
        var template = level.getStructureManager().get(FrontierEntities.id("ruined_keep")).orElseThrow();
        settings.setBoundingBox(template.getBoundingBox(settings, origin));
        helper.assertTrue(template.placeInWorld(level, origin, origin, settings, level.getRandom(), 2), "Puzzle keep placed");
        BlockPos gatePosition = transform(SiteLayouts.KEEP.gateController(), origin, settings);
        BlockEntity blockEntity = level.getBlockEntity(gatePosition);
        helper.assertTrue(blockEntity instanceof SealControllerBlockEntity, "Controller decoded from template NBT");
        SealControllerBlockEntity gate = (SealControllerBlockEntity) blockEntity;
        var bossPosition = transform(SiteLayouts.KEEP.bossPosition(), origin, settings);
        List<WarlordEntity> bosses = level.getEntitiesOfClass(WarlordEntity.class, new AABB(bossPosition).inflate(3));
        helper.assertTrue(bosses.size() == 1, "One dormant boss");
        WarlordEntity boss = bosses.get(0);
        helper.assertTrue(boss.isFrontierSealed() && boss.isNoAi()
                && !boss.hurt(boss.damageSources().generic(), 8) && boss.getHealth() == 160,
                "Mining around the door cannot damage the sealed boss");
        TestPlayer first = new TestPlayer(level);
        TestPlayer second = new TestPlayer(level);
        press(helper, settings, origin, 0, RuneSymbol.SUN, first, gatePosition);
        helper.assertTrue(gate.roomProgress(0) == 1, "First correct rune advances");
        press(helper, settings, origin, 0, RuneSymbol.SUN, first, gatePosition);
        helper.assertTrue(gate.roomProgress(0) == 0 && gate.completedSeals() == 0, "Wrong rune resets only its current sequence");
        solve(helper, settings, origin, 0, first, gatePosition, RuneSymbol.SUN, RuneSymbol.LEAF, RuneSymbol.WAVE);
        helper.assertTrue(gate.completedSeals() == 1 && !gate.isOpened(), "One seal cannot open the final door");
        press(helper, settings, origin, 1, RuneSymbol.WAVE, second, gatePosition);
        gate.load(gate.saveWithFullMetadata());
        helper.assertTrue(gate.completedSeals() == 1 && gate.roomProgress(1) == 1, "Completed seals and partial progress survive save");
        gate.resetProgress(second);
        helper.assertTrue(gate.completedSeals() == 1 && gate.roomProgress(1) == 0, "Reset preserves solved checkpoints");
        solve(helper, settings, origin, 1, second, gatePosition, RuneSymbol.WAVE, RuneSymbol.SUN, RuneSymbol.LEAF);
        assertDoor(helper, origin, settings, true);
        solve(helper, settings, origin, 2, first, gatePosition, RuneSymbol.LEAF, RuneSymbol.WAVE, RuneSymbol.SUN);
        helper.assertTrue(gate.completedSeals() == 3 && gate.isOpened(), "Three rooms share progression across players");
        assertDoor(helper, origin, settings, false);
        helper.assertTrue(!boss.isFrontierSealed() && !boss.isNoAi(), "Existing boss awakens, no duplicate is created");
        helper.assertTrue(boss.hurt(boss.damageSources().generic(), 4) && boss.getHealth() < 160, "Awakened boss can be fought");
        gate.load(gate.saveWithFullMetadata());
        helper.assertTrue(gate.isOpened() && gate.completedSeals() == 3, "Gate stays unlocked after save");
        int bossCount = level.getEntitiesOfClass(WarlordEntity.class, new AABB(bossPosition).inflate(6)).size();
        press(helper, settings, origin, 0, RuneSymbol.SUN, second, gatePosition);
        helper.assertTrue(level.getEntitiesOfClass(WarlordEntity.class, new AABB(bossPosition).inflate(6)).size() == bossCount,
                "Repeated activation never respawns bosses");
    }

    private static BlockPos transform(int[] local, BlockPos origin, StructurePlaceSettings settings) {
        return StructureTemplate.calculateRelativePosition(settings, SiteLayouts.pos(local)).offset(origin);
    }

    private static void press(GameTestHelper helper, StructurePlaceSettings settings, BlockPos origin, int room,
            RuneSymbol symbol, ServerPlayer player, BlockPos gatePosition) {
        SiteLayouts.Rune input = java.util.Arrays.stream(SiteLayouts.KEEP.rooms()[room].runes())
                .filter(rune -> rune.symbol().equalsIgnoreCase(symbol.name())).findFirst().orElseThrow();
        BlockPos position = transform(input.pos(), origin, settings);
        BlockEntity blockEntity = helper.getLevel().getBlockEntity(position);
        helper.assertTrue(blockEntity instanceof RuneStoneBlockEntity, "Rune decoded from NBT");
        RuneStoneBlockEntity rune = (RuneStoneBlockEntity) blockEntity;
        helper.assertTrue(rune.gatePosition().equals(gatePosition), "Rune binds its own transformed controller");
        rune.press(symbol, player);
    }

    private static void solve(GameTestHelper helper, StructurePlaceSettings settings, BlockPos origin, int room,
            ServerPlayer player, BlockPos gatePosition, RuneSymbol... symbols) {
        for (RuneSymbol symbol : symbols) {
            press(helper, settings, origin, room, symbol, player, gatePosition);
        }
    }

    private static void assertDoor(GameTestHelper helper, BlockPos origin, StructurePlaceSettings settings, boolean sealed) {
        for (int x = 0; x < SiteLayouts.KEEP.gateWidth(); x++) {
            for (int y = 0; y < SiteLayouts.KEEP.gateHeight(); y++) {
                BlockPos local = SiteLayouts.pos(SiteLayouts.KEEP.gateOrigin()).offset(x, y, 0);
                BlockPos position = StructureTemplate.calculateRelativePosition(settings, local).offset(origin);
                helper.assertTrue(helper.getLevel().getBlockState(position).is(PuzzleBlocks.SEAL_BARRIER.get()) == sealed,
                        "Physical gate matches seal completion");
            }
        }
    }

    private static final class TestPlayer extends ServerPlayer {
        private TestPlayer(ServerLevel level) {
            super(level.getServer(), level, new GameProfile(UUID.randomUUID(), "puzzle-test-player"));
        }

        @Override
        public void displayClientMessage(Component message, boolean actionBar) {
        }
    }
}
