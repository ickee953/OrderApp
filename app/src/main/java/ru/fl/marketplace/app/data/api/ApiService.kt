/**
 * © Panov Vitaly 2023 - All Rights Reserved
 *
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 */

package ru.fl.marketplace.app.data.api

import ru.fl.marketplace.app.data.model.Item
import retrofit2.http.GET
import retrofit2.http.Path
import ru.fl.marketplace.app.data.model.AppVersion

interface ApiService {

    @GET("items/list")
    suspend fun getItems(): List<Item>

    @GET("items/{id}")
    suspend fun getItem( @Path(value = "id") id: String?): Item

    @GET("items/{id}/view_cnt_inc")
    suspend fun viewCountInc( @Path(value = "id") id: String? ): Item

    @GET("items/{id}/rate/{rating}")
    suspend fun rate(
        @Path(value = "id") id: String?,
        @Path(value = "rating") rating: Int?
    ): Item

    @GET("app/check")
    suspend fun checkAppVersion(): AppVersion
}
