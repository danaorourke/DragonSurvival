package by.jackraidenph.dragonsurvival.client.skinPartSystem;

import by.jackraidenph.dragonsurvival.DragonSurvivalMod;
import by.jackraidenph.dragonsurvival.client.skinPartSystem.objects.CustomizationObject;
import by.jackraidenph.dragonsurvival.client.skinPartSystem.objects.CustomizationObject.Texture;
import by.jackraidenph.dragonsurvival.client.skinPartSystem.objects.SavedSkinPresets;
import by.jackraidenph.dragonsurvival.client.skinPartSystem.objects.SkinPreset;
import by.jackraidenph.dragonsurvival.misc.DragonLevel;
import by.jackraidenph.dragonsurvival.misc.DragonType;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.IReloadableResourceManager;
import net.minecraft.resources.IResourceManager;
import net.minecraft.resources.IResourceManagerReloadListener;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

import java.io.*;
import java.util.HashMap;

@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD)
public class CustomizationRegistry
{
	public static final String SAVED_FILE_NAME = "saved_customizations.json";
	public static final ResourceLocation CUSTOMIZATION = new ResourceLocation(DragonSurvivalMod.MODID, "customization.json");
	public static final HashMap<DragonType, HashMap<EnumSkinLayer, CustomizationObject.Texture[]>> CUSTOMIZATIONS = new HashMap<>();
	public static SavedSkinPresets savedCustomizations = null;
	private static boolean init = false;
	
	public static File folder;
	public static File savedFile;
	
	@OnlyIn( Dist.CLIENT)
	@SubscribeEvent
	public static void clientStart(FMLClientSetupEvent event){
		folder = new File(Minecraft.getInstance().gameDirectory + "/config/dragon-survival/");
		savedFile = new File(folder + "/" + SAVED_FILE_NAME);
		
		if(!folder.exists()){
			folder.mkdirs();
		}
		
		if(!savedFile.exists()){
			try {
				savedFile.createNewFile();
				Gson gson = new GsonBuilder().setPrettyPrinting().create();
				savedCustomizations = new SavedSkinPresets();
				
				for (DragonType type : DragonType.values()) {
					if (type == DragonType.NONE) continue;
					
					savedCustomizations.skinPresets.computeIfAbsent(type, (b) -> new HashMap<>());
					savedCustomizations.current.computeIfAbsent(type, (b) -> new HashMap<>());
					
					for (int i = 0; i < 9; i++) {
						savedCustomizations.skinPresets.get(type).computeIfAbsent(i, (b) -> new SkinPreset());
					}
					
					for (DragonLevel level : DragonLevel.values()) {
						savedCustomizations.current.get(type).put(level, 0);
					}
				}
				
				FileWriter writer = new FileWriter(savedFile);
				gson.toJson(savedCustomizations, writer);
				writer.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		
		if(savedFile.exists()) {
			try {
				Gson gson = new Gson();
				InputStream in = new FileInputStream(savedFile);
				BufferedReader reader = new BufferedReader(new InputStreamReader(in));
				savedCustomizations = gson.fromJson(reader, SavedSkinPresets.class);
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			}
		}
		
		
		CustomizationRegistry.reload(Minecraft.getInstance().getResourceManager(), CUSTOMIZATION);
		
		if (Minecraft.getInstance().getResourceManager() instanceof IReloadableResourceManager) {
			((IReloadableResourceManager) Minecraft.getInstance().getResourceManager()).registerReloadListener(
					(IResourceManagerReloadListener) manager -> {
						CUSTOMIZATIONS.clear();
						CustomizationRegistry.reload(Minecraft.getInstance().getResourceManager(), CUSTOMIZATION);
					});
		}
	}
	
	protected static void reload(IResourceManager manager, ResourceLocation location){
		try {
			Gson gson = new Gson();
			InputStream in = manager.getResource(location).getInputStream();
			
			BufferedReader reader = new BufferedReader(new InputStreamReader(in));
			CustomizationObject je = gson.fromJson(reader, CustomizationObject.class);
			
			CUSTOMIZATIONS.computeIfAbsent(DragonType.SEA, (type) -> new HashMap<>());
			CUSTOMIZATIONS.computeIfAbsent(DragonType.CAVE, (type) -> new HashMap<>());
			CUSTOMIZATIONS.computeIfAbsent(DragonType.FOREST, (type) -> new HashMap<>());
			
			dragonType(DragonType.SEA, je.sea_dragon);
			dragonType(DragonType.CAVE, je.cave_dragon);
			dragonType(DragonType.FOREST, je.forest_dragon);
			
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private static void dragonType(DragonType type, CustomizationObject.Dragon je)
	{
		if(je != null){
			if(je.layers != null){
				je.layers.forEach((layer, keys) -> {
					for (Texture key : keys) {
						if(key.key == null){
							key.key = key.texture.substring(key.texture.lastIndexOf("/")+1);
							key.key = key.key.substring(0, key.key.lastIndexOf("."));
							
						}
					}
					CUSTOMIZATIONS.get(type).put(layer, keys);
				});
			}
		}
	}
	
}