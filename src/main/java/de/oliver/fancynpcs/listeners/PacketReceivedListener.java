package de.oliver.fancynpcs.listeners;

import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import de.oliver.fancylib.ReflectionUtils;
import de.oliver.fancynpcs.FancyNpcs;
import de.oliver.fancynpcs.Npc;
import de.oliver.fancynpcs.events.NpcInteractEvent;
import de.oliver.fancynpcs.events.PacketReceivedEvent;
import me.clip.placeholderapi.PlaceholderAPI;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.minecraft.network.protocol.game.ServerboundInteractPacket;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

public class PacketReceivedListener implements Listener {

    @EventHandler
    public void onPacketReceived(PacketReceivedEvent event) {
        if (event.getPacket() instanceof ServerboundInteractPacket interactPacket) {
            Player p = event.getPlayer();

            String hand = "";

            if (interactPacket.getActionType() != ServerboundInteractPacket.ActionType.ATTACK) {
                hand = ReflectionUtils.getValue(ReflectionUtils.getValue(interactPacket, "b"), "a").toString(); // ServerboundInteractPacket.InteractionAction.hand
            }

            int entityId = interactPacket.getEntityId();
            ServerboundInteractPacket.ActionType action = interactPacket.getActionType();
            boolean isSneaking = interactPacket.isUsingSecondaryAction();

            if (action == ServerboundInteractPacket.ActionType.ATTACK || action == ServerboundInteractPacket.ActionType.INTERACT && hand.equalsIgnoreCase("MAIN_HAND")) {
                Npc npc = FancyNpcs.getInstance().getNpcManager().getNpc(entityId);
                if (npc == null) {
                    return;
                }

                NpcInteractEvent npcInteractEvent = new NpcInteractEvent(npc, npc.getPlayerCommand(), npc.getServerCommand(), npc.getOnClick(), p);
                npcInteractEvent.callEvent();

                if (npcInteractEvent.isCancelled()) {
                    return;
                }

                // onClick
                npc.getOnClick().accept(p);

                // message
                if(npc.getMessage() != null && npc.getMessage().length() > 0) {
                    String msg = npc.getMessage();
                    if(FancyNpcs.getInstance().isUsingPlaceholderAPI()) {
                        msg = PlaceholderAPI.setPlaceholders(p, msg);
                    }

                    p.sendMessage(MiniMessage.miniMessage().deserialize(msg));
                }

                // serverCommand
                if (npc.getServerCommand() != null && npc.getServerCommand().length() > 0) {
                    String command = npc.getServerCommand();
                    command = command.replace("{player}", p.getName());

                    if(FancyNpcs.getInstance().isUsingPlaceholderAPI()) {
                        command = PlaceholderAPI.setPlaceholders(p, command);
                    }

                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);
                }

                // playerCommand
                if (npc.getPlayerCommand() != null && npc.getPlayerCommand().length() > 0) {
                    String command;

                    if(FancyNpcs.getInstance().isUsingPlaceholderAPI()) {
                        command = PlaceholderAPI.setPlaceholders(p, npc.getPlayerCommand());
                    } else {
                        command = npc.getPlayerCommand();
                    }

                    if (command.toLowerCase().startsWith("server")) {
                        String[] args = npc.getPlayerCommand().split(" ");
                        if (args.length < 2) {
                            return;
                        }
                        String server = args[1];

                        ByteArrayDataOutput out = ByteStreams.newDataOutput();
                        out.writeUTF("Connect");
                        out.writeUTF(server);
                        p.sendPluginMessage(FancyNpcs.getInstance(), "BungeeCord", out.toByteArray());
                        return;
                    }

                    FancyNpcs.getInstance().getScheduler().runTask(
                            p.getLocation(),
                            () -> p.performCommand(command)
                    );
                }
            }
        }
    }
}
