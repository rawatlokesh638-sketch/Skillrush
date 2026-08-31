package com.example.data

import android.app.Activity
import android.content.Context
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID

object RewardManager {
    private const val TAG = "RewardManager"

    // Set of processed transaction IDs to prevent duplicate rewards
    private val processedTransactions = mutableSetOf<String>()

    // State to lock the reward button while an operation is active
    private val _isRewardOperationActive = MutableStateFlow(false)
    val isRewardOperationActive: StateFlow<Boolean> = _isRewardOperationActive.asStateFlow()

    /**
     * Initiates a rewarded ad flow. Ensures only one operation is active at a time.
     * Generates a unique transaction ID for tracking this specific ad view.
     */
    fun processRewardedAd(
        activity: Activity,
        rewardType: String = "coins",
        onSuccess: (amount: Int) -> Unit,
        onFailure: (reason: String) -> Unit
    ) {
        if (_isRewardOperationActive.value) {
            Log.w(TAG, "Reward operation already active. Ignoring rapid tap.")
            return
        }

        if (!AdManager.isRewardedReady()) {
            Log.w(TAG, "Rewarded ad is not ready yet.")
            onFailure("Ad not ready")
            AdManager.loadRewarded(activity)
            return
        }

        _isRewardOperationActive.value = true
        val transactionId = UUID.randomUUID().toString()
        Log.d(TAG, "Starting rewarded ad transaction: $transactionId")

        var rewardGrantedLocally = false
        var grantedAmount = 0

        AdManager.showRewarded(
            activity = activity,
            onRewardEarned = { amount ->
                Log.d(TAG, "AdMob callback: Reward earned for transaction: $transactionId, amount: $amount")
                
                // Validate amount
                if (amount <= 0) {
                    Log.e(TAG, "Invalid reward amount: $amount for transaction: $transactionId")
                    return@showRewarded
                }

                // Double verification: Ensure we only process this transaction once
                if (processedTransactions.contains(transactionId)) {
                    Log.e(TAG, "Duplicate transaction detected: $transactionId")
                    return@showRewarded
                }

                processedTransactions.add(transactionId)
                
                // Track internally that AdMob triggered the callback
                rewardGrantedLocally = true
                grantedAmount = amount
                
                Log.i(TAG, "Transaction $transactionId validated successfully.")
            },
            onAdDismissedOrFailed = {
                _isRewardOperationActive.value = false
                
                if (rewardGrantedLocally) {
                    // Ad is dismissed, and reward was granted during the session
                    Log.d(TAG, "Ad closed. Distributing validated reward: $grantedAmount")
                    grantCentralizedReward(activity, grantedAmount)
                    onSuccess(grantedAmount)
                } else {
                    // Ad dismissed early, failed to load, or no reward callback
                    Log.w(TAG, "Ad closed or failed without granting reward. Transaction: $transactionId")
                    onFailure("Ad closed early or failed")
                }
            }
        )
    }

    /**
     * Centralized place to grant the physical reward after all validations pass.
     * Prevents decentralized UI updates.
     */
    private fun grantCentralizedReward(context: Context, amount: Int) {
        // Here we could also log to Firebase later.
        Log.d(TAG, "Granting centralized reward to user: $amount coins")
        UserProfileManager.addRewardedBonus(context, amount)
    }
}
