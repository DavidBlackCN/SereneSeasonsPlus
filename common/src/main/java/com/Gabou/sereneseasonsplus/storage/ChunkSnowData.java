package com.Gabou.sereneseasonsplus.storage;

import com.Gabou.sereneseasonsplus.access.ISnowTrackedChunk;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;

/** Keeps the existing SereneSeasonsPlus chunk NBT format across chunk snapshots. */
public final class ChunkSnowData {
    private ChunkSnowData() {
    }

    public static CompoundTag save(ISnowTrackedChunk tracked) {
        CompoundTag tag = new CompoundTag();
        tag.putInt("LastWinterId", tracked.sereneseasonsplus$getLastWinterId());
        tag.putInt("SurfaceHeight", tracked.sereneseasonsplus$getSurfaceHeight());
        tag.putFloat("StormProgress", tracked.sereneseasonsplus$getStormProgress());
        tag.putInt("StormIdApplied", tracked.sereneseasonsplus$getStormIdApplied());
        tag.putInt("LastProgressTick", tracked.sereneseasonsplus$getLastProgressTick());
        tag.putInt("AvailableColumns", tracked.sereneseasonsplus$getAvailableSnowColumns());
        tag.putInt("DestroyedStormId", tracked.sereneseasonsplus$getDestroyedStormId());
        tag.putInt("SnowSyncGeneration", tracked.sereneseasonsplus$getSnowSyncGeneration());
        tag.putInt("AppliedStormCount", tracked.sereneseasonsplus$getAppliedStormCount());

        // Snow columns
        ListTag snowList = new ListTag();
        for (var e : tracked.sereneseasonsplus$getSnowColumns().entrySet()) {
            CompoundTag entry = new CompoundTag();
            entry.putLong("Pos", e.getKey().asLong());
            entry.putInt("Layers", e.getValue());
            snowList.add(entry);
        }
        tag.put("SnowColumns", snowList);

        // Ice columns
        ListTag iceList = new ListTag();
        for (var pos : tracked.sereneseasonsplus$getIceColumns()) {
            CompoundTag entry = new CompoundTag();
            entry.putLong("Pos", pos.asLong());
            iceList.add(entry);
        }
        tag.put("IceColumns", iceList);

        // Destroyed columns
        ListTag destroyedList = new ListTag();
        for (Long key : tracked.sereneseasonsplus$getDestroyedColumns()) {
            CompoundTag entry = new CompoundTag();
            entry.putLong("XZ", key);
            destroyedList.add(entry);
        }
        tag.put("DestroyedColumns", destroyedList);

        return tag;
    }

    public static void load(ISnowTrackedChunk tracked, CompoundTag tag) {
        // Base fields
        if (tag.contains("LastWinterId")) tracked.sereneseasonsplus$setLastWinterId(tag.getInt("LastWinterId").get());
        if (tag.contains("SurfaceHeight"))
            tracked.sereneseasonsplus$setSurfaceHeight(tag.getInt("SurfaceHeight").get());
        if (tag.contains("StormProgress"))
            tracked.sereneseasonsplus$setStormProgress(tag.getFloat("StormProgress").get());
        if (tag.contains("StormIdApplied"))
            tracked.sereneseasonsplus$setStormIdApplied(tag.getInt("StormIdApplied").get());
        if (tag.contains("LastProgressTick"))
            tracked.sereneseasonsplus$setLastProgressTick(tag.getInt("LastProgressTick").get());
        if (tag.contains("AvailableColumns"))
            tracked.sereneseasonsplus$setAvailableSnowColumns(tag.getInt("AvailableColumns").get());
        if (tag.contains("DestroyedStormId"))
            tracked.sereneseasonsplus$setDestroyedStormId(tag.getInt("DestroyedStormId").get());
        if (tag.contains("SnowSyncGeneration"))
            tracked.sereneseasonsplus$setSnowSyncGeneration(tag.getInt("SnowSyncGeneration").get());
        if (tag.contains("AppliedStormCount"))
            tracked.sereneseasonsplus$setAppliedStormCount(tag.getInt("AppliedStormCount").get());

        // Snow columns
        tracked.sereneseasonsplus$getSnowColumns().clear();
        ListTag snowList = tag.getListOrEmpty("SnowColumns");
        for (int i = 0; i < snowList.size(); i++) {
            CompoundTag entry = snowList.getCompound(i).get();
            BlockPos pos = BlockPos.of(entry.getLong("Pos").get());
            int layers = entry.getInt("Layers").get();
            tracked.sereneseasonsplus$getSnowColumns().put(pos.immutable(), layers);
        }

        // Ice columns
        tracked.sereneseasonsplus$getIceColumns().clear();
        ListTag iceList = tag.getListOrEmpty("IceColumns");
        for (int i = 0; i < iceList.size(); i++) {
            CompoundTag entry = iceList.getCompound(i).get();
            BlockPos pos = BlockPos.of(entry.getLong("Pos").get());
            tracked.sereneseasonsplus$getIceColumns().add(pos.immutable());
        }

        // Destroyed columns
        tracked.sereneseasonsplus$getDestroyedColumns().clear();
        ListTag destroyedList = tag.getListOrEmpty("DestroyedColumns");
        for (int i = 0; i < destroyedList.size(); i++) {
            CompoundTag entry = destroyedList.getCompound(i).get();
            tracked.sereneseasonsplus$getDestroyedColumns().add(entry.getLong("XZ").get());
        }
    }
}
