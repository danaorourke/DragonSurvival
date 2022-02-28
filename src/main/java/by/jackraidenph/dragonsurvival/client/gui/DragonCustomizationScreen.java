package by.jackraidenph.dragonsurvival.client.gui;

import by.jackraidenph.dragonsurvival.DragonSurvivalMod;
import by.jackraidenph.dragonsurvival.client.gui.widgets.CustomizationConfirmation;
import by.jackraidenph.dragonsurvival.client.gui.widgets.DragonUIRenderComponent;
import by.jackraidenph.dragonsurvival.client.gui.widgets.buttons.ColorSelectorButton;
import by.jackraidenph.dragonsurvival.client.gui.widgets.buttons.CustomizationSlotButton;
import by.jackraidenph.dragonsurvival.client.gui.widgets.buttons.DropDownButton;
import by.jackraidenph.dragonsurvival.client.gui.widgets.buttons.HelpButton;
import by.jackraidenph.dragonsurvival.client.gui.widgets.buttons.dropdown.ColoredDropdownValueEntry;
import by.jackraidenph.dragonsurvival.client.gui.widgets.buttons.dropdown.DropdownEntry;
import by.jackraidenph.dragonsurvival.client.handlers.ClientEvents;
import by.jackraidenph.dragonsurvival.client.handlers.magic.ClientMagicHUDHandler;
import by.jackraidenph.dragonsurvival.client.skinPartSystem.CustomizationRegistry;
import by.jackraidenph.dragonsurvival.client.skinPartSystem.DragonCustomizationHandler;
import by.jackraidenph.dragonsurvival.client.skinPartSystem.EnumSkinLayer;
import by.jackraidenph.dragonsurvival.client.skinPartSystem.objects.SkinPreset;
import by.jackraidenph.dragonsurvival.client.util.FakeClientPlayerUtils;
import by.jackraidenph.dragonsurvival.client.util.TextRenderUtil;
import by.jackraidenph.dragonsurvival.common.capability.DragonStateHandler;
import by.jackraidenph.dragonsurvival.common.capability.provider.DragonStateProvider;
import by.jackraidenph.dragonsurvival.common.capability.subcapabilities.SkinCap;
import by.jackraidenph.dragonsurvival.common.util.DragonUtils;
import by.jackraidenph.dragonsurvival.config.ConfigHandler;
import by.jackraidenph.dragonsurvival.misc.DragonLevel;
import by.jackraidenph.dragonsurvival.misc.DragonType;
import by.jackraidenph.dragonsurvival.network.NetworkHandler;
import by.jackraidenph.dragonsurvival.network.RequestClientData;
import by.jackraidenph.dragonsurvival.network.dragon_editor.SyncPlayerSkinPreset;
import by.jackraidenph.dragonsurvival.network.entity.player.SynchronizeDragonCap;
import by.jackraidenph.dragonsurvival.network.flight.SyncSpinStatus;
import by.jackraidenph.dragonsurvival.network.status.SyncAltarCooldown;
import by.jackraidenph.dragonsurvival.util.Functions;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.IGuiEventListener;
import net.minecraft.client.gui.IRenderable;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.button.Button;
import net.minecraft.client.gui.widget.button.CheckboxButton;
import net.minecraft.item.DyeColor;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvents;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraftforge.fml.client.gui.GuiUtils;
import net.minecraftforge.fml.client.gui.widget.ExtendedButton;
import org.apache.commons.lang3.text.WordUtils;

import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Locale;
import java.util.stream.Collectors;

public class DragonCustomizationScreen extends Screen
{
	public DragonCustomizationScreen(Screen source)
	{
		this(source, DragonType.NONE);
	}
	
	public DragonCustomizationScreen(Screen source, DragonType type)
	{
		super(new TranslationTextComponent("ds.gui.customization"));
		this.source = source;
		this.type = type;
	}
	
	private int guiLeft;
	private int guiTop;
	
	private Screen source;
	
	public DragonLevel level = DragonLevel.ADULT;
	public DragonType type;
	
	public SkinPreset preset = new SkinPreset();
	
	//TODO Update sliders when you load a saved preset
	//TODO Cleanup skin key and hue saving to support the possibility of brightness and saturation
	
