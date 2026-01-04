package com.ggtb.prefX.chat;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.advancement.Advancement;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerAdvancementDoneEvent;

public class AdvancementListener implements Listener {

    @EventHandler
    public void onAdvancement(PlayerAdvancementDoneEvent event) {
        Advancement adv = event.getAdvancement();


        if (adv.getDisplay() == null) return;
        if (adv.getDisplay().isHidden()) return;

        Component title = adv.getDisplay().title();
        Component description = adv.getDisplay().description();

        Component msg = Component.text("⭐")
                .color(NamedTextColor.GOLD)
                .append(Component.text(" | ").color(NamedTextColor.GRAY))
                .append(Component.text(event.getPlayer().getName()).color(NamedTextColor.WHITE))
                .append(Component.text(" получил достижение ").color(NamedTextColor.YELLOW))
                .append(Component.text("[").color(NamedTextColor.AQUA))
                .append(title.colorIfAbsent(NamedTextColor.AQUA)
                        .hoverEvent(HoverEvent.showText(description.colorIfAbsent(NamedTextColor.GRAY))))
                .append(Component.text("]").color(NamedTextColor.AQUA));

        Bukkit.broadcast(msg);
    }
}
