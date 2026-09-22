package com.aperture.camera.data.repository

import android.content.ContentResolver
import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.location.Location
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.exifinterface.media.ExifInterface
import com.aperture.camera.data.model.MediaItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MediaStoreRepository(private val context: Context) {

    private val contentResolver: ContentResolver get() = context.contentResolver

    private val dateFormat = SimpleDateFormat("yyyyMMdd_HHmmss_SSS", Locale.US)
    private val exifDateFormat = SimpleDateFormat("yyyy:MM:dd HH:mm:ss", Locale.US)

    suspend fun savePhoto(
        jpegBytes: ByteArray,
        focalLength: Float? = null,
        aperture: Float? = null,
        iso: Int? = null,
        exposureTimeNs: Long? = null,
        location: Location? = null,
        isRaw: Boolean = false
    ): Uri? = withContext(Dispatchers.IO) {
        val timestamp = dateFormat.format(Date())
        val extension = if (isRaw) "dng" else "jpg"
        val mimeType = if (isRaw) "image/x-adobe-dng" else "image/jpeg"
        val displayName = "IMG_${timestamp}.$extension"
        val relativePath = "${Environment.DIRECTORY_DCIM}/Camera"

        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, displayName)
            put(MediaStore.Images.Media.MIME_TYPE, mimeType)
            put(MediaStore.Images.Media.DATE_ADDED, System.currentTimeMillis() / 1000)
            put(MediaStore.Images.Media.DATE_TAKEN, System.currentTimeMillis())
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Images.Media.RELATIVE_PATH, relativePath)
                put(MediaStore.Images.Media.IS_PENDING, 1)
            }
        }

        val collectionUri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        } else {
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        }

        val uri = contentResolver.insert(collectionUri, values) ?: return@withContext null

        try {
            contentResolver.openOutputStream(uri)?.use { outputStream ->
                outputStream.write(jpegBytes)
                outputStream.flush()
            }

            // Write EXIF Metadata
            if (!isRaw) {
                try {
                    contentResolver.openFileDescriptor(uri, "rw")?.use { pfd ->
                        val exif = ExifInterface(pfd.fileDescriptor)
                        exif.setAttribute(ExifInterface.TAG_MAKE, Build.MANUFACTURER)
                        exif.setAttribute(ExifInterface.TAG_MODEL, Build.MODEL)
                        exif.setAttribute(ExifInterface.TAG_SOFTWARE, "Aperture Camera")
                        exif.setAttribute(ExifInterface.TAG_DATETIME, exifDateFormat.format(Date()))

                        focalLength?.let {
                            exif.setAttribute(ExifInterface.TAG_FOCAL_LENGTH, "$it/1")
                        }
                        aperture?.let {
                            exif.setAttribute(ExifInterface.TAG_F_NUMBER, "$it")
                        }
                        iso?.let {
                            exif.setAttribute(ExifInterface.TAG_PHOTOGRAPHIC_SENSITIVITY, "$it")
                        }
                        exposureTimeNs?.let {
                            val sec = it / 1_000_000_000.0
                            exif.setAttribute(ExifInterface.TAG_EXPOSURE_TIME, "%.6f".format(Locale.US, sec))
                        }
                        location?.let {
                            try {
                                exif.setGpsInfo(it)
                                exif.setLatLong(it.latitude, it.longitude)
                                if (it.hasAltitude()) {
                                    exif.setAltitude(it.altitude)
                                }
                            } catch (_: Exception) { }
                        }

                        exif.saveAttributes()
                    }
                } catch (_: Exception) {
                    // Ignore exif write errors on some restricted devices
                }
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                values.clear()
                values.put(MediaStore.Images.Media.IS_PENDING, 0)
                contentResolver.update(uri, values, null, null)
            }

            uri
        } catch (e: Exception) {
            e.printStackTrace()
            contentResolver.delete(uri, null, null)
            null
        }
    }

    suspend fun savePdfDocument(
        pdfBytes: ByteArray,
        displayName: String = "DOC_${dateFormat.format(Date())}.pdf"
    ): Uri? = withContext(Dispatchers.IO) {
        val relativePath = "${Environment.DIRECTORY_DOCUMENTS}/ApertureScanner"
        val values = ContentValues().apply {
            put(MediaStore.Files.FileColumns.DISPLAY_NAME, displayName)
            put(MediaStore.Files.FileColumns.MIME_TYPE, "application/pdf")
            put(MediaStore.Files.FileColumns.DATE_ADDED, System.currentTimeMillis() / 1000)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Files.FileColumns.RELATIVE_PATH, relativePath)
                put(MediaStore.Files.FileColumns.IS_PENDING, 1)
            }
        }

        val collectionUri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Files.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        } else {
            MediaStore.Files.getContentUri("external")
        }

        val uri = contentResolver.insert(collectionUri, values) ?: return@withContext null

        try {
            contentResolver.openOutputStream(uri)?.use { outputStream ->
                outputStream.write(pdfBytes)
                outputStream.flush()
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                values.clear()
                values.put(MediaStore.Files.FileColumns.IS_PENDING, 0)
                contentResolver.update(uri, values, null, null)
            }
            uri
        } catch (e: Exception) {
            e.printStackTrace()
            contentResolver.delete(uri, null, null)
            null
        }
    }

    suspend fun saveDocumentImage(
        jpegBytes: ByteArray,
        displayName: String = "SCAN_${dateFormat.format(Date())}.jpg"
    ): Uri? = withContext(Dispatchers.IO) {
        val relativePath = "${Environment.DIRECTORY_DCIM}/Camera"
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, displayName)
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            put(MediaStore.Images.Media.DATE_ADDED, System.currentTimeMillis() / 1000)
            put(MediaStore.Images.Media.DATE_TAKEN, System.currentTimeMillis())
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Images.Media.RELATIVE_PATH, relativePath)
                put(MediaStore.Images.Media.IS_PENDING, 1)
            }
        }

        val collectionUri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        } else {
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        }

        val uri = contentResolver.insert(collectionUri, values) ?: return@withContext null

        try {
            contentResolver.openOutputStream(uri)?.use { outputStream ->
                outputStream.write(jpegBytes)
                outputStream.flush()
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                values.clear()
                values.put(MediaStore.Images.Media.IS_PENDING, 0)
                contentResolver.update(uri, values, null, null)
            }
            uri
        } catch (e: Exception) {
            e.printStackTrace()
            contentResolver.delete(uri, null, null)
            null
        }
    }

    fun createVideoContentValues(): ContentValues {
        val timestamp = dateFormat.format(Date())
        val displayName = "VID_${timestamp}.mp4"
        val relativePath = "${Environment.DIRECTORY_DCIM}/Camera"

        return ContentValues().apply {
            put(MediaStore.Video.Media.DISPLAY_NAME, displayName)
            put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
            put(MediaStore.Video.Media.DATE_ADDED, System.currentTimeMillis() / 1000)
            put(MediaStore.Video.Media.DATE_TAKEN, System.currentTimeMillis())
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Video.Media.RELATIVE_PATH, relativePath)
            }
        }
    }

    suspend fun getLatestMediaItem(): MediaItem? = withContext(Dispatchers.IO) {
        val items = getRecentMediaItems(limit = 1)
        items.firstOrNull()
    }

    suspend fun getRecentMediaItems(limit: Int = 30): List<MediaItem> = withContext(Dispatchers.IO) {
        val mediaList = mutableListOf<MediaItem>()

        // 1. Query Images
        val imageProjection = arrayOf(
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.DISPLAY_NAME,
            MediaStore.Images.Media.DATE_ADDED,
            MediaStore.Images.Media.SIZE,
            MediaStore.Images.Media.WIDTH,
            MediaStore.Images.Media.HEIGHT
        )

        val imageSortOrder = "${MediaStore.Images.Media.DATE_ADDED} DESC"
        val imageUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI

        try {
            contentResolver.query(imageUri, imageProjection, null, null, imageSortOrder)?.use { cursor ->
                val idCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
                val nameCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME)
                val dateCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_ADDED)
                val sizeCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.SIZE)
                val widthCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.WIDTH)
                val heightCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.HEIGHT)

                var count = 0
                while (cursor.moveToNext() && count < limit) {
                    val id = cursor.getLong(idCol)
                    val name = cursor.getString(nameCol) ?: "Image_$id.jpg"
                    val dateAdded = cursor.getLong(dateCol)
                    val size = cursor.getLong(sizeCol)
                    val width = cursor.getInt(widthCol)
                    val height = cursor.getInt(heightCol)
                    val contentUri = ContentUris.withAppendedId(imageUri, id)

                    mediaList.add(
                        MediaItem(
                            uri = contentUri,
                            name = name,
                            isVideo = false,
                            dateAdded = dateAdded,
                            sizeBytes = size,
                            width = width,
                            height = height
                        )
                    )
                    count++
                }
            }
        } catch (_: Exception) { }

        // 2. Query Videos
        val videoProjection = arrayOf(
            MediaStore.Video.Media._ID,
            MediaStore.Video.Media.DISPLAY_NAME,
            MediaStore.Video.Media.DATE_ADDED,
            MediaStore.Video.Media.SIZE,
            MediaStore.Video.Media.WIDTH,
            MediaStore.Video.Media.HEIGHT,
            MediaStore.Video.Media.DURATION
        )

        val videoSortOrder = "${MediaStore.Video.Media.DATE_ADDED} DESC"
        val videoUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI

        try {
            contentResolver.query(videoUri, videoProjection, null, null, videoSortOrder)?.use { cursor ->
                val idCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
                val nameCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DISPLAY_NAME)
                val dateCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATE_ADDED)
                val sizeCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.SIZE)
                val widthCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.WIDTH)
                val heightCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.HEIGHT)
                val durationCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DURATION)

                var count = 0
                while (cursor.moveToNext() && count < limit) {
                    val id = cursor.getLong(idCol)
                    val name = cursor.getString(nameCol) ?: "Video_$id.mp4"
                    val dateAdded = cursor.getLong(dateCol)
                    val size = cursor.getLong(sizeCol)
                    val width = cursor.getInt(widthCol)
                    val height = cursor.getInt(heightCol)
                    val durationMs = cursor.getLong(durationCol)
                    val contentUri = ContentUris.withAppendedId(videoUri, id)

                    mediaList.add(
                        MediaItem(
                            uri = contentUri,
                            name = name,
                            isVideo = true,
                            dateAdded = dateAdded,
                            sizeBytes = size,
                            width = width,
                            height = height,
                            durationMs = durationMs
                        )
                    )
                    count++
                }
            }
        } catch (_: Exception) { }

        // Sort combined media chronologically
        mediaList.sortedByDescending { it.dateAdded }
    }

    suspend fun deleteMediaItem(uri: Uri): Boolean = withContext(Dispatchers.IO) {
        try {
            val rows = contentResolver.delete(uri, null, null)
            rows > 0
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}
