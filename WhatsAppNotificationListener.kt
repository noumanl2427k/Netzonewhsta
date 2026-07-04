package com.example.whatsappautoreply

import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log

class WhatsAppNotificationListener : NotificationListenerService() {

    private val TAG = "WhatsAppAutoReply"
    private val repliedMessages = mutableSetOf<String>()

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)

        if (sbn == null) return

        val packageName = sbn.packageName
        if (packageName != "com.whatsapp" && packageName != "com.whatsapp.w4b") {
            return // Ignore if not WhatsApp or WhatsApp Business
        }

        val notification = sbn.notification
        
        // Ignore group summary notifications to prevent double-replying
        if ((notification.flags and Notification.FLAG_GROUP_SUMMARY) != 0) {
            return
        }

        val sharedPreferences = getSharedPreferences("AutoReplyPrefs", Context.MODE_PRIVATE)
        val isAutoReplyOn = sharedPreferences.getBoolean("isOn", false)
        val replyMessage = sharedPreferences.getString("message", "")

        if (!isAutoReplyOn || replyMessage.isNullOrEmpty()) {
            return // Auto reply is OFF or the saved message is empty
        }

        val title = notification.extras.getCharSequence(Notification.EXTRA_TITLE)?.toString() ?: return
        val text = notification.extras.getCharSequence(Notification.EXTRA_TEXT)?.toString() ?: return

        // Duplicate Protection: Prevent multiple replies to the exact same notification post
        // We include sbn.postTime and sbn.key to guarantee uniqueness per message event.
        // Even if 10 messages arrive per second, they will have distinct postTimes or keys.
        val uniqueKey = "${sbn.key}:$title:$text:${sbn.postTime}"

        if (repliedMessages.contains(uniqueKey)) {
            Log.d(TAG, "Already replied to this message: $uniqueKey")
            return
        }

        // Avoid replying to system messages like "Checking for new messages..."
        if (title.contains("WhatsApp", ignoreCase = true) || text.contains("new messages", ignoreCase = true)) {
            return
        }

        var replyAction: Notification.Action? = null
        var remoteInput: android.app.RemoteInput? = null

        // Find the RemoteInput action (Reply button) in the notification
        notification.actions?.forEach { action ->
            action.remoteInputs?.forEach { ri ->
                replyAction = action
                remoteInput = ri
            }
        }

        if (replyAction != null && remoteInput != null) {
            try {
                val localIntent = Intent()
                localIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                val bundle = Bundle()
                
                // Add the user's fixed auto-reply message to the RemoteInput bundle
                bundle.putCharSequence(remoteInput!!.resultKey, replyMessage)
                android.app.RemoteInput.addResultsToIntent(
                    arrayOf(remoteInput),
                    localIntent,
                    bundle
                )
                
                // Fire the action (send the reply) instantly
                replyAction!!.actionIntent.send(this, 0, localIntent)
                
                // Cache the reply so we don't send it again for this exact notification update
                repliedMessages.add(uniqueKey)
                Log.d(TAG, "Auto-reply sent to $title")

                // Keep the cache size large enough (5000) to handle high spam rates
                // without forgetting old messages and accidentally re-replying to them.
                if (repliedMessages.size > 5000) {
                    val iterator = repliedMessages.iterator()
                    for (i in 0 until 2000) {
                        if (iterator.hasNext()) {
                            iterator.next()
                            iterator.remove()
                        }
                    }
                }

            } catch (e: PendingIntent.CanceledException) {
                Log.e(TAG, "Reply action canceled: ", e)
            } catch (e: Exception) {
                Log.e(TAG, "Error sending reply: ", e)
            }
        }
    }
}
