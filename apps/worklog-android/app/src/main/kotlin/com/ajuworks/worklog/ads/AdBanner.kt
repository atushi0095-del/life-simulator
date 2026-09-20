package com.ajuworks.worklog.ads

import android.content.res.Configuration
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.viewinterop.AndroidView
import com.ajuworks.worklog.BuildConfig
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.LoadAdError

/**
 * An adaptive banner that occupies **zero** space until an ad has actually
 * loaded, and gives the space back if one fails.
 *
 * This is the whole "ads must not degrade the app" rule in one place: no
 * placeholder, no reserved gap, no layout shift on a screen the user is
 * reading, and nothing at all when offline.
 */
@Composable
fun AdBanner(modifier: Modifier = Modifier) {
    if (!AdsController.adsAvailable) return

    var loaded by remember { mutableStateOf(false) }
    val configuration: Configuration = LocalConfiguration.current
    val widthDp = configuration.screenWidthDp

    Box(modifier = if (loaded) modifier else Modifier) {
        AndroidView(
            modifier = Modifier,
            factory = { context ->
                AdView(context).apply {
                    setAdSize(
                        AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(context, widthDp)
                    )
                    adUnitId = BuildConfig.ADMOB_BANNER_UNIT_ID
                    adListener = object : AdListener() {
                        override fun onAdLoaded() {
                            loaded = true
                        }

                        override fun onAdFailedToLoad(error: LoadAdError) {
                            loaded = false
                        }
                    }
                    runCatching { loadAd(AdRequest.Builder().build()) }
                }
            },
        )
    }
}
