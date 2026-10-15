package com.Gabou.sereneseasonsplus;

import com.Gabou.sereneseasonsplus.access.ISnowTrackedChunk;
import com.Gabou.sereneseasonsplus.storage.ChunkSnowData;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Proxy;
import java.util.*;

/** Offline regression checks. These do not replace an actual Mixin launch test. */
public final class MigrationChecks {
    public static void main(String[] args) throws Exception {
        checkSnowRoundTrip();
        checkLegacySnowData();
        for (String mixin : List.of("ChunkSerializerSnowMixin", "LevelChunkSnowMixin", "ProtoChunkSnowMixin",
                "MinecraftServerMixin", "SeasonHooksMixin", "ServerLevelAccessMixin", "ServerLevelMixin",
                "SpreadingSnowyDirtBlockMixin")) {
            checkMixin(mixin);
        }
        if (MigrationChecks.class.getResource("/com/Gabou/sereneseasonsplus/mixin/BetterDaysSleepWakeTimeMixin.class") != null) {
            checkMixin("BetterDaysSleepWakeTimeMixin");
        }
        System.out.println("PASS: snow NBT round-trip, snapshot isolation, legacy fields and Mixin target/invocation checks.");
    }

    private static void checkSnowRoundTrip() {
        ISnowTrackedChunk source = newChunk();
        source.sereneseasonsplus$setLastWinterId(7);
        source.sereneseasonsplus$setSurfaceHeight(83);
        source.sereneseasonsplus$setStormProgress(0.75f);
        source.sereneseasonsplus$setStormIdApplied(11);
        source.sereneseasonsplus$setLastProgressTick(400);
        source.sereneseasonsplus$setAvailableSnowColumns(230);
        source.sereneseasonsplus$setDestroyedStormId(11);
        source.sereneseasonsplus$setSnowSyncGeneration(3);
        source.sereneseasonsplus$setAppliedStormCount(4);
        BlockPos snow = new BlockPos(-17, 83, 32);
        BlockPos ice = new BlockPos(-18, 62, 33);
        long destroyed = ((long) -17 << 32) ^ 32L;
        source.sereneseasonsplus$getSnowColumns().put(snow, 24);
        source.sereneseasonsplus$getIceColumns().add(ice);
        source.sereneseasonsplus$getDestroyedColumns().add(destroyed);

        CompoundTag snapshot = ChunkSnowData.save(source);
        source.sereneseasonsplus$getSnowColumns().clear();
        source.sereneseasonsplus$getIceColumns().clear();
        source.sereneseasonsplus$getDestroyedColumns().clear();
        source.sereneseasonsplus$setStormIdApplied(12);
        ISnowTrackedChunk restored = newChunk();
        ChunkSnowData.load(restored, snapshot);
        require(restored.sereneseasonsplus$getLastWinterId() == 7, "winter id");
        require(restored.sereneseasonsplus$getSurfaceHeight() == 83, "surface height");
        require(restored.sereneseasonsplus$getStormProgress() == 0.75f, "progress");
        require(restored.sereneseasonsplus$getStormIdApplied() == 11, "snapshot storm id");
        require(restored.sereneseasonsplus$getLastProgressTick() == 400, "progress tick");
        require(restored.sereneseasonsplus$getAvailableSnowColumns() == 230, "available columns");
        require(restored.sereneseasonsplus$getDestroyedStormId() == 11, "destroyed storm id");
        require(restored.sereneseasonsplus$getSnowSyncGeneration() == 3, "sync generation");
        require(restored.sereneseasonsplus$getAppliedStormCount() == 4, "applied storms");
        require(restored.sereneseasonsplus$getSnowColumns().equals(Map.of(snow, 24)), "snow snapshot");
        require(restored.sereneseasonsplus$getIceColumns().equals(Set.of(ice)), "ice snapshot");
        require(restored.sereneseasonsplus$getDestroyedColumns().equals(Set.of(destroyed)), "destroyed snapshot");
        ISnowTrackedChunk other = newChunk();
        ChunkSnowData.load(other, new CompoundTag());
        require(restored.sereneseasonsplus$getSnowColumns().size() == 1, "independent chunks");
    }

    private static void checkLegacySnowData() {
        ISnowTrackedChunk chunk = newChunk();
        chunk.sereneseasonsplus$setSurfaceHeight(-1);
        chunk.sereneseasonsplus$setAvailableSnowColumns(-1);
        CompoundTag legacy = new CompoundTag();
        legacy.putInt("LastWinterId", 2);
        ChunkSnowData.load(chunk, legacy);
        require(chunk.sereneseasonsplus$getLastWinterId() == 2, "legacy winter id");
        require(chunk.sereneseasonsplus$getSurfaceHeight() == -1, "legacy surface default");
        require(chunk.sereneseasonsplus$getAvailableSnowColumns() == -1, "legacy columns default");
        require(chunk.sereneseasonsplus$getSnowColumns().isEmpty(), "legacy missing snow list");
    }

