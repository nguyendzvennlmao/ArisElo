package me.aris.ariselo;

import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

public class ArisElo extends JavaPlugin implements Listener {
    private Map<UUID, Integer> eloData = new ConcurrentHashMap<>();
    private File dataFile;
    private FileConfiguration dataConfig;
    private final Random random = new Random();

    @Override
    public void onEnable() {
        saveDefaultConfig();
        createDataConfig();
        getServer().getPluginManager().registerEvents(this, this);
        getCommand("elo").setExecutor(new EloCommand(this));
        getCommand("elo").setTabCompleter(new EloTabCompleter());

        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new EloExpansion(this).register();
        }
    }

    @Override
    public void onDisable() {
        saveEloData();
    }

    private void createDataConfig() {
        dataFile = new File(getDataFolder(), "data.yml");
        if (!dataFile.exists()) {
            dataFile.getParentFile().mkdirs();
            try { dataFile.createNewFile(); } catch (IOException e) { e.printStackTrace(); }
        }
        dataConfig = YamlConfiguration.loadConfiguration(dataFile);
    }

    public synchronized void saveEloData() {
        for (Map.Entry<UUID, Integer> entry : eloData.entrySet()) {
            dataConfig.set("players." + entry.getKey().toString(), entry.getValue());
        }
        try { dataConfig.save(dataFile); } catch (IOException e) { e.printStackTrace(); }
    }

    public int getElo(UUID uuid) {
        return eloData.getOrDefault(uuid, dataConfig.getInt("players." + uuid.toString(), 0));
    }

    public void setElo(UUID uuid, int amount) {
        eloData.put(uuid, Math.max(0, amount));
    }

    public String getRank(int elo) {
        String rankName = "No Rank";
        for (Map<?, ?> map : getConfig().getMapList("ranks")) {
            int threshold = (int) map.get("threshold");
            if (elo >= threshold) {
                rankName = (String) map.get("name");
            }
        }
        return rankName;
    }

    public int getNextRankElo(int elo) {
        for (Map<?, ?> map : getConfig().getMapList("ranks")) {
            int threshold = (int) map.get("threshold");
            if (threshold > elo) return threshold - elo;
        }
        return 0;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        UUID uuid = e.getPlayer().getUniqueId();
        Bukkit.getAsyncScheduler().runNow(this, task -> {
            int val = dataConfig.getInt("players." + uuid.toString(), 0);
            eloData.put(uuid, val);
        });
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        UUID uuid = e.getPlayer().getUniqueId();
        int val = eloData.getOrDefault(uuid, 0);
        Bukkit.getAsyncScheduler().runNow(this, task -> {
            synchronized (this) {
                dataConfig.set("players." + uuid.toString(), val);
                try { dataConfig.save(dataFile); } catch (IOException ex) { ex.printStackTrace(); }
            }
        });
        eloData.remove(uuid);
    }

    @EventHandler
    public void onKill(PlayerDeathEvent e) {
        Player victim = e.getEntity();
        Player killer = victim.getKiller();

        if (killer != null && killer != victim) {
            int kMin = getConfig().getInt("settings.kill.min");
            int kMax = getConfig().getInt("settings.kill.max");
            int dMin = getConfig().getInt("settings.death.min");
            int dMax = getConfig().getInt("settings.death.max");

            int gain = random.nextInt((kMax - kMin) + 1) + kMin;
            int loss = random.nextInt((dMax - dMin) + 1) + dMin;

            setElo(killer.getUniqueId(), getElo(killer.getUniqueId()) + gain);
            setElo(victim.getUniqueId(), getElo(victim.getUniqueId()) - loss);

            sendEloTitle(killer, "kill", gain);
            sendEloTitle(victim, "death", loss);
        }
    }

    private void sendEloTitle(Player player, String type, int amount) {
        String title = getConfig().getString("titles." + type + ".title").replace("%elo%", String.valueOf(amount));
        String subtitle = getConfig().getString("titles." + type + ".subtitle");
        player.sendTitle(translate(title), translate(subtitle), 10, 20, 10);
    }

    public String translate(String msg) {
        return msg.replace("&", "§");
    }
  }
