package com.gabri.magicteam.util;

import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;

/**
 * Detects optional addon adapters that load but inject nothing.
 *
 * <p>Optional configs use {@code defaultRequire: 0}, so when an addon update
 * renames or restructures a target method, Mixin skips the injection without
 * any error and the adapter silently stops protecting allies. After each
 * adapter is applied, the target class is scanned for calls into Magic Team
 * handler methods (Mixin keeps the original handler name inside the merged
 * name, e.g. {@code handler$zza000$magicTeam$beginAbility}). Zero calls means
 * the adapter is inert. The summary is logged once the server has started.</p>
 */
public final class OptionalAdapterDiagnostics {
    private static final Logger LOGGER = LoggerFactory.getLogger("MagicTeam/OptionalAdapters");
    private static final String HANDLER_MARKER = "magicTeam$";

    /** adapter simple name + target -> number of injected handler calls. */
    private static final Map<String, Integer> RESULTS = new ConcurrentHashMap<>();

    private OptionalAdapterDiagnostics() {
    }

    public static void recordApplied(String targetClassName, ClassNode targetClass, String mixinClassName) {
        if (targetClass == null || mixinClassName == null) {
            return;
        }
        RESULTS.put(key(mixinClassName, targetClassName), countHandlerCalls(targetClass));
    }

    /** Counts calls from target methods into merged Magic Team handlers on the same class. */
    static int countHandlerCalls(ClassNode targetClass) {
        int calls = 0;
        for (MethodNode method : targetClass.methods) {
            if (method.name.contains(HANDLER_MARKER) || method.instructions == null) {
                continue;
            }
            for (AbstractInsnNode insn = method.instructions.getFirst(); insn != null; insn = insn.getNext()) {
                if (insn instanceof MethodInsnNode call
                        && call.owner.equals(targetClass.name)
                        && call.name.contains(HANDLER_MARKER)) {
                    calls++;
                }
            }
        }
        return calls;
    }

    /**
     * Logs which optional adapters are active and warns about inert ones whose
     * addon is installed. {@code isModLoaded} decides whether an addon is present.
     */
    public static void logSummary(Predicate<String> isModLoaded) {
        Map<String, Integer> snapshot = new TreeMap<>(RESULTS);
        int active = 0;
        for (Map.Entry<String, Integer> entry : snapshot.entrySet()) {
            String adapter = entry.getKey();
            String modId = modIdFor(adapter);
            if (modId != null && !isModLoaded.test(modId)) {
                continue;
            }
            if (entry.getValue() > 0) {
                active++;
            } else {
                LOGGER.warn("Magic Team optional adapter is INERT: {} (addon '{}' is installed but the adapter's "
                                + "target method was not found, so this ability is not protected for allies). "
                                + "The addon probably changed; update the adapter.",
                        adapter, modId);
            }
        }
        LOGGER.info("Magic Team optional adapters: {} active, {} inert, {} loaded in total.",
                active, countInert(snapshot, isModLoaded), snapshot.size());
    }

    private static long countInert(Map<String, Integer> snapshot, Predicate<String> isModLoaded) {
        return snapshot.entrySet().stream()
                .filter(entry -> entry.getValue() == 0)
                .filter(entry -> {
                    String modId = modIdFor(entry.getKey());
                    return modId == null || isModLoaded.test(modId);
                })
                .count();
    }

    static String modIdFor(String adapterKey) {
        if (adapterKey.startsWith("traveloptics.")) {
            return "traveloptics";
        }
        if (adapterKey.startsWith("cataclysm.")) {
            return "cataclysm";
        }
        if (adapterKey.startsWith("geomancyplus.")) {
            return "gtbcs_geomancy_plus";
        }
        if (adapterKey.startsWith("familiars.")) {
            return "alshanex_familiars";
        }
        return null;
    }

    private static String key(String mixinClassName, String targetClassName) {
        String prefix = "com.gabri.magicteam.mixin.compat.";
        String adapter = mixinClassName.startsWith(prefix)
                ? mixinClassName.substring(prefix.length())
                : mixinClassName;
        String target = targetClassName == null ? "?" : targetClassName.substring(targetClassName.lastIndexOf('.') + 1);
        return adapter + " -> " + target;
    }

    static Map<String, Integer> snapshotForTests() {
        return new TreeMap<>(RESULTS);
    }
}
