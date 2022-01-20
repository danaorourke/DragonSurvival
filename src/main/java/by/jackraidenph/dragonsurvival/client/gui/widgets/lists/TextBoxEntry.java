package by.jackraidenph.dragonsurvival.client.gui.widgets.lists;

import com.google.common.collect.ImmutableList;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.network.chat.TextComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.gui.widget.ExtendedButton;

import java.util.List;

@OnlyIn( Dist.CLIENT )
public class TextBoxEntry extends OptionListEntry
{
	public final AbstractWidget widget;
	public AbstractWidget removeButton;
	
	private CategoryEntry category;
	
	public TextBoxEntry(OptionsList optionsList, AbstractWidget widget, CategoryEntry categoryEntry)
	{
		this.widget = widget;
		this.category = categoryEntry;
		
		removeButton = new ExtendedButton(optionsList.getScrollbarPosition() - 32 - 25, 1, 50, 20, new TextComponent("Remove"), (btn) -> {
			for (OptionListEntry child : optionsList.children()) {
				if (child.children().contains(widget)) {
					optionsList.removeEntry(child);
					optionsList.scroll(-child.getHeight());
					return;
				}
			}
		});
	}
	
	public void render(PoseStack pPoseStack , int pIndex, int pTop, int pLeft, int pWidth, int pHeight, int pMouseX, int pMouseY, boolean pIsMouseOver, float pPartialTicks)
	{
		widget.y = pTop;
		widget.visible = getHeight() != 0 && visible;
		widget.render(pPoseStack , pMouseX, pMouseY, pPartialTicks);
		
		removeButton.y = pTop;
		removeButton.visible = getHeight() != 0 && visible;
		removeButton.render(pPoseStack , pMouseX, pMouseY, pPartialTicks);
	}
	
	
	public List<? extends GuiEventListener> children() {
		return ImmutableList.of(this.widget, this.removeButton);
	}
	
	@Override
	public int getHeight()
	{
		if (category != null) {
			CategoryEntry entry = category.parent;
			while (entry != null) {
				if (!entry.enabled) {
					return 0;
				} else {
					entry = entry.parent;
				}
			}
		}
		
		return category == null || category.enabled ? 20 : 0;
	}
	
	@Override
	public List<? extends NarratableEntry> narratables()
	{
		return null;
	}
}