package me.shurikennen.signinput;

import io.netty.channel.Channel;
import net.minecraft.core.BlockPos;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.game.ClientboundBlockUpdatePacket;
import net.minecraft.network.protocol.game.ClientboundOpenSignEditorPacket;
import net.minecraft.network.protocol.game.ServerboundSignUpdatePacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerCommonPacketListenerImpl;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.bukkit.Location;
import org.bukkit.craftbukkit.v1_20_R3.CraftWorld;
import org.bukkit.craftbukkit.v1_20_R3.entity.CraftPlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.lang.reflect.Field;
import java.util.function.Consumer;

public final class SignInputPlugin extends JavaPlugin implements Listener {

    private static final String PACKET_INJECTOR_ID = "signinput:packet_handler";

    private static SignInputPlugin instance;

    public static SignInputPlugin getInstance() {
        return instance;
    }

    @Override
    public void onEnable() {
        instance = this;
        this.getServer().getPluginManager().registerEvents(this, this);
        this.getCommand("sign").setExecutor(new SignCommand());
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        Player p = e.getPlayer();
        addPacketInjector(p);
    }

    @EventHandler
    public void onLeave(PlayerQuitEvent e) {
        Player p = e.getPlayer();
        removePacketInjector(p);
    }


    public void addPacketInjector(Player p) {
        ServerPlayer sp = ((CraftPlayer) p).getHandle();

        try {
            Field connection = ServerCommonPacketListenerImpl.class.getDeclaredField("c");
            connection.setAccessible(true);
            Channel ch = ((Connection) connection.get(sp.connection)).channel;

            if (ch.pipeline().get(PACKET_INJECTOR_ID) != null) return;
            ch.pipeline().addAfter("decoder", PACKET_INJECTOR_ID, new PacketHandler(p));
        } catch (ReflectiveOperationException e) {
            e.printStackTrace();
        }
    }

    public void removePacketInjector(Player p) {
        ServerPlayer sp = ((CraftPlayer) p).getHandle();

        try {
            Field connection = ServerCommonPacketListenerImpl.class.getDeclaredField("c");
            connection.setAccessible(true);
            Channel ch = ((Connection) connection.get(sp.connection)).channel;

            if (ch.pipeline().get(PACKET_INJECTOR_ID) == null) return;
            ch.pipeline().remove(PACKET_INJECTOR_ID);
        } catch (ReflectiveOperationException e) {
            e.printStackTrace();
        }
    }

}
