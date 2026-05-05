package com.gabri.magicteam.mixin;

import com.gabri.magicteam.util.TeamUtils;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = Entity.class, priority = 2000)
public abstract class EntityMixin {

    @Inject(method = "isAlliedTo(Lnet/minecraft/world/entity/Entity;)Z", at = @At("HEAD"), cancellable = true)
    private void onIsAlliedTo(Entity other, CallbackInfoReturnable<Boolean> cir) {
        Entity self = (Entity) (Object) this;
        if (!isIronsMagicEntity(self) && !isIronsMagicEntity(other)) {
            return;
        }

        cir.setReturnValue(TeamUtils.areAllies(self, other));
    }

    @Unique
    private static boolean isIronsMagicEntity(Entity entity) {
        if (entity == null) {
            return false;
        }

        String className = entity.getClass().getName();
        return className.startsWith("io.redspace.ironsspellbooks.");
    }
}
