package appeng.client.gui.me.items;

import appeng.client.Point;
import appeng.client.gui.ICompositeWidget;
import appeng.client.gui.Icon;
import appeng.client.gui.Rect2i;
import appeng.client.gui.WidgetContainer;
import appeng.container.me.items.ContainerPatternEncodingTerm;
import net.minecraft.util.text.ITextComponent;

abstract class EncodingModePanel implements ICompositeWidget {
    protected final GuiPatternEncodingTerm screen;
    protected final ContainerPatternEncodingTerm container;
    protected final WidgetContainer widgets;
    protected Point position = Point.ZERO;
    protected int width;
    protected int height;
    protected boolean visible;

    EncodingModePanel(GuiPatternEncodingTerm screen, WidgetContainer widgets) {
        this.screen = screen;
        this.container = screen.getContainer();
        this.widgets = widgets;
    }

    @Override
    public boolean isVisible() {
        return this.visible;
    }

    public void setVisible(boolean visible) {
        this.visible = visible;
    }

    @Override
    public void setPosition(Point position) {
        this.position = position;
    }

    @Override
    public void setSize(int width, int height) {
        this.width = width;
        this.height = height;
    }

    @Override
    public Rect2i getBounds() {
        return new Rect2i(this.position.x(), this.position.y(), this.width, this.height);
    }

    abstract Icon getIcon();

    public abstract ITextComponent getTabTooltip();
}

