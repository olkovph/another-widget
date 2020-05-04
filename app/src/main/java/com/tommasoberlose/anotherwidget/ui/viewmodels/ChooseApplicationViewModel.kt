package com.tommasoberlose.anotherwidget.ui.viewmodels

import android.app.Application
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.util.Log
import androidx.lifecycle.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ChooseApplicationViewModel(application: Application) : AndroidViewModel(application) {

    val pm: PackageManager by lazy { application.packageManager }
    val appList: MutableLiveData<List<ResolveInfo>> = MutableLiveData()
    val searchInput: MutableLiveData<String> = MutableLiveData("")

    init {
        viewModelScope.launch(Dispatchers.IO) {
            val mainIntent = Intent(Intent.ACTION_MAIN, null).apply {
                addCategory(Intent.CATEGORY_LAUNCHER)
            }

            val app = application.packageManager.queryIntentActivities(mainIntent, 0)
            val sortedApp = app.sortedWith(Comparator { app1: ResolveInfo, app2: ResolveInfo ->
                app1.loadLabel(pm).toString().compareTo(app2.loadLabel(pm).toString())
            })
            withContext(Dispatchers.Main) {
                appList.postValue(sortedApp)
            }
        }
    }
}