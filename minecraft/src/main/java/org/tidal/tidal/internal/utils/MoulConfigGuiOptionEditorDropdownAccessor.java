package cc.tidal.tidal.internal.utils;

//? moul_compat {
import cc.tidal.tidal.relocator.annotations.MoulConfig;

@MoulConfig
public interface MoulConfigGuiOptionEditorDropdownAccessor {

    String[] tidal$values();

    boolean tidal$useOrdinal();

    Enum<?>[] tidal$constants();

}
//? }
