package komu.seki.data.database

import android.net.wifi.WifiSsid
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class Network (
    @PrimaryKey val ssid: String,
)