package com.ajuworks.worklog.ads

import android.app.Activity
import android.util.Log
import com.ajuworks.worklog.BuildConfig
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback

/**
 * A rare full-screen ad.
 *
 * Hard rules encoded here:
 *  - It is only ever considered after the user has *browsed history*
 *    [SHOW_EVERY_N_HISTORY_VIEWS] times.
 *  - It is never attached to clocking in, clocking out, or breaks. Those are
 *    the reason the app exists and are used every single day; interrupting
 *    them would be the fastest way to get the app deleted.
 *  - If nothing is loaded, it is skipped silently. It never delays navigation.
 */
object InterstitialAds {

    private const val TAG = "InterstitialAds"
    const val SHOW_EVERY_N_HISTORY_VIEWS = 12

    private var ad: InterstitialAd? = null
    private var loading = false

    fun shouldShow(historyViewCount: Int): Boolean =
        historyViewCount > 0 && historyViewCount % SHOW_EVERY_N_HISTORY_VIEWS == 0

    fun preload(activity: Activity) {
        if (!AdsController.adsAvailable || ad != null || loading) return
        loading = true
        runCatching {
            InterstitialAd.load(
                activity,
                BuildConfig.ADMOB_INTERSTITIAL_UNIT_ID,
                AdRequest.Builder().build(),
                object : InterstitialAdLoadCallback() {
                    override fun onAdLoaded(loaded: InterstitialAd) {
                        ad = loaded
                        loading = false
                    }

                    override fun onAdFailedToLoad(error: LoadAdError) {
                        ad = null
                        loading = false
                        Log.d(TAG, "Interstitial not loaded: ${error.message}")
                    }
                },
            )
        }.onFailure {
            loading = false
            Log.w(TAG, "Interstitial load threw", it)
        }
    }

    /** Shows a preloaded ad if there is one. Returns whether anything showed. */
    fun showIfReady(activity: Activity): Boolean {
        val ready = ad ?: return false
        ad = null
        return runCatching { ready.show(activity); true }.getOrDefault(false)
    }
}
