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
import ru.fl.marketplace.app.data.model.ItemBasket.CREATOR.ID
import ru.fl.marketplace.app.data.model.ItemBasket.CREATOR.TABLE_NAME

@Entity(
    tableName = TABLE_NAME,
    indices = [Index(value = [ID], unique = true)]
)
data class ItemBasket(
    @PrimaryKey(autoGenerate = true) @ColumnInfo(name = ID) var id: Int? = null,
    @ColumnInfo(name = ITEM) var item: Item?,
    @ColumnInfo(name = COUNT) var count: Int
) : Parcelable {

    constructor(parcel: Parcel) : this(
        parcel.readInt(),
        parcel.readParcelable(Item.javaClass.classLoader),
        parcel.readInt(),
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeInt(id!!)
        parcel.writeParcelable(item, flags)
        parcel.writeInt(count)
    }

    override fun describeContents(): Int {
        return 0
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ItemBasket

        if (id != other.id) return false
        if (item != other.item) return false
        if (count != other.count) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + (item?.hashCode() ?: 0)
        result = 31 * result + count.hashCode()

        return result
    }

    companion object CREATOR : Parcelable.Creator<ItemBasket> {

        const val TABLE_NAME = "basket"
        const val ID         = "id"
        const val ITEM       = "item"
        const val COUNT      = "count"

        override fun createFromParcel(parcel: Parcel): ItemBasket {
            return ItemBasket(parcel)
        }

        override fun newArray(size: Int): Array<ItemBasket?> {
            return arrayOfNulls(size)
        }
    }
}
