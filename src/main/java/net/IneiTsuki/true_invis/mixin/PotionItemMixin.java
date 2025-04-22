package net.IneiTsuki.true_invis.mixin;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsage;
import net.minecraft.item.PotionItem;
import net.minecraft.item.Items;
import net.minecraft.potion.PotionUtil;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.world.World;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PotionItem.class)
public class PotionItemMixin {

    @Inject(method = "finishUsing", at = @At("HEAD"), cancellable = true)
    private void onFinishUsing(ItemStack stack, World world, LivingEntity user, CallbackInfoReturnable<ItemStack> cir) {
        if (!world.isClient && stack.getItem() == Items.POTION && user instanceof PlayerEntity player) {
            // Apply potion effects manually
            for (StatusEffectInstance effect : PotionUtil.getPotionEffects(stack)) {
                StatusEffectInstance hiddenEffect = new StatusEffectInstance(
                        effect.getEffectType(),
                        effect.getDuration(),
                        effect.getAmplifier(),
                        effect.isAmbient(),
                        false, // No particles
                        false  // No icon
                );
                player.addStatusEffect(hiddenEffect);
            }

            // Return the empty bottle, same as vanilla
            ItemStack result = ItemUsage.exchangeStack(stack, player, new ItemStack(Items.GLASS_BOTTLE));
            cir.setReturnValue(result);
        }
    }
}