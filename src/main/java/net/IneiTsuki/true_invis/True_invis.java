package net.IneiTsuki.true_invis;

import net.IneiTsuki.true_invis.enchantment.TrueSightEnchantment;
import net.IneiTsuki.true_invis.mixin.EntityAccessor;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.s2c.play.EntityEquipmentUpdateS2CPacket;
import net.minecraft.network.packet.s2c.play.EntityTrackerUpdateS2CPacket;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import com.mojang.datafixers.util.Pair;
import net.minecraft.util.Identifier;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class True_invis implements ModInitializer {
    // A map to store real equipment for each player
    private static final Map<ServerPlayerEntity, List<Pair<EquipmentSlot, ItemStack>>> realEquipmentMap = new HashMap<>();
    public static final Enchantment TRUE_SIGHT = new TrueSightEnchantment();

    @Override
    public void onInitialize() {
        System.out.println("NoParticlesMod loaded with packet-based invisibility!");

        // Register the True Sight enchantment
        Registry.register(Registries.ENCHANTMENT, new Identifier("noparticles", "truesight"), TRUE_SIGHT);

        ServerTickEvents.END_SERVER_TICK.register(this::onServerTick);
    }

    private void onServerTick(MinecraftServer server) {
        for (ServerWorld world : server.getWorlds()) {
            for (ServerPlayerEntity player : world.getPlayers()) {
                if (player.hasStatusEffect(StatusEffects.INVISIBILITY)) {
                    // Save the player's real equipment if it hasn't been done yet
                    if (!realEquipmentMap.containsKey(player)) {
                        storeRealEquipment(player);
                    }

                    // Hide equipment for all players
                    for (ServerPlayerEntity other : world.getPlayers()) {
                        if (other == player) continue;

                        // Check if the other player has the True Sight enchantment
                        ItemStack helmet = other.getEquippedStack(EquipmentSlot.HEAD);
                        if (EnchantmentHelper.getLevel(True_invis.TRUE_SIGHT, helmet) > 0) {
                            sendOutlinePacket(other, player);
                        } else {
                            sendEmptyEquipmentPacket(other, player);
                            removeOutlinePacket(other, player);
                        }
                    }
                } else {
                    // Restore real equipment when invisibility ends
                    if (realEquipmentMap.containsKey(player)) {
                        for (ServerPlayerEntity other : world.getPlayers()) {
                            if (other != player) {
                                sendRealEquipmentPacket(other, player);
                            }
                        }

                        realEquipmentMap.remove(player);
                    }
                }
            }
        }
    }

    private void storeRealEquipment(ServerPlayerEntity player) {
        List<Pair<EquipmentSlot, ItemStack>> equipment = List.of(
                Pair.of(EquipmentSlot.HEAD, player.getEquippedStack(EquipmentSlot.HEAD)),
                Pair.of(EquipmentSlot.CHEST, player.getEquippedStack(EquipmentSlot.CHEST)),
                Pair.of(EquipmentSlot.LEGS, player.getEquippedStack(EquipmentSlot.LEGS)),
                Pair.of(EquipmentSlot.FEET, player.getEquippedStack(EquipmentSlot.FEET)),
                Pair.of(EquipmentSlot.MAINHAND, player.getMainHandStack()),
                Pair.of(EquipmentSlot.OFFHAND, player.getOffHandStack())
        );

        realEquipmentMap.put(player, equipment);
    }

    private void sendEmptyEquipmentPacket(ServerPlayerEntity other, ServerPlayerEntity player) {
        List<Pair<EquipmentSlot, ItemStack>> fakeEquipment = List.of(
                Pair.of(EquipmentSlot.HEAD, ItemStack.EMPTY),
                Pair.of(EquipmentSlot.CHEST, ItemStack.EMPTY),
                Pair.of(EquipmentSlot.LEGS, ItemStack.EMPTY),
                Pair.of(EquipmentSlot.FEET, ItemStack.EMPTY),
                Pair.of(EquipmentSlot.MAINHAND, ItemStack.EMPTY),
                Pair.of(EquipmentSlot.OFFHAND, ItemStack.EMPTY)
        );

        EntityEquipmentUpdateS2CPacket packet = new EntityEquipmentUpdateS2CPacket(player.getId(), fakeEquipment);
        other.networkHandler.sendPacket(packet);
    }

    private void sendRealEquipmentPacket(ServerPlayerEntity other, ServerPlayerEntity player) {
        List<Pair<EquipmentSlot, ItemStack>> realEquipment = realEquipmentMap.get(player);
        EntityEquipmentUpdateS2CPacket packet = new EntityEquipmentUpdateS2CPacket(player.getId(), realEquipment);
        other.networkHandler.sendPacket(packet);
    }

    private void sendOutlinePacket(ServerPlayerEntity viewer, ServerPlayerEntity target) {
        byte currentFlags = target.getDataTracker().get(EntityAccessor.getFlagsTrackedData());
        byte glowingFlags = (byte) (currentFlags | 0x40); // Set glowing bit

        DataTracker.Entry<Byte> glowingEntry = new DataTracker.Entry<>(EntityAccessor.getFlagsTrackedData(), glowingFlags);
        EntityTrackerUpdateS2CPacket packet = new EntityTrackerUpdateS2CPacket(
                target.getId(),
                List.of(glowingEntry.toSerialized())
        );

        viewer.networkHandler.sendPacket(packet);
    }

    private void removeOutlinePacket(ServerPlayerEntity viewer, ServerPlayerEntity target) {
        byte currentFlags = target.getDataTracker().get(EntityAccessor.getFlagsTrackedData());
        byte clearedFlags = (byte) (currentFlags & ~0x40); // Unset glowing bit

        DataTracker.Entry<Byte> normalEntry = new DataTracker.Entry<>(EntityAccessor.getFlagsTrackedData(), clearedFlags);
        EntityTrackerUpdateS2CPacket packet = new EntityTrackerUpdateS2CPacket(
                target.getId(),
                List.of(normalEntry.toSerialized())
        );

        viewer.networkHandler.sendPacket(packet);
    }

}
