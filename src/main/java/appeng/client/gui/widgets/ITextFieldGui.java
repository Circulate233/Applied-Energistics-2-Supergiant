package appeng.client.gui.widgets;

import net.minecraft.client.gui.GuiTextField;

import java.util.Collection;

public interface ITextFieldGui {

    Collection<? extends GuiTextField> getTextFields();
}
