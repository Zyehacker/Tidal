// bootstrap nodes < 26.1 are obfuscated so they need remapping
plugins {
    id("net.fabricmc.fabric-loom-remap")
    id("tidal-bootstrap")
}

dependencies {
    mappings(loom.officialMojangMappings())
}
