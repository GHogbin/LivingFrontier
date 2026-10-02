package dev.livingfrontier.puzzle;

import dev.livingfrontier.entity.WarlordEntity;
import java.util.Arrays;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

public final class SealControllerBlockEntity extends BlockEntity {
    private static final RuneSymbol[][] PATTERNS = {
            {RuneSymbol.SUN, RuneSymbol.LEAF, RuneSymbol.WAVE},
            {RuneSymbol.WAVE, RuneSymbol.SUN, RuneSymbol.LEAF},
            {RuneSymbol.LEAF, RuneSymbol.WAVE, RuneSymbol.SUN}
    };
    private final int[] progress = new int[3];
    private int completedMask;
    private boolean bound;
    private boolean opened;
    private int doorForward;
    private int doorRight;
    private int doorUp;
    private int doorWidth;
    private int doorHeight;
    private int bossForward;
    private int bossRight;
    private int bossUp;

    public SealControllerBlockEntity(BlockPos position, BlockState state) {
        super(PuzzleBlocks.CONTROLLER_ENTITY.get(), position, state);
    }

    public int completedSeals() {
        return Integer.bitCount(completedMask);
    }

    public boolean isOpened() {
        return opened;
    }

    public int roomProgress(int room) {
        return progress[room];
    }

    public void press(int room, RuneSymbol symbol, Player player) {
        if (!(level instanceof ServerLevel server) || !validBinding() || room < 0 || room >= 3) {
            player.displayClientMessage(Component.translatable("message.livingfrontier.puzzle.unbound"), true);
            return;
        }
        if ((completedMask & (1 << room)) != 0) {
            player.displayClientMessage(Component.translatable("message.livingfrontier.puzzle.already_solved", room + 1), true);
            return;
        }
        if (PATTERNS[room][progress[room]] != symbol) {
            progress[room] = 0;
            player.displayClientMessage(Component.translatable("message.livingfrontier.puzzle.wrong", room + 1), true);
            server.playSound(null, worldPosition, SoundEvents.NOTE_BLOCK_BASS.value(), SoundSource.BLOCKS, 0.8F, 0.6F);
        } else if (++progress[room] == PATTERNS[room].length) {
            completedMask |= 1 << room;
            player.displayClientMessage(Component.translatable("message.livingfrontier.puzzle.seal_complete", room + 1, completedSeals()), true);
            server.playSound(null, worldPosition, SoundEvents.BEACON_ACTIVATE, SoundSource.BLOCKS, 0.7F, 1.2F);
        } else {
            player.displayClientMessage(Component.translatable("message.livingfrontier.puzzle.progress", room + 1, progress[room]), true);
        }
        setChanged();
        if (completedMask == 7) {
            openIfLoaded(server);
        }
    }

    public void resetProgress(Player player) {
        Arrays.fill(progress, 0);
        setChanged();
        player.displayClientMessage(Component.translatable("message.livingfrontier.puzzle.reset", completedSeals()), true);
    }

    public void describe(Player player) {
        player.displayClientMessage(Component.translatable("message.livingfrontier.puzzle.status", completedSeals()), false);
    }

    public static void tick(Level level, BlockPos position, BlockState state, SealControllerBlockEntity gate) {
        if (level instanceof ServerLevel server && level.getGameTime() % 20 == 0 && gate.completedMask == 7) {
            gate.openIfLoaded(server);
        }
    }

    private boolean validBinding() {
        return bound && doorWidth > 0 && doorWidth <= 7 && doorHeight > 0 && doorHeight <= 8
                && Math.abs(doorForward) <= 64 && Math.abs(doorRight) <= 64 && Math.abs(doorUp) <= 32
                && Math.abs(bossForward) <= 64 && Math.abs(bossRight) <= 64 && Math.abs(bossUp) <= 32;
    }

    private void openIfLoaded(ServerLevel server) {
        if (!validBinding()) {
            return;
        }
        if (!opened) {
            for (int x = 0; x < doorWidth; x++) {
                BlockPos position = RuneBlock.localOffset(worldPosition, getBlockState(), doorForward, doorRight + x, doorUp);
                if (!server.hasChunkAt(position)) {
                    return;
                }
            }
            for (int x = 0; x < doorWidth; x++) {
                for (int y = 0; y < doorHeight; y++) {
                    BlockPos position = RuneBlock.localOffset(worldPosition, getBlockState(), doorForward, doorRight + x, doorUp + y);
                    if (server.getBlockState(position).is(PuzzleBlocks.SEAL_BARRIER.get())) {
                        server.setBlock(position, Blocks.AIR.defaultBlockState(), 3);
                    }
                }
            }
            opened = true;
            setChanged();
            server.playSound(null, worldPosition, SoundEvents.IRON_DOOR_OPEN, SoundSource.BLOCKS, 1.5F, 0.6F);
        }
        BlockPos bossPosition = RuneBlock.localOffset(worldPosition, getBlockState(), bossForward, bossRight, bossUp);
        if (server.hasChunkAt(bossPosition)) {
            for (WarlordEntity boss : server.getEntitiesOfClass(WarlordEntity.class, new AABB(bossPosition).inflate(6),
                    WarlordEntity::isFrontierSealed)) {
                boss.awakenFromSeals();
            }
        }
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        bound = tag.getBoolean("Bound");
        completedMask = tag.getInt("CompletedMask") & 7;
        opened = tag.getBoolean("Opened") && completedMask == 7;
        int[] saved = tag.getIntArray("Progress");
        for (int room = 0; room < 3; room++) {
            progress[room] = room < saved.length ? Math.max(0, Math.min(2, saved[room])) : 0;
        }
        doorForward = tag.getInt("DoorForward");
        doorRight = tag.getInt("DoorRight");
        doorUp = tag.getInt("DoorUp");
        doorWidth = tag.getInt("DoorWidth");
        doorHeight = tag.getInt("DoorHeight");
        bossForward = tag.getInt("BossForward");
        bossRight = tag.getInt("BossRight");
        bossUp = tag.getInt("BossUp");
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putBoolean("Bound", bound);
        tag.putInt("CompletedMask", completedMask);
        tag.putBoolean("Opened", opened);
        tag.putIntArray("Progress", progress);
        tag.putInt("DoorForward", doorForward);
        tag.putInt("DoorRight", doorRight);
        tag.putInt("DoorUp", doorUp);
        tag.putInt("DoorWidth", doorWidth);
        tag.putInt("DoorHeight", doorHeight);
        tag.putInt("BossForward", bossForward);
        tag.putInt("BossRight", bossRight);
        tag.putInt("BossUp", bossUp);
    }
}