	public boolean confirmation = false;
	
	private String[] animations = {"sit", "idle", "fly", "swim_fast", "run"};
	private int curAnimation = 0;
	
	public int currentSelected;
	private int lastSelected;
	
	private boolean hasInit = false;
	
	private DragonUIRenderComponent dragonRender;
	public DragonStateHandler handler = new DragonStateHandler();
	
	private CheckboxButton autoSaveButton;
	
	public void update()
	{
		handler.getSkin().skinPreset = preset;
		handler.setSize(level.size);
		handler.setHasWings(true);
		
		if (type != DragonType.NONE) {
			handler.setType(type);
		}
		
		if (currentSelected != lastSelected) {
			preset = CustomizationRegistry.savedCustomizations.skinPresets.get(handler.getType()).get(currentSelected);
			handler.getSkin().skinPreset = preset;
		}
		
		lastSelected = currentSelected;
		
		children.removeIf((s) -> s instanceof DragonUIRenderComponent);
		
		float yRot = -3, xRot = -5, zoom = 0;
		if(dragonRender != null){
			yRot = dragonRender.yRot;
			xRot = dragonRender.xRot;
			zoom = dragonRender.zoom;
		}
		
		dragonRender = new DragonUIRenderComponent(this, width / 2 - 70, guiTop, 140, 125, () -> FakeClientPlayerUtils.getFakeDragon(0, handler));
		dragonRender.xRot = xRot;
		dragonRender.yRot = yRot;
		dragonRender.zoom = zoom;
		
		children.add(dragonRender);
	}
	
