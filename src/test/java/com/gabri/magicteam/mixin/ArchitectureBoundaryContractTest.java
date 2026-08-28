package com.gabri.magicteam.mixin;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/** Dependency-free architecture boundaries that must remain true across addon fixes. */
public final class ArchitectureBoundaryContractTest {
    private static final Path MIXIN_ROOT = Path.of("src/main/java/com/gabri/magicteam/mixin");
    private static final Path TEAM_UTILS = Path.of("src/main/java/com/gabri/magicteam/util/TeamUtils.java");
    private static final Path CORE_MIXINS = Path.of("src/main/resources/magic_team.mixins.json");
    private static final Path TRAVELOPTICS_MIXINS = Path.of("src/main/resources/magic_team.traveloptics.mixins.json");
    private static final Path GEOMANCY_MIXINS = Path.of("src/main/resources/magic_team.geomancyplus.mixins.json");
    private static final Path FAMILIARS_MIXINS = Path.of("src/main/resources/magic_team.familiars.mixins.json");
    private static final Path CATACLYSM_MIXINS = Path.of("src/main/resources/magic_team.cataclysm.mixins.json");
    private static final Path BUILD_GRADLE = Path.of("build.gradle");

    private static final List<String> OPTIONAL_IMPORT_PREFIXES = List.of(
            "import com.gametechbc.traveloptics.",
            "import com.gametechbc.gtbcs_geomancy_plus.",
            "import net.alshanex.alshanex_familiars.",
            "import com.github.L_Ender.cataclysm."
    );

