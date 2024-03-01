/**
 * © Panov Vitaly 2023 - All Rights Reserved
 *
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 */

package ru.fl.marketplace.app.data.repository

import ru.fl.marketplace.app.data.api.ApiHelper

class ItemRepository(private val apiHelper: ApiHelper) {

    suspend fun getItems() = apiHelper.getItems()

    suspend fun getItem( id: String? ) = apiHelper.getItem( id )

    suspend fun viewCountInc( id: String? ) = apiHelper.viewCountInc( id )

    suspend fun rate( id: String?, rating: Int? ) = apiHelper.rate( id, rating )
}
