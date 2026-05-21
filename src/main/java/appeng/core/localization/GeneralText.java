package appeng.core.localization;

public enum GeneralText implements LocalizationEnum {
    ClientReadOnly("client_read_only");

    private final String translationKeySuffix;

    GeneralText(String translationKeySuffix) {
        this.translationKeySuffix = translationKeySuffix;
    }

    @Override
    public String getTranslationKey() {
        return "ae2." + this.translationKeySuffix;
    }
}
