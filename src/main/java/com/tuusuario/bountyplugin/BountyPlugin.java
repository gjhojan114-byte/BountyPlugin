package com.tuusuario.bountyplugin;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;

public class BountyPlugin extends JavaPlugin implements CommandExecutor, Listener {

    private File bountiesFile;
    private FileConfiguration bountiesConfig;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        createBountiesConfig();

        getServer().getPluginManager().registerEvents(this, this);
        getCommand("bounty").setExecutor(this);

        getLogger().info("SimpleBounty activado correctamente.");
    }

    @Override
    public void onDisable() {
        getLogger().info("SimpleBounty desactivado.");
    }

    // --- MANEJO DE COMANDOS ---
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Solo jugadores pueden usar este comando.");
            return true;
        }

        if (args.length == 0) {
            player.sendMessage(ChatColor.RED + "Uso: /bounty <jugador> <monto> O /bounty check <jugador>");
            return true;
        }

        // Consultar Recompensa: /bounty check <jugador>
        if (args[0].equalsIgnoreCase("check")) {
            if (args.length < 2) {
                player.sendMessage(ChatColor.RED + "Uso: /bounty check <jugador>");
                return true;
            }
            @SuppressWarnings("deprecation")
            OfflinePlayer target = Bukkit.getOfflinePlayer(args[1]);
            double currentBounty = getBounty(target);
            player.sendMessage(ChatColor.GOLD + "La recompensa por la cabeza de " + ChatColor.YELLOW + target.getName() + ChatColor.GOLD + " es de: " + ChatColor.GREEN + "$" + currentBounty);
            return true;
        }

        // Poner Recompensa: /bounty <jugador> <monto>
        if (args.length < 2) {
            player.sendMessage(ChatColor.RED + "Uso: /bounty <jugador> <monto>");
            return true;
        }

        @SuppressWarnings("deprecation")
        OfflinePlayer target = Bukkit.getOfflinePlayer(args[0]);

        if (target.getUniqueId().equals(player.getUniqueId())) {
            player.sendMessage(ChatColor.RED + "No puedes poner una recompensa por tu propia cabeza.");
            return true;
        }

        double amount;
        try {
            amount = Double.parseDouble(args[1]);
        } catch (NumberFormatException e) {
            player.sendMessage(ChatColor.RED + "El monto debe ser un número válido.");
            return true;
        }

        double minBounty = getConfig().getDouble("min-bounty", 100);
        if (amount < minBounty) {
            player.sendMessage(ChatColor.RED + "La recompensa mínima es de $" + minBounty);
            return true;
        }

        double newTotal = getBounty(target) + amount;
        setBounty(target, newTotal);

        Bukkit.broadcastMessage(ChatColor.DARK_RED + "☠ " + ChatColor.RED + player.getName() + 
                ChatColor.YELLOW + " ha puesto una recompensa de " + ChatColor.GREEN + "$" + amount + 
                ChatColor.YELLOW + " por la cabeza de " + ChatColor.RED + target.getName() + "!" +
                ChatColor.GOLD + " (Total: $" + newTotal + ")");

        return true;
    }

    // --- EVENTO DE MUERTE / COBRO DE RECOMPENSA ---
    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player victim = event.getEntity();
        Player killer = victim.getKiller();

        if (killer != null && !killer.equals(victim)) {
            double bounty = getBounty(victim);

            if (bounty > 0) {
                // Ejecutar comando para entregar recompensa
                String rewardCmd = getConfig().getString("reward-command", "eco give %player% %amount%")
                        .replace("%player%", killer.getName())
                        .replace("%amount%", String.valueOf((long) bounty));

                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), rewardCmd);

                Bukkit.broadcastMessage(ChatColor.GOLD + "⚔ " + ChatColor.GREEN + killer.getName() + 
                        ChatColor.YELLOW + " ha reclamado la recompensa de " + ChatColor.GREEN + "$" + bounty + 
                        ChatColor.YELLOW + " tras eliminar a " + ChatColor.RED + victim.getName() + "!");

                // Limpiar la recompensa de la víctima
                setBounty(victim, 0);
            }
        }
    }

    // --- BASE DE DATOS LOCAL (bounties.yml) ---
    private void createBountiesConfig() {
        bountiesFile = new File(getDataFolder(), "bounties.yml");
        if (!bountiesFile.exists()) {
            bountiesFile.getParentFile().mkdirs();
            saveResource("bounties.yml", false);
        }
        bountiesConfig = YamlConfiguration.loadConfiguration(bountiesFile);
    }

    private double getBounty(OfflinePlayer player) {
        return bountiesConfig.getDouble("bounties." + player.getUniqueId(), 0.0);
    }

    private void setBounty(OfflinePlayer player, double amount) {
        bountiesConfig.set("bounties." + player.getUniqueId(), amount);
        try {
            bountiesConfig.save(bountiesFile);
        } catch (IOException e) {
            getLogger().severe("Error al guardar bounties.yml");
        }
    }
}
