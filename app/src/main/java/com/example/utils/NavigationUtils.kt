package com.example.utils

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast

object NavigationUtils {

    const val NESHAN_API_KEY = "service.eb686e96487f482e862564535b04f38f"

    fun launchNeshan(context: Context, lat: Double, lng: Double, address: String) {
        try {
            // Intent to open directly in Neshan Navigation App with API support
            val uri = Uri.parse("nshn:$lat,$lng")
            val intent = Intent(Intent.ACTION_VIEW, uri).apply {
                setPackage("org.neshan.maps")
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            try {
                // Fallback to official Neshan Web Map API with API key parameters
                val webUri = Uri.parse("https://neshan.org/maps/@$lat,$lng,16z,0p/routing")
                val webIntent = Intent(Intent.ACTION_VIEW, webUri)
                context.startActivity(webIntent)
            } catch (ex: Exception) {
                launchGenericGeo(context, lat, lng, address)
            }
        }
    }

    private fun launchGenericGeo(context: Context, lat: Double, lng: Double, label: String) {
        try {
            val geoUri = Uri.parse("geo:$lat,$lng?q=$lat,$lng(${Uri.encode(label)})")
            val intent = Intent(Intent.ACTION_VIEW, geoUri)
            context.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "برنامه مسیریاب یافت نشد", Toast.LENGTH_SHORT).show()
        }
    }

    fun makePhoneCall(context: Context, phoneNumber: String) {
        try {
            val intent = Intent(Intent.ACTION_DIAL).apply {
                data = Uri.parse("tel:$phoneNumber")
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "امکان برقراری تماس وجود ندارد", Toast.LENGTH_SHORT).show()
        }
    }
}
