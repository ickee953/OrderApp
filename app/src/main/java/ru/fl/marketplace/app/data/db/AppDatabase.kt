/**
 * © Panov Vitaly 2023 - All Rights Reserved
 *
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 */

package ru.fl.marketplace.app.data.db

import android.content.Context
import android.util.Log
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.sqlite.db.SupportSQLiteDatabase
import ru.fl.marketplace.app.data.model.ItemBasket
import ru.fl.marketplace.app.data.model.Item
import ru.fl.marketplace.app.data.model.ItemCategory
import ru.fl.marketplace.app.utils.Converters

@Database( entities = [Item::class, ItemCategory::class, ItemBasket::class], version = 1 )
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun itemsDao(): ItemDao

    abstract fun basketDao(): ItemBasketDao

    companion object {
        private var INSTANCE: AppDatabase? = null
        private const val  DB_NAME = "items.db"

        fun getDatabase( context: Context ): AppDatabase {
            if( INSTANCE == null ){
                synchronized( AppDatabase::class.java ){
                    if (INSTANCE == null) {
                        INSTANCE = Room.databaseBuilder(context.applicationContext, AppDatabase::class.java, DB_NAME)
                            .addCallback(object : Callback() {
                                override fun onCreate(db: SupportSQLiteDatabase) {
                                    super.onCreate(db)
                                    Log.d("AppDatabase", "Created local database.")
                                }
                            }).build()
                    }
                }
            }

            return INSTANCE!!
        }
    }
}
