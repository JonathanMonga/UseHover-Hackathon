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

package com.jonathan.usehover.famillybudget.view.expenseedit

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.app.Activity
import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Observer
import com.hover.sdk.actions.HoverAction
import com.hover.sdk.api.Hover
import com.hover.sdk.api.HoverParameters
import com.hover.sdk.permissions.PermissionActivity
import com.jonathan.usehover.famillybudget.R
import com.jonathan.usehover.famillybudget.helper.*
import com.jonathan.usehover.famillybudget.parameters.Parameters
import com.jonathan.usehover.famillybudget.view.DatePickerDialogFragment
import com.jonathan.usehover.famillybudget.view.main.MainActivity
import kotlinx.android.synthetic.main.activity_expense_edit.*
import org.koin.android.ext.android.inject
import org.koin.android.viewmodel.ext.android.viewModel
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.abs

/**
 * Activity to add a new expense
 *
 * @author Jonathan MONGA
 */
class ExpenseEditActivity : BaseActivity() {
    private val parameters: Parameters by inject()
    private val viewModel: ExpenseEditViewModel by viewModel()

// -------------------------------------->

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_expense_edit)

        setSupportActionBar(toolbar)

        supportActionBar?.setDisplayShowHomeEnabled(true)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        Hover.initialize(this)

        viewModel.existingExpenseEventStream.observe(this, Observer { existingValues ->
            if (existingValues != null) {
                setUpTextFields(existingValues.title, existingValues.amount)
            } else {
                setUpTextFields(description = null, amount = null)
            }
        })

        if (savedInstanceState == null) {
            viewModel.initWithDateAndExpense(Date(intent.getLongExtra("date", 0)), intent.getParcelableExtra("expense"))
        }

        setUpButtons()

        setResult(Activity.RESULT_CANCELED)

        if (willAnimateActivityEnter()) {
            animateActivityEnter(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    description_edittext.setFocus()
                    save_expense_fab.animateFABAppearance()
                }
            })
        } else {
            description_edittext.setFocus()
            save_expense_fab.animateFABAppearance()
        }

        date_button.removeButtonBorder()

        viewModel.editTypeLiveData.observe(this, Observer { (isRevenue, isEdit) ->
            setExpenseTypeTextViewLayout(isRevenue, isEdit)
        })

        viewModel.expenseDateLiveData.observe(this, Observer { date ->
            setUpDateButton(date)
        })

        viewModel.finishEventStream.observe(this, Observer {
            setResult(Activity.RESULT_OK)
            finish()
        })

        viewModel.expenseAddBeforeInitDateEventStream.observe(this, Observer {
            AlertDialog.Builder(this)
                    .setTitle(R.string.expense_add_before_init_date_dialog_title)
                    .setMessage(R.string.expense_add_before_init_date_dialog_description)
                    .setPositiveButton(R.string.expense_add_before_init_date_dialog_positive_cta) { _, _ ->
                        viewModel.onAddExpenseBeforeInitDateConfirmed(getCurrentAmount(), description_edittext.text.toString())
                    }
                    .setNegativeButton(R.string.expense_add_before_init_date_dialog_negative_cta) { _, _ ->
                        viewModel.onAddExpenseBeforeInitDateCancelled()
                    }
                    .show()
        })
    }

