package com.gabri.magicteam.mixin;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Dependency-free architecture contract for the global-first friendly-fire model. */
public final class GlobalFirstArchitectureContractTest {
    private static final Path CORE_CONFIG = Path.of("src/main/resources/magic_team.mixins.json");
    private static final Path UTIL_ROOT = Path.of("src/main/java/com/gabri/magicteam/util");
    private static final Path EXCEPTION_AUDIT = Path.of("docs/audits/2026-09-26-global-first-adapter-migration.md");
    private static final Pattern MIXIN_ENTRY = Pattern.compile("\\\"([^\\\"]+)\\\"");

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
        everyRemainingOptionalAdapterIsAuditedException();
    }

    private static void coreConfigKeepsSharedGlobalHooks() throws IOException {
        String core = Files.readString(CORE_CONFIG);
        for (String required : List.of(
                "AbstractSpellMixin",
                "AbstractMagicProjectileMixin",
                "AoeEntityMixin",
                "EntityMagicAttributionMixin",
                "MobEffectInstanceMagicAttributionMixin",
                "EntityMagicSideEffectMixin",
                "LivingEntityMagicSideEffectMixin",
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
        for (String file : List.of("MagicAttribution.java", "MagicAttributionIndex.java", "MagicEffectAttributionIndex.java")) {
            Path path = UTIL_ROOT.resolve(file);
            check(Files.isRegularFile(path), "generic attribution component is missing: " + file);
            String source = Files.readString(path);
            for (String addonMarker : ADDON_MARKERS) {
                check(!source.contains(addonMarker),
                        "generic attribution hard-links addon implementation: " + path + " -> " + addonMarker);
            }
        }
    }

    private static void everyRemainingOptionalAdapterIsAuditedException() throws IOException {
        check(Files.isRegularFile(EXCEPTION_AUDIT), "global-first exception audit is missing");
        String audit = Files.readString(EXCEPTION_AUDIT);
        Set<String> adapters = new LinkedHashSet<>();

        for (Path config : OPTIONAL_CONFIGS) {
            adapters.addAll(readMixinRegistrations(Files.readString(config)));
        }

        for (String adapter : adapters) {
            check(audit.contains("`" + adapter + "`"),
                    "registered optional adapter is missing from global-first audit: " + adapter);
            check(audit.contains("GLOBAL_FIRST_EXCEPTION"),
                    "global-first exception audit must use explicit exception marker");
        }

        check(!audit.contains("`compat.traveloptics.OrbitalVoidFriendlyFireMixin` — **GLOBAL_FIRST_EXCEPTION:"),
                "Orbital Void must remain removed rather than documented as a permanent exception");
    }

    private static Set<String> readMixinRegistrations(String json) {
        int marker = json.indexOf("\"mixins\"");
        int open = json.indexOf('[', marker);
        int close = json.indexOf(']', open);
        check(marker >= 0 && open >= 0 && close > open, "could not locate mixins array");
        Set<String> result = new LinkedHashSet<>();
        Matcher matcher = MIXIN_ENTRY.matcher(json.substring(open + 1, close));
        while (matcher.find()) {
            result.add(matcher.group(1));
        }
        return result;
    }

    private static void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
