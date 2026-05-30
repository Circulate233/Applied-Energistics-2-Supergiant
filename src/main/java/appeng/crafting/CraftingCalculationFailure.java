package appeng.crafting;

import appeng.core.localization.PlayerMessages;

import net.minecraft.util.text.ITextComponent;

public class CraftingCalculationFailure extends RuntimeException {
    private final PlayerMessages messageKey;

    public CraftingCalculationFailure(PlayerMessages messageKey) {
        super(messageKey.getTranslationKey());
        this.messageKey = messageKey;
    }

    public PlayerMessages getMessageKey() {
        return this.messageKey;
    }

    public ITextComponent getLocalizedMessageComponent() {
        return this.messageKey.text();
    }
}
