package fr.iban.bukkitcore.manager;

import com.earth2me.essentials.Essentials;
import com.earth2me.essentials.User;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;
import fr.iban.bukkitcore.CoreBukkitPlugin;
import fr.iban.bukkitcore.plan.PlanDataManager;
import fr.iban.bukkitcore.utils.PluginMessageHelper;
import fr.iban.bukkitcore.utils.SLocationUtils;
import fr.iban.common.messaging.CoreChannel;
import fr.iban.common.teleport.*;
import io.papermc.lib.PaperLib;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;

public class TeleportManager {

    private final CoreBukkitPlugin plugin;
    private final ListMultimap<UUID, TpRequest> tpRequests = ArrayListMultimap.create();
    private final List<UUID> pendingTeleports = new ArrayList<>();
    private final Map<UUID, Location> unsafeTpPending = new HashMap<>();
    private final Map<UUID, String> lastSurvivalServer = new HashMap<>();

    public TeleportManager(CoreBukkitPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Teleport player to SLocation
     */
    public void teleport(Player player, SLocation sloc) {
        teleport(player, sloc, 0);
    }

    /**
     * Delayed teleport player to SLocation.
     *
     * @param delay delay in seconds
     */
    public void teleport(Player player, SLocation sloc, int delay) {
        setLastLocation(player.getUniqueId());
        plugin.getMessagingManager().sendMessage("TeleportToLocationBungee", new TeleportToLocation(player.getUniqueId(), sloc, delay));
    }

    /**
     * Teleport player to another player
     *
     * @param uuid - player's uuid
     */
    public void teleport(UUID uuid, UUID target) {
        teleport(uuid, target, 0);
    }

    /**
     * Delayed teleport player to another player
     *
     * @param uuid   - player's uuid
     * @param target - target player's uuid
     * @param delay  in seconds
     */
    public void teleport(UUID uuid, UUID target, int delay) {
        setLastLocation(uuid);
        plugin.getMessagingManager().sendMessage("TeleportToPlayerBungee", new TeleportToPlayer(uuid, target, delay));
    }

    /**
     * Used for essentials /back
     *
     * @param uuid
     */
    private void setLastLocation(UUID uuid) {
        Essentials essentials = plugin.getEssentials();

        if (essentials != null) {
            User user = essentials.getUser(uuid);
            if (user != null) {
                user.setLastLocation();
            }
        }
    }

    /**
     * Send teleport request to player.
     */
    public void sendTeleportRequest(UUID from, UUID to, RequestType type) {
        plugin.getMessagingManager().sendMessage("TeleportRequestBungee", new TpRequest(from, to, type));
    }

    public List<UUID> getPendingTeleports() {
        return pendingTeleports;
    }

    public void removeTeleportWaiting(UUID uuid) {
        pendingTeleports.remove(uuid);
        plugin.getMessagingManager().sendMessage(CoreChannel.REMOVE_PENDING_TP_CHANNEL, uuid.toString());
    }

    public boolean isTeleportWaiting(UUID uuid) {
        return pendingTeleports.contains(uuid);
    }

    public ListMultimap<UUID, TpRequest> getTpRequests() {
        return tpRequests;
    }

    public List<TpRequest> getTpRequests(Player player) {
        return tpRequests.get(player.getUniqueId());
    }

    public TpRequest getTpRequestFrom(Player player, UUID from) {
        for (TpRequest request : getTpRequests(player)) {
            if (request.getPlayerFrom().equals(from)) {
                return request;
            }
        }
        return null;
    }

    public void removeTpRequest(UUID uuid, TpRequest tpRequest) {
        tpRequests.remove(uuid, tpRequest);
        plugin.getMessagingManager().sendMessage(CoreChannel.REMOVE_TP_REQUEST_CHANNEL, tpRequest);
    }

    public void performTeleportToLocation(TeleportToLocation teleportToLocation) {
        if (!teleportToLocation.getLocation().getServer().equals(CoreBukkitPlugin.getInstance().getServerName())) {
            return;
        }
        SLocation sloc = teleportToLocation.getLocation();
        Location loc = SLocationUtils.getLocation(sloc);
        new BukkitRunnable() {
            int count = 0;

            @Override
            public void run() {

                Player player = Bukkit.getPlayer(teleportToLocation.getUuid());

                if (player != null) {
                    tpAsync(player, loc);
                    cancel();
                }

                count++;

                if (count > 20) {
                    cancel();
                }

            }
        }.runTaskTimer(CoreBukkitPlugin.getInstance(), 1L, 1L);
    }

    public void performTeleportToPlayer(TeleportToPlayer teleportToPlayer) {
        Player target = Bukkit.getPlayer(teleportToPlayer.getTargetId());

        if (target == null) {
            return;
        }

        new BukkitRunnable() {

            @Override
            public void run() {

                Player target = Bukkit.getPlayer(teleportToPlayer.getTargetId());
                Player player = Bukkit.getPlayer(teleportToPlayer.getUuid());

                if (target == null) {
                    cancel();
                    return;
                }

                if (player != null) {
                    tpAsync(player, target.getLocation());
                    cancel();
                }

            }
        }.runTaskTimer(CoreBukkitPlugin.getInstance(), 1L, 1L);
    }

    public void performRandomTeleport(RandomTeleportMessage rtpMessage) {
        if (!plugin.getServerName().equalsIgnoreCase(rtpMessage.getTargetServer())) {
            return;
        }
        String world = switch (rtpMessage.getWorld()) {
            case "world" -> Bukkit.getWorlds().get(0).getName();
            case "world_nether" -> Bukkit.getWorlds().get(1).getName();
            case "world_the_end" -> Bukkit.getWorlds().get(2).getName();
            default -> rtpMessage.getWorld();
        };

        new BukkitRunnable() {
            int count = 0;

            @Override
            public void run() {

                Player player = Bukkit.getPlayer(rtpMessage.getUuid());

                if (player != null) {
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "rtp player " + player.getName() + " " + world);
                    cancel();
                }

                count++;

                if (count > 20) {
                    cancel();
                }

            }
        }.runTaskTimer(CoreBukkitPlugin.getInstance(), 1L, 1L);

    }


