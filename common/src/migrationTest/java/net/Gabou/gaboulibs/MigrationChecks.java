package net.Gabou.gaboulibs;

import net.Gabou.gaboulibs.storage.SnowRecord;
import net.Gabou.gaboulibs.util.WorldContext;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;
import java.io.InputStream;
import java.util.Arrays;

/** Regression checks on the actual target dependencies; does not replace a client launch. */
public final class MigrationChecks {
    public static void main(String[] args) throws Exception {
        for (int[] distribution : new int[][] {null, new int[0], {0, 2, 8, 32}}) {
            SnowRecord original = new SnowRecord(0.5f, 2.5f, 8.0f, distribution);
            var tag = original.toTag();
            SnowRecord restored = SnowRecord.fromTag(tag);
            require(restored.minLayers == original.minLayers && restored.avgLayers == original.avgLayers
                    && restored.maxLayers == original.maxLayers, "snow scalars");
            require(Arrays.equals(restored.distributionPattern, distribution), "optional snow distribution");
            require(tag.contains("MinLayers") && tag.contains("AvgLayers") && tag.contains("MaxLayers"), "legacy NBT keys");
            require(tag.contains("DistributionPattern") == (distribution != null), "optional NBT key");
        }
        WorldContext.clear();
        require(WorldContext.getOverworld() == null, "cleared world reference");
        ClassNode mixin = read("net/Gabou/gaboulibs/mixin/ScreenMixin");
        ClassNode target = read("net/minecraft/client/gui/screens/Screen");
        int checkedFields = 0;
        int checkedMethods = 0;
        for (FieldNode field : mixin.fields) {
            if (field.visibleAnnotations == null || field.visibleAnnotations.stream().noneMatch(a -> a.desc.endsWith("/Shadow;"))) continue;
            checkedFields++;
            require(target.fields.stream().anyMatch(f -> f.name.equals(field.name) && f.desc.equals(field.desc)
                    && (f.access & Opcodes.ACC_STATIC) == (field.access & Opcodes.ACC_STATIC)), "shadow field " + field.name);
        }
        for (MethodNode method : mixin.methods) {
            if (method.visibleAnnotations == null || method.visibleAnnotations.stream().noneMatch(a -> a.desc.endsWith("/Shadow;"))) continue;
            checkedMethods++;
            require(target.methods.stream().anyMatch(m -> m.name.equals(method.name) && m.desc.equals(method.desc)
                    && (m.access & Opcodes.ACC_STATIC) == (method.access & Opcodes.ACC_STATIC)), "shadow method " + method.name);
        }
        require(checkedFields == 2 && checkedMethods == 2, "all four ScreenMixin shadows checked");
        System.out.println("PASS: SnowRecord NBT round-trip (null/empty/populated distribution), WorldContext cleanup, ScreenMixin shadows.");
    }
    private static ClassNode read(String name) throws Exception {
        try (InputStream stream = MigrationChecks.class.getResourceAsStream("/" + name + ".class")) {
            require(stream != null, "class " + name);
            ClassNode node = new ClassNode(); new ClassReader(stream).accept(node, 0); return node;
        }
    }
    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
