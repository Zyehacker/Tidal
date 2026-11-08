package cc.tidal.tidal.internal.utils;

//? moul_compat {
import io.github.notenoughupdates.moulconfig.Config;
import cc.tidal.tidal.relocator.annotations.MoulConfig;

@MoulConfig
public interface MoulConfigProcessorAccessor<T extends Config> {

    T tidal$getConfig();

}
//? }