package fr.iban.bukkitcore.manager;

import fr.iban.bukkitcore.CoreBukkitPlugin;
import fr.iban.bukkitcore.utils.PluginMessageHelper;
import fr.iban.common.manager.PlayerManager;
import fr.iban.common.messaging.CoreChannel;
import fr.iban.common.messaging.message.PlayerBoolean;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

public class BukkitPlayerManager extends PlayerManager {

    private final Map<String, UUID> offlinePlayers = new ConcurrentHashMap<>();
    private final Map<UUID, String> onlinePlayers = new ConcurrentHashMap<>();
    private final CoreBukkitPlugin plugin;

    public BukkitPlayerManager(CoreBukkitPlugin plugin) {
        this.plugin = plugin;
        CompletableFuture.supplyAsync(this::getProxyPlayerNamesFromDB).thenAccept(this.onlinePlayers::putAll);
        CompletableFuture.supplyAsync(this::getPlayerNamesFromDb).thenAccept(this.offlinePlayers::putAll);
    }

    public void sendMessageIfOnline(UUID uuid, String message) {
        if (onlinePlayers.containsKey(uuid)) {
            PluginMessageHelper.sendMessage(onlinePlayers.get(uuid), message);
        }
    }

    public void sendMessageRawIfOnline(UUID uuid, String message) {
        if (onlinePlayers.containsKey(uuid)) {
            PluginMessageHelper.sendMessageRaw(onlinePlayers.get(uuid), message);
        }
    }

    public UUID getOfflinePlayerUUID(String name) {
        return offlinePlayers.get(name);
    }

    public UUID getOnlinePlayerUUID(String name) {
        for (Map.Entry<UUID, String> entry : getOnlinePlayers().entrySet()) {
            if (entry.getValue().equals(name)) {
                return entry.getKey();
            }
        }
        return null;
    }

    public Map<UUID, String> getOnlinePlayers() {
        return onlinePlayers;
    }

    public Map<String, UUID> getOfflinePlayers() {
        return offlinePlayers;
    }

    public String getName(UUID uuid) {
        return offlinePlayers.entrySet().stream()
                .filter(entry -> entry.getValue().equals(uuid))
                .map(Map.Entry::getKey).findFirst().orElse(null);
    }

    public void setVanishedAndSync(UUID uuid, boolean newValue) {
        setVanished(uuid, newValue);
        plugin.getMessagingManager().sendMessage(CoreChannel.VANISH_STATUS_CHANGE_CHANNEL, new PlayerBoolean(uuid, newValue));
    }
}
