package com.gabri.magicteam.mixin;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/** Dependency-free architecture boundaries that must remain true across addon fixes. */
public final class ArchitectureBoundaryContractTest {
    private static final Path MIXIN_ROOT = Path.of("src/main/java/com/gabri/magicteam/mixin");
    private static final Path TEAM_UTILS = Path.of("src/main/java/com/gabri/magicteam/util/TeamUtils.java");
    private static final List<String> OPTIONAL_IMPORT_PREFIXES = List.of(
            "import com.gametechbc.traveloptics.",
            "import com.gametechbc.gtbcs_geomancy_plus.",
            "import net.alshanex.alshanex_familiars.",
            "import com.github.L_Ender.cataclysm."
    );

    private ArchitectureBoundaryContractTest() {
    }

    public static void main(String[] args) throws Exception {
        pseudoMixinsDoNotHardLinkOptionalAddons();
        allianceIdentityDoesNotDependOnFriendlyFire();
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

    private static void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
