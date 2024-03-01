/**
 * © Panov Vitaly 2023 - All Rights Reserved
 *
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 */

package ru.fl.marketplace.app.data.repository

import ru.fl.marketplace.app.data.api.ApiHelper

class AppVersionRepository(private val apiHelper: ApiHelper) {

    suspend fun checkAppVersion() = apiHelper.checkAppVersion()

}
