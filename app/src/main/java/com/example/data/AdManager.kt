package com.example.data

import android.app.Activity
import android.content.Context
import android.util.Log
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.appopen.AppOpenAd
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback

object AdManager {
    private const val TAG = "AdManager"

    const val APP_ID = "ca-app-pub-2330525119428447~6431350396"
    const val BANNER_ID = "ca-app-pub-2330525119428447/3805187050"
    const val INTERSTITIAL_ID = "ca-app-pub-2330525119428447/6893173370"
    const val REWARDED_ID = "ca-app-pub-2330525119428447/9120715611"
    const val APP_OPEN_ID = "ca-app-pub-2330525119428447/6969194996"
    const val NATIVE_ID = "ca-app-pub-2330525119428447/7616062258"

    private var isInitialized = false

    private var interstitialAd: InterstitialAd? = null
    private var isInterstitialLoading = false

    private var rewardedAd: RewardedAd? = null
    private var isRewardedLoading = false

    private var appOpenAd: AppOpenAd? = null
    private var isAppOpenLoading = false
    private var isShowingAppOpen = false

    private var lastInterstitialTime = 0L
    private const val INTERSTITIAL_COOLDOWN_MS = 120_000L

    fun initialize(context: Context) {
        if (isInitialized) return
        try {
            MobileAds.initialize(context) { initializationStatus ->
                isInitialized = true
                Log.d(TAG, "AdMob Initialized: $initializationStatus")
                preloadAds(context)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing AdMob", e)
        }
    }

    fun preloadAds(context: Context) {
        loadInterstitial(context)
        loadRewarded(context)
        loadAppOpen(context)
    }

    fun loadInterstitial(context: Context) {
        if (interstitialAd != null || isInterstitialLoading) return
        isInterstitialLoading = true
        val adRequest = AdRequest.Builder().build()
        InterstitialAd.load(
            context,
            INTERSTITIAL_ID,
            adRequest,
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) {
                    interstitialAd = ad
                    isInterstitialLoading = false
                }
                override fun onAdFailedToLoad(error: LoadAdError) {
                    interstitialAd = null
                    isInterstitialLoading = false
                }
            }
        )
    }

    fun showInterstitialIfNeeded(activity: Activity, onAdDismissed: () -> Unit = {}) {
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastInterstitialTime < INTERSTITIAL_COOLDOWN_MS) {
            onAdDismissed()
            return
        }

        val ad = interstitialAd
        if (ad != null) {
            lastInterstitialTime = currentTime
            interstitialAd = null
            ad.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                    loadInterstitial(activity)
                    onAdDismissed()
                }
                override fun onAdFailedToShowFullScreenContent(error: AdError) {
                    loadInterstitial(activity)
                    onAdDismissed()
                }
            }
            ad.show(activity)
        } else {
            loadInterstitial(activity)
            onAdDismissed()
        }
    }

    fun loadRewarded(context: Context) {
        if (rewardedAd != null || isRewardedLoading) return
        isRewardedLoading = true
        val adRequest = AdRequest.Builder().build()
        RewardedAd.load(
            context,
            REWARDED_ID,
            adRequest,
            object : RewardedAdLoadCallback() {
                override fun onAdLoaded(ad: RewardedAd) {
                    rewardedAd = ad
                    isRewardedLoading = false
                }
                override fun onAdFailedToLoad(error: LoadAdError) {
                    rewardedAd = null
                    isRewardedLoading = false
                }
            }
        )
    }

    fun isRewardedReady(): Boolean = rewardedAd != null

    fun showRewarded(
        activity: Activity,
        onRewardEarned: (rewardAmount: Int) -> Unit,
        onAdDismissedOrFailed: () -> Unit
    ) {
        val ad = rewardedAd
        if (ad != null) {
            rewardedAd = null
            var rewardGranted = false

            ad.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                    loadRewarded(activity)
                    if (!rewardGranted) {
                        onAdDismissedOrFailed()
                    }
                }
                override fun onAdFailedToShowFullScreenContent(error: AdError) {
                    loadRewarded(activity)
                    if (!rewardGranted) {
                        onAdDismissedOrFailed()
                    }
                }
            }

            ad.show(activity) { rewardItem ->
                if (!rewardGranted) {
                    rewardGranted = true
                    val amount = if (rewardItem.amount > 0) rewardItem.amount else 50
                    onRewardEarned(amount)
                }
            }
        } else {
            loadRewarded(activity)
            onAdDismissedOrFailed()
        }
    }

    fun loadAppOpen(context: Context) {
        if (appOpenAd != null || isAppOpenLoading) return
        isAppOpenLoading = true
        val request = AdRequest.Builder().build()
        AppOpenAd.load(
            context,
            APP_OPEN_ID,
            request,
            AppOpenAd.APP_OPEN_AD_ORIENTATION_PORTRAIT,
            object : AppOpenAd.AppOpenAdLoadCallback() {
                override fun onAdLoaded(ad: AppOpenAd) {
                    appOpenAd = ad
                    isAppOpenLoading = false
                }
                override fun onAdFailedToLoad(error: LoadAdError) {
                    appOpenAd = null
                    isAppOpenLoading = false
                }
            }
        )
    }

    fun showAppOpenIfReady(activity: Activity) {
        if (isShowingAppOpen) return
        val ad = appOpenAd
        if (ad != null) {
            isShowingAppOpen = true
            ad.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                    appOpenAd = null
                    isShowingAppOpen = false
                    loadAppOpen(activity)
                }
                override fun onAdFailedToShowFullScreenContent(error: AdError) {
                    appOpenAd = null
                    isShowingAppOpen = false
                    loadAppOpen(activity)
                }
            }
            ad.show(activity)
        } else {
            loadAppOpen(activity)
        }
    }
}
