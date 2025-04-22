package net.IneiTsuki.true_invis.enchantment;

import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentTarget;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.text.Text;

public class TrueSightEnchantment extends Enchantment {

    public TrueSightEnchantment() {
        super(Rarity.UNCOMMON, EnchantmentTarget.ARMOR_HEAD, new EquipmentSlot[] {EquipmentSlot.HEAD});
    }

    @Override
    public int getMaxLevel() {
        return 1;
    }

    @Override
    public Text getName(int level) {
        return Text.translatable("enchantment.true_invis.truesight");
    }
}
