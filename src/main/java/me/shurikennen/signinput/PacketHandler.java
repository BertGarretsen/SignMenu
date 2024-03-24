package me.shurikennen.signinput;

import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import net.minecraft.network.protocol.Packet;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Predicate;

public class PacketHandler extends ChannelDuplexHandler {


    public static final Map<UUID, Predicate<Packet<?>>> PACKET_HANDLERS = new HashMap<>();

    private final Player player;

    public PacketHandler(Player player) {
        this.player = player;
    }


    @Override
    public void channelRead(ChannelHandlerContext ctx, Object packetO) throws Exception {
        if (!(packetO instanceof Packet<?> packet)) {
            super.channelRead(ctx, packetO);
            return;
        }

        Predicate<Packet<?>> handler = PACKET_HANDLERS.get(player.getUniqueId());
        if (handler != null) new BukkitRunnable() {
            @Override
            public void run() {
                boolean success = handler.test(packet);
                if (success) PACKET_HANDLERS.remove(player.getUniqueId());
            }
        }.runTask(SignInputPlugin.getInstance());

        super.channelRead(ctx, packetO);
    }
}
