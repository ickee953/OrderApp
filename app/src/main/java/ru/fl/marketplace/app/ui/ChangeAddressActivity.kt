/**
 * © Panov Vitaly 2023 - All Rights Reserved
 *
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 */

package ru.fl.marketplace.app.ui

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.text.Spannable
import android.text.TextWatcher
import android.text.style.ForegroundColorSpan
import android.view.*
import android.widget.Toast
import androidx.activity.viewModels
import androidx.annotation.ColorInt
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.widget.doAfterTextChanged
import androidx.lifecycle.flowWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import com.yandex.mapkit.*
import com.yandex.mapkit.geometry.BoundingBox
import com.yandex.mapkit.geometry.Circle
import com.yandex.mapkit.geometry.Point
import com.yandex.mapkit.map.*
import com.yandex.mapkit.map.Map
import com.yandex.mapkit.search.*
import com.yandex.runtime.image.ImageProvider
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import ru.fl.marketplace.app.R
import ru.fl.marketplace.app.data.viewmodel.*
import ru.fl.marketplace.app.databinding.ActivityChangeAdressBinding
import ru.fl.marketplace.app.databinding.ItemSuggestBinding
import kotlin.math.sqrt


class ChangeAddressActivity: AppCompatActivity() {
    private var _binding: ActivityChangeAdressBinding? = null
    // This property is only valid between onCreateView and
    // onDestroyView.
    val binding get() = _binding!!

    companion object {
        private val START_POSITION = CameraPosition(Point(55.753284, 37.622034), 15.0f, 0f, 0f)
    }

    private lateinit var editQueryTextWatcher: TextWatcher

    //private val map by lazy { binding.mapview.mapWindow.map }
    private val viewModel: MapViewModel by viewModels()

    private val suggestAdapter = SuggestsListAdapter()

    private lateinit var searchItems: List<SearchResponseItem>

    private val cameraListener = CameraListener { _, cameraPosition, reason, _ ->
        // Updating current visible region to apply research on map moved by user gestures.
        if (reason == CameraUpdateReason.GESTURES) {
            viewModel.setVisibleRegion(binding.mapview.map.visibleRegion)
            binding.mapview.map.mapObjects.clear()

            val point = binding.mapview.screenToWorld(ScreenPoint(
                binding.mapPin.x + binding.mapPin.width *0.5f,
                binding.mapPin.y + binding.mapPin.height
            ))

            viewModel.startSearch(point)
        }
    }

    /*
        public Bitmap drawSimpleBitmap(String number) {
            int picSize = {5f};
            Bitmap bitmap = Bitmap.createBitmap(picSize, picSize, Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(bitmap);
            // отрисовка плейсмарка
            Paint paint = new Paint();
            paint.setColor(Color.Green);
            paint.setStyle(Paint.Style.FILL);
            canvas.drawCircle(picSize / 2, picSize / 2, picSize / 2, paint);
            // отрисовка текста
            paint.setColor(Color.WHITE);
            paint.setAntiAlias(true);
            paint.setTextSize({Нужный размер текста});
            paint.setTextAlign(Paint.Align.CENTER);
            canvas.drawText(number, picSize / 2,
                picSize / 2 - ((paint.descent() + paint.ascent()) / 2), paint);
            return bitmap;
        }

        var source: Bitmap =
            BitmapFactory.decodeResource(context.getResources(), R.drawable.your_icon_name)

        // создаем mutable копию, чтобы можно было рисовать поверх
        var bitmap = source.copy(Bitmap.Config.ARGB_8888, true)

        // инициализируем канвас
        var canvas: Canvas = Canvas(bitmap)
        // рисуем текст на канвасе аналогично примеру выше

        А затем отрисовать с помощью

        addPlacemark(getPoint(),
        ImageProvider.fromBitmap(drawSimpleBitmap(number))
    */
    private fun drawSelectionCircle(point: Point){
        val circle = Circle(
            point,
            100f
        )

        binding.mapview.map.mapObjects.addCircle(
            circle,
            ContextCompat.getColor(this@ChangeAddressActivity, R.color.text_secondary),
            2f,
            ContextCompat.getColor(this@ChangeAddressActivity, R.color.bg_alpha)
        )
    }

