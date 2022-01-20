package by.jackraidenph.dragonsurvival.commands;

import by.jackraidenph.dragonsurvival.common.capability.Capabilities;
import by.jackraidenph.dragonsurvival.common.capability.caps.DragonStateHandler;
import by.jackraidenph.dragonsurvival.config.ConfigHandler;
import by.jackraidenph.dragonsurvival.misc.DragonLevel;
import by.jackraidenph.dragonsurvival.misc.DragonType;
import by.jackraidenph.dragonsurvival.network.NetworkHandler;
import by.jackraidenph.dragonsurvival.network.entity.player.SynchronizeDragonCap;
import by.jackraidenph.dragonsurvival.network.flight.SyncSpinStatus;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.tree.ArgumentCommandNode;
import com.mojang.brigadier.tree.LiteralCommandNode;
import com.mojang.brigadier.tree.RootCommandNode;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.selector.EntitySelector;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.PacketDistributor;

import java.util.List;

public class DragonCommand
{
	public static void register(CommandDispatcher<CommandSourceStack> commandDispatcher)
	{
    RootCommandNode<CommandSourceStack> rootCommandNode = commandDispatcher.getRoot();
    LiteralCommandNode<CommandSourceStack> dragon = Commands.literal("dragon").requires(commandSource -> commandSource.hasPermission(2)).executes(context -> {
        String type = context.getArgument("dragon_type", String.class);
        return runCommand(type, 1, false, context.getSource().getPlayerOrException());
    }).build();
    
    ArgumentCommandNode<CommandSourceStack, String> dragonType = Commands.argument("dragon_type", StringArgumentType.string()).suggests((context, builder) -> SharedSuggestionProvider.suggest(new String[]{"cave", "sea", "forest", "human"}, builder)).executes(context -> {
        String type = context.getArgument("dragon_type", String.class);
	    ServerPlayer serverPlayer = context.getSource().getPlayerOrException();
        return runCommand(type, 1, false, serverPlayer);
    }).build();
    
    ArgumentCommandNode<CommandSourceStack, Integer> dragonStage = Commands.argument("dragon_stage", IntegerArgumentType.integer(1, 3)).suggests((context, builder) -> SharedSuggestionProvider.suggest(new String[]{"1", "2", "3"}, builder)).executes(context -> {
        String type = context.getArgument("dragon_type", String.class);
        int stage = context.getArgument("dragon_stage", Integer.TYPE);
	    ServerPlayer serverPlayer = context.getSource().getPlayerOrException();
        return runCommand(type, stage, false, serverPlayer);
    }).build();
    
    ArgumentCommandNode<CommandSourceStack, Boolean> giveWings = Commands.argument("wings", BoolArgumentType.bool()).executes(context -> {
        String type = context.getArgument("dragon_type", String.class);
        int stage = context.getArgument("dragon_stage", Integer.TYPE);
        boolean wings = context.getArgument("wings", Boolean.TYPE);
	    ServerPlayer serverPlayer = context.getSource().getPlayerOrException();
        return runCommand(type, stage, wings, serverPlayer);
    }).build();
    
    ArgumentCommandNode<CommandSourceStack, EntitySelector> target = Commands.argument("target", EntityArgument.players()).executes(context -> {
        String type = context.getArgument("dragon_type", String.class);
        int stage = context.getArgument("dragon_stage", Integer.TYPE);
        boolean wings = context.getArgument("wings", Boolean.TYPE);
        EntitySelector selector = context.getArgument("target", EntitySelector.class);
        List<ServerPlayer> serverPlayers = selector.findPlayers(context.getSource());
        serverPlayers.forEach((player) -> runCommand(type, stage, wings, player));
        return 1;
    }).build();
    
    rootCommandNode.addChild(dragon);
    dragon.addChild(dragonType);
    dragonType.addChild(dragonStage);
    dragonStage.addChild(giveWings);
    giveWings.addChild(target);
}
	
	private static int runCommand(String type, int stage, boolean wings, ServerPlayer serverPlayer)
	{
	    serverPlayer.getCapability(Capabilities.DRAGON_CAPABILITY).ifPresent(dragonStateHandler -> {
	        DragonType dragonType1 = type.equalsIgnoreCase("human") ? DragonType.NONE : DragonType.valueOf(type.toUpperCase());
			
		    if(dragonType1 == DragonType.NONE && dragonStateHandler.getType() != DragonType.NONE){
			    reInsertClawTools(serverPlayer, dragonStateHandler);
		    }
			
	        dragonStateHandler.setType(dragonType1);
	        DragonLevel dragonLevel = DragonLevel.values()[stage - 1];
	        dragonStateHandler.setHasWings(wings);
			dragonStateHandler.getMovementData().spinLearned = wings;
	        dragonStateHandler.setSize(dragonLevel.size, serverPlayer);
	        dragonStateHandler.setPassengerId(0);
			
			dragonStateHandler.growing = true;
		    NetworkHandler.CHANNEL.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> serverPlayer), new SyncSpinStatus(serverPlayer.getId(), dragonStateHandler.getMovementData().spinAttack, dragonStateHandler.getMovementData().spinCooldown, dragonStateHandler.getMovementData().spinLearned));
		    NetworkHandler.CHANNEL.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> serverPlayer), new SynchronizeDragonCap(serverPlayer.getId(), false, dragonType1, dragonLevel.size, wings, ConfigHandler.SERVER.caveLavaSwimmingTicks.get(), dragonStateHandler.getPassengerId()));
	        serverPlayer.refreshDimensions();
	    });
	    return 1;
	}
	
	public static void reInsertClawTools(ServerPlayer serverPlayer, DragonStateHandler dragonStateHandler)
	{
		for(int i = 0; i < 4; i++){
			ItemStack stack =  dragonStateHandler.getClawInventory().getClawsInventory().getItem(i);
			
			if(!serverPlayer.addItem(stack)){
				if(serverPlayer.level.addFreshEntity(new ItemEntity(serverPlayer.level, serverPlayer.position().x, serverPlayer.position().y, serverPlayer.position().z, stack))){
					dragonStateHandler.getClawInventory().getClawsInventory().removeItem(i, stack.getCount());
				}
			}else{
				dragonStateHandler.getClawInventory().getClawsInventory().removeItem(i, stack.getCount());
			}
		}
	}
}