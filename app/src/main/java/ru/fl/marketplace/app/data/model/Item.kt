/**
 * © Panov Vitaly 2023 - All Rights Reserved
 *
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 */

package ru.fl.marketplace.app.data.model

import android.os.Parcel
import android.os.Parcelable
import androidx.room.*
import com.google.gson.annotations.SerializedName
import ru.fl.marketplace.app.data.model.Item.CREATOR.ID
import ru.fl.marketplace.app.data.model.Item.CREATOR.TABLE_NAME
import java.math.BigDecimal

@Entity(
    tableName = TABLE_NAME,
    indices = [Index(value = [ID], unique = true)]
)
data class Item(
    @SerializedName(ID) @PrimaryKey @ColumnInfo(name = ID) var id: String,
    @SerializedName(TITLE) @ColumnInfo(name = TITLE) var title: String?,
    @SerializedName(TITLE_PIC) @ColumnInfo(name = TITLE_PIC) var titlePicUrl: String?,
    @SerializedName(DESCRIPTION) @ColumnInfo(name = DESCRIPTION) var description: String?,
    @SerializedName(DESCRIPTION_SHORT) @ColumnInfo(name = DESCRIPTION_SHORT) var descriptionShort: String?,
    @SerializedName(PRICE) @ColumnInfo(name = PRICE) var price: BigDecimal,
    @SerializedName(VIEW_COUNT) @ColumnInfo(name = VIEW_COUNT) var viewCount:  Int,
    @SerializedName(RATING) @Embedded var rating: Rating?,
    @SerializedName(CATEGORY) @ColumnInfo(name = CATEGORY) var category: ItemCategory?
) : Parcelable {

    constructor(parcel: Parcel) : this(
        parcel.readString()!!,
        parcel.readString(),
        parcel.readString(),
        parcel.readString(),
        parcel.readString(),
        BigDecimal.valueOf(parcel.readDouble()),
        parcel.readInt(),
        parcel.readParcelable(Rating.javaClass.classLoader),
        parcel.readParcelable(ItemCategory.javaClass.classLoader)
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(id)
        parcel.writeString(title)
        parcel.writeString(titlePicUrl)
        parcel.writeString(description)
        parcel.writeString(descriptionShort)
        parcel.writeDouble(price.toDouble())
        parcel.writeInt(viewCount)
        parcel.writeParcelable(rating, flags)
        parcel.writeParcelable(category, flags)
    }

    override fun describeContents(): Int {
        return 0
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Item

        if (id != other.id) return false
        if (title != other.title) return false
        if (titlePicUrl != other.titlePicUrl) return false
        if (description != other.description) return false
        if (descriptionShort != other.descriptionShort) return false
        if (price.toDouble() != other.price.toDouble()) return false
        //if (viewCount != other.viewCount) return false
        if (rating != other.rating) return false
        if (category != other.category) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + (title?.hashCode() ?: 0)
        result = 31 * result + (titlePicUrl?.hashCode() ?: 0)
        result = 31 * result + (description?.hashCode() ?: 0)
        result = 31 * result + (descriptionShort?.hashCode() ?: 0)
        result = 31 * result + price.hashCode()
        //result = 31 * result + viewCount
        result = 31 * result + (rating?.hashCode() ?: 0)
        result = 31 * result + (category?.hashCode() ?: 0)
        return result
    }

    companion object CREATOR : Parcelable.Creator<Item> {

        const val TABLE_NAME        = "item"
        const val ID                = "id"
        const val TITLE             = "title"
        const val TITLE_PIC         = "titlePicUrl"
        const val DESCRIPTION       = "description"
        const val DESCRIPTION_SHORT = "descriptionShort"
        const val PRICE             = "price"
        const val VIEW_COUNT        = "viewCount"
        const val RATING            = "rating"
        const val CATEGORY          = "category"

        override fun createFromParcel(parcel: Parcel): Item {
            return Item(parcel)
        }

        override fun newArray(size: Int): Array<Item?> {
            return arrayOfNulls(size)
        }
    }
}
