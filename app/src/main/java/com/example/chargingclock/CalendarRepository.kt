package com.example.chargingclock

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class CalendarRepository(private val context: Context) {

    suspend fun getNextEvent(): String? {
        return withContext(Dispatchers.IO) {
            try {
                val projection = arrayOf("title", "dtstart")
                val now = System.currentTimeMillis()
                val selection = "dtstart >= ?"
                val selectionArgs = arrayOf(now.toString())
                val sortOrder = "dtstart ASC LIMIT 1"

                // ВАЖНО: Проверку разрешений нужно делать в Activity перед вызовом этого метода
                val cursor = context.contentResolver.query(
                    Uri.parse("content://com.android.calendar/events"),
                    projection, selection, selectionArgs, sortOrder
                )

                if (cursor != null && cursor.moveToFirst()) {
                    val titleIndex = cursor.getColumnIndex("title")
                    val timeIndex = cursor.getColumnIndex("dtstart")
                    val title = if (titleIndex >= 0) cursor.getString(titleIndex) else "Event"
                    val time = if (timeIndex >= 0) cursor.getLong(timeIndex) else 0L

                    cursor.close()

                    val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
                    val timeStr = sdf.format(Date(time))
                    return@withContext "$timeStr $title"
                } else {
                    return@withContext null
                }
            } catch (e: Exception) {
                e.printStackTrace()
                return@withContext null
            }
        }
    }
}