package net.IneiTsuki.true_invis.mixin;


import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public class InvisibilityArmorHideMixin {

    @Inject(method = "addStatusEffect*", at = @At("TAIL"))
    private void onAddStatusEffect(StatusEffectInstance effect, CallbackInfoReturnable<Boolean> cir) {
        if ((Object) this instanceof PlayerEntity player) {
            if (effect.getEffectType() == StatusEffects.INVISIBILITY) {
                player.setInvisible(true); // Fully hide player, including armor/items
            }
        }
    }
}
