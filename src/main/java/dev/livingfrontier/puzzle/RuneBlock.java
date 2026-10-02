package dev.livingfrontier.puzzle;

import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;

public final class RuneBlock extends BaseEntityBlock {
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final BooleanProperty MIRRORED = BooleanProperty.create("mirrored");
    private final @Nullable RuneSymbol symbol;
    private final boolean controller;

    public RuneBlock(@Nullable RuneSymbol symbol, boolean controller) {
        super(BlockBehaviour.Properties.of().strength(-1, 3600000).sound(SoundType.STONE).noLootTable());
        this.symbol = symbol;
        this.controller = controller;
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(MIRRORED, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, MIRRORED);
    }

    @Override
    public BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    public BlockState mirror(BlockState state, Mirror mirror) {
        if (mirror == Mirror.NONE) {
            return state;
        }
        return rotate(state, mirror.getRotation(state.getValue(FACING))).setValue(MIRRORED, !state.getValue(MIRRORED));
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos position, BlockState state) {
        return controller ? new SealControllerBlockEntity(position, state) : new RuneStoneBlockEntity(position, state);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return controller && !level.isClientSide
                ? createTickerHelper(type, PuzzleBlocks.CONTROLLER_ENTITY.get(), SealControllerBlockEntity::tick) : null;
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos position, Player player,
            InteractionHand hand, BlockHitResult hit) {
        if (hand != InteractionHand.MAIN_HAND) {
            return InteractionResult.PASS;
        }
        if (!level.isClientSide) {
            BlockEntity blockEntity = level.getBlockEntity(position);
            if (blockEntity instanceof SealControllerBlockEntity gate) {
                if (player.isShiftKeyDown()) {
                    gate.resetProgress(player);
                } else {
                    gate.describe(player);
                }
            } else if (blockEntity instanceof RuneStoneBlockEntity rune && symbol != null) {
                rune.press(symbol, player);
            } else {
                player.displayClientMessage(Component.translatable("message.livingfrontier.puzzle.unbound"), true);
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    public static BlockPos localOffset(BlockPos origin, BlockState state, int forward, int right, int up) {
        Direction facing = state.getValue(FACING);
        Direction lateral = state.getValue(MIRRORED) ? facing.getCounterClockWise() : facing.getClockWise();
        return origin.relative(facing, forward).relative(lateral, right).above(up);
    }
}
