package com.gabri.magicteam.mixin.compat.traveloptics;

import com.gabri.magicteam.util.TeamUtils;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(targets = "com.gametechbc.traveloptics.spells.aqua.AquaMissilesSpell", remap = false)
public abstract class AquaMissilesFriendlyFireMixin {
    @Redirect(
            method = "getRandomlyLookingAtEntityFor(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/entity/LivingEntity;D)Lnet/minecraft/world/entity/Entity;",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;m_7307_(Lnet/minecraft/world/entity/Entity;)Z", remap = false),
            require = 2,
            remap = false
    )
    private static boolean magicTeam$useFriendlyFireForRandomTarget(Entity source, Entity target) {
        return TeamUtils.shouldBlockFriendlyFire(source, target);
    }

    @Inject(
            method = "getRaycastTarget(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/entity/LivingEntity;D)Lnet/minecraft/world/entity/Entity;",
            at = @At("RETURN"),
            cancellable = true,
            remap = false
    )
    private void magicTeam$filterProtectedRaycastTarget(Level level, LivingEntity caster, double range,
                                                         CallbackInfoReturnable<Entity> cir) {
        Entity target = cir.getReturnValue();
        if (target != null && TeamUtils.shouldBlockFriendlyFire(caster, target)) {
            cir.setReturnValue(null);
        }
    }
}
