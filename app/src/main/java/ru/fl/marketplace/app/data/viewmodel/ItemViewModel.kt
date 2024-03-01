package ru.fl.marketplace.app.data.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.liveData
import ru.fl.marketplace.app.data.api.ApiHelper
import ru.fl.marketplace.app.data.api.RetrofitBuilder
import ru.fl.marketplace.app.data.db.AppDatabase
import ru.fl.marketplace.app.data.db.ItemDao
import ru.fl.marketplace.app.data.repository.ItemRepository
import ru.fl.marketplace.app.utils.Resource
import kotlinx.coroutines.Dispatchers

class ItemViewModel(application: Application) : AndroidViewModel(application) {

    companion object {
        const val DEFAULT_ERR_MESSAGE = "Error Occured!"
    }

    private val repository: ItemRepository = ItemRepository( ApiHelper( RetrofitBuilder.apiService) )
    private val itemDao: ItemDao = AppDatabase.getDatabase(application).itemsDao()

    fun getItems() = liveData (Dispatchers.IO) {
        emit(Resource.loading(data = null))
        val items = itemDao.allItems
        if(items.isNotEmpty()) emit(Resource.localSuccess(data = items))
        try {
            val received = repository.getItems()
            //Compare two lists
            if( !isEqual(received.sortedBy { it.id }, items.sortedBy { it.id }) ){
                itemDao.deleteAll()
                received.forEach { item ->  itemDao.insert( item ) }
                emit(Resource.remoteSuccess(data = received))
            }
        } catch (exception: Exception) {
            emit(Resource.error(data = items, message = exception.message ?: DEFAULT_ERR_MESSAGE))
        }
    }

    fun getItem( id: String? ) = liveData (Dispatchers.IO) {
        emit(Resource.loading(data = null))
        val item = itemDao.findById( id!! )
        item?.let {
            emit(Resource.localSuccess(data = item))
        }
        try {
            val received = repository.getItem( id )
            if(received != item){
                itemDao.update( received )
                emit(Resource.remoteSuccess(data = received))
            }
        } catch (exception: Exception) {
            emit(Resource.error(data = item, message = exception.message ?: DEFAULT_ERR_MESSAGE))
        }
    }

    fun viewCountInc( id: String? ) = liveData(Dispatchers.IO) {
        try{
            val item = repository.viewCountInc( id!! )
            item?.let {
                emit(Resource.remoteSuccess(data=item))
            }
        } catch (exception: Exception){
            emit(Resource.error(data = null, message = exception.message ?: DEFAULT_ERR_MESSAGE))
        }

    }

    fun rate( id: String?, rating: Int? ) = liveData(Dispatchers.IO) {
        try{
            val item = repository.rate( id, rating )
            item?.let {
                emit(Resource.remoteSuccess(data = item))
            }
        } catch (exception: Exception){
            emit(Resource.error(data = null, message = exception.message ?: DEFAULT_ERR_MESSAGE))
        }
    }

    fun<T> isEqual(first: List<T>, second: List<T>): Boolean {

        if (first.size != second.size) {
            return false
        }

        return first.zip(second).all { (x, y) -> x == y }
    }

}