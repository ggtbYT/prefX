package com.ggtb.prefX;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.HashSet;
import java.util.UUID;

public class Commands implements CommandExecutor {

    private final PrefX plugin;

    public Commands(PrefX plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (cmd.getName().equalsIgnoreCase("pref")) {
            if (!sender.isOp()) {
                sender.sendMessage(Component.text("Только OP!").color(NamedTextColor.RED));
                return true;
            }
            if (args.length == 0) return false;

            switch (args[0].toLowerCase()) {
                case "create" -> {
                    if (args.length != 4) return false;
                    String name = args[1];
                    TextColor color = plugin.parseColor(args[2]);
                    boolean bold = Boolean.parseBoolean(args[3]);

                    if (plugin.getPrefixMap().containsKey(name.toLowerCase())) {
                        sender.sendMessage(Component.text("Префикс уже существует!").color(NamedTextColor.RED));
                        return true;
                    }

                    Prefix prefix = new Prefix(name, color, bold, new HashSet<>());
                    plugin.getPrefixMap().put(name.toLowerCase(), prefix);
                    sender.sendMessage(Component.text("Префикс создан!").color(NamedTextColor.GREEN));
                    plugin.updateAllScoreboardPrefixes();
                }
                case "add" -> {
                    if (args.length != 3) return false;
                    String name = args[1];
                    OfflinePlayer target = Bukkit.getOfflinePlayer(args[2]);
                    Prefix p = plugin.getPrefixMap().get(name.toLowerCase());
                    if (p == null) {
                        sender.sendMessage(Component.text("Префикс не найден!").color(NamedTextColor.RED));
                        return true;
                    }
                    p.players.add(target.getUniqueId());
                    plugin.getPlayerPrefix().put(target.getUniqueId(), name);
                    if (target.isOnline()) plugin.updateScoreboardPrefix((Player) target);
                    sender.sendMessage(Component.text("Префикс добавлен!").color(NamedTextColor.GREEN));
                }
                case "rem" -> {
                    if (args.length != 3) return false;
                    String name = args[1];
                    OfflinePlayer target = Bukkit.getOfflinePlayer(args[2]);
                    Prefix p = plugin.getPrefixMap().get(name.toLowerCase());
                    if (p == null || !p.players.remove(target.getUniqueId())) {
                        sender.sendMessage(Component.text("Не найдено!").color(NamedTextColor.RED));
                        return true;
                    }
                    plugin.getPlayerPrefix().remove(target.getUniqueId());
                    if (target.isOnline()) plugin.updateScoreboardPrefix((Player) target);
                    sender.sendMessage(Component.text("Префикс убран!").color(NamedTextColor.GREEN));
                }
                case "del" -> {
                    if (args.length != 2) return false;
                    String name = args[1];
                    Prefix p = plugin.getPrefixMap().remove(name.toLowerCase());
                    if (p == null) {
                        sender.sendMessage(Component.text("Префикс не найден!").color(NamedTextColor.RED));
                        return true;
                    }
                    for (UUID uuid : p.players) {
                        plugin.getPlayerPrefix().remove(uuid);
                        OfflinePlayer op = Bukkit.getOfflinePlayer(uuid);
                        if (op.isOnline()) plugin.updateScoreboardPrefix((Player) op);
                    }
                    sender.sendMessage(Component.text("Префикс удалён!").color(NamedTextColor.GREEN));
                    plugin.updateAllScoreboardPrefixes();
                }
            }
            return true;
        }

        if (cmd.getName().equalsIgnoreCase("clan")) {
            if (args.length < 1) return false;

            switch (args[0].toLowerCase()) {
                case "create" -> {
                    if (!sender.isOp()) {
                        sender.sendMessage(Component.text("Только OP!").color(NamedTextColor.RED));
                        return true;
                    }
                    if (args.length != 4) return false;

                    String clanName = args[1];
                    String colorStr = args[2];
                    String leaderNick = args[3];

                    OfflinePlayer leaderPlayer = Bukkit.getOfflinePlayer(leaderNick);

                    if (!leaderPlayer.hasPlayedBefore() || leaderPlayer.getUniqueId() == null) {
                        sender.sendMessage(Component.text("Игрок " + leaderNick + " никогда не заходил на сервер или ник написан неверно!")
                                .color(NamedTextColor.RED));
                        return true;
                    }

                    UUID leaderUUID = leaderPlayer.getUniqueId();

                    TextColor color = plugin.parseColor(colorStr);
                    if (color == null) {
                        sender.sendMessage(Component.text("Неверный цвет!").color(NamedTextColor.RED));
                        return true;
                    }

                    String lowerKey = clanName.toLowerCase();
                    if (plugin.getClanMap().containsKey(lowerKey)) {
                        sender.sendMessage(Component.text("Клан уже существует!").color(NamedTextColor.RED));
                        return true;
                    }

                    Clan clan = new Clan(clanName, color, leaderUUID, new HashSet<>());
                    plugin.getClanMap().put(lowerKey, clan);
                    plugin.getPlayerClan().put(leaderUUID, lowerKey);

                    if (leaderPlayer.isOnline()) {
                        plugin.updateScoreboardPrefix(leaderPlayer.getPlayer());
                    }

                    sender.sendMessage(Component.text("Клан " + clanName + " создан с главой " + leaderNick + "!")
                            .color(NamedTextColor.GREEN));

                    plugin.updateAllScoreboardPrefixes();
                }

                case "reset" -> {
                    if (!sender.isOp()) {
                        sender.sendMessage(Component.text("Только OP!").color(NamedTextColor.RED));
                        return true;
                    }
                    if (args.length != 3) return false;

                    String clanName = args[1];
                    String newLeaderNick = args[2];

                    String lowerKey = clanName.toLowerCase();
                    Clan c = plugin.getClanMap().get(lowerKey);
                    if (c == null) {
                        sender.sendMessage(Component.text("Клан не найден!").color(NamedTextColor.RED));
                        return true;
                    }

                    OfflinePlayer newLeaderPlayer = Bukkit.getOfflinePlayer(newLeaderNick);

                    if (!newLeaderPlayer.hasPlayedBefore() || newLeaderPlayer.getUniqueId() == null) {
                        sender.sendMessage(Component.text("Новый глава " + newLeaderNick + " никогда не заходил на сервер или ник неверный!")
                                .color(NamedTextColor.RED));
                        return true;
                    }

                    UUID newLeaderUUID = newLeaderPlayer.getUniqueId();

                    plugin.getPlayerClan().remove(c.leader);

                    c.leader = newLeaderUUID;
                    plugin.getPlayerClan().put(newLeaderUUID, lowerKey);

                    if (newLeaderPlayer.isOnline()) {
                        plugin.updateScoreboardPrefix(newLeaderPlayer.getPlayer());
                    }

                    sender.sendMessage(Component.text("Глава клана " + c.name + " сменён на " + newLeaderNick + "!")
                            .color(NamedTextColor.GREEN));

                    plugin.updateAllScoreboardPrefixes();
                }

                case "add" -> {
                    if (!(sender instanceof Player player)) {
                        sender.sendMessage(Component.text("Только игрок!").color(NamedTextColor.RED));
                        return true;
                    }
                    if (args.length != 3) return false;

                    String clanName = args[1];
                    String nick = args[2];

                    String lowerKey = clanName.toLowerCase();
                    Clan c = plugin.getClanMap().get(lowerKey);
                    if (c == null || !c.leader.equals(player.getUniqueId()) || !sender.isOp()) {
                        sender.sendMessage(Component.text("Ты не лидер этого клана!").color(NamedTextColor.RED));
                        return true;
                    }

                    OfflinePlayer target = Bukkit.getOfflinePlayer(nick);
                    UUID targetUUID = target.getUniqueId();

                    c.members.add(targetUUID);
                    plugin.getPlayerClan().put(targetUUID, lowerKey);

                    if (target.isOnline()) {
                        plugin.updateScoreboardPrefix(target.getPlayer());
                    }

                    sender.sendMessage(Component.text("Игрок " + nick + " добавлен в клан!").color(NamedTextColor.GREEN));
                    plugin.updateAllScoreboardPrefixes();
                }

                case "rem" -> {
                    if (!(sender instanceof Player player)) {
                        sender.sendMessage(Component.text("Только игрок!").color(NamedTextColor.RED));
                        return true;
                    }
                    if (args.length != 3) return false;

                    String clanName = args[1];
                    String nick = args[2];

                    String lowerKey = clanName.toLowerCase();
                    Clan c = plugin.getClanMap().get(lowerKey);
                    if (c == null || !c.leader.equals(player.getUniqueId()) || !sender.isOp()) {
                        sender.sendMessage(Component.text("Ты не лидер этого клана!").color(NamedTextColor.RED));
                        return true;
                    }

                    OfflinePlayer target = Bukkit.getOfflinePlayer(nick);
                    UUID targetUUID = target.getUniqueId();

                    c.members.remove(targetUUID);
                    plugin.getPlayerClan().remove(targetUUID);

                    if (target.isOnline()) {
                        plugin.updateScoreboardPrefix(target.getPlayer());
                    }

                    sender.sendMessage(Component.text("Игрок " + nick + " удалён из клана!").color(NamedTextColor.GREEN));
                    plugin.updateAllScoreboardPrefixes();
                }

                case "del" -> {
                    if (!sender.isOp()) {
                        sender.sendMessage(Component.text("Только OP!").color(NamedTextColor.RED));
                        return true;
                    }
                    if (args.length != 2) return false;

                    String clanName = args[1];
                    String lowerKey = clanName.toLowerCase();

                    Clan c = plugin.getClanMap().remove(lowerKey);
                    if (c == null) {
                        sender.sendMessage(Component.text("Клан не найден!").color(NamedTextColor.RED));
                        return true;
                    }

                    for (UUID uuid : c.members) {
                        plugin.getPlayerClan().remove(uuid);
                        OfflinePlayer op = Bukkit.getOfflinePlayer(uuid);
                        if (op.isOnline()) plugin.updateScoreboardPrefix(op.getPlayer());
                    }
                    plugin.getPlayerClan().remove(c.leader);
                    OfflinePlayer lp = Bukkit.getOfflinePlayer(c.leader);
                    if (lp.isOnline()) plugin.updateScoreboardPrefix(lp.getPlayer());

                    sender.sendMessage(Component.text("Клан " + c.name + " удалён!").color(NamedTextColor.GREEN));
                    plugin.updateAllScoreboardPrefixes();
                }

                case "leave" -> {
                    if (!(sender instanceof Player player)) {
                        sender.sendMessage(Component.text("Только игрок!").color(NamedTextColor.RED));
                        return true;
                    }

                    String myClanKey = plugin.getPlayerClan().get(player.getUniqueId());
                    if (myClanKey == null) {
                        sender.sendMessage(Component.text("Ты не состоишь в клане!").color(NamedTextColor.RED));
                        return true;
                    }

                    Clan c = plugin.getClanMap().get(myClanKey);
                    if (c.leader.equals(player.getUniqueId())) {
                        sender.sendMessage(Component.text("Лидер не может покинуть клан! Используй /clan reset").color(NamedTextColor.RED));
                        return true;
                    }

                    c.members.remove(player.getUniqueId());
                    plugin.getPlayerClan().remove(player.getUniqueId());
                    plugin.updateScoreboardPrefix(player);

                    sender.sendMessage(Component.text("Ты покинул клан " + c.name + "!").color(NamedTextColor.GREEN));
                }

                default -> { return false; }
            }
            return true;
        }

        if (cmd.getName().equalsIgnoreCase("display")) {
            if (!(sender instanceof Player p)) return true;
            if (args.length != 1) return false;

            DisplayMode mode = switch (args[0].toLowerCase()) {
                case "clan" -> DisplayMode.CLAN;
                case "prefix" -> DisplayMode.PREFIX;
                default -> null;
            };
            if (mode == null) return false;

            plugin.getPlayerDisplay().put(p.getUniqueId(), mode);
            plugin.updateScoreboardPrefix(p);
            sender.sendMessage(Component.text("Режим отображения: " + args[0]).color(NamedTextColor.GREEN));
            return true;
        }

        if (cmd.getName().equalsIgnoreCase("prefixdata")) {
            if (!sender.isOp()) return true;
            if (args.length != 1 || !args[0].equalsIgnoreCase("save")) return false;
            plugin.saveData();
            sender.sendMessage(Component.text("Данные сохранены!").color(NamedTextColor.GREEN));
            return true;
        }

        return false;
    }
}
