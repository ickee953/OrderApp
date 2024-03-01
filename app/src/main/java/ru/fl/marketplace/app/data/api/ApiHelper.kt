/**
 * © Panov Vitaly 2023 - All Rights Reserved
 *
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 */

package ru.fl.marketplace.app.data.api

class ApiHelper( private val apiService: ApiService) {

    suspend fun getItems() = apiService.getItems()

    suspend fun getItem( id: String? ) = apiService.getItem( id )

    suspend fun viewCountInc( id: String? ) = apiService.viewCountInc( id )

    suspend fun rate( id: String?, rating: Int? ) = apiService.rate( id, rating )

    suspend fun checkAppVersion() = apiService.checkAppVersion()
}