// ----------------------------------->

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                finish()
                return true
            }
        }

        return super.onOptionsItemSelected(item)
    }

    /**
     * Validate user inputs
     *
     * @return true if user inputs are ok, false otherwise
     */
    private fun validateInputs(): Boolean {
        var ok = true

        val description = description_edittext.text.toString()
        if (description.trim { it <= ' ' }.isEmpty()) {
            description_edittext.error = resources.getString(R.string.no_description_error)
            ok = false
        }

        val amount = amount_edittext.text.toString()
        if (amount.trim { it <= ' ' }.isEmpty()) {
            amount_edittext.error = resources.getString(R.string.no_amount_error)
            ok = false
        } else {
            try {
                val value = java.lang.Double.valueOf(amount)
                if (value <= 0) {
                    amount_edittext.error = resources.getString(R.string.negative_amount_error)
                    ok = false
                }
            } catch (e: Exception) {
                amount_edittext.error = resources.getString(R.string.invalid_amount)
                ok = false
            }
        }

        return ok
    }

    /**
     * Set-up revenue and payment buttons
     */
    private fun setUpButtons() {
        expense_type_switch.setOnCheckedChangeListener { _, isChecked ->
            viewModel.onExpenseRevenueValueChanged(isChecked)
        }

        expense_type_tv.setOnClickListener {
            viewModel.onExpenseRevenueValueChanged(!expense_type_switch.isChecked)
        }

        save_expense_fab.setOnClickListener {
            if (validateInputs()) {
                AlertDialog.Builder(this)
                        .setTitle("Pay with UseHover")
                        .setMessage("Do you want to pay with UseHover Now ?")
                        .setPositiveButton(R.string.expense_add_before_init_date_dialog_positive_cta) { _, _ ->
                            ActivityCompat.startActivityForResult(this, Intent(this, PermissionActivity::class.java), 1, null)

                            val startIntent: Intent = HoverParameters.Builder(applicationContext)
                                    .request("af723253") // Add your action ID here
                                    .buildIntent()
                            ActivityCompat.startActivityForResult(this, startIntent, 0, null)
                        }

                        .setNegativeButton("Without") { _, _ ->
                            viewModel.onSave(getCurrentAmount(), description_edittext.text.toString())
                        }
                        .show()
            }
        }
    }

    /**
     * Set revenue text view layout
     */
    private fun setExpenseTypeTextViewLayout(isRevenue: Boolean, isEdit: Boolean) {
        if (isRevenue) {
            expense_type_tv.setText(R.string.income)
            expense_type_tv.setTextColor(ContextCompat.getColor(this, R.color.budget_green))

            expense_type_switch.isChecked = true

            setTitle(if (isEdit) R.string.title_activity_edit_income else R.string.title_activity_add_income)
        } else {
            expense_type_tv.setText(R.string.payment)
            expense_type_tv.setTextColor(ContextCompat.getColor(this, R.color.budget_red))

            expense_type_switch.isChecked = false

            setTitle(if (isEdit) R.string.title_activity_edit_expense else R.string.title_activity_add_expense)
        }
    }

    /**
     * Set up text field focus behavior
     */
    private fun setUpTextFields(description: String?, amount: Double?) {
        amount_inputlayout.hint = resources.getString(R.string.amount, parameters.getUserCurrency().symbol)

        if (description != null) {
            description_edittext.setText(description)
            description_edittext.setSelection(description_edittext.text?.length
                    ?: 0) // Put focus at the end of the text
        }

        amount_edittext.preventUnsupportedInputForDecimals()

        if (amount != null) {
            amount_edittext.setText(CurrencyHelper.getFormattedAmountValue(abs(amount)))
        }
    }

    /**
     * Set up the date button
     */
    private fun setUpDateButton(date: Date) {
        val formatter = SimpleDateFormat(resources.getString(R.string.add_expense_date_format), Locale.getDefault())
        date_button.text = formatter.format(date)

        date_button.setOnClickListener {
            val fragment = DatePickerDialogFragment(date, DatePickerDialog.OnDateSetListener { _, year, monthOfYear, dayOfMonth ->
                val cal = Calendar.getInstance()

                cal.set(Calendar.YEAR, year)
                cal.set(Calendar.MONTH, monthOfYear)
                cal.set(Calendar.DAY_OF_MONTH, dayOfMonth)

                viewModel.onDateChanged(cal.time)
            })

            fragment.show(supportFragmentManager, "datePicker")
        }
    }

    private fun getCurrentAmount(): Double {
        return java.lang.Double.parseDouble(amount_edittext.text.toString())
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == 0)
            if (resultCode == Activity.RESULT_OK)
                viewModel.onSave(getCurrentAmount(), description_edittext.text.toString())
            else
                Toast.makeText(applicationContext, "Action cancel.", Toast.LENGTH_LONG).show()
        else
            if (resultCode == Activity.RESULT_OK)
                Toast.makeText(applicationContext, "Good choice!", Toast.LENGTH_LONG).show()
    }
}
