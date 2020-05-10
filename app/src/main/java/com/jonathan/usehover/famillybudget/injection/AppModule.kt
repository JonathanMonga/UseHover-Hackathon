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

package com.jonathan.usehover.famillybudget.injection

import androidx.collection.ArrayMap
import com.jonathan.usehover.famillybudget.auth.Auth
import com.jonathan.usehover.famillybudget.auth.FirebaseAuth
import com.jonathan.usehover.famillybudget.cloudstorage.CloudStorage
import com.jonathan.usehover.famillybudget.cloudstorage.FirebaseStorage
import com.jonathan.usehover.famillybudget.parameters.Parameters
import com.jonathan.usehover.famillybudget.iab.Iab
import com.jonathan.usehover.famillybudget.iab.IabImpl
import com.jonathan.usehover.famillybudget.model.Expense
import com.jonathan.usehover.famillybudget.db.DB
import com.jonathan.usehover.famillybudget.db.impl.CachedDBImpl
import com.jonathan.usehover.famillybudget.db.impl.CacheDBStorage
import com.jonathan.usehover.famillybudget.db.impl.DBImpl
import com.jonathan.usehover.famillybudget.db.impl.RoomDB
import org.koin.dsl.module
import java.util.*
import java.util.concurrent.Executor
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

val appModule = module {
    single { Parameters(get()) }

    single<Iab> { IabImpl(get(), get()) }

    single<CacheDBStorage> { object : CacheDBStorage {
        override val expenses: MutableMap<Date, List<Expense>> = ArrayMap()
        override val balances: MutableMap<Date, Double> = ArrayMap()
    } }

    single<Executor> { Executors.newSingleThreadExecutor() }

    single<Auth> { FirebaseAuth(com.google.firebase.auth.FirebaseAuth.getInstance()) }

    single<CloudStorage> { FirebaseStorage(com.google.firebase.storage.FirebaseStorage.getInstance().apply {
        maxOperationRetryTimeMillis = TimeUnit.SECONDS.toMillis(10)
        maxDownloadRetryTimeMillis = TimeUnit.SECONDS.toMillis(10)
        maxUploadRetryTimeMillis = TimeUnit.SECONDS.toMillis(10)
    }) }

    factory<DB> { CachedDBImpl(DBImpl(RoomDB.create(get())), get(), get()) }

    factory { CachedDBImpl(DBImpl(RoomDB.create(get())), get(), get()) }
}