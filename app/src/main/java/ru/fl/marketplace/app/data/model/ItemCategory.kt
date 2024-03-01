/**
 * © Panov Vitaly 2023 - All Rights Reserved
 *
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 */

package ru.fl.marketplace.app.data.model

import android.os.Parcel
import android.os.Parcelable
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.google.gson.annotations.SerializedName
import ru.fl.marketplace.app.data.model.ItemCategory.CREATOR.ID
import ru.fl.marketplace.app.data.model.ItemCategory.CREATOR.TABLE_NAME

@Entity(
    tableName = TABLE_NAME,
    indices = [Index(value = [ID], unique = true)]
)
data class ItemCategory (
    @SerializedName("id") @PrimaryKey @ColumnInfo(name = ID) var id: String,
    @SerializedName("name") @ColumnInfo(name = NAME) var name: String
) : Parcelable {

    constructor( parcel: Parcel ) : this (
        parcel.readString()!!,
        parcel.readString()!!
    )

    override fun describeContents(): Int {
        return 0
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString( id )
        parcel.writeString( name )
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ItemCategory

        if (id != other.id) return false
        if (name != other.name) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + name.hashCode()
        return result
    }

    companion object CREATOR : Parcelable.Creator<ItemCategory> {

        const val TABLE_NAME = "item_category"
        const val ID         = "id"
        const val NAME       = "name"

        override fun createFromParcel(parcel: Parcel): ItemCategory {
            return ItemCategory(parcel)
        }

        override fun newArray(size: Int): Array<ItemCategory?> {
            return arrayOfNulls(size)
        }
    }
}
