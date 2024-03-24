package me.shurikennen.signinput;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.network.protocol.game.ClientboundBlockUpdatePacket;
import net.minecraft.network.protocol.game.ClientboundOpenSignEditorPacket;
import net.minecraft.network.protocol.game.ServerboundSignUpdatePacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.entity.SignText;
import net.minecraft.world.level.block.state.BlockState;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.craftbukkit.v1_20_R3.CraftWorld;
import org.bukkit.craftbukkit.v1_20_R3.entity.CraftPlayer;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.function.Consumer;

public class SignCommand implements CommandExecutor {
    @Override
    public boolean onCommand(CommandSender commandSender, Command command, String s, String[] strings) {
        sendSign((Player) commandSender, lines -> {
            String message = String.join(" ", lines).trim();
            commandSender.sendMessage(message);
        });

        return false;
    }

    public void sendSign(Player p, Consumer<String[]> lines) {

        SignInputPlugin.getInstance().addPacketInjector(p); // Ensure a packet play is present

        Location l = p.getLocation();
        BlockPos pos = new BlockPos(l.getBlockX(), l.getBlockY(), l.getBlockZ()); // Create a sign GUI on the player
        ServerLevel level = ((CraftWorld) l.getWorld()).getHandle();
        BlockState old = level.getBlockState(pos); // Get the old block state for that position

        BlockState signDefaultBlockState = Blocks.OAK_SIGN.defaultBlockState();
        ClientboundBlockUpdatePacket sent1 = new ClientboundBlockUpdatePacket(pos, signDefaultBlockState);
        ((CraftPlayer) p).getHandle().connection.send(sent1); // Set that position to a sign

        // construct a fake tile entity to update the sign's text
        SignBlockEntity signBlockEntity = new SignBlockEntity(pos, signDefaultBlockState);
        this.setMessages(signBlockEntity, "","^^^^^^","somePrompt","Foo Bar");
        ClientboundBlockEntityDataPacket updatePacket = ClientboundBlockEntityDataPacket.create(signBlockEntity);
        // send the text to the sign
        ((CraftPlayer) p).getHandle().connection.send(updatePacket);


        ClientboundOpenSignEditorPacket sent3 = new ClientboundOpenSignEditorPacket(pos, true);
        ((CraftPlayer) p).getHandle().connection.send(sent3); // Open the sign editor


        PacketHandler.PACKET_HANDLERS.put(p.getUniqueId(), packetO -> {
            if (!(packetO instanceof ServerboundSignUpdatePacket packet)) return false; // Only intercept sign packets

            ClientboundBlockUpdatePacket sent4 = new ClientboundBlockUpdatePacket(pos, old);
            ((CraftPlayer) p).getHandle().connection.send(sent4); // Reset the block state for that packet

            lines.accept(packet.getLines()); // Accept the consumer here
            return true;
        });
        new BukkitRunnable() {
            @Override
            public void run() {

            }
        }.runTaskLater(SignInputPlugin.getInstance(), 5);

    }



    private void setMessages(SignBlockEntity entity, String... lines) {
        SignText text = new SignText();
        for (int i = 0; i < lines.length; i++) {
            text = text.setMessage(i, Component.literal(lines[i]));
        }
        entity.setText(text, true);
    }
}
