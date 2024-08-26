package gregtech.integration.theoneprobe.provider.AdvancedProvider;

import java.util.List;
import java.util.function.Supplier;

public interface IAdvancedDataProvider {

    List<InfoPair> provideInformation(Supplier<Boolean> isPlayerSneaking);
}
