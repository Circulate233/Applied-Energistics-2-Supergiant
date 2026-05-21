package appeng.core.localization;

public enum HeiText implements LocalizationEnum {
    ChargerRequiredPower,
    Consumed,
    FlowingFluidName,
    EntropyManipulatorHeat,
    EntropyManipulatorCool,
    RightClick,
    ShiftRightClick,
    TransformCategory,
    Explosion,
    SubmergeIn;

    @Override
    public String getTranslationKey() {
        return "ae2.hei." + name();
    }
}
