package net.IneiTsuki.true_invis;

import net.IneiTsuki.true_invis.enchantment.TrueSightEnchantment;
import net.IneiTsuki.true_invis.mixin.EntityAccessor;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
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
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class True_invis implements ModInitializer {
    public static final Logger LOGGER = LogManager.getLogger("TrueInvis");

    // A map to store real equipment for each player
    private static final Map<UUID, List<Pair<EquipmentSlot, ItemStack>>> realEquipmentMap = new HashMap<>();
    private final Map<UUID, Map<UUID, Boolean>> trueSightMap = new HashMap<>();
    public static final Enchantment TRUE_SIGHT = new TrueSightEnchantment();

    private int tickCounter = 0;

    @Override
    public void onInitialize() {
        LOGGER.info("TrueInvis initialized successfully!");

        Registry.register(Registries.ENCHANTMENT, new Identifier("true_invis", "truesight"), TRUE_SIGHT);
        ServerTickEvents.END_SERVER_TICK.register(this::onServerTick);

        // Clean up when players disconnect
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> realEquipmentMap.remove(handler.player.getUuid()));
    }

    private void onServerTick(MinecraftServer server) {
        tickCounter++;
        if (tickCounter % 10 != 0) return; // Run every 10 ticks to reduce load

        // First, handle invisibility and equipment updates
        for (ServerWorld world : server.getWorlds()) {
            for (ServerPlayerEntity player : world.getPlayers()) {
                handlePlayerInvisibility(world, player);
            }
        }

        // Periodically resend glowing outlines to True Sight players to prevent de-sync
        if (tickCounter % 200 == 0) { // every ~10 seconds
            for (ServerWorld world : server.getWorlds()) {
                for (ServerPlayerEntity player : world.getPlayers()) {
                    boolean hasEffect = player.hasStatusEffect(StatusEffects.INVISIBILITY);
                    boolean isFlagInvisible = (player.getDataTracker().get(EntityAccessor.getFlagsTrackedData()) & 0x20) != 0;

                    if (hasEffect || isFlagInvisible) {
                        for (ServerPlayerEntity other : world.getPlayers()) {
                            if (other == player) continue;

                            ItemStack helmet = other.getEquippedStack(EquipmentSlot.HEAD);
                            if (EnchantmentHelper.getLevel(True_invis.TRUE_SIGHT, helmet) > 0) {
                                sendOutlinePacket(other, player);
                            }
                        }
                    }
                }
            }
        }
    }

    private void handlePlayerInvisibility(ServerWorld world, ServerPlayerEntity target) {
        UUID targetUUID = target.getUuid();

        boolean hasEffect = target.hasStatusEffect(StatusEffects.INVISIBILITY);
        boolean isFlagInvisible = (target.getDataTracker().get(EntityAccessor.getFlagsTrackedData()) & 0x20) != 0;
        boolean isInvisible = hasEffect || isFlagInvisible;

        // Initialize observer map if absent
        trueSightMap.putIfAbsent(targetUUID, new HashMap<>());
        Map<UUID, Boolean> observersSeeing = trueSightMap.get(targetUUID);

        if (isInvisible) {
            // FIRST TICK: Force all observers to see correct state
            if (observersSeeing.isEmpty()) {
                for (ServerPlayerEntity observer : world.getPlayers()) {
                    if (observer == target) continue;

                    ItemStack helmet = observer.getEquippedStack(EquipmentSlot.HEAD);
                    boolean hasTrueSight = EnchantmentHelper.getLevel(TRUE_SIGHT, helmet) > 0;

                    if (hasTrueSight) {
                        sendOutlinePacket(observer, target);
                        sendRealEquipmentPacket(observer, target, realEquipmentMap.get(targetUUID));
                        observersSeeing.put(observer.getUuid(), true);
                    } else {
                        sendEmptyEquipmentPacket(observer, target);
                        removeOutlinePacket(observer, target);
                        observersSeeing.put(observer.getUuid(), false);
                    }
                }
            }

            // Dynamic equipment update every 10 ticks
            if (tickCounter % 10 == 0) {
                List<Pair<EquipmentSlot, ItemStack>> storedEquipment = realEquipmentMap.get(targetUUID);
                boolean needsUpdate = storedEquipment == null ||
                        !storedEquipment.get(0).getSecond().equals(target.getEquippedStack(EquipmentSlot.HEAD)) ||
                        !storedEquipment.get(1).getSecond().equals(target.getEquippedStack(EquipmentSlot.CHEST)) ||
                        !storedEquipment.get(2).getSecond().equals(target.getEquippedStack(EquipmentSlot.LEGS)) ||
                        !storedEquipment.get(3).getSecond().equals(target.getEquippedStack(EquipmentSlot.FEET)) ||
                        !storedEquipment.get(4).getSecond().equals(target.getMainHandStack()) ||
                        !storedEquipment.get(5).getSecond().equals(target.getOffHandStack());

                if (needsUpdate) storeRealEquipment(target);
            }

            // Update observers if True Sight status changed
            for (ServerPlayerEntity observer : world.getPlayers()) {
                if (observer == target) continue;

                boolean hasTrueSight = EnchantmentHelper.getLevel(TRUE_SIGHT, observer.getEquippedStack(EquipmentSlot.HEAD)) > 0;
                boolean currentlySeeing = observersSeeing.getOrDefault(observer.getUuid(), false);

                if (hasTrueSight && !currentlySeeing) {
                    sendOutlinePacket(observer, target);
                    sendRealEquipmentPacket(observer, target, realEquipmentMap.get(targetUUID));
                    observersSeeing.put(observer.getUuid(), true);
                } else if (!hasTrueSight && currentlySeeing) {
                    sendEmptyEquipmentPacket(observer, target);
                    removeOutlinePacket(observer, target);
                    observersSeeing.put(observer.getUuid(), false);
                }
            }
        } else {
            // Target is no longer invisible → restore equipment for everyone
            List<Pair<EquipmentSlot, ItemStack>> realEquipment = realEquipmentMap.get(targetUUID);
            if (realEquipment != null) {
                for (ServerPlayerEntity observer : world.getPlayers()) {
                    if (observer == target) continue;
                    sendRealEquipmentPacket(observer, target, realEquipment);
                }
            }
            realEquipmentMap.remove(targetUUID);
            trueSightMap.remove(targetUUID);
        }
    }

    private void storeRealEquipment(ServerPlayerEntity player) {
        UUID uuid = player.getUuid();

        // Capture current equipment
        List<Pair<EquipmentSlot, ItemStack>> equipment = List.of(
                Pair.of(EquipmentSlot.HEAD, player.getEquippedStack(EquipmentSlot.HEAD).copy()),
                Pair.of(EquipmentSlot.CHEST, player.getEquippedStack(EquipmentSlot.CHEST).copy()),
                Pair.of(EquipmentSlot.LEGS, player.getEquippedStack(EquipmentSlot.LEGS).copy()),
                Pair.of(EquipmentSlot.FEET, player.getEquippedStack(EquipmentSlot.FEET).copy()),
                Pair.of(EquipmentSlot.MAINHAND, player.getMainHandStack().copy()),
                Pair.of(EquipmentSlot.OFFHAND, player.getOffHandStack().copy())
        );

        realEquipmentMap.put(uuid, equipment);
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

    private void sendRealEquipmentPacket(ServerPlayerEntity other, ServerPlayerEntity player, List<Pair<EquipmentSlot, ItemStack>> equipment) {
        if (equipment == null) {
            equipment = List.of(
                    Pair.of(EquipmentSlot.HEAD, ItemStack.EMPTY),
                    Pair.of(EquipmentSlot.CHEST, ItemStack.EMPTY),
                    Pair.of(EquipmentSlot.LEGS, ItemStack.EMPTY),
                    Pair.of(EquipmentSlot.FEET, ItemStack.EMPTY),
                    Pair.of(EquipmentSlot.MAINHAND, ItemStack.EMPTY),
                    Pair.of(EquipmentSlot.OFFHAND, ItemStack.EMPTY)
            );
        }
        EntityEquipmentUpdateS2CPacket packet = new EntityEquipmentUpdateS2CPacket(player.getId(), equipment);
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
