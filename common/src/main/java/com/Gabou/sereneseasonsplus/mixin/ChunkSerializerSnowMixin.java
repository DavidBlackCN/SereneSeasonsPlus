package com.Gabou.sereneseasonsplus.mixin;

import com.Gabou.sereneseasonsplus.access.ISnowChunkData;
import com.Gabou.sereneseasonsplus.access.ISnowTrackedChunk;
import com.Gabou.sereneseasonsplus.storage.ChunkSnowData;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.village.poi.PoiManager;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ImposterProtoChunk;
import net.minecraft.world.level.chunk.PalettedContainerFactory;
import net.minecraft.world.level.chunk.ProtoChunk;
import net.minecraft.world.level.chunk.storage.RegionStorageInfo;
import net.minecraft.world.level.chunk.storage.SerializableChunkData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(SerializableChunkData.class)
public abstract class ChunkSerializerSnowMixin implements ISnowChunkData {
    @Unique
    private CompoundTag sereneseasonsplus$snowData;

    @Override
    public void sereneseasonsplus$setSnowData(CompoundTag tag) {
        this.sereneseasonsplus$snowData = tag;
    }

    // Capture on the server thread with the vanilla chunk snapshot. The snapshot,
    // not the live chunk or a global coordinate cache, owns the pending NBT.
    @Inject(method = "copyOf", at = @At("RETURN"))
    private static void ssp$capture(ServerLevel level, ChunkAccess chunk,
                                   CallbackInfoReturnable<SerializableChunkData> cir) {
        if (chunk instanceof ISnowTrackedChunk tracked) {
            ((ISnowChunkData) (Object) cir.getReturnValue())
                    .sereneseasonsplus$setSnowData(ChunkSnowData.save(tracked));
        }
    }

    @Inject(method = "write", at = @At("RETURN"))
    private void ssp$write(CallbackInfoReturnable<CompoundTag> cir) {
        if (this.sereneseasonsplus$snowData != null) {
            cir.getReturnValue().put("SereneSeasonsPlus", this.sereneseasonsplus$snowData.copy());
        }
    }

    @Inject(method = "parse", at = @At("RETURN"))
    private static void ssp$readTag(LevelHeightAccessor level, PalettedContainerFactory factory, CompoundTag nbt,
                                   CallbackInfoReturnable<SerializableChunkData> cir) {
        if (cir.getReturnValue() != null) {
            nbt.getCompound("SereneSeasonsPlus").ifPresent(tag ->
                    ((ISnowChunkData) (Object) cir.getReturnValue()).sereneseasonsplus$setSnowData(tag.copy()));
        }
    }

    @Inject(method = "read", at = @At("RETURN"))
    private void ssp$restore(ServerLevel level, PoiManager poi, RegionStorageInfo info, ChunkPos pos,
                             CallbackInfoReturnable<ProtoChunk> cir) {
        if (this.sereneseasonsplus$snowData == null) return;
        ChunkAccess chunk = cir.getReturnValue();
        // Already-generated chunks are returned wrapped in an ImposterProtoChunk.
        if (chunk instanceof ImposterProtoChunk imposter) chunk = imposter.getWrapped();
        if (chunk instanceof ISnowTrackedChunk tracked) {
            ChunkSnowData.load(tracked, this.sereneseasonsplus$snowData);
        }
    }
}
