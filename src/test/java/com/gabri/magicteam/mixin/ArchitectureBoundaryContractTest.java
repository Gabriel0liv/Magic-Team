package com.gabri.magicteam.mixin;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Dependency-free architecture boundaries that must remain true across addon fixes. */
public final class ArchitectureBoundaryContractTest {
    private static final Path MIXIN_ROOT = Path.of("src/main/java/com/gabri/magicteam/mixin");
    private static final Path TEAM_UTILS = Path.of("src/main/java/com/gabri/magicteam/util/TeamUtils.java");
    private static final Path CORE_MIXINS = Path.of("src/main/resources/magic_team.mixins.json");
    private static final Path BUILD_GRADLE = Path.of("build.gradle");
    private static final List<Path> OPTIONAL_CONFIGS = List.of(
            Path.of("src/main/resources/magic_team.traveloptics.mixins.json"),
            Path.of("src/main/resources/magic_team.geomancyplus.mixins.json"),
            Path.of("src/main/resources/magic_team.familiars.mixins.json"),
            Path.of("src/main/resources/magic_team.cataclysm.mixins.json")
    );
    private static final List<String> OPTIONAL_IMPORT_PREFIXES = List.of(
            "import com.gametechbc.traveloptics.",
            "import com.gametechbc.gtbcs_geomancy_plus.",
            "import net.alshanex.alshanex_familiars.",
            "import com.github.L_Ender.cataclysm."
    );
    private static final Pattern MIXIN_ENTRY = Pattern.compile("\\\"([^\\\"]+)\\\"");
    private static final String OPTIONAL_PLUGIN =
            "com.gabri.magicteam.mixin.compat.OptionalAddonMixinPlugin";

    private ArchitectureBoundaryContractTest() {
    }

    public static void main(String[] args) throws Exception {
        pseudoMixinsDoNotHardLinkOptionalAddons();
        allianceIdentityDoesNotDependOnFriendlyFire();
        magicProtectionDoesNotDependOnVanillaFriendlyFire();
        optionalAddonMixinConfigsAreIsolatedAndFailSoft();
    }

    private static void pseudoMixinsDoNotHardLinkOptionalAddons() throws IOException {
        try (var paths = Files.walk(MIXIN_ROOT)) {
            for (Path path : paths.filter(candidate -> candidate.toString().endsWith(".java")).toList()) {
                String source = Files.readString(path);
                if (!source.contains("@Pseudo")) {
                    continue;
                }
                for (String forbiddenImport : OPTIONAL_IMPORT_PREFIXES) {
                    check(!source.contains(forbiddenImport),
                            "@Pseudo mixin hard-links an optional addon: " + path + " -> " + forbiddenImport);
                }
            }
        }
    }

    private static void allianceIdentityDoesNotDependOnFriendlyFire() throws IOException {
        String source = Files.readString(TEAM_UTILS);
        String signature = "public static boolean areAllies(Entity a, Entity b)";
        int start = source.indexOf(signature);
        check(start >= 0, "TeamUtils.areAllies was not found");
        int nextMethod = source.indexOf("public static Entity getRootOwner", start);
        check(nextMethod > start, "could not isolate TeamUtils.areAllies body");
        String body = source.substring(start, nextMethod);

        check(!body.contains("shouldBlockFriendlyFire"), "areAllies must remain relationship-only");
        check(!body.contains("isAllowFriendlyFire"), "areAllies must ignore vanilla friendlyFire permission");
        check(body.contains("ENTITY_RELATIONS.areAllies"),
                "areAllies must delegate relationship identity to BabelEntityRelations");
    }

