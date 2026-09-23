package com.ajuworks.atonannichi.ads

import android.app.Activity
import android.content.Context
import android.util.Log
import com.ajuworks.atonannichi.BuildConfig
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.ump.ConsentInformation
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.UserMessagingPlatform
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.atomic.AtomicBoolean

/**
 * AdMob と UMP（同意管理）。
 *
 * - 起動時に同意状況を確認し、必要な地域（EEA/英国など）でだけ同意フォームを出す
 * - 同意情報の更新が通信エラーで失敗しても、端末にキャッシュ済みの canRequestAds() が
 *   true なら広告を続ける（別アプリで、通信エラー時に広告が永久停止した不具合の再発防止）
 * - 広告が読めなくてもアプリの機能は一切止めない（オフラインでも全機能が動く）
 */
object AdsManager {
    private const val TAG = "AtonannichiAds"

    /** 何回の圧縮完了ごとに全画面広告を出すか。 */
    const val INTERSTITIAL_EVERY = 3

    /** 全画面広告どうしの最短間隔。 */
    private const val MIN_INTERVAL_MS = 90_000L

    private var consentInfo: ConsentInformation? = null
    private val sdkStarted = AtomicBoolean(false)
    private val consentStarted = AtomicBoolean(false)

    private val _ready = MutableStateFlow(false)
    val ready: StateFlow<Boolean> = _ready.asStateFlow()

    private val _privacyOptionsRequired = MutableStateFlow(false)
    val privacyOptionsRequired: StateFlow<Boolean> = _privacyOptionsRequired.asStateFlow()

    private var interstitial: InterstitialAd? = null
    private var loadingInterstitial = false
    private var lastShownAt = 0L

    fun start(activity: Activity) {
        val info = UserMessagingPlatform.getConsentInformation(activity)
        consentInfo = info
        // 前回の起動で同意済みなら、今回の問い合わせを待たずに始める
        if (info.canRequestAds()) startSdk(activity)
        if (!consentStarted.compareAndSet(false, true)) return

        val params = ConsentRequestParameters.Builder().build()
        info.requestConsentInfoUpdate(
            activity,
            params,
            {
                UserMessagingPlatform.loadAndShowConsentFormIfRequired(activity) { error ->
                    if (error != null) Log.w(TAG, "consent form: ${error.message}")
                    updatePrivacyOptions(info)
                    if (info.canRequestAds()) startSdk(activity)
                }
            },
            { error ->
                Log.w(TAG, "consent update: ${error.message}")
                if (info.canRequestAds()) startSdk(activity)
            },
        )
    }

    /** 設定画面の「広告のプライバシー設定」から呼ぶ。 */
    fun showPrivacyOptions(activity: Activity) {
        UserMessagingPlatform.showPrivacyOptionsForm(activity) { error ->
            if (error != null) Log.w(TAG, "privacy options: ${error.message}")
            consentInfo?.let(::updatePrivacyOptions)
        }
    }

    private fun updatePrivacyOptions(info: ConsentInformation) {
        _privacyOptionsRequired.value =
            info.privacyOptionsRequirementStatus == ConsentInformation.PrivacyOptionsRequirementStatus.REQUIRED
    }

    private fun startSdk(context: Context) {
        if (!sdkStarted.compareAndSet(false, true)) return
        val app = context.applicationContext
        Thread {
            MobileAds.initialize(app) {
                _ready.value = true
                preloadInterstitial(app)
            }
        }.start()
    }

    private fun preloadInterstitial(context: Context) {
        if (!_ready.value || interstitial != null || loadingInterstitial) return
        loadingInterstitial = true
        InterstitialAd.load(
            context,
            BuildConfig.INTERSTITIAL_AD_UNIT_ID,
            AdRequest.Builder().build(),
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) {
                    loadingInterstitial = false
                    interstitial = ad
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    loadingInterstitial = false
                    Log.i(TAG, "interstitial not loaded: ${error.code}")
                }
            },
        )
    }

    /**
     * 結果画面を閉じたときに呼ぶ。条件を満たし、広告が読み込み済みのときだけ表示する。
     * 圧縮開始時や処理中には絶対に呼ばない。
     */
    fun maybeShowAfterResult(activity: Activity, completedRuns: Int) {
        val ad = interstitial
        val now = System.currentTimeMillis()
        val due = completedRuns > 0 && completedRuns % INTERSTITIAL_EVERY == 0
        if (!due || ad == null || now - lastShownAt < MIN_INTERVAL_MS) {
            preloadInterstitial(activity.applicationContext)
            return
        }
        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                preloadInterstitial(activity.applicationContext)
            }

            override fun onAdFailedToShowFullScreenContent(error: AdError) {
                preloadInterstitial(activity.applicationContext)
            }
        }
        interstitial = null
        lastShownAt = now
        ad.show(activity)
    }
}
