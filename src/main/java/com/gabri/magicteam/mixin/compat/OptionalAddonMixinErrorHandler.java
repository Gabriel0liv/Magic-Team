package com.gabri.magicteam.mixin.compat;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.mixin.extensibility.IMixinConfig;
import org.spongepowered.asm.mixin.extensibility.IMixinErrorHandler;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

/**
 * Fail-soft diagnostics for the small optional compatibility layer.
 *
 * <p>Prepare-time failure can safely skip an optional adapter. Apply-time
 * failure may occur after target transformation has started, so Magic Team logs
 * the degradation but preserves Mixin's original action instead of forcing WARN
 * and risking a partially transformed class.</p>
 */
public final class OptionalAddonMixinErrorHandler implements IMixinErrorHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger("MagicTeam/OptionalAdapters");
    private static final String MIXIN_PREFIX = "com.gabri.magicteam.mixin.";

    @Override
    public ErrorAction onPrepareError(IMixinConfig config,
                                      Throwable throwable,
                                      IMixinInfo mixin,
                                      ErrorAction action) {
        String mixinName = mixin == null ? null : mixin.getClassName();
        if (!isOptionalMixin(mixinName)) {
            return action;
        }

        logDegradation("prepare", null, throwable, mixinName, ErrorAction.WARN);
        return ErrorAction.WARN;
    }

    @Override
    public ErrorAction onApplyError(String targetClassName,
                                    Throwable throwable,
                                    IMixinInfo mixin,
                                    ErrorAction action) {
        String mixinName = mixin == null ? null : mixin.getClassName();
        if (!isOptionalMixin(mixinName)) {
            return action;
        }

        logDegradation("apply", targetClassName, throwable, mixinName, action);
        return action;
    }

    private static void logDegradation(String phase,
                                       String targetClassName,
                                       Throwable throwable,
                                       String mixinName,
                                       ErrorAction resultingAction) {
        LOGGER.warn(
                "Magic Team optional adapter degraded: phase={}, family={}, adapter={}, target={}, action={}. "
                        + "Only adapter-specific compatibility is affected; generic Magic Team protection "
                        + "continues where applicable and addon-native fallback remains in effect. Cause: {}",
                phase,
                familyFor(mixinName),
                mixinName,
                targetClassName == null ? "<prepare>" : targetClassName,
                resultingAction,
                throwable == null ? "unknown" : throwable.toString()
        );
    }

    static boolean isOptionalMixin(String mixinName) {
        if (mixinName == null) {
            return false;
        }
        return mixinName.startsWith(MIXIN_PREFIX + "compat.traveloptics.")
                || mixinName.startsWith(MIXIN_PREFIX + "compat.geomancyplus.")
                || mixinName.startsWith(MIXIN_PREFIX + "compat.familiars.")
                || mixinName.startsWith(MIXIN_PREFIX + "compat.cataclysm.");
    }

    private static String familyFor(String mixinName) {
        if (mixinName == null) {
            return "unknown";
        }
        if (mixinName.startsWith(MIXIN_PREFIX + "compat.traveloptics.")) {
            return "Travel Optics";
        }
        if (mixinName.startsWith(MIXIN_PREFIX + "compat.geomancyplus.")) {
            return "Geomancy Plus";
        }
        if (mixinName.startsWith(MIXIN_PREFIX + "compat.familiars.")) {
            return "Alshanex Familiars";
        }
        if (mixinName.startsWith(MIXIN_PREFIX + "compat.cataclysm.")) {
            return "Cataclysm";
        }
        return "optional addon";
    }
}
