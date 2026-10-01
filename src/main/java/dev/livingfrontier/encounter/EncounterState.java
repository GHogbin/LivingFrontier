package dev.livingfrontier.encounter;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.saveddata.SavedData;

public final class EncounterState extends SavedData {
    private final Map<UUID, Entry> players = new HashMap<>();

    public static EncounterState load(CompoundTag tag) {
        EncounterState state = new EncounterState();
        ListTag entries = tag.getList("Players", Tag.TAG_COMPOUND);
        for (int index = 0; index < entries.size(); index++) {
            CompoundTag entry = entries.getCompound(index);
            state.players.put(entry.getUUID("Player"),
                    new Entry(entry.getLong("NextTick"), BlockPos.of(entry.getLong("Anchor"))));
        }
        return state;
    }

    public boolean contains(UUID player) {
        return players.containsKey(player);
    }

    public boolean isDue(UUID player, long now) {
        Entry entry = players.get(player);
        return entry != null && now >= entry.nextTick;
    }

    public boolean hasTravelled(UUID player, BlockPos position) {
        Entry entry = players.get(player);
        return entry != null && entry.anchor.distSqr(position) >= 32 * 32;
    }

    public void schedule(UUID player, long nextTick, BlockPos anchor) {
        players.put(player, new Entry(nextTick, anchor.immutable()));
        setDirty();
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        ListTag entries = new ListTag();
        players.forEach((player, entry) -> {
            CompoundTag value = new CompoundTag();
            value.putUUID("Player", player);
            value.putLong("NextTick", entry.nextTick);
            value.putLong("Anchor", entry.anchor.asLong());
            entries.add(value);
        });
        tag.put("Players", entries);
        return tag;
    }

    private record Entry(long nextTick, BlockPos anchor) {
    }
}