    private static final List<String> TRAVELOPTICS_ADAPTERS = List.of(
            "AnnihilationSpellMixin",
            "compat.traveloptics.AbyssalHideFriendlyFireMixin",
            "compat.traveloptics.AbyssalStrikeFriendlyFireMixin",
            "compat.traveloptics.AcidRainAoeContextMixin",
            "compat.traveloptics.AquaMissilesFriendlyFireMixin",
            "compat.traveloptics.AquaVortexFriendlyFireMixin",
            "compat.traveloptics.ArcaneArtilleryTargetingMixin",
            "compat.traveloptics.BanishFriendlyFireMixin",
            "compat.traveloptics.BlackoutAntiMagicFieldContextMixin",
            "compat.traveloptics.CataclysmAmethystClusterFriendlyFireMixin",
            "compat.traveloptics.CataclysmAncientDesertSteleFriendlyFireMixin",
            "compat.traveloptics.CataclysmAxeBladeFriendlyFireMixin",
            "compat.traveloptics.CataclysmFlameJetFriendlyFireMixin",
            "compat.traveloptics.CataclysmLaserBeamFriendlyFireMixin",
            "compat.traveloptics.CataclysmPhantomArrowFriendlyFireMixin",
            "compat.traveloptics.CataclysmVoidRuneFriendlyFireMixin",
            "compat.traveloptics.CataclysmVoidVortexFriendlyFireMixin",
            "compat.traveloptics.ChargedSandsLevelOneFriendlyFireMixin",
            "compat.traveloptics.ChargedSandsLevelTwoFriendlyFireMixin",
            "compat.traveloptics.CoralBoltFriendlyFireMixin",
            "compat.traveloptics.CrimsonDescendFriendlyFireMixin",
            "compat.traveloptics.CursedWraithbladeFriendlyFireMixin",
            "compat.traveloptics.DimensionalSpikeFriendlyFireMixin",
            "compat.traveloptics.DragonSpiritFriendlyFireMixin",
            "compat.traveloptics.DragonSpiritSpellFriendlyFireMixin",
            "compat.traveloptics.DyingStarFriendlyFireMixin",
            "compat.traveloptics.EndEruptionFriendlyFireMixin",
            "compat.traveloptics.EnsnareFriendlyFireMixin",
            "compat.traveloptics.EternalMaelstromTridentFriendlyFireMixin",
            "compat.traveloptics.ExtendedAbyssBlastFriendlyFireMixin",
            "compat.traveloptics.ExtendedDeathLaserFriendlyFireMixin",
            "compat.traveloptics.ExtendedFlameStrikeFriendlyFireMixin",
            "compat.traveloptics.ExtendedIgnisFireballContextMixin",
            "compat.traveloptics.ExtendedPhantomArrowFriendlyFireMixin",
            "compat.traveloptics.ExtendedPhantomHalberdFriendlyFireMixin",
            "compat.traveloptics.ExtendedSandstormFriendlyFireMixin",
            "compat.traveloptics.ExtendedStormSerpentFriendlyFireMixin",
            "compat.traveloptics.ExtendedWaterBoltFriendlyFireMixin",
            "compat.traveloptics.ExtendedWaterSpearFriendlyFireMixin",
            "compat.traveloptics.ExtendedWaveFriendlyFireMixin",
            "compat.traveloptics.ExtendedWitherHomingMissileContextMixin",
            "compat.traveloptics.ExtendedWitherHowitzerContextMixin",
            "compat.traveloptics.FlareVacuumAttributionMixin",
            "compat.traveloptics.FloodSlashFriendlyFireMixin",
            "compat.traveloptics.ForlornHarbingerFriendlyFireMixin",
            "compat.traveloptics.ForgeServerEventsFriendlyFireMixin",
            "compat.traveloptics.GalenaMarkFriendlyFireMixin",
            "compat.traveloptics.GalenaShatterFriendlyFireMixin",
            "compat.traveloptics.GyroSlashFriendlyFireMixin",
            "compat.traveloptics.HarbingersWrathFriendlyFireMixin",
            "compat.traveloptics.LightningSandstormAoeFriendlyFireMixin",
            "compat.traveloptics.LightningSandstormPierceFriendlyFireMixin",
            "compat.traveloptics.MaelstromFriendlyFireMixin",
            "compat.traveloptics.MaelstromTridentPhantomFriendlyFireMixin",
            "compat.traveloptics.MechanizedExoskeletonFriendlyFireMixin",
            "compat.traveloptics.MeteorStormFriendlyFireMixin",
            "compat.traveloptics.NightwardenCloneBaseFriendlyFireMixin",
            "compat.traveloptics.NightwardenSpinCloneFriendlyFireMixin",
            "compat.traveloptics.OrbitalVoidFriendlyFireMixin",
            "compat.traveloptics.PrimordialCrestFriendlyFireMixin",
            "compat.traveloptics.PsychicBoltFriendlyFireMixin",
            "compat.traveloptics.RainfallInteractionMixin",
            "compat.traveloptics.ReturningWaveFriendlyFireMixin",
            "compat.traveloptics.ReversalFriendlyFireMixin",
            "compat.traveloptics.ScourgeOfTheSandsTargetingMixin",
            "compat.traveloptics.ScourgeOfTheSandsLevelThreeTargetingMixin",
            "compat.traveloptics.SpiritDamageHelperFriendlyFireMixin",
            "compat.traveloptics.StellothornContextMixin",
            "compat.traveloptics.SupermassiveBlackHoleFriendlyFireMixin",
            "compat.traveloptics.SyncedAoeEntityFriendlyFireMixin",
            "compat.traveloptics.TectonicCrestFriendlyFireMixin",
            "compat.traveloptics.TidalGraspEffectContextMixin",
            "compat.traveloptics.TidalGraspFriendlyFireMixin",
            "compat.traveloptics.VoidSlashContextMixin",
            "compat.traveloptics.VortexPunchFriendlyFireMixin"
    );

    private static final List<String> GEOMANCY_ADAPTERS = List.of(
            "compat.geomancyplus.SolarStormFriendlyFireMixin",
            "compat.geomancyplus.TremorStepFriendlyFireMixin"
    );

    private static final List<String> FAMILIARS_ADAPTERS = List.of(
            "compat.familiars.HikenFriendlyFireMixin",
            "compat.familiars.IllusionistDecoyContextMixin",
            "compat.familiars.MayhemDirectHitFriendlyFireMixin",
            "compat.familiars.DragonEggFriendlyFireMixin",
            "compat.familiars.LullabyFriendlyFireMixin",
            "compat.familiars.SonataFriendlyFireMixin",
            "compat.familiars.HarpExplosionFriendlyFireMixin",
            "compat.familiars.ServerEventsRetaliationFriendlyFireMixin"
    );

    private static final List<String> CATACLYSM_ADAPTERS = List.of(
            "CataclysmFlareBombMixin",
            "CataclysmWitherHowitzerMixin"
    );

    private ArchitectureBoundaryContractTest() {
    }

