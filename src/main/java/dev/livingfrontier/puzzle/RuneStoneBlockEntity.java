package dev.livingfrontier.puzzle;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public final class RuneStoneBlockEntity extends BlockEntity {
    private boolean bound;
    private int room = -1;
    private int gateForward;
    private int gateRight;
    private int gateUp;

    public RuneStoneBlockEntity(BlockPos position, BlockState state) {
        super(PuzzleBlocks.RUNE_ENTITY.get(), position, state);
    }

    public BlockPos gatePosition() {
        return RuneBlock.localOffset(worldPosition, getBlockState(), gateForward, gateRight, gateUp);
    }

    public void press(RuneSymbol symbol, Player player) {
        if (!bound || room < 0 || room >= 3 || level == null || Math.abs(gateForward) > 64 || Math.abs(gateRight) > 64
                || Math.abs(gateUp) > 32 || !level.hasChunkAt(gatePosition())
                || !(level.getBlockEntity(gatePosition()) instanceof SealControllerBlockEntity gate)) {
            player.displayClientMessage(Component.translatable("message.livingfrontier.puzzle.unbound"), true);
            return;
        }
        gate.press(room, symbol, player);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        bound = tag.getBoolean("Bound");
        room = tag.getInt("Room");
        gateForward = tag.getInt("GateForward");
        gateRight = tag.getInt("GateRight");
        gateUp = tag.getInt("GateUp");
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putBoolean("Bound", bound);
        tag.putInt("Room", room);
        tag.putInt("GateForward", gateForward);
        tag.putInt("GateRight", gateRight);
        tag.putInt("GateUp", gateUp);
    }
}
