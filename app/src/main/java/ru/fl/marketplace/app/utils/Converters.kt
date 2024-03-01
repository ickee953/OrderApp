/**
 * © Panov Vitaly 2023 - All Rights Reserved
 *
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 */

package ru.fl.marketplace.app.utils

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import ru.fl.marketplace.app.data.model.Item
import ru.fl.marketplace.app.data.model.ItemCategory
import ru.fl.marketplace.app.data.model.Rating
import java.math.BigDecimal

class Converters {
    companion object {
        @TypeConverter
        @JvmStatic
        fun fromBigDecimal( value: BigDecimal? ): Double? {
            return value!!.toDouble()
        }

        @TypeConverter
        @JvmStatic
        fun toBigDecimal( value: Double? ): BigDecimal? {
            return BigDecimal.valueOf( value!! )
        }

        @TypeConverter
        @JvmStatic
        fun fromItemCategory( value: ItemCategory? ): String? {
            val type = object: TypeToken<ItemCategory>(){}.type
            return Gson().toJson(value, type)
        }

        @TypeConverter
        @JvmStatic
        fun toItemCategory( value: String? ): ItemCategory? {
            val type = object: TypeToken<ItemCategory>(){}.type
            return Gson().fromJson(value, type)
        }

        @TypeConverter
        @JvmStatic
        fun fromItem( value: Item? ): String?{
            val type = object: TypeToken<Item>(){}.type
            return Gson().toJson(value, type)
        }

        @TypeConverter
        @JvmStatic
        fun toItem( value: String? ): Item?{
            val type = object: TypeToken<Item>(){}.type
            return Gson().fromJson(value, type)
        }

        @TypeConverter
        @JvmStatic
        fun fromRating( value: Rating? ): String? {
            val type = object: TypeToken<Rating>(){}.type
            return Gson().toJson(value, type)
        }

        @TypeConverter
        @JvmStatic
        fun toRating( value: String? ): Rating? {
            val type = object: TypeToken<Rating>(){}.type
            return Gson().fromJson( value, type )
        }

    }
}
