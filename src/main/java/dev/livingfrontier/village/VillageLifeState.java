package dev.livingfrontier.village;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

public final class VillageLifeState extends SavedData {
    private final Set<BlockPos> villages = new HashSet<>();
    private final Set<BlockPos> pendingTraders = new HashSet<>();
    private final Map<UUID, Errand> errands = new HashMap<>();

    public static VillageLifeState get(ServerLevel level) {
        return level.getServer().overworld().getDataStorage().computeIfAbsent(
                VillageLifeState::load, VillageLifeState::new, "livingfrontier_village_life");
    }

    public static VillageLifeState load(CompoundTag tag) {
        VillageLifeState state = new VillageLifeState();
        for (long position : tag.getLongArray("Villages")) {
            state.villages.add(BlockPos.of(position));
        }
        for (long position : tag.getLongArray("PendingTraders")) {
            state.pendingTraders.add(BlockPos.of(position));
        }
        ListTag jobs = tag.getList("Errands", Tag.TAG_COMPOUND);
        for (int index = 0; index < jobs.size(); index++) {
            CompoundTag job = jobs.getCompound(index);
            state.errands.put(job.getUUID("Player"), new Errand(job.getUUID("Villager"), job.getString("Supply"),
                    job.getLong("Day"), job.getBoolean("Completed")));
        }
        return state;
    }

    public boolean hasVillage(BlockPos position) {
        return villages.stream().anyMatch(center -> center.distSqr(position) < 96 * 96);
    }

    public void recordVillage(BlockPos position) {
        villages.add(position.immutable());
        setDirty();
    }

    public void queueTrader(BlockPos position) {
        if (pendingTraders.add(position.immutable())) setDirty();
    }

    public void completeTrader(BlockPos position) {
        if (pendingTraders.remove(position)) setDirty();
    }

    public Set<BlockPos> pendingTradersNear(BlockPos position, int radius) {
        long maximum = (long) radius * radius;
        Set<BlockPos> result = new HashSet<>();
        for (BlockPos pending : pendingTraders) {
            if (pending.distSqr(position) <= maximum) result.add(pending);
        }
        return Set.copyOf(result);
    }

    public Errand getErrand(UUID player) {
        return errands.get(player);
    }

    public void setErrand(UUID player, Errand job) {
        errands.put(player, job);
        setDirty();
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        tag.putLongArray("Villages", villages.stream().mapToLong(BlockPos::asLong).toArray());
        tag.putLongArray("PendingTraders", pendingTraders.stream().mapToLong(BlockPos::asLong).toArray());
        ListTag jobs = new ListTag();
        errands.forEach((player, job) -> {
            CompoundTag value = new CompoundTag();
            value.putUUID("Player", player);
            value.putUUID("Villager", job.villager());
            value.putString("Supply", job.supply());
            value.putLong("Day", job.day());
            value.putBoolean("Completed", job.completed());
            jobs.add(value);
        });
        tag.put("Errands", jobs);
        return tag;
    }

    public record Errand(UUID villager, String supply, long day, boolean completed) {
    }
}