    private static ISnowTrackedChunk newChunk() {
        Map<String, Object> values = new HashMap<>();
        values.put("SnowColumns", new HashMap<BlockPos, Integer>());
        values.put("IceColumns", new HashSet<BlockPos>());
        values.put("DestroyedColumns", new HashSet<Long>());
        return (ISnowTrackedChunk) Proxy.newProxyInstance(MigrationChecks.class.getClassLoader(),
                new Class<?>[]{ISnowTrackedChunk.class}, (proxy, method, args) -> {
                    String name = method.getName().substring("sereneseasonsplus$".length());
                    String field = name.substring(3);
                    if (name.startsWith("set")) {
                        values.put(field, args[0]);
                        return null;
                    }
                    if (values.containsKey(field)) return values.get(field);
                    if (method.getReturnType() == float.class) return 0.0f;
                    return 0;
                });
    }

    private static void checkMixin(String name) throws IOException {
        ClassNode mixin = readClass("com/Gabou/sereneseasonsplus/mixin/" + name);
        AnnotationNode annotation = mixin.invisibleAnnotations.stream()
                .filter(a -> a.desc.equals("Lorg/spongepowered/asm/mixin/Mixin;")).findFirst().orElseThrow();
        List<?> values = (List<?>) value(annotation, "value");
        String target = values == null ? ((List<String>) value(annotation, "targets")).getFirst().replace('.', '/')
                : ((Type) values.getFirst()).getInternalName();
        ClassNode targetClass = readClass(target);
        for (MethodNode handler : mixin.methods) {
            if (handler.visibleAnnotations == null) continue;
            for (AnnotationNode injection : handler.visibleAnnotations) {
                if (!injection.desc.equals("Lorg/spongepowered/asm/mixin/injection/Inject;")
                        && !injection.desc.equals("Lorg/spongepowered/asm/mixin/injection/Redirect;")) continue;
                for (String selector : (List<String>) value(injection, "method")) {
                    List<MethodNode> matches = targetClass.methods.stream()
                            .filter(m -> selector.equals(m.name) || selector.equals(m.name + m.desc)).toList();
                    require(matches.size() == 1, name + ": ambiguous or missing method " + selector);
                    MethodNode method = matches.getFirst();
                    require((method.access & Opcodes.ACC_STATIC) == (handler.access & Opcodes.ACC_STATIC),
                            name + ": static/instance mismatch " + selector);
                    Object at = value(injection, "at");
                    for (AnnotationNode point : at instanceof List<?> list ? (List<AnnotationNode>) list : List.of((AnnotationNode) at)) {
                        int start = 0;
                        int end = method.instructions.size() - 1;
                        Object slices = value(injection, "slice");
                        if (slices != null) {
                            for (AnnotationNode slice : slices instanceof List<?> list ? (List<AnnotationNode>) list : List.of((AnnotationNode) slices)) {
                                start = boundary(name, method, (AnnotationNode) value(slice, "from"), start);
                                end = boundary(name, method, (AnnotationNode) value(slice, "to"), end);
                            }
                        }
                        require(start <= end, name + ": reversed slice");
                        checkInvocation(name, method, point, start, end);
                    }
                }
            }
        }
        System.out.println("Checked Mixin targets: " + name);
    }

    private static int boundary(String mixin, MethodNode method, AnnotationNode at, int fallback) {
        if (at == null) return fallback;
        require("INVOKE".equals(value(at, "value")), mixin + ": unsupported slice boundary");
        String target = (String) value(at, "target");
        for (int i = 0; i < method.instructions.size(); i++) {
            if (method.instructions.get(i) instanceof MethodInsnNode call
                    && target.equals("L" + call.owner + ";" + call.name + call.desc)) return i;
        }
        throw new AssertionError(mixin + ": missing slice boundary " + target);
    }

    private static void checkInvocation(String mixin, MethodNode method, AnnotationNode at, int start, int end) {
        if (!"INVOKE".equals(value(at, "value"))) return;
        String target = (String) value(at, "target");
        int count = 0;
        for (int i = start; i <= end; i++) {
            AbstractInsnNode instruction = method.instructions.get(i);
            if (instruction instanceof MethodInsnNode call
                    && target.equals("L" + call.owner + ";" + call.name + call.desc)) count++;
        }
        Integer ordinal = (Integer) value(at, "ordinal");
        require(count > (ordinal == null ? 0 : ordinal), mixin + ": missing invocation " + target);
    }

    private static Object value(AnnotationNode annotation, String key) {
        if (annotation.values != null) {
            for (int i = 0; i < annotation.values.size(); i += 2) {
                if (key.equals(annotation.values.get(i))) return annotation.values.get(i + 1);
            }
        }
        return null;
    }

    private static ClassNode readClass(String name) throws IOException {
        try (InputStream stream = MigrationChecks.class.getResourceAsStream("/" + name + ".class")) {
            require(stream != null, "missing target class " + name);
            ClassNode node = new ClassNode();
            new ClassReader(stream).accept(node, 0);
            return node;
        }
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
