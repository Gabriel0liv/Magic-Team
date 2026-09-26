package com.gabri.magicteam.mixin;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Dependency-free architecture contract for the global-first friendly-fire model.
 *
 * <p>The contract deliberately checks source layout and registrations instead of
 * loading Minecraft classes, so it can run with plain javac/java.</p>
 */
public final class GlobalFirstArchitectureContractTest {
    private static final Path CORE_CONFIG = Path.of("src/main/resources/magic_team.mixins.json");
    private static final Path MIXIN_ROOT = Path.of("src/main/java/com/gabri/magicteam/mixin");
    private static final Path UTIL_ROOT = Path.of("src/main/java/com/gabri/magicteam/util");

    private static final List<Path> OPTIONAL_CONFIGS = List.of(
            Path.of("src/main/resources/magic_team.traveloptics.mixins.json"),
            Path.of("src/main/resources/magic_team.geomancyplus.mixins.json"),
            Path.of("src/main/resources/magic_team.familiars.mixins.json"),
            Path.of("src/main/resources/magic_team.cataclysm.mixins.json")
    );

    private static final List<String> ADDON_MARKERS = List.of(
            "com.gametechbc.traveloptics",
            "com.gametechbc.gtbcs_geomancy_plus",
            "net.alshanex.alshanex_familiars",
            "com.github.L_Ender.cataclysm"
    );

    private GlobalFirstArchitectureContractTest() {
    }

    public static void main(String[] args) throws Exception {
        coreConfigKeepsSharedGlobalHooks();
        optionalConfigsRemainOptionalEnhancements();
        genericAttributionIsAddonNeutral();
        spellSpecificAdaptersDeclareWhyTheyStillExist();
    }

    private static void coreConfigKeepsSharedGlobalHooks() throws IOException {
        String core = Files.readString(CORE_CONFIG);
        for (String required : List.of(
                "AbstractSpellMixin",
                "AbstractMagicProjectileMixin",
                "AoeEntityMixin",
                "DamageSourcesMixin",
                "LivingEntityMixin")) {
            check(core.contains("\"" + required + "\""),
                    "global core hook is not registered: " + required);
        }

        check(core.contains("\"required\": true"), "core mixin config must remain strict");
        check(core.contains("\"defaultRequire\": 1"), "core injectors must remain strict");
    }

    private static void optionalConfigsRemainOptionalEnhancements() throws IOException {
        for (Path config : OPTIONAL_CONFIGS) {
            String source = Files.readString(config);
            check(source.contains("\"required\": false"),
                    "addon compatibility must remain optional: " + config);
            check(source.contains("\"defaultRequire\": 0"),
                    "addon compatibility injectors must remain fail-soft: " + config);
        }
    }

    private static void genericAttributionIsAddonNeutral() throws IOException {
        Path attribution = UTIL_ROOT.resolve("MagicAttribution.java");
        Path index = UTIL_ROOT.resolve("MagicAttributionIndex.java");
        check(Files.isRegularFile(attribution), "generic MagicAttribution is missing");
        check(Files.isRegularFile(index), "generic MagicAttributionIndex is missing");

        for (Path path : List.of(attribution, index)) {
            String source = Files.readString(path);
            for (String addonMarker : ADDON_MARKERS) {
                check(!source.contains(addonMarker),
                        "generic attribution hard-links addon implementation: " + path + " -> " + addonMarker);
            }
        }
    }

    private static void spellSpecificAdaptersDeclareWhyTheyStillExist() throws IOException {
        Path compatRoot = MIXIN_ROOT.resolve("compat");
        if (!Files.isDirectory(compatRoot)) {
            return;
        }

        try (var paths = Files.walk(compatRoot)) {
            for (Path path : paths.filter(candidate -> candidate.toString().endsWith(".java")).toList()) {
                String source = Files.readString(path);
                String fileName = path.getFileName().toString();

                if (fileName.equals("OptionalAddonMixinPlugin.java")
                        || fileName.equals("OptionalAddonMixinErrorHandler.java")) {
                    continue;
                }

                if (!looksSpellSpecific(fileName)) {
                    continue;
                }

                check(source.contains("GLOBAL_FIRST_EXCEPTION:"),
                        "spell-specific adapter lacks global-first justification: " + path);
            }
        }
    }

    private static boolean looksSpellSpecific(String fileName) {
        String lower = fileName.toLowerCase();
        return lower.contains("spell")
                || lower.contains("friendlyfire")
                || lower.contains("targeting")
                || lower.contains("context")
                || lower.contains("attribution");
    }

    private static void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
