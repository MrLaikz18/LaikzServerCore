package fr.iban.bukkitcore.listeners;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import fr.iban.bukkitcore.CoreBukkitPlugin;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerCommandSendEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public class CommandsListener implements Listener {

    private final CoreBukkitPlugin plugin;
    private final Multimap<UUID, String> approvedCommands = ArrayListMultimap.create();

    public CommandsListener(CoreBukkitPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onCommandSend(PlayerCommandSendEvent e) {
        Player player = e.getPlayer();

        if (player.hasPermission("servercore.admin")) {
            return;
        }

        List<String> allowed = new ArrayList<>(plugin.getTrustedCommandManager().getBukkitPlayerCommands());

        if (player.hasPermission("servercore.moderation")) {
            allowed.addAll(plugin.getTrustedCommandManager().getBukkitStaffCommands());
        }

        e.getCommands().clear();
        e.getCommands().addAll(allowed);
    }

    @EventHandler
    public void onCommand(PlayerCommandPreprocessEvent e) {
        Player player = e.getPlayer();

        if(!plugin.getConfig().getBoolean("command-approval", true)) {
            return;
        }

        if (plugin.getTrustedUserManager().isTrusted(player)) {
            return;
        }

        String command = e.getMessage().split(" ")[0].replace("/", "");

        if (plugin.getTrustedCommandManager().getTrustedBukkitCommands().contains(command.toLowerCase())) {
            return;
        }

        if (approvedCommands.get(player.getUniqueId()).contains(command.toLowerCase())) {
            approvedCommands.remove(player.getUniqueId(), command);
            return;
        }

        Command bukkitCommand = Bukkit.getServer().getPluginCommand(command);
        if (bukkitCommand != null) {
            if(!bukkitCommand.testPermission(player)) return;
            e.setCancelled(true);
            player.sendMessage("§cApprobation requise.");
            plugin.getApprovalManager().sendRequest(player,
                    player.getName() + " (" + Objects.requireNonNull(player.getAddress()).getHostString() + ") essaye d'exécuter la commande " + e.getMessage() + ".",
                    result -> {
                        if (result) {
                            Bukkit.getScheduler().runTask(plugin, () -> {
                                approvedCommands.put(player.getUniqueId(), command);
                                player.chat(e.getMessage());
                            });
                        }
                    });
        }
    }

}
