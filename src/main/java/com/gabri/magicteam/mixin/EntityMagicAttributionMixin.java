package com.gabri.magicteam.mixin;

import com.gabri.magicteam.util.MagicAttribution;
import com.gabri.magicteam.util.MagicAttributionIndex;
import com.gabri.magicteam.util.MagicEffectAttributionIndex;
import com.gabri.magicteam.util.MagicTeamEffectContext;
import com.gabri.magicteam.util.TeamUtils;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.function.BooleanSupplier;

/**
 * Generic server boundary for magic entities that outlive their cast stack.
 *
 * <p>Entities spawned while a proven spell context is active inherit only the
 * stable MagicAttribution value. Later, when the server ticks that attributed
 * entity, its tick executes inside a temporary magic context. This covers
 * custom addon projectiles/AOEs/delayed entities without naming their classes.</p>
 */
@Mixin(ServerLevel.class)
public abstract class EntityMagicAttributionMixin {
    private static final long CLEANUP_INTERVAL_TICKS = 200L;

    @Inject(
            method = "addFreshEntity(Lnet/minecraft/world/entity/Entity;)Z",
            at = @At("HEAD")
    )
    private void magicTeam$inheritAttributionOnSpawn(Entity entity, CallbackInfoReturnable<Boolean> cir) {
        if (!TeamUtils.isEnabled() || entity == null) {
            return;
        }

        MagicAttribution attribution = MagicTeamEffectContext.currentAttribution();
        if (attribution != null) {
            MagicAttributionIndex.record(entity, attribution);
        }
    }

    /**
     * Wrap the actual Entity#tick invocation rather than an implementation-detail
     * helper such as Consumer.accept. Entity#tick is the stable common boundary
     * for every non-passenger entity processed by ServerLevel.
     */
    @Redirect(
            method = "tickNonPassenger",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/Entity;tick()V"
            )
    )
    private void magicTeam$withPersistentMagicContext(Entity entity) {
        if (!TeamUtils.isEnabled()) {
            entity.tick();
            return;
        }

        ServerLevel level = (ServerLevel) (Object) this;
        MagicAttribution attribution = MagicAttributionIndex.refresh(entity, level.getGameTime());
        if (attribution == null) {
            entity.tick();
            return;
        }

        MagicTeamEffectContext.push(entity, attribution);
        try {
            entity.tick();
        } finally {
            MagicTeamEffectContext.pop();
        }
    }

    @Inject(
            method = "tick(Ljava/util/function/BooleanSupplier;)V",
            at = @At("RETURN")
    )
    private void magicTeam$cleanupExpiredAttribution(BooleanSupplier hasTimeLeft, CallbackInfo ci) {
        ServerLevel level = (ServerLevel) (Object) this;
        long gameTime = level.getGameTime();
        if (gameTime % CLEANUP_INTERVAL_TICKS != 0L) {
            return;
        }

        MagicAttributionIndex.cleanup(gameTime);
        MagicEffectAttributionIndex.cleanup(gameTime);
    }
}
