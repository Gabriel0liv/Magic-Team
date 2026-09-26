package com.gabri.magicteam.mixin.compat;

import org.spongepowered.asm.lib.tree.ClassNode;
import org.spongepowered.asm.mixin.Mixins;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.util.List;
import java.util.Set;

/**
 * Shared plugin for optional addon mixin configs.
 *
 * <p>The plugin deliberately does not decide whether a mixin should apply. Its
 * only bootstrap responsibility is registering the fail-soft error handler
 * before optional adapters are transformed.</p>
 */
public final class OptionalAddonMixinPlugin implements IMixinConfigPlugin {
    private static boolean errorHandlerRegistered;

    @Override
    public void onLoad(String mixinPackage) {
        if (!errorHandlerRegistered) {
            Mixins.registerErrorHandlerClass(OptionalAddonMixinErrorHandler.class.getName());
            errorHandlerRegistered = true;
        }
    }

    @Override
    public String getRefMapperConfig() {
        return null;
    }

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        return true;
    }

    @Override
    public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {
    }

    @Override
    public List<String> getMixins() {
        return null;
    }

    @Override
    public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
    }

    @Override
    public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
    }
}
