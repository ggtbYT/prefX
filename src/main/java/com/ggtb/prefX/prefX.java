package com.ggtb.prefX;

import com.ggtb.prefX.chat.AdvancementListener;
import com.ggtb.prefX.chat.PetListener;
import com.ggtb.prefX.chat.SysListener;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

import java.io.File;
import java.io.IOException;
import java.util.*;

public final class PrefX extends JavaPlugin implements Listener {


    private final Map<String, Prefix> prefixMap = new HashMap<>();
    private final Map<String, Clan> clanMap = new HashMap<>();
    private final Map<UUID, String> playerPrefix = new HashMap<>();
    private final Map<UUID, String> playerClan = new HashMap<>();
    private final Map<UUID, DisplayMode> playerDisplay = new HashMap<>();

    private Scoreboard mainScoreboard;


    private File prefixFile, clanFile, displayFile;
    private FileConfiguration prefixConfig, clanConfig, displayConfig;

    @Override
    public void onEnable() {
        getServer().getPluginManager().registerEvents(this, this);

        getServer().getPluginManager().registerEvents(new SysListener(), this);
        getServer().getPluginManager().registerEvents(new PetListener(), this);
        getServer().getPluginManager().registerEvents(new AdvancementListener(), this);

        Objects.requireNonNull(getCommand("pref")).setExecutor(new Commands(this));
        Objects.requireNonNull(getCommand("clan")).setExecutor(new Commands(this));
        Objects.requireNonNull(getCommand("display")).setExecutor(new Commands(this));
        Objects.requireNonNull(getCommand("prefixdata")).setExecutor(new Commands(this));

        mainScoreboard = Bukkit.getScoreboardManager().getMainScoreboard();

        loadData();

        Bukkit.getOnlinePlayers().forEach(this::updateScoreboardPrefix);
        Bukkit.getScheduler().runTaskLater(this, () -> Bukkit.getOnlinePlayers().forEach(this::updateScoreboardPrefix), 40L);
    }

    @Override
    public void onDisable() {
        saveData();
    }


    public TextColor parseColor(String input) {
        if (input == null) return NamedTextColor.GRAY;
        if (input.startsWith("#")) return TextColor.fromHexString(input);
        return NamedTextColor.NAMES.value(input.toLowerCase()) != null
                ? NamedTextColor.NAMES.value(input.toLowerCase())
                : NamedTextColor.GRAY;
    }


