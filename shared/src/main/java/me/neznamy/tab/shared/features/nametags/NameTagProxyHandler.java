package me.neznamy.tab.shared.features.nametags;

import lombok.RequiredArgsConstructor;
import me.neznamy.tab.shared.TabConstants;
import me.neznamy.tab.shared.features.proxy.ProxyPlayer;
import me.neznamy.tab.shared.features.types.ProxyFeature;
import me.neznamy.tab.shared.platform.Scoreboard;
import me.neznamy.tab.shared.platform.TabPlayer;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Class for handling proxy players in NameTag feature.
 * Separated to avoid the main class getting too massive.
 */
@RequiredArgsConstructor
public class NameTagProxyHandler implements ProxyFeature {

    @NotNull
    private final NameTag feature;
    @NotNull
    private final Map<UUID, String> lastSentData = new ConcurrentHashMap<>();

    public void sendProxyMessage(@NotNull TabPlayer player) {
        sendProxyMessage(player, false);
    }

    public void sendProxyMessage(@NotNull TabPlayer player, boolean force) {
        if (feature.getProxy() != null) {
            String payload = player.teamData.teamName + '\u0000' + player.teamData.prefix.get() + '\u0000' + player.teamData.suffix.get() + '\u0000' + player.teamData.getTeamVisibility(player);
            String previous = lastSentData.put(player.getUniqueId(), payload);
            if (!force && payload.equals(previous)) return;
            feature.getProxy().sendMessage(new NameTagProxyPlayerData(
                    feature,
                    feature.getProxy().getIdCounter().incrementAndGet(),
                    player.getUniqueId(),
                    player.teamData.teamName,
                    player.teamData.prefix.get(),
                    player.teamData.suffix.get(),
                    player.teamData.getTeamVisibility(player) ? Scoreboard.NameVisibility.ALWAYS : Scoreboard.NameVisibility.NEVER
            ));
        }
    }

    public void removeLocalPlayer(@NotNull UUID playerId) {
        lastSentData.remove(playerId);
    }

    @Override
    public void onProxyLoadRequest() {
        for (TabPlayer all : feature.getOnlinePlayers().getPlayers()) {
            sendProxyMessage(all, true);
        }
    }

    @Override
    public void onQuit(@NotNull ProxyPlayer player) {
        if (player.getNametag() == null) {
            // One of the two options is being forcibly unregistered when real player joined
            return;
        }
        feature.unregisterTeam(player);
    }

    @Override
    public void onJoin(@NotNull ProxyPlayer player) {
        if (player.getNametag() == null) return; // Player not loaded yet
        for (TabPlayer viewer : feature.getOnlinePlayers().getPlayers()) {
            if (!viewer.server.canSee(player.server)) continue;
            if (player.isVanished() && !viewer.hasPermission(TabConstants.Permission.SEE_VANISHED)) continue;
            viewer.teamData.registerTeam(
                    player,
                    player.getNametag().getResolvedTeamName(),
                    feature.getPrefixCache().get(player.getNametag().getPrefix()),
                    feature.getSuffixCache().get(player.getNametag().getSuffix()),
                    player.getNametag().getNameVisibility(),
                    Scoreboard.CollisionRule.ALWAYS,
                    Collections.singletonList(player.getNickname()),
                    feature.getTeamOptions(),
                    feature.getLastColorCache().get(player.getNametag().getPrefix()).getLastStyle().toEnumChatFormat()
            );
        }
    }
}