	@Override
	public void init()
	{
		super.init();
		
		this.guiLeft = (this.width - 256) / 2;
		this.guiTop = (this.height - 120) / 2;
		
		
		float yRot = -3, xRot = -5, zoom = 0;
		if(dragonRender != null){
			yRot = dragonRender.yRot;
			xRot = dragonRender.xRot;
			zoom = dragonRender.zoom;
		}
		
		dragonRender = new DragonUIRenderComponent(this, width / 2 - 70, guiTop, 140, 125, () -> FakeClientPlayerUtils.getFakeDragon(0, handler));
		dragonRender.xRot = xRot;
		dragonRender.yRot = yRot;
		dragonRender.zoom = zoom;
	
		children.add(dragonRender);
		
		DragonStateHandler localHandler = DragonStateProvider.getCap(getMinecraft().player).orElse(null);
		
		if (!hasInit) {
			level = localHandler.getLevel();
			dragonRender.zoom = level.size;
			
			if (type == DragonType.NONE) {
				type = localHandler.getType();
			}
			
			currentSelected = CustomizationRegistry.savedCustomizations.current.getOrDefault(type, new HashMap<>()).getOrDefault(level, 0);
			preset = CustomizationRegistry.savedCustomizations.skinPresets.getOrDefault(localHandler.getType(), new HashMap<>()).getOrDefault(currentSelected, new SkinPreset());
			handler.getSkin().skinPreset = preset;
			
			this.handler.setHasWings(true);
			this.handler.setType(type);
			hasInit = true;
			update();
		}
		
		addButton(new HelpButton(type, guiLeft - 10, 10, 16, 16, "ds.help.customization"));
		
		addButton(new Button(width / 2 - 180, guiTop - 30, 120, 20, new TranslationTextComponent("ds.level.newborn"), (btn) -> {
			level = DragonLevel.BABY;
			dragonRender.zoom = level.size;
			handler.getSkin().updateLayers.addAll(Arrays.stream(EnumSkinLayer.values()).distinct().collect(Collectors.toList()));
			update();
		})
		{
			@Override
			public void renderButton(MatrixStack stack, int p_230431_2_, int p_230431_3_, float p_230431_4_)
			{
				int j = isHovered || level == DragonLevel.BABY ? 16777215 : 10526880;
				TextRenderUtil.drawCenteredScaledText(stack, x + (width / 2), y + 4, 1.5f, this.getMessage().getString(), j | MathHelper.ceil(this.alpha * 255.0F) << 24);
			}
		});
		addButton(new Button(width / 2 - 60, guiTop - 30, 120, 20, new TranslationTextComponent("ds.level.young"), (btn) -> {
			level = DragonLevel.YOUNG;
			dragonRender.zoom = level.size;
			handler.getSkin().updateLayers.addAll(Arrays.stream(EnumSkinLayer.values()).distinct().collect(Collectors.toList()));
			update();
		})
		{
			@Override
			public void renderButton(MatrixStack stack, int p_230431_2_, int p_230431_3_, float p_230431_4_)
			{
				int j = isHovered || level == DragonLevel.YOUNG ? 16777215 : 10526880;
				TextRenderUtil.drawCenteredScaledText(stack, x + (width / 2), y + 4, 1.5f, this.getMessage().getString(), j | MathHelper.ceil(this.alpha * 255.0F) << 24);
			}
		});
		addButton(new Button(width / 2 + 60, guiTop - 30, 120, 20, new TranslationTextComponent("ds.level.adult"), (btn) -> {
			level = DragonLevel.ADULT;
			dragonRender.zoom = level.size;
			handler.getSkin().updateLayers.addAll(Arrays.stream(EnumSkinLayer.values()).distinct().collect(Collectors.toList()));
			update();
		})
		{
			@Override
			public void renderButton(MatrixStack stack, int p_230431_2_, int p_230431_3_, float p_230431_4_)
			{
				int j = isHovered || level == DragonLevel.ADULT ? 16777215 : 10526880;
				TextRenderUtil.drawCenteredScaledText(stack, x + (width / 2), y + 4, 1.5f, this.getMessage().getString(), j | MathHelper.ceil(this.alpha * 255.0F) << 24);
			}
		});
		
		int maxWidth = -1;
		
		for (EnumSkinLayer layers : EnumSkinLayer.values()) {
			String name = layers.name().substring(0, 1).toUpperCase(Locale.ROOT) + layers.name().toLowerCase().substring(1).replace("_", " ");
			maxWidth = (int)Math.max(maxWidth, font.width(name) * 1.45F);
		}
		
		int i = 0;
		for (EnumSkinLayer layers : EnumSkinLayer.values()) {
			ArrayList<String> valueList = DragonCustomizationHandler.getKeys(type, layers);
			
			if(layers != EnumSkinLayer.BASE){
				valueList.add(0, SkinCap.defaultSkinValue);
			}
			
			String[] values = valueList.toArray(new String[0]);
			String curValue = preset.skinAges.get(level).layerSettings.get(layers).selectedSkin;
			DropDownButton btn = new DropDownButton(i < 5 ? width / 2 - 100 - 100 : width / 2 + 70, guiTop + 10 + ((i >= 5 ? (i - 5) * 30 : i * 30)), 100, 15, curValue, values, (s) -> {
				preset.skinAges.get(level).layerSettings.get(layers).selectedSkin = s;
				handler.getSkin().updateLayers.add(layers);
				update();
			}){
				@Override
				public DropdownEntry createEntry(int pos, String val)
				{
					return new ColoredDropdownValueEntry(this, pos, val, setter);
				}
				
				@Override
				public void render(MatrixStack p_230430_1_, int p_230430_2_, int p_230430_3_, float p_230430_4_)
				{
					super.render(p_230430_1_, p_230430_2_, p_230430_3_, p_230430_4_);
					String curValue = preset.skinAges.get(level).layerSettings.get(layers).selectedSkin;
					
					if(curValue != this.current){
						this.current = curValue;
						updateMessage();
					}
					
					ArrayList<String> valueList = DragonCustomizationHandler.getKeys(type, layers);
					
					if(layers != EnumSkinLayer.BASE){
						valueList.add(0, SkinCap.defaultSkinValue);
					}
					
					this.values = valueList.toArray(new String[0]);
					
					this.active = !preset.skinAges.get(level).defaultSkin;
				}
			};
			
			addButton(btn);

			addButton(new ColorSelectorButton(this, layers, btn.x + btn.getWidth() + 2, btn.y, btn.getHeight(), btn.getHeight(), (s) -> {
				preset.skinAges.get(level).layerSettings.get(layers).hue = s.floatValue();
				handler.getSkin().updateLayers.add(layers);
				update();
			}));
			i++;
		}
		
		addButton(new Button(width / 2 + 30, height / 2 + 75 - 7, 15, 15, new TranslationTextComponent(""), (btn) -> {
			curAnimation += 1;
			
			if (curAnimation >= animations.length) {
				curAnimation = 0;
			}
		})
		{
			@Override
			public void renderButton(MatrixStack stack, int p_230431_2_, int p_230431_3_, float p_230431_4_)
			{
				Minecraft.getInstance().getTextureManager().bind(ClientMagicHUDHandler.widgetTextures);
				
				if (isHovered()) {
					blit(stack, x, y, 66 / 2, 222 / 2, 11, 17, 128, 128);
				} else {
					blit(stack, x, y, 44 / 2, 222 / 2, 11, 17, 128, 128);
				}
			}
		});
		
		addButton(new Button(width / 2 - 30 - 15, height / 2 + 75 - 7, 15, 15, new TranslationTextComponent(""), (btn) -> {
			curAnimation -= 1;
			
			if (curAnimation < 0) {
				curAnimation = animations.length - 1;
			}
		})
		{
			@Override
			public void renderButton(MatrixStack stack, int p_230431_2_, int p_230431_3_, float p_230431_4_)
			{
				Minecraft.getInstance().getTextureManager().bind(ClientMagicHUDHandler.widgetTextures);
				
				if (isHovered()) {
					blit(stack, x, y, 22 / 2, 222 / 2, 11, 17, 128, 128);
				} else {
					blit(stack, x, y, 0, 222 / 2, 11, 17, 128, 128);
				}
			}
		});
		
		for (int num = 1; num <= 9; num++) {
			addButton(new CustomizationSlotButton(width / 2 + 195, guiTop + ((num - 1) * 12) + 5 + 20, num, this));
		}
		
		autoSaveButton = addButton(new CheckboxButton(guiLeft - 75, height - 25, 20, 20, new TranslationTextComponent("ds.gui.customization.auto_save"), autoSaveButton == null || autoSaveButton.selected()));
		
		addButton(new CheckboxButton(guiLeft + (width / 2), height - 15, 100, 10, new TranslationTextComponent("ds.gui.customization.wings"), preset.skinAges.get(level).wings){
			final ResourceLocation TEXTURE = new ResourceLocation("textures/gui/checkbox.png");
			
			@Override
			public void renderButton(MatrixStack pMatrixStack, int pMouseX, int pMouseY, float pPartialTicks)
			{
				pMatrixStack.pushPose();
				pMatrixStack.translate(0,0,100);
				Minecraft minecraft = Minecraft.getInstance();
				minecraft.getTextureManager().bind(TEXTURE);
				RenderSystem.enableDepthTest();
				FontRenderer fontrenderer = minecraft.font;
				RenderSystem.color4f(1.0F, 1.0F, 1.0F, this.alpha);
				RenderSystem.enableBlend();
				RenderSystem.defaultBlendFunc();
				RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
				blit(pMatrixStack, this.x, this.y, this.isHovered() || this.isFocused() ? 10.0F : 0.0F, this.selected() ? 10.0F : 0.0F, 10, this.height, 64/2, 64/2);
				this.renderBg(pMatrixStack, minecraft, pMouseX, pMouseY);
				drawString(pMatrixStack, fontrenderer, this.getMessage(), this.x + 14, this.y + (this.height - 8) / 2, 14737632 | MathHelper.ceil(this.alpha * 255.0F) << 24);
				pMatrixStack.popPose();
				this.selected = preset.skinAges.get(level).wings;
			}
			
			@Override
			public void onPress()
			{
				preset.skinAges.get(level).wings = !preset.skinAges.get(level).wings;
			}
		});
		
		addButton(new CheckboxButton(guiLeft + (width / 2), height - 28, 100, 10, new TranslationTextComponent("ds.gui.customization.default_skin"), preset.skinAges.get(level).defaultSkin){
			final ResourceLocation TEXTURE = new ResourceLocation("textures/gui/checkbox.png");
			
			@Override
			public void renderButton(MatrixStack pMatrixStack, int pMouseX, int pMouseY, float pPartialTicks)
			{
				pMatrixStack.pushPose();
				pMatrixStack.translate(0,0,100);
				Minecraft minecraft = Minecraft.getInstance();
				minecraft.getTextureManager().bind(TEXTURE);
				RenderSystem.enableDepthTest();
				FontRenderer fontrenderer = minecraft.font;
				RenderSystem.color4f(1.0F, 1.0F, 1.0F, this.alpha);
				RenderSystem.enableBlend();
				RenderSystem.defaultBlendFunc();
				RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
				blit(pMatrixStack, this.x, this.y, this.isHovered() || this.isFocused() ? 10.0F : 0.0F, this.selected() ? 10.0F : 0.0F, 10, this.height, 64/2, 64/2);
				this.renderBg(pMatrixStack, minecraft, pMouseX, pMouseY);
				drawString(pMatrixStack, fontrenderer, this.getMessage(), this.x + 14, this.y + (this.height - 8) / 2, 14737632 | MathHelper.ceil(this.alpha * 255.0F) << 24);
				pMatrixStack.popPose();
				this.selected = preset.skinAges.get(level).defaultSkin;
			}
			
			@Override
			public void onPress()
			{
				preset.skinAges.get(level).defaultSkin = !preset.skinAges.get(level).defaultSkin;
			}
		});
		
		addButton(new ExtendedButton(width / 2 - 75 - 10, height - 25, 75, 20, new StringTextComponent("Save"), null)
		{
			@Override
			public void renderButton(MatrixStack mStack, int mouseX, int mouseY, float partial)
			{
				super.renderButton(mStack, mouseX, mouseY, partial);
				if (isHovered) {
					GuiUtils.drawHoveringText(mStack, Arrays.asList(new TranslationTextComponent("ds.gui.customization.tooltip.done")), mouseX, mouseY, Minecraft.getInstance().screen.width, Minecraft.getInstance().screen.height, 200, Minecraft.getInstance().font);
				}
			}
			
			@Override
			public void onPress()
			{
				DragonStateProvider.getCap(minecraft.player).ifPresent(cap -> {
					minecraft.player.level.playSound(minecraft.player, minecraft.player.blockPosition(), SoundEvents.ITEM_PICKUP, SoundCategory.PLAYERS, 1, 0.7f);
					
					if (cap.getType() != type && cap.getType() != DragonType.NONE) {
						if (!ConfigHandler.SERVER.saveAllAbilities.get() || !ConfigHandler.SERVER.saveGrowthStage.get()) {
							confirmation = true;
							return;
						}
					}
					if(!confirmation) {
						confirm();
					}
				});
			}
		});
		
		addButton(new ExtendedButton(width / 2 + 10, height - 25, 75, 20, new TranslationTextComponent("ds.gui.customization.back"), null)
		{
			@Override
			public void renderButton(MatrixStack mStack, int mouseX, int mouseY, float partial)
			{
				super.renderButton(mStack, mouseX, mouseY, partial);
				
				if (isHovered) {
					GuiUtils.drawHoveringText(mStack, Arrays.asList(new TranslationTextComponent("ds.gui.customization.tooltip.back")), mouseX, mouseY, Minecraft.getInstance().screen.width, Minecraft.getInstance().screen.height, 200, Minecraft.getInstance().font);
				}
			}
			
			@Override
			public void onPress()
			{
				Minecraft.getInstance().setScreen(source);
			}
		});
		
		addButton(new Button(guiLeft + 256, 9, 19, 19, new TranslationTextComponent(""), (btn) -> {
			preset = new SkinPreset();
			update();
		})
		{
			@Override
			public void renderToolTip(MatrixStack p_230443_1_, int p_230443_2_, int p_230443_3_)
			{
				GuiUtils.drawHoveringText(p_230443_1_, Arrays.asList(new TranslationTextComponent("ds.gui.customization.reset")), p_230443_2_, p_230443_3_, Minecraft.getInstance().screen.width, Minecraft.getInstance().screen.height, 200, Minecraft.getInstance().font);
			}
			
			@Override
			public void renderButton(MatrixStack stack, int p_230431_2_, int p_230431_3_, float p_230431_4_)
			{
				Minecraft.getInstance().getTextureManager().bind(new ResourceLocation(DragonSurvivalMod.MODID, "textures/gui/reset_button.png"));
				blit(stack, x, y, 0, 0, width, height, width, height);
				
				if (this.isHovered()) {
					this.renderToolTip(stack, p_230431_2_, p_230431_3_);
				}
			}
		});
		
		addButton(new Button(guiLeft + 256 + 30, 9, 19, 19, new TranslationTextComponent(""), (btn) -> {
			for (EnumSkinLayer layer : EnumSkinLayer.values()) {
				ArrayList<String> keys = DragonCustomizationHandler.getKeys(FakeClientPlayerUtils.getFakePlayer(0, handler), layer);
				
				if (layer != EnumSkinLayer.BASE) {
					keys.add(SkinCap.defaultSkinValue);
				}
				
				if (keys.size() > 0) {
					preset.skinAges.get(level).layerSettings.get(layer).selectedSkin = keys.get(minecraft.player.level.random.nextInt(keys.size()));
					preset.skinAges.get(level).layerSettings.get(layer).hue = 0.25f + (minecraft.player.level.random.nextFloat() * 0.5f);
					preset.skinAges.get(level).layerSettings.get(layer).saturation = 0.25f + (minecraft.player.level.random.nextFloat() * 0.5f);
					preset.skinAges.get(level).layerSettings.get(layer).brightness = 0.25f + (minecraft.player.level.random.nextFloat() * 0.5f);
					preset.skinAges.get(level).layerSettings.get(layer).modifiedColor = true;
				}
			}
			
			update();
		})
		{
			@Override
			public void renderToolTip(MatrixStack p_230443_1_, int p_230443_2_, int p_230443_3_)
			{
				GuiUtils.drawHoveringText(p_230443_1_, Arrays.asList(new TranslationTextComponent("ds.gui.customization.random")), p_230443_2_, p_230443_3_, Minecraft.getInstance().screen.width, Minecraft.getInstance().screen.height, 200, Minecraft.getInstance().font);
			}
			
			@Override
			public void renderButton(MatrixStack stack, int p_230431_2_, int p_230431_3_, float p_230431_4_)
			{
				Minecraft.getInstance().getTextureManager().bind(new ResourceLocation(DragonSurvivalMod.MODID, "textures/gui/random_icon.png"));
				blit(stack, x, y, 0, 0, width, height, width, height);
				
				if (this.isHovered()) {
					this.renderToolTip(stack, p_230431_2_, p_230431_3_);
				}
			}
		});
		
		children.add(new CustomizationConfirmation(this, width / 2 - 100, height / 2 - (150 / 2), 200, 150));
	}
	
	
	private static ResourceLocation backgroundTexture = new ResourceLocation("textures/block/dirt.png");
	
