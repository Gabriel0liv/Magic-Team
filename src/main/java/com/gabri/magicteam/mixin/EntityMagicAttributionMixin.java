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
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.function.Consumer;

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

    @Redirect(
            method = "tickNonPassenger",
            at = @At(
                    value = "INVOKE",
                    target = "Ljava/util/function/Consumer;accept(Ljava/lang/Object;)V"
            )
    )
    @SuppressWarnings({"unchecked", "rawtypes"})
    private void magicTeam$withPersistentMagicContext(Consumer ticker, Object rawEntity) {
        if (!(rawEntity instanceof Entity entity) || !TeamUtils.isEnabled()) {
            ticker.accept(rawEntity);
            return;
        }

        ServerLevel level = (ServerLevel) (Object) this;
        MagicAttribution attribution = MagicAttributionIndex.get(entity, level.getGameTime());
        if (attribution == null) {
            ticker.accept(rawEntity);
            return;
        }

        MagicTeamEffectContext.push(entity, attribution);
        try {
            ticker.accept(rawEntity);
        } finally {
            MagicTeamEffectContext.pop();
        }
    }
}
