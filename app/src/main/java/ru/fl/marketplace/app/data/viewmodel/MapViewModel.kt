/**
 * © Panov Vitaly 2023 - All Rights Reserved
 *
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 */

package ru.fl.marketplace.app.data.viewmodel

import android.text.Spannable
import android.text.style.ForegroundColorSpan
import androidx.annotation.ColorInt
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yandex.mapkit.SpannableString
import com.yandex.mapkit.geometry.BoundingBox
import com.yandex.mapkit.geometry.Geometry
import com.yandex.mapkit.geometry.Point
import com.yandex.mapkit.map.VisibleRegion
import com.yandex.mapkit.map.VisibleRegionUtils
import com.yandex.mapkit.search.Response
import com.yandex.mapkit.search.SearchFactory
import com.yandex.mapkit.search.SearchManagerType
import com.yandex.mapkit.search.SearchOptions
import com.yandex.mapkit.search.Session
import com.yandex.mapkit.search.Session.SearchListener
import com.yandex.mapkit.search.SuggestItem
import com.yandex.mapkit.search.SuggestOptions
import com.yandex.mapkit.search.SuggestSession
import com.yandex.mapkit.search.SuggestType
import com.yandex.runtime.Error
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import ru.fl.marketplace.app.ui.*
import kotlin.time.Duration.Companion.seconds

class MapViewModel : ViewModel() {
    private val searchManager =
        SearchFactory.getInstance().createSearchManager(SearchManagerType.COMBINED)
    var searchSession: Session? = null
    private val suggestSession: SuggestSession = searchManager.createSuggestSession()
    private var zoomToSearchResult = false

    private val region = MutableStateFlow<VisibleRegion?>(null)

    @OptIn(FlowPreview::class)
    private val throttledRegion = region.debounce(1.seconds)
    private val query = MutableStateFlow("")
    val searchState = MutableStateFlow<SearchState>(SearchState.Off)
    val suggestState = MutableStateFlow<SuggestState>(SuggestState.Off)

    val uiState: StateFlow<MapUiState> = combine(
        query,
        searchState,
        suggestState,
    ) { query, searchState, suggestState ->
        MapUiState(
            query = query,
            searchState = searchState,
            suggestState = suggestState,
        )
    }.stateIn(viewModelScope, SharingStarted.Lazily, MapUiState())

    fun setQueryText(value: String) {
        query.value = value
    }

    fun setVisibleRegion(region: VisibleRegion) {
        this.region.value = region
    }

    fun startSearch(point : Point){
        submitSearch(point)
    }

    fun startSearch(searchText: String? = null) {
        val text = searchText ?: query.value
        if (query.value.isEmpty()) return
        val region = region.value?.let {
            VisibleRegionUtils.toPolygon(it)
        } ?: return

        submitSearch(text, region)
    }

    fun reset() {
        searchSession?.cancel()
        searchSession = null
        searchState.value = SearchState.Off
        resetSuggest()
        query.value = ""
    }

    /**
     * Resubmitting suggests when query, region or searchState changes.
     */
    fun subscribeForSuggest(): Flow<*> {
        return combine(
            query,
            throttledRegion,
            searchState,
        ) { query, region, searchState ->
            if (query.isNotEmpty() && region != null && searchState == SearchState.Off) {
                submitSuggest(query, region.toBoundingBox())
            } else {
                resetSuggest()
            }
        }
    }

    /**
     * Performs the search again when the map position changes.
     */
    fun subscribeForSearch(): Flow<*> {
        return throttledRegion.filter { it != null }
            .filter { searchState.value is SearchState.Success }
            .mapNotNull { it }
            .onEach { region ->
                searchSession?.let {
                    it.setSearchArea(VisibleRegionUtils.toPolygon(region))
                    it.resubmit(searchSessionListener)
                    searchState.value = SearchState.Loading
                    zoomToSearchResult = false
                }
            }
    }

    private fun submitUriSearch(uri: String) {
        searchSession?.cancel()
        searchManager.searchByURI(
            uri,
            SearchOptions(),
            searchSessionListener
        )
        searchState.value = SearchState.Loading
        zoomToSearchResult = true
    }

    private val searchSessionListener = object : SearchListener {
        override fun onSearchResponse(response: Response) {
            val items = response.collection.children.mapNotNull {
                val point = it.obj?.geometry?.firstOrNull()?.point ?: return@mapNotNull null
                SearchResponseItem(point, it.obj)
            }
            val boundingBox = response.metadata.boundingBox ?: return

            searchState.value = SearchState.Success(
                items,
                zoomToSearchResult,
                boundingBox,
            )
        }

        override fun onSearchError(error: Error) {
            searchState.value = SearchState.Error
        }
    }

