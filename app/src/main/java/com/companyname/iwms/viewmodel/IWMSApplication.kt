package com.companyname.iwms.viewmodel

import android.app.Application
import com.companyname.iwms.roomdb.AppDatabase
import com.companyname.iwms.viewmodel.EnvironmentRepository

class IWMSApplication : Application() {
    val database by lazy { AppDatabase.getDatabase(this) }
    val repository by lazy { EnvironmentRepository(database.environmentDao()) }
}
