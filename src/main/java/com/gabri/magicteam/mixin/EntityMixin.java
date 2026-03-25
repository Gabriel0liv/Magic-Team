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
        // Se a aliança global estiver desligada, deixamos a lógica original
        if (!com.gabri.magicteam.MagicTeamConfig.SERVER.enableGlobalAlliance.get()) return;

        // Se o bloqueio de dano estiver DESLIGADO, não forçamos a aliança (permite dano)
        if (!com.gabri.magicteam.MagicTeamConfig.SERVER.enableDamageBlock.get()) return;

        if (TeamUtils.areAllies((Entity) (Object) this, other)) {
            cir.setReturnValue(true);
        }
    }
}
