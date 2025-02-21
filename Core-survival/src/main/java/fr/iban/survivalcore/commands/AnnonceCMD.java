package fr.iban.survivalcore.commands;

import fr.iban.bukkitcore.utils.PluginMessageHelper;
import fr.iban.survivalcore.SurvivalCorePlugin;
import me.leoko.advancedban.manager.PunishmentManager;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.concurrent.TimeUnit;

public class AnnonceCMD implements CommandExecutor {

	private HashMap<String, Long> cooldowns = new HashMap<>();
	private SurvivalCorePlugin plugin;

	public AnnonceCMD(SurvivalCorePlugin plugin) {
		this.plugin = plugin;
	}

	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		Economy econ = plugin.getEconomy();
		Player player = (Player) sender;
		int cooldownTime = 3600; // Get number of seconds from wherever you want
		
		if(cooldowns.containsKey(sender.getName())) {
			long secondsLeft = ((cooldowns.get(sender.getName())/1000)+cooldownTime) - (System.currentTimeMillis()/1000);
			if(secondsLeft > 0) {
				sender.sendMessage("§cVous pourrez refaire une annonce dans " +TimeUnit.SECONDS.toMinutes(secondsLeft)+ " minutes!");
				return true;
			}
		}
		if(args.length <= 0) {
			player.sendMessage("§cUtilisation: /annonce (message)");
			cooldowns.remove(sender.getName());
		} else {

			if(PunishmentManager.get().isMuted(player.getUniqueId().toString())) {
				player.sendMessage("§cVous ne pouvez pas faire d'annonce !");
				return true;
			}

			cooldowns.put(sender.getName(), System.currentTimeMillis());
			StringBuilder message = new StringBuilder(); {
				for (String part : args) {
					if (!message.toString().equals("")) message.append(" ");
					message.append(part);
				}
				if (econ.getBalance(player) >= 250) {
					econ.withdrawPlayer(player, 250);
					PluginMessageHelper.sendAnnonce(player, message.toString());
				} else {
					player.sendMessage("§cIl vous faut 250$ pour faire une annonce !");
					cooldowns.remove(sender.getName());
				}
			}
		}
		return true;
	}

}
