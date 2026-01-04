package com.ggtb.prefX.chat;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Tameable;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.ItemStack;

public class PetListener implements Listener {

    private final LegacyComponentSerializer serializer = LegacyComponentSerializer.legacyAmpersand();

    @EventHandler
    public void onPetDeath(EntityDeathEvent event) {

        if (!(event.getEntity() instanceof Tameable tameable)) return;
        if (!tameable.isTamed()) return;
        if (!(tameable.getOwner() instanceof Player owner)) return;

        LivingEntity pet = event.getEntity();

        Component petNameComponent = pet.customName() != null
                ? pet.customName()
                : Component.text(pet.getType().name().toLowerCase());

        EntityDamageByEntityEvent last = pet.getLastDamageCause() instanceof EntityDamageByEntityEvent e ? e : null;

        Component message;

        if (last != null && last.getDamager() instanceof Player killer) {
            ItemStack weapon = killer.getInventory().getItemInMainHand();
            Component weaponComponent;

            if (weapon == null || weapon.getType().isAir()) {
                weaponComponent = Component.text("руками")
                        .color(NamedTextColor.DARK_RED);
            } else {
                weaponComponent = Component.text("")
                        .color(NamedTextColor.DARK_GRAY)
                        .append(weapon.displayName().colorIfAbsent(NamedTextColor.RED))
                        .append(Component.text("").color(NamedTextColor.DARK_GRAY))
                        .hoverEvent(weapon.asHoverEvent());
            }

            message = Component.text("💔")
                    .color(TextColor.fromHexString("#7a1c1c"))
                    .append(Component.text(" | ").color(NamedTextColor.GRAY))
                    .append(petNameComponent.colorIfAbsent(NamedTextColor.WHITE))
                    .append(Component.text(" питомец ").color(NamedTextColor.RED))
                    .append(Component.text(killer.getName()).color(NamedTextColor.WHITE))
                    .append(Component.text(" был убит с помощью ").color(NamedTextColor.DARK_RED))
                    .append(weaponComponent);

        } else if (last != null && last.getDamager() instanceof LivingEntity mob) {
            String mobName = mobName(mob.getType());
            message = Component.text("💔")
                    .color(TextColor.fromHexString("#7a1c1c"))
                    .append(Component.text(" | ").color(NamedTextColor.GRAY))
                    .append(petNameComponent.colorIfAbsent(NamedTextColor.WHITE))
                    .append(Component.text(" питомец ").color(NamedTextColor.RED))
                    .append(Component.text("был убит ").color(NamedTextColor.DARK_RED))
                    .append(Component.text(mobName).color(NamedTextColor.RED));

        } else {
            String reason = resolveDeathReason(pet);
            message = Component.text("💔")
                    .color(TextColor.fromHexString("#7a1c1c"))
                    .append(Component.text(" | ").color(NamedTextColor.GRAY))
                    .append(petNameComponent.colorIfAbsent(NamedTextColor.WHITE))
                    .append(Component.text(" питомец ").color(NamedTextColor.RED))
                    .append(Component.text(reason).color(NamedTextColor.DARK_RED));
        }

        Bukkit.broadcast(message);
    }

    // утилиты

    private String resolveDeathReason(LivingEntity pet) {
        if (pet.getLastDamageCause() == null) return "погиб";

        return switch (pet.getLastDamageCause().getCause()) {
            case LAVA -> "сгорел в лаве";
            case FIRE, FIRE_TICK -> "сгорел";
            case DROWNING -> "утонул";
            case FALL -> "разбился";
            case VOID -> "упал в пустоту";
            case STARVATION -> "умер от голода";
            default -> "погиб";
        };
    }

    private String mobName(EntityType type) {
        return switch (type) {
            case ZOMBIE -> "зомби";
            case SKELETON -> "скелетом";
            case CREEPER -> "крипером";
            case SPIDER -> "паук";
            case CAVE_SPIDER -> "пещерный паук";
            case ENDERMAN -> "эндермен";
            case WITHER -> "визер";
            case WARDEN -> "варден";
            case BLAZE -> "ифрит";
            case PIGLIN -> "пиглин";
            case PIGLIN_BRUTE -> "пиглин-брут";
            case GHAST -> "гаст";
            case SLIME -> "слизень";
            case MAGMA_CUBE -> "магма-слизень";
            case PHANTOM -> "фантом";
            default -> "существо";
        };
    }
}
