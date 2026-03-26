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
        // Surgical override for damage/collision systems
        if (com.gabri.magicteam.util.TeamUtils.FORCE_ENEMIES_SCOPE.get()) {
            cir.setReturnValue(false);
            return;
        }

        boolean areAllies = TeamUtils.areAllies((Entity) (Object) this, other);
        
        if (areAllies) {
            // Se são aliados pelo Magic-Team (mesmo time e sem bypass)
            // Retornamos TRUE apenas se a aliança global estiver LIGADA.
            // Se estiver DESLIGADA, retornamos FALSE para forçar o Minecraft a permitir colisões de projéteis.
            cir.setReturnValue(com.gabri.magicteam.MagicTeamConfig.SERVER.enableGlobalAlliance.get());
        } else {
            // Se NÃO são aliados (Bypass ativo ou times diferentes),
            // forçamos o retorno FALSE ignorando qualquer time do Minecraft Vanilla.
            cir.setReturnValue(false);
        }
    }
}
