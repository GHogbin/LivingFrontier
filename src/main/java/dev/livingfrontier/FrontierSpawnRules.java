package dev.livingfrontier;

import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.levelgen.Heightmap;

public final class FrontierSpawnRules {
    private FrontierSpawnRules() {
    }

    public static boolean isOutdoors(LevelReader level, BlockPos position) {
        if (level.canSeeSky(position)) {
            return true;
        }
        int top = level.getHeight(Heightmap.Types.WORLD_SURFACE, position.getX(), position.getZ());
        if (top - position.getY() > 24) {
            return false;
        }
        for (int y = position.getY(); y < top; y++) {
            var block = level.getBlockState(new BlockPos(position.getX(), y, position.getZ()));
            if (block.blocksMotion() && !block.is(BlockTags.LEAVES)) {
                return false;
            }
        }
        return true;
    }
}
