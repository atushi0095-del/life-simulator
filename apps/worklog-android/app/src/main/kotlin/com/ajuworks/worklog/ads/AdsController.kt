package com.ajuworks.worklog.ads

import android.app.Activity
import android.content.Context
import android.util.Log
import com.google.android.gms.ads.MobileAds
import com.google.android.ump.ConsentDebugSettings
import com.google.android.ump.ConsentInformation
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.UserMessagingPlatform
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Consent and SDK initialisation.
 *
 * Every path through this class is allowed to fail. Nothing here blocks the
 * UI, gates a screen, or is awaited before the user can punch in: if consent
 * cannot be gathered or the Ads SDK never initialises, WorkLog simply shows no
 * ads and works exactly as it otherwise would. That is a hard requirement -
 * clocking in must never depend on a network.
 */
object AdsController {

    private const val TAG = "AdsController"

    private val initialised = AtomicBoolean(false)

    /** True once the Ads SDK is ready. Ad composables render nothing until then. */
    @Volatile
    var adsAvailable: Boolean = false
        private set

    private var consentInformation: ConsentInformation? = null

    /**
     * Requests consent where required (EEA/UK), then initialises the Ads SDK.
     * Safe to call on every activity start.
     */
    fun start(activity: Activity) {
        val params = ConsentRequestParameters.Builder()
            .setTagForUnderAgeOfConsent(false)
            .apply {
                if (com.ajuworks.worklog.BuildConfig.DEBUG) {
                    setConsentDebugSettings(
                        ConsentDebugSettings.Builder(activity).build()
                    )
                }
            }
            .build()

        val info = UserMessagingPlatform.getConsentInformation(activity)
        consentInformation = info

        runCatching {
            info.requestConsentInfoUpdate(
                activity,
                params,
                {
                    UserMessagingPlatform.loadAndShowConsentFormIfRequired(activity) { formError ->
                        if (formError != null) {
                            Log.w(TAG, "Consent form skipped: ${formError.message}")
                        }
                        initializeAdsIfAllowed(activity)
                    }
                },
                { requestError ->
                    // No consent info: carry on without ads rather than
                    // retrying or blocking anything.
                    Log.w(TAG, "Consent update failed: ${requestError.message}")
                    initializeAdsIfAllowed(activity)
                },
            )
        }.onFailure {
            Log.w(TAG, "UMP unavailable", it)
        }
    }

    /** Whether the "ad privacy options" entry should do anything. */
    fun isPrivacyOptionsRequired(): Boolean =
        consentInformation?.privacyOptionsRequirementStatus ==
            ConsentInformation.PrivacyOptionsRequirementStatus.REQUIRED

    /** Re-opens the consent form from Settings. */
    fun showPrivacyOptionsForm(activity: Activity) {
        runCatching {
            UserMessagingPlatform.showPrivacyOptionsForm(activity) { error ->
                if (error != null) Log.w(TAG, "Privacy form failed: ${error.message}")
            }
        }.onFailure { Log.w(TAG, "Privacy form unavailable", it) }
    }

    private fun initializeAdsIfAllowed(context: Context) {
        val info = consentInformation
        if (info != null && !info.canRequestAds()) return
        if (!initialised.compareAndSet(false, true)) return

        runCatching {
            MobileAds.initialize(context) {
                adsAvailable = true
            }
        }.onFailure {
            Log.w(TAG, "Ads SDK failed to initialise", it)
            initialised.set(false)
        }
    }
}
