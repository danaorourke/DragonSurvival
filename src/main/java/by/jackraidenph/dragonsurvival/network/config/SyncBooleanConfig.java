package by.jackraidenph.dragonsurvival.network.config;

import by.jackraidenph.dragonsurvival.config.ConfigHandler;
import by.jackraidenph.dragonsurvival.network.IMessage;
import by.jackraidenph.dragonsurvival.network.NetworkHandler;
import com.electronwill.nightconfig.core.UnmodifiableConfig;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.common.ForgeConfigSpec.BooleanValue;
import net.minecraftforge.network.NetworkEvent.Context;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.PacketDistributor;

import java.util.function.Supplier;


public class SyncBooleanConfig implements IMessage<SyncBooleanConfig>
{
	public SyncBooleanConfig() {}
	
	public String key;
	public boolean value;
	public String type;
	
	public SyncBooleanConfig(String key, boolean value, String type)
	{
		this.key = key;
		this.value = value;
		this.type = type;
	}
	
	@Override
	public void encode(SyncBooleanConfig message, FriendlyByteBuf buffer)
	{
		buffer.writeUtf(message.type);
		buffer.writeBoolean(message.value);
		buffer.writeUtf(message.key);
	}
	
	@Override
	public SyncBooleanConfig decode(FriendlyByteBuf buffer)
	{
		String type = buffer.readUtf();
		boolean value = buffer.readBoolean();
		String key = buffer.readUtf();
		return new SyncBooleanConfig(key, value, type);
	}
	
	@Override
	public void handle(SyncBooleanConfig message, Supplier<Context> supplier)
	{
		if(supplier.get().getDirection() == NetworkDirection.PLAY_TO_SERVER){
			ServerPlayer entity = supplier.get().getSender();
			if(entity == null || !entity.hasPermissions(2)) return;
			NetworkHandler.CHANNEL.send(PacketDistributor.ALL.noArg() , new SyncBooleanConfig(message.key, message.value, message.type));
		}
		
		UnmodifiableConfig spec = message.type.equalsIgnoreCase("server") ? ConfigHandler.serverSpec.getValues() : ConfigHandler.commonSpec.getValues();
		Object ob = spec.get(message.type + "." + message.key);
		
		if (ob instanceof BooleanValue) {
			BooleanValue booleanValue = (BooleanValue)ob;
			
			try {
				booleanValue.set(message.value);
				booleanValue.save();
			}catch (Exception ignored){}
		}
	}
}