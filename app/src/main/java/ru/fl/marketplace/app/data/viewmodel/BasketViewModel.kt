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
import ru.fl.marketplace.app.data.db.AppDatabase
import ru.fl.marketplace.app.data.db.ItemBasketDao
import ru.fl.marketplace.app.data.model.ItemBasket
import ru.fl.marketplace.app.data.model.Item
import ru.fl.marketplace.app.utils.Resource
import kotlin.streams.toList

class BasketViewModel(application: Application): AndroidViewModel(application) {
    private val basketDao: ItemBasketDao = AppDatabase.getDatabase(application).basketDao()

    fun getBasket() = liveData(Dispatchers.IO) {
        emit(Resource.loading(data = null))
        /*val basket = ArrayList<Item>(basketDao.allItems.size)
        basketDao.allItems.forEach {
            it.item?.let { it1 -> basket.add(it1) }
        }
        emit(Resource.localSuccess(data = basket))*/
        emit(Resource.localSuccess(data = basketDao.allItems))
    }

    fun addItem(item: Item, count: Int) = liveData(Dispatchers.IO) {
        emit(Resource.loading(data = null))
        var basketItem = getFirstForItem(item)
        if( basketItem == null ){
            basketItem = ItemBasket(item = item, count = count)
            basketDao.insert(basketItem)
        } else {
            basketItem.count += count
            basketDao.update(basketItem)
        }
        emit(Resource.localSuccess(data = basketItem))
    }

    fun removeItem(item: Item, count: Int ) = liveData(Dispatchers.IO) {
        emit(Resource.loading(data = null))
        var basketItem = getFirstForItem(item)
        if( basketItem != null ){
            basketItem.count -= count
            if(basketItem.count <= 0) {
                basketDao.delete(basketItem)
            } else {
                basketDao.update(basketItem)
            }
        }
        emit(Resource.localSuccess(data = basketItem))
    }

    fun getForItem(item: Item) = liveData(Dispatchers.IO) {
        emit(Resource.loading(data = null))
        val basketItem = getFirstForItem(item)
        emit(Resource.localSuccess(data = basketItem))
    }

    fun getAllForItemList(list: List<Item>) = liveData(Dispatchers.IO) {
        emit(Resource.loading(data = null))
        val result = ArrayList<ItemBasket>(list.size)
        list.forEach { item ->
            val basketItem = getFirstForItem(item)
            if(basketItem == null){
                result.add(ItemBasket(item = item, count = 0));
            } else {
                result.add(basketItem)
            }
        }

        emit(Resource.localSuccess(data = result))
    }

    private fun getFirstForItem(item: Item): ItemBasket?{
        var basketItems: List<ItemBasket> = basketDao.allItems
        basketItems = basketItems.filter {
            it.item == item
        }

        return if(basketItems.isEmpty()) {
            null
        } else {
            basketItems[0]
        }
    }

}
