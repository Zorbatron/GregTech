package gregtech.integration.theoneprobe.provider.AdvancedProvider;

public class InfoPair {

    private final TOPType key;
    private final String value;

    public InfoPair(TOPType key, String value) {
        this.key = key;
        this.value = value;
    }

    public TOPType getKey() {
        return key;
    }

    public String getValue() {
        return value;
    }
}
