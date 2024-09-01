package gregtech.integration.theoneprobe.provider.AdvancedProvider;

import java.util.List;

public interface IAdvancedDataProvider {

    List<InfoPair> provideInformation(boolean isPlayerSneaking);
}
