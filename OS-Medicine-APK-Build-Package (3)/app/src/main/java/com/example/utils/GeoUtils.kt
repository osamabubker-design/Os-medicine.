package com.example.utils

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

object GeoUtils {
    const val PHARMACY_SEARCH_RADIUS_KM = 5.0
    const val DEFAULT_ADDIS_LAT = 9.005401
    const val DEFAULT_ADDIS_LNG = 38.763611

    fun haversineKm(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val r = 6371.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2).pow(2) +
            cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(dLon / 2).pow(2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return r * c
    }

    fun openGoogleMapsDirections(context: Context, lat: Double, lng: Double, label: String) {
        val gmmUri = Uri.parse("google.navigation:q=$lat,$lng")
        val mapIntent = Intent(Intent.ACTION_VIEW, gmmUri).apply {
            setPackage("com.google.android.apps.maps")
        }
        if (mapIntent.resolveActivity(context.packageManager) != null) {
            context.startActivity(mapIntent)
        } else {
            val webUri = Uri.parse(
                "https://www.google.com/maps/dir/?api=1&destination=$lat,$lng&travelmode=driving"
            )
            context.startActivity(Intent(Intent.ACTION_VIEW, webUri))
        }
    }

    fun dialPhone(context: Context, phone: String) {
        val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phone"))
        context.startActivity(intent)
    }

    fun copyToClipboard(context: Context, label: String, text: String) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText(label, text))
    }

    fun isValidEthiopianPhone(phone: String): Boolean {
        val cleaned = phone.replace("\\s+|-".toRegex(), "")
        return cleaned.matches("^(\\+?251|0)?[79]\\d{8}$".toRegex())
    }
}