	@Override
	public void renderBackground(MatrixStack pMatrixStack)
	{
		super.renderBackground(pMatrixStack);
		DragonAltarGUI.renderBorders(backgroundTexture, 0, width, 32, height - 32, width, height);
	}
	
	float tick = 0;
	
	@Override
	public void render(MatrixStack stack, int p_230430_2_, int p_230430_3_, float p_230430_4_)
	{
		if(autoSaveButton.selected()){
			tick += p_230430_4_;
			
			if(tick % Functions.minutesToTicks(1) == 0){
				save();
				tick = 0;
			}
		}
		
		FakeClientPlayerUtils.getFakePlayer(0, handler).animationSupplier = () -> animations[curAnimation];
		
		stack.pushPose();
		stack.translate(0,0,-600);
		this.renderBackground(stack);
		stack.popPose();
		
		TextRenderUtil.drawCenteredScaledText(stack, width / 2, 10, 2f, title.getString(), DyeColor.WHITE.getTextColor());
		
		int i = 0;
		for (EnumSkinLayer layers : EnumSkinLayer.values()) {
			String name = layers.name;
			SkinsScreen.drawNonShadowLineBreak(stack, font, new StringTextComponent(name), (i < 5 ? width / 2 - 100 - 100 : width / 2 + 70) + 50, guiTop + 10 + ((i >= 5 ? (i - 5) * 30 : i * 30)) - 12, DyeColor.WHITE.getTextColor());
			i++;
		}
		
		SkinsScreen.drawNonShadowLineBreak(stack, font, new StringTextComponent(WordUtils.capitalize(animations[curAnimation].replace("_", " "))), width / 2, height / 2 + 72, DyeColor.GRAY.getTextColor());
		
		Minecraft.getInstance().getTextureManager().bind(new ResourceLocation(DragonSurvivalMod.MODID, "textures/gui/save_icon.png"));
		blit(stack,width / 2 + 193, guiTop + 5, 0, 0, 16, 16,16, 16);
		
		super.render(stack, p_230430_2_, p_230430_3_, p_230430_4_);
		
		for(int x = 0; x < this.children.size(); ++x) {
			IGuiEventListener ch = children.get(x);
			if(ch instanceof IRenderable){
				((IRenderable)ch).render(stack, p_230430_2_, p_230430_3_, p_230430_4_);
			}
		}
	}
	
	
	public void confirm()
	{
		DragonStateProvider.getCap(minecraft.player).ifPresent(cap -> {
			minecraft.player.level.playSound(minecraft.player, minecraft.player.blockPosition(), SoundEvents.ITEM_PICKUP, SoundCategory.PLAYERS, 1, 0.7f);
			
			if(cap.getType() != type) {
				Minecraft.getInstance().player.sendMessage(new TranslationTextComponent("ds." + type.name().toLowerCase() + "_dragon_choice"), Minecraft.getInstance().player.getUUID());
				cap.setType(type);
				
				if (!ConfigHandler.SERVER.saveGrowthStage.get() || cap.getSize() == 0) {
					cap.setSize(DragonLevel.BABY.size);
				}
				
				cap.setHasWings(ConfigHandler.SERVER.saveGrowthStage.get() ? cap.hasWings() || ConfigHandler.SERVER.startWithWings.get() : ConfigHandler.SERVER.startWithWings.get());
				cap.setIsHiding(false);
				cap.getMovementData().spinLearned = ConfigHandler.SERVER.saveGrowthStage.get() && cap.getMovementData().spinLearned;
				
				NetworkHandler.CHANNEL.sendToServer(new SyncAltarCooldown(Minecraft.getInstance().player.getId(), Functions.secondsToTicks(ConfigHandler.SERVER.altarUsageCooldown.get())));
				NetworkHandler.CHANNEL.sendToServer(new SynchronizeDragonCap(minecraft.player.getId(), cap.isHiding(), cap.getType(), cap.getSize(), cap.hasWings(), ConfigHandler.SERVER.caveLavaSwimmingTicks.get(), 0));
				NetworkHandler.CHANNEL.sendToServer(new SyncSpinStatus(Minecraft.getInstance().player.getId(), cap.getMovementData().spinAttack, cap.getMovementData().spinCooldown, cap.getMovementData().spinLearned));
				ClientEvents.sendClientData(new RequestClientData(cap.getType(), cap.getLevel()));
				
			}
		});
		
		save();
		
		Minecraft.getInstance().player.closeContainer();
	}
	
	public void save(){
		DragonType type = DragonUtils.getDragonType(minecraft.player);
		NetworkHandler.CHANNEL.sendToServer(new SyncPlayerSkinPreset(minecraft.player.getId(), preset));
		
		CustomizationRegistry.savedCustomizations.skinPresets.computeIfAbsent(type, (t) -> new HashMap<>());
		CustomizationRegistry.savedCustomizations.skinPresets.get(type).put(currentSelected, preset);
		
		try {
			Gson gson = new GsonBuilder().setPrettyPrinting().create();
			FileWriter writer = new FileWriter(CustomizationRegistry.savedFile);
			gson.toJson(CustomizationRegistry.savedCustomizations, writer);
			writer.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	@Override
	public boolean mouseDragged(double pMouseX, double pMouseY, int pButton, double pDragX, double pDragY)
	{
		if(!super.mouseDragged(pMouseX, pMouseY, pButton, pDragX, pDragY)){
			if(dragonRender != null && dragonRender.isMouseOver(pMouseX, pMouseY)){
				return dragonRender.mouseDragged(pMouseX, pMouseY, pButton, pDragX, pDragY);
			}
		}
		
		return super.mouseDragged(pMouseX, pMouseY, pButton, pDragX, pDragY);
	}
}