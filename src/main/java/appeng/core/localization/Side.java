package appeng.core.localization;

/**
 * Used to describe to the user on which sides of the block (when looking at it from the front), something can be
 * automated.
 */
public enum Side implements LocalizationEnum {

    North,
    South,
    East,
    West,
    Up,
    Down;

    @Override
    public String getTranslationKey() {
        return "ae2.side." + name();
    }
}