    private void tpAsync(Player player, Location loc) {
        player.sendActionBar("§aChargement de la zone...");
        loc.getWorld().getChunkAtAsyncUrgently(loc).thenAccept(chunk -> {
            if (SLocationUtils.isSafeLocation(loc) || player.getGameMode() != GameMode.SURVIVAL) {
                player.teleportAsync(loc).thenAccept(result -> {
                    if (result) {
                        player.sendActionBar("§aTéléportation effectuée !");
                    } else {
                        player.sendActionBar("§cLa téléportation a échoué !");
                    }
                });
            } else {
                unsafeTpPending.put(player.getUniqueId(), loc);
                player.sendMessage(Component.text("⚠ La zone de téléportation n'est pas sécurisée.\nSi vous souhaitez tout de même vous y téléporter, cliquez ici.")
                        .color(NamedTextColor.RED)
                        .clickEvent(ClickEvent.runCommand("/tplastunsafe"))
                        .hoverEvent(HoverEvent.showText(Component.text("Se téléporter à vos risques et périls.", NamedTextColor.WHITE).decorate(TextDecoration.BOLD))));
            }
        });
    }

    public void tpAsyncLastUnsafe(Player player) {
        Location location = unsafeTpPending.get(player.getUniqueId());
        if (location != null) {
            player.teleportAsync(location).thenAccept(result -> {
                if (result) {
                    player.sendActionBar("§aTéléportation effectuée !");
                } else {
                    player.sendActionBar("§cLa téléportation a échoué !");
                }
            });
            unsafeTpPending.remove(player.getUniqueId());
        } else {
            player.sendMessage("§cVous n'avez pas de téléportation en attente.");
        }
    }

    public String getLastSurvivalServer(UUID uuid) {
        return lastSurvivalServer.get(uuid);
    }

    public void setLastSurvivalServer(UUID uuid, String server) {
        lastSurvivalServer.put(uuid, server);
    }

    public void teleportToSurvivalServer(Player player) {
        if (!plugin.isSurvivalServer()) {
            if (getLastSurvivalServer(player.getUniqueId()) != null) {
                PluginMessageHelper.sendPlayerToServer(player, getLastSurvivalServer(player.getUniqueId()));
            } else {
                PluginMessageHelper.sendPlayerToServer(player, plugin.getConfig().getString("survie-servername", "survie"));
            }
        } else {
            player.sendMessage("§cVous êtes déjà dans un serveur survie.");
        }
    }

    public void randomTeleportToSurvivalServer(Player player) {
        randomTeleport(player, determineTargetServer(getSurvivalServers()), "world");
    }

    public void randomTeleport(Player player, String server, String world) {
        RandomTeleportMessage randomTeleportMessage = new RandomTeleportMessage(player.getUniqueId(), server, world);
        if (plugin.getServerName().equalsIgnoreCase(randomTeleportMessage.getTargetServer())) {
            performRandomTeleport(randomTeleportMessage);
        } else {
            plugin.getMessagingManager().sendMessage(CoreChannel.RANDOM_TELEPORT, randomTeleportMessage);
            PluginMessageHelper.sendPlayerToServer(player, server);
        }
    }


    /**
     * Get the less played server or random if plan is not hooked
     *
     * @param possibleTargets possible target server names
     * @return best server
     */
    private String determineTargetServer(List<String> possibleTargets) {
        if (PlanDataManager.usePlanIntegration()) {
            PlanDataManager.fetchPlanIfNeeded(); // Pull fresh plan data if needed
            String server = PlanDataManager.getServerWithLowestPlayTime(possibleTargets);
            if (server != null) {
                return server;
            }
        }

        Collections.shuffle(possibleTargets);
        return possibleTargets.get(0);
    }

    private List<String> getSurvivalServers() {
        return plugin.getConfig().getStringList("survival-servers");
    }
}