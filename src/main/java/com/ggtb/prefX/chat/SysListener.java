package com.ggtb.prefX.chat;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.EnderCrystal;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;

public class SysListener implements Listener {

    // =========================
    // зашёл вышел
    // =========================

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        event.joinMessage(null);

        Component msg = Component.text("+")
                .color(NamedTextColor.GREEN)
                .append(Component.text(" | ").color(NamedTextColor.GRAY))
                .append(Component.text(event.getPlayer().getName())
                        .color(NamedTextColor.WHITE))
                .append(Component.text(" присоединился")
                        .color(NamedTextColor.GREEN));

        Bukkit.broadcast(msg);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        event.quitMessage(null);

        Component msg = Component.text("-")
                .color(NamedTextColor.RED)
                .append(Component.text(" | ").color(NamedTextColor.GRAY))
                .append(Component.text(event.getPlayer().getName())
                        .color(NamedTextColor.WHITE))
                .append(Component.text(" вышел")
                        .color(NamedTextColor.RED));

        Bukkit.broadcast(msg);
    }

    // =========================
    // смерти
    // =========================

    @EventHandler
    public void onDeath(PlayerDeathEvent event) {
        event.deathMessage(null);

        Player victim = event.getEntity();
        Component message;

        if (victim.getKiller() != null) {
            message = pvpDeath(event);
        } else {
            Component mob = mobDeath(event);
            if (mob != null) {
                message = mob;
            } else {
                message = environmentDeath(event);
            }
        }

        Bukkit.broadcast(message);
    }

    // =========================
    // PvP
    // =========================

    private Component pvpDeath(PlayerDeathEvent event) {
        Player victim = event.getEntity();
        Player killer = victim.getKiller();

        ItemStack weapon = killer.getInventory().getItemInMainHand();

        Component weaponComponent;

        if (weapon == null || weapon.getType().isAir()) {
            weaponComponent = Component.text("рук")
                    .color(NamedTextColor.DARK_RED);
        } else {
            // 🔥 ВАЖНО: берём имя предмета ПРАВИЛЬНО
            Component itemName = weapon.displayName();

            weaponComponent = Component.text("")
                    .color(NamedTextColor.DARK_GRAY)
                    .append(itemName.colorIfAbsent(NamedTextColor.RED))
                    .append(Component.text("")
                            .color(NamedTextColor.DARK_GRAY))
                    .hoverEvent(weapon.asHoverEvent());
        }

        return skullPrefix()
                .append(name(victim))
                .append(text(" был убит "))
                .append(name(killer))
                .append(text(" с помощью "))
                .append(weaponComponent);
    }

    // =========================
    // питомец вмер
    // =========================

    private Component mobDeath(PlayerDeathEvent event) {
        Player victim = event.getEntity();

        if (!(victim.getLastDamageCause() instanceof EntityDamageByEntityEvent e))
            return null;

        if (!(e.getDamager() instanceof LivingEntity mob))
            return null;

        String mobName = mobName(mob.getType());

        return skullPrefix()
                .append(name(victim))
                .append(text(" был убит "))
                .append(Component.text(mobName)
                        .color(NamedTextColor.RED));
    }

    // утилиты 2

    private Component environmentDeath(PlayerDeathEvent event) {
        Player victim = event.getEntity();
        String reason = environmentReason(victim);

        return skullPrefix()
                .append(name(victim))
                .append(text(" " + reason));
    }

    private String environmentReason(Player player) {
        if (player.getLastDamageCause() == null)
            return "умер";

        var last = player.getLastDamageCause();
        DamageCause cause = last.getCause();

        // END CRYSTAL
        if (cause == DamageCause.ENTITY_EXPLOSION
                && last instanceof EntityDamageByEntityEvent e
                && e.getDamager() instanceof EnderCrystal) {
            return "был взорван эндер-кристаллом";
        }

        return switch (cause) {
            case VOID -> "упал в пустоту";
            case WORLD_BORDER -> "погиб за границей мира";
            case FALL -> "разбился насмерть";
            case LAVA -> "сгорел в лаве";
            case FIRE, FIRE_TICK -> "сгорел заживо";
            case DROWNING -> "утонул";
            case STARVATION -> "умер от голода";
            case MAGIC -> "погиб от магии";
            case WITHER -> "был уничтожен визером";
            case SUFFOCATION -> "задохнулся в блоках";
            case CRAMMING -> "был раздавлен";
            case HOT_FLOOR -> "сгорел на магме";
            case ENTITY_EXPLOSION -> "погиб при взрыве";
            default -> "умер";
        };
    }

    // ЧЗХ

    private Component skullPrefix() {
        return Component.text("☠")
                .color(TextColor.fromHexString("#7a1c1c"))
                .append(Component.text(" | ")
                        .color(NamedTextColor.GRAY));
    }

    private Component name(Player player) {
        return Component.text(player.getName())
                .color(NamedTextColor.WHITE);
    }

    private Component text(String text) {
        return Component.text(text)
                .color(NamedTextColor.DARK_RED);
    }

    private String mobName(EntityType type) {
        return switch (type) {
            case ZOMBIE -> "зомби";
            case SKELETON -> "скелетом";
            case CREEPER -> "крипером";
            case SPIDER -> "пауком";
            case CAVE_SPIDER -> "пещерным пауком";
            case ENDERMAN -> "эндерменом";
            case WITHER -> "визером";
            case WARDEN -> "варденом";
            case BLAZE -> "ифритом";
            case PIGLIN -> "пиглином";
            case PIGLIN_BRUTE -> "пиглином-брутом";
            case GHAST -> "гастом";
            case SLIME -> "слизнем";
            case MAGMA_CUBE -> "магма-слизнем";
            case PHANTOM -> "фантомом";
            default -> "существом";
        };
    }

    private String formatItemName(ItemStack item) {
        if (item.getItemMeta() != null && item.getItemMeta().hasDisplayName()) {
            return Component.text()
                    .append(item.getItemMeta().displayName())
                    .build()
                    .content();
        }

        return item.getType().name()
                .toLowerCase()
                .replace("_", " ");
    }
}
