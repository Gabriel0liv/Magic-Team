package com.gabri.magicteam.mixin.compat;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.mixin.extensibility.IMixinConfig;
import org.spongepowered.asm.mixin.extensibility.IMixinErrorHandler;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.util.Set;

/**
 * Keeps optional addon compatibility failures from becoming server-wide
 * startup failures. Core Magic Team mixins are deliberately excluded.
 */
public final class OptionalAddonMixinErrorHandler implements IMixinErrorHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger("MagicTeam/OptionalAdapters");
    private static final String MIXIN_PREFIX = "com.gabri.magicteam.mixin.";
    private static final Set<String> OPTIONAL_ROOT_MIXINS = Set.of(
            MIXIN_PREFIX + "AnnihilationSpellMixin",
            MIXIN_PREFIX + "CataclysmFlareBombMixin",
            MIXIN_PREFIX + "CataclysmWitherHowitzerMixin"
    );

    @Override
    public ErrorAction onPrepareError(IMixinConfig config,
                                      Throwable throwable,
                                      IMixinInfo mixin,
                                      ErrorAction action) {
        return handleOptionalFailure(null, throwable, mixin, action);
    }

    @Override
    public ErrorAction onApplyError(String targetClassName,
                                    Throwable throwable,
                                    IMixinInfo mixin,
                                    ErrorAction action) {
        return handleOptionalFailure(targetClassName, throwable, mixin, action);
    }

    private static ErrorAction handleOptionalFailure(String targetClassName,
                                                     Throwable throwable,
                                                     IMixinInfo mixin,
                                                     ErrorAction action) {
        String mixinName = mixin == null ? null : mixin.getClassName();
        if (!isOptionalMixin(mixinName)) {
            return action;
        }

        String family = familyFor(mixinName);
        LOGGER.warn(
                "Magic Team optional adapter degraded: family={}, adapter={}, target={}. "
                        + "The adapter-specific protection was skipped; generic Magic Team protection "
                        + "continues where applicable and addon-native fallback remains in effect. Cause: {}",
                family,
                mixinName,
                targetClassName == null ? "<prepare>" : targetClassName,
                throwable == null ? "unknown" : throwable.toString()
        );
        return ErrorAction.WARN;
    }

    static boolean isOptionalMixin(String mixinName) {
        if (mixinName == null) {
            return false;
        }
        return mixinName.startsWith(MIXIN_PREFIX + "compat.traveloptics.")
                || mixinName.startsWith(MIXIN_PREFIX + "compat.geomancyplus.")
                || mixinName.startsWith(MIXIN_PREFIX + "compat.familiars.")
                || OPTIONAL_ROOT_MIXINS.contains(mixinName);
    }

    private static String familyFor(String mixinName) {
        if (mixinName == null) {
            return "unknown";
        }
        if (mixinName.startsWith(MIXIN_PREFIX + "compat.traveloptics.")
                || mixinName.endsWith("AnnihilationSpellMixin")) {
            return "Travel Optics";
        }
        if (mixinName.startsWith(MIXIN_PREFIX + "compat.geomancyplus.")) {
            return "Geomancy Plus";
        }
        if (mixinName.startsWith(MIXIN_PREFIX + "compat.familiars.")) {
            return "Alshanex Familiars";
        }
        if (mixinName.endsWith("CataclysmFlareBombMixin")
                || mixinName.endsWith("CataclysmWitherHowitzerMixin")) {
            return "Cataclysm";
        }
        return "optional addon";
    }
}