    private void loadData() {
        if (!getDataFolder().exists()) getDataFolder().mkdirs();

        prefixFile = new File(getDataFolder(), "prefix.yml");
        clanFile = new File(getDataFolder(), "clan.yml");
        displayFile = new File(getDataFolder(), "display.yml");

        prefixConfig = YamlConfiguration.loadConfiguration(prefixFile);
        clanConfig = YamlConfiguration.loadConfiguration(clanFile);
        displayConfig = YamlConfiguration.loadConfiguration(displayFile);

        createFileIfNotExists(prefixFile, prefixConfig);
        createFileIfNotExists(clanFile, clanConfig);
        createFileIfNotExists(displayFile, displayConfig);

        if (prefixConfig.contains("prefixes")) {
            for (String key : prefixConfig.getConfigurationSection("prefixes").getKeys(false)) {
                String path = "prefixes." + key;
                String originalName = prefixConfig.getString(path + ".originalName", key); // сохраняем регистр
                TextColor color = parseColor(prefixConfig.getString(path + ".color", "#FFFFFF"));
                boolean bold = prefixConfig.getBoolean(path + ".bold", false);
                List<String> playersStr = prefixConfig.getStringList(path + ".players");
                Set<UUID> players = new HashSet<>();
                for (String s : playersStr) {
                    try { players.add(UUID.fromString(s)); } catch (Exception ignored) {}
                }

                Prefix prefix = new Prefix(originalName, color, bold, players);
                prefixMap.put(key.toLowerCase(), prefix);

                for (UUID uuid : players) playerPrefix.put(uuid, key.toLowerCase());
            }
        }

        if (clanConfig.contains("clans")) {
            for (String key : clanConfig.getConfigurationSection("clans").getKeys(false)) {
                String path = "clans." + key;
                String originalName = clanConfig.getString(path + ".originalName", key);
                TextColor color = parseColor(clanConfig.getString(path + ".color", "#FFFFFF"));

                String leaderStr = clanConfig.getString(path + ".leader");
                UUID leader = null;
                if (leaderStr != null) {
                    try { leader = UUID.fromString(leaderStr); } catch (Exception e) {
                        getLogger().warning("Повреждённый leader в клане " + key + ", пропускаем");
                    }
                }

                List<String> membersStr = clanConfig.getStringList(path + ".members");
                Set<UUID> members = new HashSet<>();
                for (String s : membersStr) {
                    try { members.add(UUID.fromString(s)); } catch (Exception ignored) {}
                }

                if (leader == null) {
                    getLogger().warning("Клан " + key + " без лидера — пропущен при загрузке");
                    continue;
                }

                Clan clan = new Clan(originalName, color, leader, members);
                clanMap.put(key.toLowerCase(), clan);

                for (UUID uuid : members) playerClan.put(uuid, key.toLowerCase());
                playerClan.put(leader, key.toLowerCase());
            }
        }

        if (displayConfig.contains("players")) {
            for (String uuidStr : displayConfig.getConfigurationSection("players").getKeys(false)) {
                try {
                    UUID uuid = UUID.fromString(uuidStr);
                    String mode = displayConfig.getString("players." + uuidStr).toUpperCase();
                    playerDisplay.put(uuid, DisplayMode.valueOf(mode));
                } catch (Exception ignored) {}
            }
        }
    }

    private void createFileIfNotExists(File file, FileConfiguration config) {
        if (!file.exists()) {
            try {
                file.createNewFile();
                config.save(file);
            } catch (IOException e) {
                getLogger().severe("Не удалось создать файл " + file.getName() + ": " + e.getMessage());
            }
        }
    }

    public void saveData() {
        prefixConfig.set("prefixes", null);
        for (Map.Entry<String, Prefix> e : prefixMap.entrySet()) {
            String key = e.getKey();
            Prefix p = e.getValue();
            String path = "prefixes." + key;
            prefixConfig.set(path + ".originalName", p.name); // сохраняем оригинальный регистр
            prefixConfig.set(path + ".color", p.color.asHexString());
            prefixConfig.set(path + ".bold", p.bold);
            List<String> players = p.players.stream().map(UUID::toString).toList();
            prefixConfig.set(path + ".players", players);
        }

        clanConfig.set("clans", null);
        for (Map.Entry<String, Clan> e : clanMap.entrySet()) {
            String key = e.getKey();
            Clan c = e.getValue();
            String path = "clans." + key;
            clanConfig.set(path + ".originalName", c.name);
            clanConfig.set(path + ".color", c.color.asHexString());
            clanConfig.set(path + ".leader", c.leader.toString());
            List<String> members = c.members.stream().map(UUID::toString).toList();
            clanConfig.set(path + ".members", members);
        }

        displayConfig.set("players", null);
        for (Map.Entry<UUID, DisplayMode> e : playerDisplay.entrySet()) {
            displayConfig.set("players." + e.getKey(), e.getValue().name().toLowerCase());
        }

        try {
            prefixConfig.save(prefixFile);
            clanConfig.save(clanFile);
            displayConfig.save(displayFile);
        } catch (IOException ex) {
            getLogger().severe("Ошибка сохранения: " + ex.getMessage());
        }
    }