    public static void main(String[] args) throws Exception {
        pseudoMixinsDoNotHardLinkOptionalAddons();
        allianceIdentityDoesNotDependOnFriendlyFire();
        optionalAddonMixinConfigsAreIsolated();
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
        String areAllies = source.substring(start, nextMethod);

        check(!areAllies.contains("shouldBlockFriendlyFire"),
                "areAllies must not delegate to hostile friendly-fire policy");
        check(!areAllies.contains("isAllowFriendlyFire"),
                "areAllies must not inspect scoreboard friendly-fire permission");
        check(!areAllies.toLowerCase().contains("friendlyfire"),
                "areAllies must remain relationship-only");
        check(areAllies.contains("ENTITY_RELATIONS.areAllies"),
                "areAllies must delegate relationship identity to BabelEntityRelations");
    }

    private static void optionalAddonMixinConfigsAreIsolated() throws IOException {
        String core = Files.readString(CORE_MIXINS);
        check(core.contains("\"required\": true"), "core mixin config must remain required");
        check(core.contains("\"defaultRequire\": 1"), "core mixin config must remain strict");

        for (String forbidden : List.of(
                "compat.traveloptics.",
                "compat.geomancyplus.",
                "compat.familiars.",
                "AnnihilationSpellMixin",
                "CataclysmFlareBombMixin",
                "CataclysmWitherHowitzerMixin")) {
            check(!core.contains(forbidden), "optional adapter leaked into required core config: " + forbidden);
        }

        String travelOptics = readOptionalConfig(TRAVELOPTICS_MIXINS);
        String geomancy = readOptionalConfig(GEOMANCY_MIXINS);
        String familiars = readOptionalConfig(FAMILIARS_MIXINS);
        String cataclysm = readOptionalConfig(CATACLYSM_MIXINS);
        List<String> optionalConfigs = List.of(travelOptics, geomancy, familiars, cataclysm);

        assertFamilyMembership(TRAVELOPTICS_ADAPTERS, travelOptics, optionalConfigs, "Travel Optics");
        assertFamilyMembership(GEOMANCY_ADAPTERS, geomancy, optionalConfigs, "Geomancy Plus");
        assertFamilyMembership(FAMILIARS_ADAPTERS, familiars, optionalConfigs, "Familiars");
        assertFamilyMembership(CATACLYSM_ADAPTERS, cataclysm, optionalConfigs, "Cataclysm");

        for (String adapter : allOptionalAdapters()) {
            Path sourcePath = MIXIN_ROOT.resolve(adapter.replace('.', '/') + ".java");
            check(Files.exists(sourcePath), "configured optional mixin source is missing: " + adapter);
            String source = Files.readString(sourcePath);
            check(source.contains("@Pseudo"), "optional addon mixin must use @Pseudo: " + adapter);
            for (String forbiddenImport : OPTIONAL_IMPORT_PREFIXES) {
                check(!source.contains(forbiddenImport),
                        "optional addon mixin hard-links an optional addon: " + adapter + " -> " + forbiddenImport);
            }
        }

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

    private static String readOptionalConfig(Path path) throws IOException {
        check(Files.exists(path), "optional mixin config is missing: " + path);
        String content = Files.readString(path);
        check(content.contains("\"required\": false"), "optional mixin config must not be required: " + path);
        check(content.contains("\"defaultRequire\": 1"), "optional mixin config must keep strict injector matching: " + path);
        check(content.contains("\"refmap\": \"magic_team.refmap.json\""), "optional mixin config must use the shared refmap: " + path);
        return content;
    }

    private static void assertFamilyMembership(List<String> expectedAdapters, String familyConfig,
                                               List<String> allOptionalConfigs, String familyName) {
        for (String adapter : expectedAdapters) {
            check(familyConfig.contains("\"" + adapter + "\""),
                    familyName + " config is missing adapter: " + adapter);
            int occurrences = 0;
            for (String config : allOptionalConfigs) {
                if (config.contains("\"" + adapter + "\"")) {
                    occurrences++;
                }
            }
            check(occurrences == 1, "optional adapter must appear in exactly one optional config: " + adapter);
        }
    }

    private static List<String> allOptionalAdapters() {
        return java.util.stream.Stream.of(
                        TRAVELOPTICS_ADAPTERS,
                        GEOMANCY_ADAPTERS,
                        FAMILIARS_ADAPTERS,
                        CATACLYSM_ADAPTERS)
                .flatMap(List::stream)
                .toList();
    }

    private static void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