    private val searchByPointSessionListener = object : SearchListener {
        override fun onSearchResponse(response: Response) {
            val items = response.collection.children.mapNotNull {
                val point = it.obj?.geometry?.firstOrNull()?.point ?: return@mapNotNull null
                SearchResponseItem(point, it.obj)
            }
            val boundingBox = response.metadata.boundingBox ?: return

            searchState.value = SearchState.SuccessByPoint(
                items,
                zoomToSearchResult,
                boundingBox,
            )
        }

        override fun onSearchError(error: Error) {
            searchState.value = SearchState.Error
        }
    }

    private fun submitSearch(query: String, geometry: Geometry) {
        searchSession?.cancel()
        searchSession = searchManager.submit(
            query,
            geometry,
            SearchOptions().apply {
                resultPageSize = 32
            },
            searchSessionListener
        )
        searchState.value = SearchState.Loading
        zoomToSearchResult = true
    }

    private fun submitSearch(point: Point) {
        searchSession?.cancel()
        searchSession = searchManager.submit(
            point,
            20,
            SearchOptions().apply {
                resultPageSize = 32
            },
            searchByPointSessionListener
        )
        searchState.value = SearchState.Loading
        //zoomToSearchResult = true
    }

    private val suggestSessionListener = object : SuggestSession.SuggestListener {
        override fun onResponse(suggestItems: MutableList<SuggestItem>) {
            suggestState.value = SuggestState.Success(
                suggestItems.map {
                    SuggestHolderItem(
                        title = it.title,
                        subtitle = it.subtitle,
                    ) {
                        // For Action.SUBSTITUTE we need just to substitute
                        // query text.
                        setQueryText(it.displayText ?: "")
                        // For Action.SEARCH also need to start search immediately.
                        if (it.action == SuggestItem.Action.SEARCH) {
                            val uri = it.uri
                            if (uri != null) {
                                // Search by URI if exists.
                                submitUriSearch(uri)
                            } else {
                                // Otherwise, search by searchText.
                                startSearch(it.searchText)
                            }
                        }
                    }
                }
            )
        }

        override fun onError(error: Error) {
            suggestState.value = SuggestState.Error
        }
    }

    private fun submitSuggest(
        query: String,
        box: BoundingBox,
        options: SuggestOptions = SUGGEST_OPTIONS,
    ) {
        suggestSession.suggest(query, box, options, suggestSessionListener)
        suggestState.value = SuggestState.Loading
    }

    fun resetSuggest() {
        suggestSession.reset()
        suggestState.value = SuggestState.Off
    }

    companion object {
        private const val SUGGEST_NUMBER_LIMIT = 20
        private val SUGGEST_OPTIONS = SuggestOptions().setSuggestTypes(
            SuggestType.GEO.value
                    //or SuggestType.BIZ.value
                    //or SuggestType.TRANSIT.value
        )
    }
}

fun VisibleRegion.toBoundingBox() = BoundingBox(bottomLeft, topRight)

data class MapUiState(
    val query: String = "",
    val searchState: SearchState = SearchState.Off,
    val suggestState: SuggestState = SuggestState.Off,
)

sealed interface SearchState {
    object Off : SearchState
    object Loading : SearchState
    object Error : SearchState
    data class Success(
        val items: List<SearchResponseItem>,
        val zoomToItems: Boolean,
        val itemsBoundingBox: BoundingBox,
    ) : SearchState

    data class SuccessByPoint(
        val items: List<SearchResponseItem>,
        val zoomToItems: Boolean,
        val itemsBoundingBox: BoundingBox,
    ) : SearchState
}

data class SuggestHolderItem(
    val title: SpannableString,
    val subtitle: SpannableString?,
    val onClick: () -> Unit,
)

sealed interface SuggestState {
    object Off : SuggestState
    object Loading : SuggestState
    object Error : SuggestState
    data class Success(val items: List<SuggestHolderItem>) : SuggestState
}

fun SearchState.toTextStatus(): String {
    return when (this) {
        SearchState.Error -> "Error"
        SearchState.Loading -> "Loading"
        SearchState.Off -> "Off"
        is SearchState.Success -> "Success"
        is SearchState.SuccessByPoint -> "SuccessByPoint"
    }
}

fun SpannableString.toSpannable(@ColorInt color: Int): Spannable {
    val spannableString = android.text.SpannableString(text)
    spans.forEach {
        spannableString.setSpan(
            ForegroundColorSpan(color),
            it.begin,
            it.end,
            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )
    }
    return spannableString
}
