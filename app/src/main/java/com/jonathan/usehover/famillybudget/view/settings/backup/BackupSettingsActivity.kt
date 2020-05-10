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

package com.jonathan.usehover.famillybudget.view.settings.backup

import android.content.Intent
import android.os.Bundle
import android.text.format.DateUtils
import android.view.MenuItem
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.SwitchCompat
import androidx.lifecycle.Observer
import com.jonathan.usehover.famillybudget.R
import com.jonathan.usehover.famillybudget.helper.BaseActivity
import kotlinx.android.synthetic.main.activity_backup_settings.*
import org.koin.android.viewmodel.ext.android.viewModel
import java.util.*
import kotlin.system.exitProcess

class BackupSettingsActivity : BaseActivity() {

    private val viewModel: BackupSettingsViewModel by viewModel()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_backup_settings)

        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        viewModel.cloudBackupStateStream.observe(this, Observer{ cloudBackupState ->
            when(cloudBackupState) {
                BackupCloudStorageState.NotAuthenticated -> {
                    backup_settings_cloud_storage_not_authenticated_state.visibility = View.VISIBLE
                    backup_settings_cloud_storage_authenticating_state.visibility = View.GONE
                    backup_settings_cloud_storage_not_activated_state.visibility = View.GONE
                }
                BackupCloudStorageState.Authenticating -> {
                    backup_settings_cloud_storage_not_authenticated_state.visibility = View.GONE
                    backup_settings_cloud_storage_authenticating_state.visibility = View.VISIBLE
                    backup_settings_cloud_storage_not_activated_state.visibility = View.GONE
                }
                is BackupCloudStorageState.NotActivated -> {
                    backup_settings_cloud_storage_not_authenticated_state.visibility = View.GONE
                    backup_settings_cloud_storage_authenticating_state.visibility = View.GONE
                    backup_settings_cloud_storage_not_activated_state.visibility = View.VISIBLE

                    backup_settings_cloud_storage_email.text = cloudBackupState.currentUser.email
                    backup_settings_cloud_storage_logout_button.visibility = View.VISIBLE
                    backup_settings_cloud_storage_backup_switch.visibility = View.VISIBLE
                    backup_settings_cloud_storage_backup_switch_description.visibility = View.VISIBLE
                    backup_settings_cloud_storage_backup_switch_description.text = getString(R.string.backup_settings_cloud_backup_status, getString(R.string.backup_settings_cloud_backup_status_disabled))
                    backup_settings_cloud_storage_backup_switch.isChecked = false
                    backup_settings_cloud_storage_activated_description.visibility = View.GONE
                    backup_settings_cloud_last_update.visibility = View.GONE
                    backup_settings_cloud_backup_cta.visibility = View.GONE
                    backup_settings_cloud_restore_cta.visibility = View.GONE
                    backup_settings_cloud_storage_restore_description.visibility = View.GONE
                    backup_settings_cloud_storage_restore_explanation.visibility = View.GONE
                    backup_settings_cloud_storage_delete_title.visibility = View.GONE
                    backup_settings_cloud_storage_delete_explanation.visibility = View.GONE
                    backup_settings_cloud_delete_cta.visibility = View.GONE
                    backup_settings_cloud_backup_loading_progress.visibility = View.GONE
                }
                is BackupCloudStorageState.Activated -> {
                    backup_settings_cloud_storage_not_authenticated_state.visibility = View.GONE
                    backup_settings_cloud_storage_authenticating_state.visibility = View.GONE
                    backup_settings_cloud_storage_not_activated_state.visibility = View.VISIBLE

                    backup_settings_cloud_storage_email.text = cloudBackupState.currentUser.email
                    backup_settings_cloud_storage_logout_button.visibility = View.VISIBLE
                    backup_settings_cloud_storage_backup_switch_description.visibility = View.VISIBLE
                    backup_settings_cloud_storage_backup_switch_description.text = getString(R.string.backup_settings_cloud_backup_status, getString(R.string.backup_settings_cloud_backup_status_activated))
                    backup_settings_cloud_storage_backup_switch.visibility = View.VISIBLE
                    backup_settings_cloud_storage_backup_switch.isChecked = true
                    backup_settings_cloud_storage_activated_description.visibility = View.VISIBLE
                    showLastUpdateDate(cloudBackupState.lastBackupDate)
                    backup_settings_cloud_last_update.visibility = View.VISIBLE

                    if( cloudBackupState.backupNowAvailable ) {
                        backup_settings_cloud_backup_cta.visibility = View.VISIBLE
                    } else {
                        backup_settings_cloud_backup_cta.visibility = View.GONE
                    }

                    if( cloudBackupState.restoreAvailable ) {
                        backup_settings_cloud_storage_restore_description.visibility = View.VISIBLE
                        backup_settings_cloud_storage_restore_explanation.visibility = View.VISIBLE
                        backup_settings_cloud_restore_cta.visibility = View.VISIBLE

                        backup_settings_cloud_storage_delete_title.visibility = View.VISIBLE
                        backup_settings_cloud_storage_delete_explanation.visibility = View.VISIBLE
                        backup_settings_cloud_delete_cta.visibility = View.VISIBLE
                    } else {
                        backup_settings_cloud_restore_cta.visibility = View.GONE
                        backup_settings_cloud_storage_restore_description.visibility = View.GONE
                        backup_settings_cloud_storage_restore_explanation.visibility = View.GONE

                        backup_settings_cloud_storage_delete_title.visibility = View.GONE
                        backup_settings_cloud_storage_delete_explanation.visibility = View.GONE
                        backup_settings_cloud_delete_cta.visibility = View.GONE
                    }

                    backup_settings_cloud_backup_loading_progress.visibility = View.GONE
                }
                is BackupCloudStorageState.BackupInProgress -> {
                    backup_settings_cloud_storage_not_authenticated_state.visibility = View.GONE
                    backup_settings_cloud_storage_authenticating_state.visibility = View.GONE
                    backup_settings_cloud_storage_not_activated_state.visibility = View.VISIBLE

                    backup_settings_cloud_storage_email.text = cloudBackupState.currentUser.email
                    backup_settings_cloud_storage_logout_button.visibility = View.GONE
                    backup_settings_cloud_storage_backup_switch.visibility = View.GONE
                    backup_settings_cloud_storage_backup_switch_description.visibility = View.GONE
                    backup_settings_cloud_storage_activated_description.visibility = View.GONE
                    backup_settings_cloud_last_update.visibility = View.GONE
                    backup_settings_cloud_backup_cta.visibility = View.GONE
                    backup_settings_cloud_restore_cta.visibility = View.GONE
                    backup_settings_cloud_storage_restore_description.visibility = View.GONE
                    backup_settings_cloud_storage_restore_explanation.visibility = View.GONE
                    backup_settings_cloud_storage_delete_title.visibility = View.GONE
                    backup_settings_cloud_storage_delete_explanation.visibility = View.GONE
                    backup_settings_cloud_delete_cta.visibility = View.GONE
                    backup_settings_cloud_backup_loading_progress.visibility = View.VISIBLE
                }
                is BackupCloudStorageState.RestorationInProgress -> {
                    backup_settings_cloud_storage_not_authenticated_state.visibility = View.GONE
                    backup_settings_cloud_storage_authenticating_state.visibility = View.GONE
                    backup_settings_cloud_storage_not_activated_state.visibility = View.VISIBLE

                    backup_settings_cloud_storage_email.text = cloudBackupState.currentUser.email
                    backup_settings_cloud_storage_logout_button.visibility = View.GONE
                    backup_settings_cloud_storage_backup_switch.visibility = View.GONE
                    backup_settings_cloud_storage_backup_switch_description.visibility = View.GONE
                    backup_settings_cloud_storage_activated_description.visibility = View.GONE
                    backup_settings_cloud_last_update.visibility = View.GONE
                    backup_settings_cloud_backup_cta.visibility = View.GONE
                    backup_settings_cloud_restore_cta.visibility = View.GONE
                    backup_settings_cloud_storage_restore_description.visibility = View.GONE
                    backup_settings_cloud_storage_restore_explanation.visibility = View.GONE
                    backup_settings_cloud_storage_delete_title.visibility = View.GONE
                    backup_settings_cloud_storage_delete_explanation.visibility = View.GONE
                    backup_settings_cloud_delete_cta.visibility = View.GONE
                    backup_settings_cloud_backup_loading_progress.visibility = View.VISIBLE
                }
                is BackupCloudStorageState.DeletionInProgress -> {
                    backup_settings_cloud_storage_not_authenticated_state.visibility = View.GONE
                    backup_settings_cloud_storage_authenticating_state.visibility = View.GONE
                    backup_settings_cloud_storage_not_activated_state.visibility = View.VISIBLE

                    backup_settings_cloud_storage_email.text = cloudBackupState.currentUser.email
                    backup_settings_cloud_storage_logout_button.visibility = View.GONE
                    backup_settings_cloud_storage_backup_switch.visibility = View.GONE
                    backup_settings_cloud_storage_backup_switch_description.visibility = View.GONE
                    backup_settings_cloud_storage_activated_description.visibility = View.GONE
                    backup_settings_cloud_last_update.visibility = View.GONE
                    backup_settings_cloud_backup_cta.visibility = View.GONE
                    backup_settings_cloud_restore_cta.visibility = View.GONE
                    backup_settings_cloud_storage_restore_description.visibility = View.GONE
                    backup_settings_cloud_storage_restore_explanation.visibility = View.GONE
                    backup_settings_cloud_storage_delete_title.visibility = View.GONE
                    backup_settings_cloud_storage_delete_explanation.visibility = View.GONE
                    backup_settings_cloud_delete_cta.visibility = View.GONE
                    backup_settings_cloud_backup_loading_progress.visibility = View.VISIBLE
                }
            }
        })

        viewModel.backupNowErrorEvent.observe(this, Observer {
            AlertDialog.Builder(this)
                .setTitle(R.string.backup_now_error_title)
                .setMessage(R.string.backup_now_error_message)
                .setPositiveButton(android.R.string.ok, null)
        })

        viewModel.previousBackupAvailableEvent.observe(this, Observer { lastBackupDate ->
            AlertDialog.Builder(this)
                .setTitle(R.string.backup_already_exist_title)
                .setMessage(getString(R.string.backup_already_exist_message, lastBackupDate.formatLastBackupDate()))
                .setPositiveButton(R.string.backup_already_exist_positive_cta) { _, _ ->
                    viewModel.onRestorePreviousBackupButtonPressed()
                }
                .setNegativeButton(R.string.backup_already_exist_negative_cta) { _, _ ->
                    viewModel.onIgnorePreviousBackupButtonPressed()
                }
                .show()
        })

        viewModel.restorationErrorEvent.observe(this, Observer {
            AlertDialog.Builder(this)
                .setTitle(R.string.backup_restore_error_title)
                .setMessage(R.string.backup_restore_error_message)
                .setPositiveButton(android.R.string.ok, null)
        })

        viewModel.appRestartEvent.observe(this, Observer {
            val intent = packageManager.getLaunchIntentForPackage(packageName)
            finishAffinity()
            startActivity(intent)
            exitProcess(0)
        })

        viewModel.restoreConfirmationDisplayEvent.observe(this, Observer { lastBackupDate ->
            AlertDialog.Builder(this)
                .setTitle(R.string.backup_restore_confirmation_title)
                .setMessage(getString(R.string.backup_restore_confirmation_message, lastBackupDate.formatLastBackupDate()))
                .setPositiveButton(R.string.backup_restore_confirmation_positive_cta) { _, _ ->
                    viewModel.onRestoreBackupConfirmationConfirmed()
                }
                .setNegativeButton(R.string.backup_restore_confirmation_negative_cta) { _, _ ->
                    viewModel.onRestoreBackupConfirmationCancelled()
                }
                .show()
        })

        viewModel.authenticationConfirmationDisplayEvent.observe(this, Observer {
            AlertDialog.Builder(this)
                .setTitle(R.string.backup_settings_not_authenticated_privacy_title)
                .setMessage(R.string.backup_settings_not_authenticated_privacy_message)
                .setPositiveButton(R.string.backup_settings_not_authenticated_privacy_positive_cta) { _, _ ->
                    viewModel.onAuthenticationConfirmationConfirmed(this)
                }
                .setNegativeButton(R.string.backup_settings_not_authenticated_privacy_negative_cta) { _, _ ->
                    viewModel.onAuthenticationConfirmationCancelled()
                }
                .show()
        })

        viewModel.deleteConfirmationDisplayEvent.observe(this, Observer {
            AlertDialog.Builder(this)
                .setTitle(R.string.backup_wipe_data_confirmation_title)
                .setMessage(R.string.backup_wipe_data_confirmation_message)
                .setPositiveButton(R.string.backup_wipe_data_confirmation_positive_cta) { _, _ ->
                    viewModel.onDeleteBackupConfirmationConfirmed()
                }
                .setNegativeButton(R.string.backup_wipe_data_confirmation_negative_cta) { _, _ ->
                    viewModel.onDeleteBackupConfirmationCancelled()
                }
                .show()
        })

        viewModel.backupDeletionErrorEvent.observe(this, Observer { _ ->
            AlertDialog.Builder(this)
                .setTitle(R.string.backup_wipe_data_error_title)
                .setMessage(R.string.backup_wipe_data_error_message)
                .setPositiveButton(android.R.string.ok, null)
                .show()
        })

        backup_settings_cloud_storage_authenticate_button.setOnClickListener {
            viewModel.onAuthenticateButtonPressed()
        }

        backup_settings_cloud_storage_logout_button.setOnClickListener {
            viewModel.onLogoutButtonPressed()
        }

        backup_settings_cloud_storage_backup_switch.setOnCheckedChangeListener { _, checked ->
            if( checked ) {
                viewModel.onBackupActivated()
            } else {
                viewModel.onBackupDeactivated()
            }
        }

        backup_settings_cloud_backup_cta.setOnClickListener {
            viewModel.onBackupNowButtonPressed()
        }

        backup_settings_cloud_restore_cta.setOnClickListener {
            viewModel.onRestoreButtonPressed()
        }

        backup_settings_cloud_delete_cta.setOnClickListener {
            viewModel.onDeleteBackupButtonPressed()
        }
    }

    private fun showLastUpdateDate(lastBackupDate: Date?) {
        backup_settings_cloud_last_update.text = if( lastBackupDate != null ) {
            val timeFormatted = DateUtils.getRelativeDateTimeString(
                this,
                lastBackupDate.time,
                DateUtils.MINUTE_IN_MILLIS,
                DateUtils.WEEK_IN_MILLIS,
                DateUtils.FORMAT_SHOW_TIME
            )

            getString(R.string.backup_last_update_date, timeFormatted)
        } else {
            getString(R.string.backup_last_update_date, getString(R.string.backup_last_update_date_never))
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                finish()
                return true
            }
        }

        return super.onOptionsItemSelected(item)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        viewModel.handleActivityResult(requestCode, resultCode, data)
    }

    private fun Date.formatLastBackupDate(): String {
        return DateUtils.formatDateTime(
            this@BackupSettingsActivity,
            this.time,
            DateUtils.FORMAT_SHOW_TIME or DateUtils.FORMAT_SHOW_DATE or DateUtils.FORMAT_SHOW_YEAR
        )
    }
}
