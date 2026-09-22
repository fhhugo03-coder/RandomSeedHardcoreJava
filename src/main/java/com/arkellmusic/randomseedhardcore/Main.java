package com.arkellmusic.randomseedhardcore;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.Random;

public class Main extends JavaPlugin implements Listener {

    private World currentWorld;
    private boolean switching = false;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        getServer().getPluginManager().registerEvents(this, this);
        currentWorld = createRandomWorld();
        getLogger().info("Mundo inicial generado: " + currentWorld.getName());
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        if (switching) return;
        switching = true;

        int delay = getConfig().getInt("delay-seconds", 5);

        String deathMsg = getConfig().getString("death-message",
                        "&c{player} ha muerto. &eNuevo mundo en {seconds}s...")
                .replace("{player}", event.getEntity().getName())
                .replace("{seconds}", String.valueOf(delay))
                .replace('&', '§');
        Bukkit.broadcastMessage(deathMsg);

        Bukkit.getScheduler().runTaskLater(this, () -> {
            World oldWorld = currentWorld;
            World newWorld = createRandomWorld();
            currentWorld = newWorld;

            for (Player p : Bukkit.getOnlinePlayers()) {
                p.teleport(newWorld.getSpawnLocation());
                p.setHealth(p.getMaxHealth());
                p.setFoodLevel(20);
                p.setFireTicks(0);
            }

            if (getConfig().getBoolean("delete-old-worlds", true) && oldWorld != null) {
                String oldName = oldWorld.getName();
                Bukkit.unloadWorld(oldWorld, false);
                deleteWorldFolder(oldName);
            }

            String newMsg = getConfig().getString("new-world-message",
                            "&a¡Nuevo mundo generado! Bienvenidos a &e{world}")
                    .replace("{world}", newWorld.getName())
                    .replace('&', '§');
            Bukkit.broadcastMessage(newMsg);

            switching = false;
        }, 20L * delay);
    }

    private World createRandomWorld() {
        String name = "hardcore_" + System.currentTimeMillis();
        long seed = new Random().nextLong();

        WorldCreator creator = new WorldCreator(name);
        creator.seed(seed);
        creator.environment(World.Environment.NORMAL);

        return creator.createWorld();
    }

    private void deleteWorldFolder(String name) {
        File folder = new File(Bukkit.getWorldContainer(), name);
        deleteRecursively(folder);
    }

    private void deleteRecursively(File file) {
        if (file.isDirectory()) {
            File[] children = file.listFiles();
            if (children != null) {
                for (File child : children) {
                    deleteRecursively(child);
                }
            }
        }
        file.delete();
    }
}
