package by.jackraidenph.dragonsurvival.client.render.entity.dragon;

import by.jackraidenph.dragonsurvival.DragonSurvivalMod;
import by.jackraidenph.dragonsurvival.client.render.ClientDragonRender;
import by.jackraidenph.dragonsurvival.common.entity.DragonEntity;
import by.jackraidenph.dragonsurvival.config.ConfigHandler;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import software.bernie.geckolib3.core.processor.IBone;
import software.bernie.geckolib3.renderers.geo.GeoEntityRenderer;
import software.bernie.geckolib3.renderers.geo.GeoLayerRenderer;

import java.awt.Color;

public class DragonArmorRenderLayer extends GeoLayerRenderer<DragonEntity>
{
	
	private final GeoEntityRenderer<DragonEntity> renderer;
	
	public DragonArmorRenderLayer(GeoEntityRenderer<DragonEntity> entityRendererIn)
	{
		super(entityRendererIn);
		this.renderer = entityRendererIn;
	}
	
	@Override
	public void render(PoseStack stack, MultiBufferSource bufferIn, int packedLightIn, DragonEntity entitylivingbaseIn, float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks, float netHeadYaw, float headPitch)
	{
		if(!ConfigHandler.CLIENT.armorRenderLayer.get()) return;
		
		Player player = entitylivingbaseIn.getPlayer();
		
		if(player.isSpectator()) return;
		
		final IBone neck = ClientDragonRender.dragonArmorModel.getAnimationProcessor().getBone("Neck");
		
		if (neck != null) {
			neck.setHidden(false);
		}
		
		ResourceLocation helmetTexture = new ResourceLocation(DragonSurvivalMod.MODID, constructArmorTexture(player, EquipmentSlot.HEAD));
		ResourceLocation chestPlateTexture = new ResourceLocation(DragonSurvivalMod.MODID, constructArmorTexture(player, EquipmentSlot.CHEST));
		ResourceLocation legsTexture = new ResourceLocation(DragonSurvivalMod.MODID, constructArmorTexture(player, EquipmentSlot.LEGS));
		ResourceLocation bootsTexture = new ResourceLocation(DragonSurvivalMod.MODID, constructArmorTexture(player, EquipmentSlot.FEET));
		
		renderArmorPiece(renderer.helmet, stack, bufferIn, packedLightIn, entitylivingbaseIn, partialTicks, helmetTexture);
		renderArmorPiece(renderer.chestplate, stack, bufferIn, packedLightIn, entitylivingbaseIn, partialTicks, chestPlateTexture);
		renderArmorPiece(renderer.leggings, stack, bufferIn, packedLightIn, entitylivingbaseIn, partialTicks, legsTexture);
		renderArmorPiece(renderer.boots, stack, bufferIn, packedLightIn, entitylivingbaseIn, partialTicks, bootsTexture);
	}
	
	private void renderArmorPiece(ItemStack stack, PoseStack pstack, MultiBufferSource bufferIn, int packedLightIn, DragonEntity entitylivingbaseIn, float partialTicks, ResourceLocation helmetTexture)
	{
		Color armorColor = new Color(1f, 1f, 1f);
		
		if(stack.getItem() instanceof DyeableArmorItem){
			int colorCode = ((DyeableArmorItem)stack.getItem()).getColor(stack);
			armorColor = new Color(colorCode);
		}
		
		ClientDragonRender.dragonModel.setCurrentTexture(helmetTexture);
		ClientDragonRender.dragonArmor.copyPosition(entitylivingbaseIn);
		RenderType type = renderer.getRenderType(entitylivingbaseIn, partialTicks, pstack, bufferIn, null, packedLightIn, helmetTexture);
		VertexConsumer vertexConsumer = bufferIn.getBuffer(type);
		
		((DragonRenderer)renderer).isLayer = true;
		renderer.render(ClientDragonRender.dragonModel.getModel(ClientDragonRender.dragonModel.getModelLocation(null)), entitylivingbaseIn, partialTicks, type, pstack, bufferIn, vertexConsumer, packedLightIn, OverlayTexture.NO_OVERLAY, armorColor.getRed() / 255F, armorColor.getGreen() / 255F, armorColor.getBlue() / 255F, 1F);
		((DragonRenderer)renderer).isLayer = false;
	}
	
	public static String constructArmorTexture(Player playerEntity, EquipmentSlot equipmentSlot) {
		String texture = "textures/armor/";
		Item item = playerEntity.getItemBySlot(equipmentSlot).getItem();
		if (item instanceof ArmorItem) {
			ArmorItem armorItem = (ArmorItem) item;
			ArmorMaterial armorMaterial = armorItem.getMaterial();
			if (armorMaterial.getClass() == ArmorMaterial.class) {
				if (armorMaterial == ArmorMaterials.NETHERITE) {
					texture += "netherite_";
				} else if (armorMaterial == ArmorMaterials.DIAMOND) {
					texture += "diamond_";
				} else if (armorMaterial == ArmorMaterials.IRON) {
					texture += "iron_";
				} else if (armorMaterial == ArmorMaterials.LEATHER) {
					texture += "leather_";
				} else if (armorMaterial == ArmorMaterials.GOLD) {
					texture += "gold_";
				} else if (armorMaterial == ArmorMaterials.CHAIN) {
					texture += "chainmail_";
				} else if (armorMaterial == ArmorMaterials.TURTLE)
					texture += "turtle_";
				else {
					return texture + "empty_armor.png";
				}
				
				texture += "dragon_";
				switch (equipmentSlot) {
					case HEAD:
						texture += "helmet";
						break;
					case CHEST:
						texture += "chestplate";
						break;
					case LEGS:
						texture += "leggings";
						break;
					case FEET:
						texture += "boots";
						break;
				}
				texture += ".png";
				return texture;
			} else {
				int defense = armorItem.getDefense();
				switch (equipmentSlot) {
					case FEET:
						texture += Mth.clamp(defense, 1, 4) + "_dragon_boots";
						break;
					case CHEST:
						texture += Mth.clamp(defense / 2, 1, 4) + "_dragon_chestplate";
						break;
					case HEAD:
						texture += Mth.clamp(defense, 1, 4) + "_dragon_helmet";
						break;
					case LEGS:
						texture += Mth.clamp((int) (defense / 1.5), 1, 4) + "_dragon_leggings";
						break;
				}
				return texture + ".png";
			}
		}
		return texture + "empty_armor.png";
	}
}