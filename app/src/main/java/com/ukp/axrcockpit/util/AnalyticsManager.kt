package com.ukp.axrcockpit.util

import com.ukp.axrcockpit.util.Logger

import com.google.firebase.analytics.FirebaseAnalytics
import android.content.Context
import android.os.Bundle

object AnalyticsManager {
    private val logger = Logger.get(AnalyticsManager::class.java)


    private var firebaseAnalytics: FirebaseAnalytics? = null

    fun initialize(context: Context) {
        if (firebaseAnalytics == null) {
            firebaseAnalytics = FirebaseAnalytics.getInstance(context.applicationContext)
            logger.d("Firebase Analytics initialized")
        }
    }

    fun logEvent(name: String, category: String, label: String, endpoint: String) {
        logger.d("Logging Event: $name, category=$category, label=$label, endpoint=$endpoint")
        val bundle = Bundle().apply {
            putString("category", category)
            putString("label", label)
            putString("endpoint", endpoint)
        }
        firebaseAnalytics?.logEvent(name, bundle)
    }
}
