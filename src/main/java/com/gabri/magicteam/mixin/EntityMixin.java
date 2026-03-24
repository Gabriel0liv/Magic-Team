package com.gabri.magicteam.mixin;

import com.gabri.magicteam.util.TeamUtils;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Entity.class)
public abstract class EntityMixin {

    @Inject(method = "isAlliedTo(Lnet/minecraft/world/entity/Entity;)Z", at = @At("HEAD"), cancellable = true)
    private void onIsAlliedTo(Entity other, CallbackInfoReturnable<Boolean> cir) {
        if (TeamUtils.areAllies((Entity) (Object) this, other)) {
            // DEBUG LOGGING
            System.out.println("[MagicTeam] EntityMixin: marking " + ((Entity)(Object)this).getName().getString() + " and " + other.getName().getString() + " as ALLIES");
            cir.setReturnValue(true);
        }
    }
}
