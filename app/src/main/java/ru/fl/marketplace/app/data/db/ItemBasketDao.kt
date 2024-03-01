/**
 * © Panov Vitaly 2023 - All Rights Reserved
 *
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 */

package ru.fl.marketplace.app.data.db

import androidx.room.*
import ru.fl.marketplace.app.data.model.ItemBasket

@Dao
interface ItemBasketDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert( basket: ItemBasket)

    @Update(onConflict = OnConflictStrategy.REPLACE)
    fun update( basket: ItemBasket)

    @Delete
    fun delete( basket: ItemBasket )

    @Query("DELETE FROM basket")
    fun deleteAll()

    @get:Query("SELECT * FROM basket")
    val allItems: List<ItemBasket>
}
