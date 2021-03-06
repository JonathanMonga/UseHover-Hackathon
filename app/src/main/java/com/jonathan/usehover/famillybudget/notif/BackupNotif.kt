/*
 *   Copyright 2020 Jonathan MONGA
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */

package com.jonathan.usehover.famillybudget.notif

import android.app.IntentService
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat

import com.jonathan.usehover.famillybudget.R
import com.jonathan.usehover.famillybudget.helper.Logger
import com.jonathan.usehover.famillybudget.view.main.MainActivity

import com.jonathan.usehover.famillybudget.notif.CHANNEL_NEW_FEATURES

class BackupNotif : IntentService("BackupNotif") {

    override fun onHandleIntent(intent: Intent?) {
        if (intent == null) {
            Logger.error("BackupNotif launched with null intent, abort")
            return
        }

        Logger.debug("BackupNotif: received intent: " + intent.action)

        if ( INTENT_SETTINGS_ACTION == intent.action ) {
            val openSettingsIntent = Intent(this, MainActivity::class.java)
            openSettingsIntent.putExtra(MainActivity.INTENT_REDIRECT_TO_SETTINGS_FOR_BACKUP_EXTRA, true)
            openSettingsIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(openSettingsIntent)
        }

        NotificationManagerCompat.from(this).cancel(NOTIFICATION_ID)
    }

    companion object {
        private const val NOTIFICATION_ID = 10043
        private const val INTENT_SETTINGS_ACTION = "backup_notif_intent"

        // ---------------------------------------->

        fun showBackupNotif(context: Context) {
            try {
                val redirectIntent = Intent(context, BackupNotif::class.java)
                redirectIntent.action = INTENT_SETTINGS_ACTION
                val redirectPendingIntent = PendingIntent.getService(context, 0, redirectIntent, PendingIntent.FLAG_CANCEL_CURRENT)

                val notifBuilder = NotificationCompat.Builder(context, CHANNEL_NEW_FEATURES)
                    .setSmallIcon(R.drawable.ic_push)
                    .setContentTitle(context.resources.getString(R.string.app_name))
                    .setContentText(context.resources.getString(R.string.backup_notif_message))
                    .setStyle(NotificationCompat.BigTextStyle().bigText(context.resources.getString(R.string.backup_notif_message)))
                    .addAction(R.drawable.ic_backup_cta, context.resources.getString(R.string.backup_notif_cta), redirectPendingIntent)
                    .setContentIntent(redirectPendingIntent)
                    .setColor(ContextCompat.getColor(context, R.color.accent))

                NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notifBuilder.build())
            } catch (e: Exception) {
                Logger.error("Error while showing backup optin notif", e)
            }

        }
    }
}