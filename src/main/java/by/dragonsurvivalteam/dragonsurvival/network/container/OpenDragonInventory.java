package by.dragonsurvivalteam.dragonsurvival.network.container;

import by.dragonsurvivalteam.dragonsurvival.network.IMessage;
import by.dragonsurvivalteam.dragonsurvival.server.containers.DragonContainer;
import by.dragonsurvivalteam.dragonsurvival.util.DragonUtils;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class OpenDragonInventory implements IMessage<OpenDragonInventory> {
	@Override
	public void encode(final OpenDragonInventory message, final FriendlyByteBuf buffer) { /* Nothing to do */ }

	@Override
	public OpenDragonInventory decode(final FriendlyByteBuf buffer) {
		return new OpenDragonInventory();
	}

	@Override
	public void handle(final OpenDragonInventory message, final Supplier<NetworkEvent.Context> supplier) {
		NetworkEvent.Context context = supplier.get();
		ServerPlayer sender = context.getSender();

		if (DragonUtils.isDragon(sender)) {
			context.enqueueWork(() -> {
				sender.containerMenu.removed(sender);
				sender.openMenu(new SimpleMenuProvider((containerId, inventory, player) -> new DragonContainer(containerId, inventory), Component.empty()));
			});
		}

		context.setPacketHandled(true);
	}
}