    private fun fillAddress(geoObject: GeoObject?){
        geoObject.let {

            val city = it?.metadataContainer?.getItem(ToponymObjectMetadata::class.java)
                ?.address
                ?.components
                ?.firstOrNull { it.kinds.contains(Address.Component.Kind.LOCALITY) }
                ?.name

            val street = it?.metadataContainer?.getItem(ToponymObjectMetadata::class.java)
                ?.address
                ?.components
                ?.firstOrNull { it.kinds.contains(Address.Component.Kind.STREET) }
                ?.name

            val house = it?.metadataContainer?.getItem(ToponymObjectMetadata::class.java)
                ?.address
                ?.components
                ?.firstOrNull { it.kinds.contains(Address.Component.Kind.HOUSE) }
                ?.name

            var addrStr = it?.name
            if( city != null && street != null && house != null ){
                addrStr = "$city, $street, $house"
            }

            if(addrStr == null){
                binding.address.setText("")
                binding.saveAdress.visibility = View.GONE
            } else {
                binding.address.setText(addrStr)
                binding.saveAdress.visibility = View.VISIBLE
                binding.saveAdress.setOnClickListener {
                    val intent = Intent()
                    intent.putExtra("address", addrStr)
                    setResult(Activity.RESULT_OK, intent) // It is used to set the RESULT OK and a custom data values which we wants to send back.

                    finish()
                }
            }
        }
    }

    private fun selectAddress(geoObject: GeoObject?, point: Point ){

        drawSelectionCircle(point)

        fillAddress(geoObject)

        focusCamera(
            arrayListOf(point),
            binding.mapview.map.visibleRegion.toBoundingBox()
        )
    }

    private val searchResultPlacemarkTapListener = MapObjectTapListener { mapObject, point ->
        val selectedGeoObject = (mapObject.userData as? GeoObject)
        selectAddress(selectedGeoObject, point)

        true
    }


