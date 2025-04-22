package net.IneiTsuki.true_invis.mixin;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.player.PlayerEntity;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public class InvisibilityRemoveMixin {

    @Inject(method = "removeStatusEffect", at = @At("TAIL"))
    private void onRemoveStatusEffect(StatusEffect type, CallbackInfoReturnable<Boolean> cir) {
        if ((Object) this instanceof PlayerEntity player) {
            if (type == StatusEffects.INVISIBILITY) {
                player.setInvisible(false); // Restore visibility
            }
        }
    }
}
