plugins {
    id("net.fabricmc.fabric-loom")
    `tidal-bridge` // creates the modImplementation and friends configurations
    `tidal-fabric`
}

dependencies {
    if (versionedCatalog.has("modmenu"))
    implementation(versionedCatalog["modmenu"])
}