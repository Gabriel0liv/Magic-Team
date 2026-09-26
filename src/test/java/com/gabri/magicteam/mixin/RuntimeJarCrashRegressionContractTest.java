package com.gabri.magicteam.mixin;

import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Structural regression checks for mixin failures observed against the real
 * Forge/Arclight runtime jars used by the server smoke test.
 */
public final class RuntimeJarCrashRegressionContractTest {
    private static final Path MIXIN_ROOT = Path.of("src/main/java/com/gabri/magicteam/mixin");

    private RuntimeJarCrashRegressionContractTest() {
    }

    public static void main(String[] args) throws Exception {
        playerCastTickTargetsTheCompiledLambdaBody();
        aquaMissilesMatchesTheSingleRuntimeAllianceCheck();
        solarStormGuardsTheTargetPredicateWithoutRedirectingInternals();
        tidalGraspGuardsReleaseAtMethodBoundary();
    }

    private static void playerCastTickTargetsTheCompiledLambdaBody() throws Exception {
        String source = Files.readString(MIXIN_ROOT.resolve("MagicManagerCastDispatchMixin.java"));

        check(source.contains("method = \"lambda$tick$0\""),
                "MagicManager player cast redirect must target the compiled lambda body used by Iron's 3.15.3");
        check(!source.contains("method = \"tick\""),
                "redirecting MagicManager.tick misses the onServerCastTick invocation inside the compiled lambda");
    }

    private static void aquaMissilesMatchesTheSingleRuntimeAllianceCheck() throws Exception {
        String source = Files.readString(
                MIXIN_ROOT.resolve("compat/traveloptics/AquaMissilesFriendlyFireMixin.java"));

        check(source.contains("require = 1"),
                "Travel Optics 6.3.0 Aqua Missiles has one matching alliance check in the runtime jar");
        check(!source.contains("require = 2"),
                "requiring two Aqua Missiles redirect matches crashes Travel Optics 6.3.0 at startup");
    }

    private static void solarStormGuardsTheTargetPredicateWithoutRedirectingInternals() throws Exception {
        String source = Files.readString(
                MIXIN_ROOT.resolve("compat/geomancyplus/SolarStormFriendlyFireMixin.java"));

        check(source.contains("@Inject("),
                "Solar Storm should guard the addon target predicate directly");
        check(source.contains("at = @At(\"HEAD\")"),
                "Solar Storm guard must run before the addon target predicate performs side-independent checks");
        check(source.contains("cancellable = true"),
                "Solar Storm guard must be able to reject protected allied targets");
        check(!source.contains("@Redirect("),
                "Solar Storm must not depend on an internal vanilla alliance call that is absent in Geomancy Plus 2.0.0");
        check(source.contains("TeamUtils.shouldBlockFriendlyFire"),
                "Solar Storm direct predicate guard must still use the Magic Team policy");
    }

    private static void tidalGraspGuardsReleaseAtMethodBoundary() throws Exception {
        String source = Files.readString(
                MIXIN_ROOT.resolve("compat/traveloptics/TidalGraspFriendlyFireMixin.java"));

        check(source.contains("method = \"onCast\""),
                "Tidal Grasp must recheck its selected target at release");
        check(source.contains("at = @At(\"HEAD\")"),
                "Tidal Grasp release protection must not depend on addon-internal teleport/effect calls");
        check(source.contains("cancellable = true"),
                "Tidal Grasp release guard must be able to cancel the protected cast before side effects");
        check(!source.contains("@Redirect("),
                "Tidal Grasp 6.3.0 runtime bytecode does not contain the assumed teleport redirect target");
        check(source.contains("TeamUtils.shouldBlockFriendlyFire"),
                "Tidal Grasp release guard must still use the Magic Team policy");
    }

    private static void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