    private val sizeChangedListener = SizeChangedListener { _, _, _ -> updateFocusRect() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        _binding = ActivityChangeAdressBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()

        binding.saveAdress.visibility = View.GONE

        binding.mapview.map.move(START_POSITION, Animation(Animation.Type.SMOOTH, 0.5f)) {
            val point = binding.mapview.screenToWorld(ScreenPoint(
                binding.mapPin.x + binding.mapPin.width *0.5f,
                binding.mapPin.y + binding.mapPin.height
            ))

            viewModel.startSearch(point)
        }

        binding.mapview.map.addCameraListener(cameraListener)

        viewModel.setVisibleRegion(binding.mapview.map.visibleRegion)

        binding.mapview.mapWindow.addSizeChangedListener(sizeChangedListener)
        updateFocusRect()

        binding.listSuggests.adapter = suggestAdapter

        editQueryTextWatcher = binding.address.doAfterTextChanged { text ->
            if (text.toString() == viewModel.uiState.value.query) return@doAfterTextChanged
            viewModel.setQueryText(text.toString())
        }

        binding.address.setOnEditorActionListener { _, _, _ ->
            viewModel.startSearch()
            true
        }

        viewModel.uiState
            .flowWithLifecycle(lifecycle)
            .onEach {

                if (it.suggestState is SuggestState.Error) {
                    Toast.makeText(this, "Suggest error, check your network connection", Toast.LENGTH_LONG).show()
                }

                suggestAdapter.items =
                    (it.suggestState as? SuggestState.Success)?.items ?: emptyList()

                if (it.searchState is SearchState.Success) {

                    val successSearchState = it.searchState as? SearchState.Success
                    searchItems = successSearchState?.items ?: emptyList()
                    updateSearchResponsePlacemarks(searchItems)
                    if (successSearchState?.zoomToItems == true) {
                        focusCamera(
                            searchItems.map { item -> item.point },
                            successSearchState.itemsBoundingBox
                        )
                    }

                }

                if (it.searchState is SearchState.SuccessByPoint) {
                    val successSearchState = it.searchState as? SearchState.SuccessByPoint
                    searchItems = successSearchState?.items ?: emptyList()

                    val pinScreenPoint = ScreenPoint(binding.mapPin.x + binding.mapPin.width * 0.5f, binding.mapPin.y + binding.mapPin.height)

                    if(searchItems.isNotEmpty()){
                        var minIndex = 0

                        val distLongitude = searchItems[0].point.longitude - binding.mapview.screenToWorld(pinScreenPoint).longitude
                        val distLatitude = searchItems[0].point.latitude - binding.mapview.screenToWorld(pinScreenPoint).latitude

                        var minDistancePrev = sqrt( distLongitude * distLongitude + distLatitude * distLatitude )

                        searchItems.forEachIndexed { index, item ->
                            if( index > 0 ){
                                val distLongitude = item.point.longitude - binding.mapview.screenToWorld(pinScreenPoint).longitude
                                val distLatitude = item.point.latitude - binding.mapview.screenToWorld(pinScreenPoint).latitude

                                val minDistanceCur = sqrt( distLongitude * distLongitude + distLatitude * distLatitude )

                                if( minDistanceCur < minDistancePrev ){
                                    minDistancePrev = minDistanceCur
                                    minIndex = index
                                }
                            }
                        }

                        selectAddress(searchItems[minIndex].geoObject, searchItems[minIndex].point)

                        //viewModel.searchSession?.cancel()
                        //viewModel.searchSession = null
                        viewModel.searchState.value = SearchState.Off
                        viewModel.resetSuggest()
                        //viewModel.suggestState.value = SuggestState.Off

                    }
                }

                binding.apply {
                    binding.address.apply {
                        if (text.toString() != it.query) {
                            removeTextChangedListener(editQueryTextWatcher)
                            setText(it.query)
                            addTextChangedListener(editQueryTextWatcher)
                        }
                    }
                }

            }
            .launchIn(lifecycleScope)

        viewModel.subscribeForSuggest().flowWithLifecycle(lifecycle).launchIn(lifecycleScope)
        viewModel.subscribeForSearch().flowWithLifecycle(lifecycle).launchIn(lifecycleScope)

    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                finish()
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onStop() {
        binding.mapview.onStop()
        MapKitFactory.getInstance().onStop()
        super.onStop()
    }

    override fun onStart() {
        super.onStart()
        MapKitFactory.getInstance().onStart()
        binding.mapview.onStart()
    }

    private fun focusCamera(points: List<Point>, boundingBox: BoundingBox) {
        if (points.isEmpty()) return

        val position = if (points.size == 1) {
            binding.mapview.map.cameraPosition.run {
                CameraPosition(points.first(), 17.0f, azimuth, tilt)
            }
        } else {
            binding.mapview.map.cameraPosition(boundingBox)
        }

        binding.mapview.map.move(position, Animation(Animation.Type.SMOOTH, 0.5f), null)
    }

    private fun updateSearchResponsePlacemarks(items: List<SearchResponseItem>) {
        binding.mapview.map.mapObjects.clear()

        if(items.size == 1){
            selectAddress(items[0].geoObject, items[0].point)
        } else {
            val imageProvider = ImageProvider.fromResource(applicationContext, R.drawable.map_pin)

            items.forEach {
                binding.mapview.map.mapObjects.addPlacemark(
                    it.point,
                    imageProvider
                ).apply {
                    addTapListener(searchResultPlacemarkTapListener)
                    userData = it.geoObject
                }
            }
        }
    }

    private fun updateFocusRect() {
        val horizontal = resources.getDimension(R.dimen.content_margin_min)
        val vertical = resources.getDimension(R.dimen.content_margin_min)
        val window = binding.mapview.mapWindow

        window.focusRect = ScreenRect(
            ScreenPoint(horizontal, vertical),
            ScreenPoint(window.width() - horizontal, window.height() - vertical),
        )
    }
}

data class SearchResponseItem(
    val point: Point,
    val geoObject: GeoObject?,
)

class SuggestHolder(
    private val binding: ItemSuggestBinding
) : RecyclerView.ViewHolder(binding.root) {

    private val spanColor = ContextCompat.getColor(binding.root.context, android.R.color.black)

    fun bind(item: SuggestHolderItem) = with(binding) {
        textTitle.text = item.title.toSpannable(spanColor)
        textSubtitle.text = item.subtitle?.toSpannable(spanColor)
        binding.root.setOnClickListener { item.onClick() }
    }
}

class SuggestsListAdapter : RecyclerView.Adapter<SuggestHolder>() {
    var items: List<SuggestHolderItem> = emptyList()
        set(value) {
            field = value
            notifyDataSetChanged()
        }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SuggestHolder {
        return SuggestHolder(
            ItemSuggestBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        )
    }

    override fun getItemCount() = items.size

    override fun onBindViewHolder(holder: SuggestHolder, position: Int) =
        holder.bind(items[position])
}
