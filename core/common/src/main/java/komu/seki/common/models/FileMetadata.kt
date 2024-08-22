package komu.seki.common.models

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable

@Parcelize
@Serializable
data class FileMetadata(
    val fileName: String,
    val fileType: String,
    val fileSize: Long,
    val uri: String,
    val creationDate: String?,
    val modificationDate: String?,
) : Parcelable