    public void updateScoreboardPrefix(Player player) {
        UUID uuid = player.getUniqueId();
        DisplayMode mode = playerDisplay.getOrDefault(uuid, DisplayMode.CLAN);

        String prefixStr = "[-] ";
        TextColor color = NamedTextColor.GRAY;
        boolean bold = false;

        if (mode == DisplayMode.PREFIX) {
            String prefKey = playerPrefix.get(uuid);
            if (prefKey != null) {
                Prefix pref = prefixMap.get(prefKey);
                if (pref != null) {
                    prefixStr = "[" + pref.name + "] ";
                    color = pref.color;
                    bold = pref.bold;
                }
            }
        } else {
            String clanKey = playerClan.get(uuid);
            if (clanKey != null) {
                Clan clan = clanMap.get(clanKey);
                if (clan != null) {
                    prefixStr = "[" + clan.name + "] ";
                    color = clan.color;
                }
            }
        }

        String teamName = "z" + uuid.toString().substring(0, 10);
        Team team = mainScoreboard.getTeam(teamName);
        if (team == null) {
            team = mainScoreboard.registerNewTeam(teamName);
            team.setOption(Team.Option.NAME_TAG_VISIBILITY, Team.OptionStatus.ALWAYS);
            team.setOption(Team.Option.COLLISION_RULE, Team.OptionStatus.ALWAYS);
        }

        team.prefix(Component.text(prefixStr).color(color).decorate(bold ? TextDecoration.BOLD : TextDecoration.ITALIC));
        team.addEntry(player.getName());
    }

    public void updateAllScoreboardPrefixes() {
        Bukkit.getOnlinePlayers().forEach(this::updateScoreboardPrefix);
    }


    @EventHandler(priority = EventPriority.HIGHEST)
    public void onChat(AsyncPlayerChatEvent event) {
        event.setCancelled(true);
        Player player = event.getPlayer();

        DisplayMode mode = playerDisplay.getOrDefault(player.getUniqueId(), DisplayMode.CLAN);
        Component prefixComp = Component.text("[-]").color(NamedTextColor.GRAY);

        if (mode == DisplayMode.PREFIX) {
            String prefKey = playerPrefix.get(player.getUniqueId());
            if (prefKey != null) {
                Prefix pref = prefixMap.get(prefKey);
                if (pref != null) {
                    prefixComp = Component.text("[" + pref.name + "]")
                            .color(pref.color)
                            .decorate(pref.bold ? TextDecoration.BOLD : TextDecoration.ITALIC);
                }
            }
        } else {
            String clanKey = playerClan.get(player.getUniqueId());
            if (clanKey != null) {
                Clan clan = clanMap.get(clanKey);
                if (clan != null) {
                    prefixComp = Component.text("[" + clan.name + "]").color(clan.color);
                }
            }
        }

        Component fullMessage = Component.empty()
                .append(prefixComp)
                .append(Component.text(" "))
                .append(Component.text(player.getName()).color(NamedTextColor.WHITE))
                .append(Component.text(": ").color(NamedTextColor.GRAY))
                .append(Component.text(event.getMessage()).color(NamedTextColor.WHITE));

        for (Player p : Bukkit.getOnlinePlayers()) {
            p.sendMessage(fullMessage);
        }

        getLogger().info(player.getName() + ": " + event.getMessage());
    }


    public Map<String, Prefix> getPrefixMap() { return prefixMap; }
    public Map<String, Clan> getClanMap() { return clanMap; }
    public Map<UUID, String> getPlayerPrefix() { return playerPrefix; }
    public Map<UUID, String> getPlayerClan() { return playerClan; }
    public Map<UUID, DisplayMode> getPlayerDisplay() { return playerDisplay; }
}

enum DisplayMode {
    CLAN, PREFIX
}

class Prefix {
    final String name;
    final TextColor color;
    final boolean bold;
    final Set<UUID> players;

    Prefix(String name, TextColor color, boolean bold, Set<UUID> players) {
        this.name = name;
        this.color = color;
        this.bold = bold;
        this.players = players;
    }
}

class Clan {
    final String name;
    final TextColor color;
    UUID leader;
    final Set<UUID> members;

    Clan(String name, TextColor color, UUID leader, Set<UUID> members) {
        this.name = name;
        this.color = color;
        this.leader = leader;
        this.members = members;
    }
}
