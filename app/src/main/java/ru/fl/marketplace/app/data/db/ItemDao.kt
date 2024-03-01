/**
 * © Panov Vitaly 2023 - All Rights Reserved
 *
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 */

package ru.fl.marketplace.app.data.db

import androidx.room.*
import ru.fl.marketplace.app.data.model.Item

@Dao
interface ItemDao {
    @Query("SELECT * FROM item WHERE id = :id LIMIT 1")
    fun findById( id : String ): Item?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert( item: Item)

    @Update(onConflict = OnConflictStrategy.REPLACE)
    fun update( item: Item)

    @Query("DELETE FROM item")
    fun deleteAll()

    @get:Query("SELECT * FROM item ORDER BY title ASC")
    val allItems: List<Item>
}
