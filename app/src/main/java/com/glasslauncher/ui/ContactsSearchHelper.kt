package com.glasslauncher.ui

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.ContactsContract
import androidx.core.content.ContextCompat

data class QuickContact(
    val id: Long,
    val name: String,
    val number: String,
    val photoUri: String?
)

object ContactsSearchHelper {
    fun hasPermission(context: Context): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.READ_CONTACTS
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun searchContacts(context: Context, query: String, limit: Int = 4): List<QuickContact> {
        val q = query.trim()
        if (q.length < 2 || !hasPermission(context)) return emptyList()

        val results = mutableListOf<QuickContact>()
        val uri = ContactsContract.CommonDataKinds.Phone.CONTENT_URI
        val projection = arrayOf(
            ContactsContract.CommonDataKinds.Phone.CONTACT_ID,
            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME_PRIMARY,
            ContactsContract.CommonDataKinds.Phone.NUMBER,
            ContactsContract.CommonDataKinds.Phone.PHOTO_THUMBNAIL_URI
        )
        val selection = "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME_PRIMARY} LIKE ?"
        val selectionArgs = arrayOf("%$q%")
        val sortOrder = "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME_PRIMARY} ASC LIMIT $limit"

        try {
            context.contentResolver.query(uri, projection, selection, selectionArgs, sortOrder)?.use { cursor ->
                val idIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.CONTACT_ID)
                val nameIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME_PRIMARY)
                val numberIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                val photoIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.PHOTO_THUMBNAIL_URI)

                val seenNumbers = mutableSetOf<String>()
                while (cursor.moveToNext() && results.size < limit) {
                    val id = cursor.getLong(idIdx)
                    val name = cursor.getString(nameIdx) ?: continue
                    val number = cursor.getString(numberIdx) ?: continue
                    val cleanNum = number.replace(" ", "").replace("-", "")
                    if (seenNumbers.contains(cleanNum)) continue
                    seenNumbers.add(cleanNum)

                    val photo = if (photoIdx >= 0) cursor.getString(photoIdx) else null
                    results.add(QuickContact(id, name, number, photo))
                }
            }
        } catch (_: Exception) {}

        return results
    }

    fun makeCall(context: Context, number: String) {
        val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${Uri.encode(number)}"))
        context.startSafely(intent)
    }

    fun sendMessage(context: Context, number: String) {
        val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:${Uri.encode(number)}"))
        context.startSafely(intent)
    }
}
