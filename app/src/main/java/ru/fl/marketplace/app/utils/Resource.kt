package ru.fl.marketplace.app.utils

data class Resource<out T>(val status: Status, val data: T?, val message: String?) {
    companion object {
        fun <T> remoteSuccess(data: T): Resource<T> = Resource(status = Status.SUCCESS_REMOTE, data = data, message = null)

        fun <T> localSuccess(data: T): Resource<T> = Resource(status = Status.SUCCESS_LOCAL, data = data, message = null)

        fun <T> error(data: T?, message: String): Resource<T> =
            Resource(status = Status.ERROR, data = data, message = message)

        fun <T> loading(data: T?): Resource<T> = Resource(status = Status.LOADING, data = data, message = null)
    }
}
