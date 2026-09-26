package com.gabri.magicteam.mixin;

import com.gabri.magicteam.util.MagicAttribution;
import com.gabri.magicteam.util.MagicAttributionIndex;
import com.gabri.magicteam.util.MagicTeamEffectContext;
import com.gabri.magicteam.util.TeamUtils;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Captures magic attribution at the shared server entity-spawn boundary.
 * Any entity spawned while a proven spell context is active can therefore
 * carry caster/spell classification into later ticks without a spell adapter.
 */
@Mixin(ServerLevel.class)
public abstract class ServerLevelMagicAttributionMixin {

    @Inject(method = "addFreshEntity", at = @At("HEAD"))
    private void magicTeam$captureSpawnAttribution(Entity entity, CallbackInfoReturnable<Boolean> cir) {
        if (!TeamUtils.isEnabled() || entity == null) {
            return;
        }

        MagicAttribution attribution = MagicTeamEffectContext.currentAttribution();
        if (attribution != null) {
            MagicAttributionIndex.record(entity, attribution);
        }
    }
}
