package gregtech.integration.theoneprobe.provider.AdvancedProvider;

import gregtech.api.GTValues;
import gregtech.api.capability.GregtechCapabilities;
import gregtech.integration.theoneprobe.provider.CapabilityInfoProvider;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraftforge.common.capabilities.Capability;

import mcjty.theoneprobe.api.IProbeHitData;
import mcjty.theoneprobe.api.IProbeInfo;
import org.jetbrains.annotations.NotNull;

public class AdvancedInfoProvider extends CapabilityInfoProvider<IAdvancedDataProvider> {

    @Override
    public String getID() {
        return GTValues.MODID + ":advanced_info_provider";
    }

    @Override
    protected @NotNull Capability<IAdvancedDataProvider> getCapability() {
        return GregtechCapabilities.ADVANCED_DATA_PROVIDER;
    }

    @Override
    protected void addProbeInfo(IAdvancedDataProvider capability, IProbeInfo probeInfo, EntityPlayer player,
                                TileEntity tileEntity, IProbeHitData data) {
        capability.provideInformation(player.isSneaking()).forEach(infoPair -> pairParser(probeInfo, infoPair));
    }

    private void pairParser(IProbeInfo probeInfo, InfoPair infoPair) {
        switch (infoPair.getKey()) {
            case TEXT:
                probeInfo.text(infoPair.getValue());
                break;

            case PROGRESS:
                // I don't know of a good way to do this... At all :despair:
                // In fact, I don't think it's possible to do this without just passing the IProbeInfo to the TE
                break;
        }
    }
}