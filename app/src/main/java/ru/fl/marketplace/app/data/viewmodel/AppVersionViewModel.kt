/**
 * © Panov Vitaly 2023 - All Rights Reserved
 *
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 */

package ru.fl.marketplace.app.data.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.liveData
import kotlinx.coroutines.Dispatchers
import ru.fl.marketplace.app.data.api.ApiHelper
import ru.fl.marketplace.app.data.api.RetrofitBuilder
import ru.fl.marketplace.app.data.repository.AppVersionRepository
import ru.fl.marketplace.app.utils.Resource

class AppVersionViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: AppVersionRepository = AppVersionRepository( ApiHelper( RetrofitBuilder.apiService ) )

    fun checkAppVersion() = liveData(Dispatchers.IO) {
        emit(Resource.loading(data = null))
        try{
            val appVersion = repository.checkAppVersion()
            emit(Resource.remoteSuccess(appVersion))
        }catch (exception: Exception){
            emit(Resource.error(data = null, message = exception.message ?: ItemViewModel.DEFAULT_ERR_MESSAGE))
        }
    }

}
