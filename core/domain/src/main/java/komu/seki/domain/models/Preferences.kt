package komu.seki.domain.models

data class PreferencesSettings(
    val autoDiscovery: Boolean,
    val imageClipboard: Boolean,
    val storageLocation: String,
)