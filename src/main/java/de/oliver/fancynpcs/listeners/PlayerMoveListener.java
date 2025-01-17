package de.oliver.fancynpcs.listeners;

import de.oliver.fancynpcs.FancyNpcs;
import de.oliver.fancynpcs.Npc;
import net.minecraft.server.level.ServerPlayer;
import org.bukkit.Location;
import org.bukkit.craftbukkit.v1_20_R1.entity.CraftPlayer;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;

public class PlayerMoveListener implements Listener {

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Location loc = event.getTo();

        for (Npc npc : FancyNpcs.getInstance().getNpcManager().getAllNpcs()) {
            if (!npc.isSpawnEntity()) {
                continue;
            }

            if (loc.getWorld() != npc.getLocation().getWorld()) {
                continue;
            }

            CraftPlayer cp = ((CraftPlayer) event.getPlayer());
            ServerPlayer sp = cp.getHandle();

            double distance = loc.distance(npc.getLocation());
            if (Double.isNaN(distance))
                continue;

            boolean isCurrentlyVisible = npc.getIsVisibleForPlayer().getOrDefault(cp.getUniqueId(), false);

            if (distance > FancyNpcs.getInstance().getFancyNpcConfig().getVisibilityDistance() && isCurrentlyVisible) {
                npc.remove(cp);
            } else if (distance < FancyNpcs.getInstance().getFancyNpcConfig().getVisibilityDistance() && !isCurrentlyVisible) {
                npc.spawn(cp);
            }

            if (npc.isTurnToPlayer() && distance < FancyNpcs.getInstance().getFancyNpcConfig().getTurnToPlayerDistance()) {
                Location newLoc = loc.clone();
                newLoc.setDirection(newLoc.subtract(npc.getLocation()).toVector());
                npc.lookAt(sp, newLoc);
            }
        }
    }
}