    private static void magicProtectionDoesNotDependOnVanillaFriendlyFire() throws IOException {
        String source = Files.readString(TEAM_UTILS);
        String signature = "public static boolean shouldBlockFriendlyFire(Entity attacker, Entity target)";
        int start = source.indexOf(signature);
        check(start >= 0, "TeamUtils.shouldBlockFriendlyFire was not found");
        int nextMethod = source.indexOf("public static void sendBlockedMessage", start);
        check(nextMethod > start, "could not isolate TeamUtils.shouldBlockFriendlyFire body");
        String body = source.substring(start, nextMethod);

        check(!body.contains("isAllowFriendlyFire"),
                "Magic Team hostile-magic protection must ignore vanilla friendlyFire permission");
        check(!body.contains("getTeam()"),
                "Magic Team hostile-magic protection must not inspect scoreboard permission");
        check(body.contains("ENTITY_RELATIONS.areAllies"),
                "Magic Team hostile-magic protection must still use Babel alliance identity");
    }

    private static void optionalAddonMixinConfigsAreIsolatedAndFailSoft() throws IOException {
        String core = Files.readString(CORE_MIXINS);
        check(core.contains("\"required\": true"), "core mixin config must remain required");
        check(core.contains("\"defaultRequire\": 1"), "core mixin config must remain strict");
        check(!core.contains(OPTIONAL_PLUGIN), "optional fail-soft plugin must not attach to core config");

        for (String forbidden : List.of(
                "compat.traveloptics.",
                "compat.geomancyplus.",
                "compat.familiars.",
                "AnnihilationSpellMixin",
                "CataclysmFlareBombMixin",
                "CataclysmWitherHowitzerMixin")) {
            check(!core.contains(forbidden), "optional adapter leaked into required core config: " + forbidden);
        }

        Set<String> allAdapters = new LinkedHashSet<>();
        for (Path configPath : OPTIONAL_CONFIGS) {
            check(Files.isRegularFile(configPath), "optional mixin config is missing: " + configPath);
            String config = Files.readString(configPath);
            check(config.contains("\"required\": false"), "optional config must not be required: " + configPath);
            check(config.contains("\"defaultRequire\": 0"),
                    "optional config must make missing injector targets non-fatal: " + configPath);
            check(config.contains("\"plugin\": \"" + OPTIONAL_PLUGIN + "\""),
                    "optional config must install fail-soft diagnostics: " + configPath);
            check(config.contains("\"refmap\": \"magic_team.refmap.json\""),
                    "optional config must use the shared refmap: " + configPath);

            for (String adapter : readMixinRegistrations(config)) {
                check(allAdapters.add(adapter), "optional adapter appears in more than one config: " + adapter);
                Path sourcePath = MIXIN_ROOT.resolve(adapter.replace('.', '/') + ".java");
                check(Files.isRegularFile(sourcePath), "configured optional mixin source is missing: " + adapter);
                String source = Files.readString(sourcePath);
                check(source.contains("@Pseudo"), "optional addon mixin must use @Pseudo: " + adapter);
                for (String forbiddenImport : OPTIONAL_IMPORT_PREFIXES) {
                    check(!source.contains(forbiddenImport),
                            "optional addon mixin hard-links an optional addon: " + adapter + " -> " + forbiddenImport);
                }
            }
        }

        check(allAdapters.contains("AnnihilationSpellMixin"), "Travel Optics root adapter must remain optional");
        check(allAdapters.contains("compat.geomancyplus.SolarStormFriendlyFireMixin"),
                "Geomancy Plus adapters must remain optional");
        check(allAdapters.contains("compat.familiars.HikenFriendlyFireMixin"),
                "Familiars adapters must remain optional");
        check(allAdapters.contains("CataclysmFlareBombMixin"), "Cataclysm root adapters must remain optional");

        String buildGradle = Files.readString(BUILD_GRADLE);
        for (String configName : List.of(
                "magic_team.mixins.json",
                "magic_team.traveloptics.mixins.json",
                "magic_team.geomancyplus.mixins.json",
                "magic_team.familiars.mixins.json",
                "magic_team.cataclysm.mixins.json")) {
            check(buildGradle.contains(configName), "build.gradle must register mixin config: " + configName);
        }
    }

    private static Set<String> readMixinRegistrations(String json) {
        int marker = json.indexOf("\"mixins\"");
        int open = json.indexOf('[', marker);
        int close = json.indexOf(']', open);
        check(marker >= 0 && open >= 0 && close > open, "could not locate optional mixins array");
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
