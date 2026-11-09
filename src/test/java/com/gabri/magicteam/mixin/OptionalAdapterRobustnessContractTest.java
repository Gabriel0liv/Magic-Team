package com.gabri.magicteam.mixin;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

/**
 * Dependency-free contract keeping optional addon adapters tolerant of addon
 * updates. Optional configs use defaultRequire 0, so a fragile target makes an
 * adapter silently inert rather than crashing; these rules keep adapters bound
 * to stable method names instead of how an addon's method body is compiled.
 */
public final class OptionalAdapterRobustnessContractTest {
    private static final Path COMPAT = Path.of("src/main/java/com/gabri/magicteam/mixin/compat");

    /** Adapters that still redirect a call, each for a documented reason. */
    private static final Set<String> REDIRECT_ALLOWLIST = Set.of(
            // Retaliation is produced inside a Forge event; gates the side effects themselves.
            "ServerEventsRetaliationFriendlyFireMixin.java",
            // Filters the target list because the shockwave also resets invulnerability.
            "TremorStepFriendlyFireMixin.java",
            // Restores the owner of re-spawned reflected projectiles.
            "ReversalFriendlyFireMixin.java"
    );

    /** Context-bridge adapters: one stable method, paired begin/end. */
    private static final List<String> BRIDGES = List.of(
            "traveloptics/AbyssalHideFriendlyFireMixin.java",
            "traveloptics/ForlornHarbingerFriendlyFireMixin.java",
            "traveloptics/PrimordialCrestFriendlyFireMixin.java",
            "traveloptics/TectonicCrestFriendlyFireMixin.java",
            "traveloptics/MechanizedExoskeletonFriendlyFireMixin.java",
            "traveloptics/ChargedSandsLevelOneFriendlyFireMixin.java",
            "traveloptics/ChargedSandsLevelTwoFriendlyFireMixin.java",
            "traveloptics/CursedWraithbladeFriendlyFireMixin.java",
            "traveloptics/SpiritDamageHelperFriendlyFireMixin.java",
            "traveloptics/ReversalFriendlyFireMixin.java",
            "cataclysm/FlareBombFriendlyFireMixin.java",
            "cataclysm/WitherHowitzerFriendlyFireMixin.java"
    );

    private OptionalAdapterRobustnessContractTest() {
    }

    public static void main(String[] args) throws Exception {
        try (Stream<Path> files = Files.walk(COMPAT)) {
            for (Path file : files.filter(path -> path.toString().endsWith("FriendlyFireMixin.java")).toList()) {
                String source = Files.readString(file);
                String name = file.getFileName().toString();
                check(!source.contains("lambda$"),
                        name + " must not target compiler-generated lambda methods; their names change between builds");
                check(!source.contains("ordinal"),
                        name + " must not depend on the position of a call inside the addon method");
                check(!source.contains("@Redirect") || REDIRECT_ALLOWLIST.contains(name),
                        name + " must use a context bridge or a HEAD inject instead of redirecting addon internals");
                check(source.contains("@Pseudo"), name + " must stay @Pseudo so a missing addon class is skipped");
            }
        }

        for (String bridge : BRIDGES) {
            String source = Files.readString(COMPAT.resolve(bridge));
            int begins = count(source, "AdapterContextBridge.beginHostile(");
            int ends = count(source, "AdapterContextBridge.end()");
            check(begins > 0 && begins == ends, bridge + " must pair every beginHostile with an end");
            check(count(source, "@At(\"HEAD\")") == begins && count(source, "@At(\"RETURN\")") == ends,
                    bridge + " must open the bridge at HEAD and close it at RETURN");
        }

        String bridgeSource = Files.readString(Path.of(
                "src/main/java/com/gabri/magicteam/util/AdapterContextBridge.java"));
        check(bridgeSource.contains("isClientSide()") && bridgeSource.contains("TeamUtils.isEnabled()"),
                "context bridge must ignore client calls and disabled Magic Team");
        check(bridgeSource.contains("scopes.push(pushed)"),
                "context bridge must record whether it pushed so end() stays balanced");

        String plugin = Files.readString(COMPAT.resolve("OptionalAddonMixinPlugin.java"));
        check(plugin.contains("OptionalAdapterDiagnostics.recordApplied"),
                "optional plugin must record whether each adapter actually injected");
        check(!Files.exists(COMPAT.resolve("OptionalAdapterDiagnostics.java")),
                "diagnostics must live outside the mixin package so normal code may load it");
    }

    private static int count(String source, String token) {
        int count = 0;
        for (int index = source.indexOf(token); index >= 0; index = source.indexOf(token, index + token.length())) {
            count++;
        }
        return count;
    }

    private static void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
