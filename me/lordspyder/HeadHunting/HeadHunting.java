package net.pappapronta.headhunting;

import java.io.File;
import java.util.HashMap;

import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

public class HeadHunting extends JavaPlugin implements Listener {


	String prefix = ChatColor.GOLD + "[" + ChatColor.AQUA + "HeadHunting" + ChatColor.GOLD + "] "; 
	HashMap<String, Integer> hunting = new HashMap<String, Integer>();

	public static Economy econ = null;

	FileConfiguration config;
	File cfile;

	public void onEnable() {
		config = getConfig();
		config.options().copyDefaults(true);
		saveDefaultConfig();
		cfile = new File(getDataFolder(), "config.yml");
		Bukkit.getServer().getPluginManager().registerEvents(this, this);
		if (!setupEconomy() ) {
			getLogger().severe(String.format("[%s] - Disabled due to no Vault dependency found!", getDescription().getName()));
			getServer().getPluginManager().disablePlugin(this);
			return;
		}
	}

	private boolean setupEconomy() {
		if (getServer().getPluginManager().getPlugin("Vault") == null) {
			return false;
		}
		RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
		if (rsp == null) {
			return false;
		}
		econ = rsp.getProvider();
		return econ != null;
	}
	
	public boolean onCommand(CommandSender sender, Command cmd, String Comandlabel, String[] args) {
		Player p = (Player) sender;
		if (cmd.getName().equalsIgnoreCase("hunting")) {
			if (args.length != 2) {
				sender.sendMessage(prefix + ChatColor.translateAlternateColorCodes('&', this.getConfig().getString("CorrectUsage")));//Change in conf
				return true;
			}
			if(args[0].equals(p.getName())) {
				p.sendMessage(prefix + ChatColor.translateAlternateColorCodes('&', this.getConfig().getString("HeadHuntingYourSelf")));
				return true;
			}
			Player target = Bukkit.getServer().getPlayer(args[0]);
			if (target == null) {
				sender.sendMessage(prefix + ChatColor.translateAlternateColorCodes('&', this.getConfig().getString("NotFindPlayer")));
				return true;
			}
			
			int taglia = 0;
			try {
				taglia = Integer.parseInt(args[1]);
			} catch(NumberFormatException e) {
				p.sendMessage(prefix + ChatColor.translateAlternateColorCodes('&', this.getConfig().getString("NotANumber")));
				return true;
			}
			
			EconomyResponse r = econ.withdrawPlayer(p, taglia);
			if (r.transactionSuccess()) {
				hunting.put(target.getName(), taglia);
				sender.sendMessage(prefix + ChatColor.translateAlternateColorCodes('&', this.getConfig().getString("HeadHuntingOn").replaceAll("%p", target.getName())));
				getServer().broadcastMessage(prefix + ChatColor.translateAlternateColorCodes('&', this.getConfig().getString("BroadcastHeadHuntingOn").replaceAll("%s", p.getName()).replaceAll("%t", target.getName()).replaceAll("%b", Integer.toString(taglia))));
				target.setPlayerListName(ChatColor.RED + target.getName());
				return true;
			} else {
				p.sendMessage(prefix + ChatColor.translateAlternateColorCodes('&', this.getConfig().getString("NotHaveMoney")));
				return true;
			}
		}
		return true;
	}
	
	@EventHandler
	public void onDeath(PlayerDeathEvent e){
		Player player = e.getEntity();
		if(player.getKiller() != null && hunting.containsKey(player.getName())){
			Player killer = player.getKiller();
			int taglia = hunting.get(player.getName());
			e.setDeathMessage(prefix + ChatColor.translateAlternateColorCodes('&', this.getConfig().getString("WasSlainBy").replaceAll("%d", player.getName()).replaceAll("%k", killer.getName())));
			player.setPlayerListName(ChatColor.WHITE + player.getName());
			EconomyResponse r = econ.depositPlayer(killer, taglia);
			if(!r.transactionSuccess()) {
				killer.sendMessage(prefix + ChatColor.RED + "An error as occurred");
				return;
			}
			hunting.remove(player);
		}
	}
}