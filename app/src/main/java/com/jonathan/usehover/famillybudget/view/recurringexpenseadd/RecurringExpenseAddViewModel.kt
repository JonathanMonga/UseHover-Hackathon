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

package com.jonathan.usehover.famillybudget.view.recurringexpenseadd

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jonathan.usehover.famillybudget.helper.Logger
import com.jonathan.usehover.famillybudget.helper.SingleLiveEvent
import com.jonathan.usehover.famillybudget.model.Expense
import com.jonathan.usehover.famillybudget.model.RecurringExpenseType
import com.jonathan.usehover.famillybudget.db.DB
import kotlinx.coroutines.launch
import java.util.*
import com.jonathan.usehover.famillybudget.model.RecurringExpense
import com.jonathan.usehover.famillybudget.parameters.Parameters
import com.jonathan.usehover.famillybudget.parameters.getInitTimestamp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext


class RecurringExpenseAddViewModel(private val db: DB,
                                   private val parameters: Parameters) : ViewModel() {
    val expenseDateLiveData = MutableLiveData<Date>()
    val editTypeIsRevenueLiveData = MutableLiveData<Boolean>()
    val savingIsRevenueEventStream = SingleLiveEvent<Boolean>()
    val finishLiveData = MutableLiveData<Unit>()
    val expenseAddBeforeInitDateEventStream = SingleLiveEvent<Unit>()
    val errorEventStream = SingleLiveEvent<Unit>()

    fun initWithDateAndExpense(date: Date) {
        this.expenseDateLiveData.value = date
        this.editTypeIsRevenueLiveData.value = false
    }

    fun onExpenseRevenueValueChanged(isRevenue: Boolean) {
        editTypeIsRevenueLiveData.value = isRevenue
    }

    fun onSave(value: Double, description: String, recurringExpenseType: RecurringExpenseType) {
        val isRevenue = editTypeIsRevenueLiveData.value ?: return
        val date = expenseDateLiveData.value ?: return

        if( date.before(Date(parameters.getInitTimestamp())) ) {
            expenseAddBeforeInitDateEventStream.value = Unit
            return
        }

        doSaveExpense(value, description, recurringExpenseType, isRevenue, date)
    }

    fun onAddExpenseBeforeInitDateConfirmed(value: Double, description: String, recurringExpenseType: RecurringExpenseType) {
        val isRevenue = editTypeIsRevenueLiveData.value ?: return
        val date = expenseDateLiveData.value ?: return

        doSaveExpense(value, description, recurringExpenseType, isRevenue, date)
    }

    fun onAddExpenseBeforeInitDateCancelled() {
        // No-op
    }

    private fun doSaveExpense(value: Double, description: String, recurringExpenseType: RecurringExpenseType, isRevenue: Boolean, date: Date) {
        savingIsRevenueEventStream.value = isRevenue

        viewModelScope.launch {
            val inserted = withContext(Dispatchers.Default) {
                val insertedExpense = try {
                    db.persistRecurringExpense(RecurringExpense(description, if (isRevenue) -value else value, date, recurringExpenseType))
                } catch (t: Throwable) {
                    Logger.error(false, "Error while inserting recurring expense into DB: addRecurringExpense returned false")
                    return@withContext false
                }

                if( !flattenExpensesForRecurringExpense(insertedExpense, date) ) {
                    Logger.error(false, "Error while flattening expenses for recurring expense: flattenExpensesForRecurringExpense returned false")
                    return@withContext false
                }

                return@withContext true
            }

            if( inserted ) {
                finishLiveData.value = null
            } else {
                errorEventStream.value = null
            }
        }
    }

    private suspend fun flattenExpensesForRecurringExpense(expense: RecurringExpense, date: Date): Boolean
    {
        val cal = Calendar.getInstance()
        cal.time = date

        when (expense.type) {
            RecurringExpenseType.DAILY -> {
                // Add up to 5 years of expenses
                for (i in 0 until 365*5) {
                    try {
                        db.persistExpense(Expense(expense.title, expense.originalAmount, cal.time, expense))
                    } catch (t: Throwable) {
                        Logger.error(false, "Error while inserting expense for recurring expense into DB: persistExpense returned false")
                        return false
                    }

                    cal.add(Calendar.DAY_OF_YEAR, 1)
                }
            }
            RecurringExpenseType.WEEKLY -> {
                // Add up to 5 years of expenses
                for (i in 0 until 12*4*5) {
                    try {
                        db.persistExpense(Expense(expense.title, expense.originalAmount, cal.time, expense))
                    } catch (t: Throwable) {
                        Logger.error(false, "Error while inserting expense for recurring expense into DB: persistExpense returned false", t)
                        return false
                    }

                    cal.add(Calendar.WEEK_OF_YEAR, 1)
                }
            }
            RecurringExpenseType.BI_WEEKLY -> {
                // Add up to 5 years of expenses
                for (i in 0 until 12*4*5) {
                    try {
                        db.persistExpense(Expense(expense.title, expense.originalAmount, cal.time, expense))
                    } catch (t: Throwable) {
                        Logger.error(false, "Error while inserting expense for recurring expense into DB: persistExpense returned false", t)
                        return false
                    }

                    cal.add(Calendar.WEEK_OF_YEAR, 2)
                }
            }
            RecurringExpenseType.TER_WEEKLY -> {
                // Add up to 5 years of expenses
                for (i in 0 until 12*4*5) {
                    try {
                        db.persistExpense(Expense(expense.title, expense.originalAmount, cal.time, expense))
                    } catch (t: Throwable) {
                        Logger.error(false, "Error while inserting expense for recurring expense into DB: persistExpense returned false", t)
                        return false
                    }

                    cal.add(Calendar.WEEK_OF_YEAR, 3)
                }
            }
            RecurringExpenseType.FOUR_WEEKLY -> {
                // Add up to 5 years of expenses
                for (i in 0 until 12*4*5) {
                    try {
                        db.persistExpense(Expense(expense.title, expense.originalAmount, cal.time, expense))
                    } catch (t: Throwable) {
                        Logger.error(false, "Error while inserting expense for recurring expense into DB: persistExpense returned false", t)
                        return false
                    }

                    cal.add(Calendar.WEEK_OF_YEAR, 4)
                }
            }
            RecurringExpenseType.MONTHLY -> {
                // Add up to 10 years of expenses
                for (i in 0 until 12*10) {
                    try {
                        db.persistExpense(Expense(expense.title, expense.originalAmount, cal.time, expense))
                    } catch (t: Throwable) {
                        Logger.error(false, "Error while inserting expense for recurring expense into DB: persistExpense returned false", t)
                        return false
                    }

                    cal.add(Calendar.MONTH, 1)
                }
            }
            RecurringExpenseType.TER_MONTHLY -> {
                // Add up to 25 years of expenses
                for (i in 0 until 4*25) {
                    try {
                        db.persistExpense(Expense(expense.title, expense.originalAmount, cal.time, expense))
                    } catch (t: Throwable) {
                        Logger.error(false, "Error while inserting expense for recurring expense into DB: persistExpense returned false", t)
                        return false
                    }

                    cal.add(Calendar.MONTH, 3)
                }
            }
            RecurringExpenseType.SIX_MONTHLY -> {
                // Add up to 25 years of expenses
                for (i in 0 until 2*25) {
                    try {
                        db.persistExpense(Expense(expense.title, expense.originalAmount, cal.time, expense))
                    } catch (t: Throwable) {
                        Logger.error(false, "Error while inserting expense for recurring expense into DB: persistExpense returned false", t)
                        return false
                    }

                    cal.add(Calendar.MONTH, 6)
                }
            }
            RecurringExpenseType.YEARLY -> {
                // Add up to 100 years of expenses
                for (i in 0 until 100) {
                    try {
                        db.persistExpense(Expense(expense.title, expense.originalAmount, cal.time, expense))
                    } catch (t: Throwable) {
                        Logger.error(false, "Error while inserting expense for recurring expense into DB: persistExpense returned false")
                        return false
                    }

                    cal.add(Calendar.YEAR, 1)
                }
            }
        }

        return true
    }

    fun onDateChanged(date: Date) {
        this.expenseDateLiveData.value = date
    }

    override fun onCleared() {
        db.close()

        super.onCleared()
    }
}