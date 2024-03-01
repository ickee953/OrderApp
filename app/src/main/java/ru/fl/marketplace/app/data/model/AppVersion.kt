/**
 * © Panov Vitaly 2023 - All Rights Reserved
 *
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 */

package ru.fl.marketplace.app.data.model

import com.google.gson.annotations.SerializedName

data class AppVersion(
    @SerializedName(LAST_VERSION) val lastVersion : String,
    @SerializedName(VERSION_CODE) val versionCode : Int,
    @SerializedName(APP_URL) val appUrl : String
) {

    companion object CREATOR {
        const val LAST_VERSION = "lastVersion"
        const val VERSION_CODE = "versionCode"
        const val APP_URL      = "appUrl"
    }

}
