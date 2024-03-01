/**
 * © Panov Vitaly 2023 - All Rights Reserved
 *
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 */

package ru.fl.marketplace.app.data.model

import android.os.Parcel
import android.os.Parcelable
import com.google.gson.annotations.SerializedName

data class Rating(
    @SerializedName("oneStarCount") var oneStarCount    : Int,
    @SerializedName("twoStarCount") var twoStarCount    : Int,
    @SerializedName("threeStarCount") var threeStarCount  : Int,
    @SerializedName("fourStarCount") var fourStarCount   : Int,
    @SerializedName("fiveStarCount") var fiveStarCount   : Int
) : Parcelable, Comparable<Rating> {

    constructor(parcel : Parcel) : this(
        parcel.readInt(),
        parcel.readInt(),
        parcel.readInt(),
        parcel.readInt(),
        parcel.readInt()
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeInt( oneStarCount )
        parcel.writeInt( twoStarCount )
        parcel.writeInt( threeStarCount )
        parcel.writeInt( fourStarCount )
        parcel.writeInt( fiveStarCount )
    }

    override fun describeContents(): Int {
        return 0
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Rating

        if (oneStarCount != other.oneStarCount) return false
        if (twoStarCount != other.twoStarCount) return false
        if (threeStarCount != other.threeStarCount) return false
        if (fourStarCount != other.fourStarCount) return false
        if (fiveStarCount != other.fiveStarCount) return false

        return true
    }

    override fun hashCode(): Int {
        var result = oneStarCount.hashCode()
        result = 31 * result + twoStarCount
        result = 31 * result + threeStarCount
        result = 31 * result + fourStarCount
        result = 31 * result + fiveStarCount
        return result
    }

    companion object CREATOR : Parcelable.Creator<Rating>{

        override fun createFromParcel(parcel: Parcel): Rating {
            return Rating(parcel)
        }

        override fun newArray(size: Int): Array<Rating?> {
            return arrayOfNulls(size)
        }
    }

    fun overageRating() : Double {
        var overageRating = 0.0

        var divider = oneStarCount + twoStarCount + threeStarCount + fourStarCount + fiveStarCount
        overageRating = if( divider == 0 ){
            0.0
        } else {
            (( oneStarCount + 2 * twoStarCount + 3 * threeStarCount + 4 * fourStarCount + 5 * fiveStarCount ) / divider).toDouble()
        }

        return overageRating
    }

    override fun compareTo(other: Rating): Int {
        return this.overageRating().compareTo(other.overageRating())
    }
}
