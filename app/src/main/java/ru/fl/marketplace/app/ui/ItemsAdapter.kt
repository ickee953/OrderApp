/**
 * © Panov Vitaly 2023 - All Rights Reserved
 *
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 */

package ru.fl.marketplace.app.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.fragment.NavHostFragment
import androidx.recyclerview.widget.RecyclerView
import coil.ImageLoader
import coil.load
import coil.request.CachePolicy
import coil.transform.RoundedCornersTransformation
import ru.fl.marketplace.app.BuildConfig
import ru.fl.marketplace.app.R
import ru.fl.marketplace.app.data.api.RetrofitBuilder
import ru.fl.marketplace.app.data.model.Item
import ru.fl.marketplace.app.data.model.ItemBasket
import ru.fl.marketplace.app.data.viewmodel.BasketViewModel
import ru.fl.marketplace.app.utils.Status


class ItemsAdapter(private val items: List<Item>,
                   private val onClickListener: View.OnClickListener,
                   private val viewModel: BasketViewModel,
                   private val callback: UpdateUICallBack? = null
                   ) : RecyclerView.Adapter<ItemsAdapter.ItemViewHolder>(){

    inner class ItemViewHolder(view : View) : RecyclerView.ViewHolder(view) {
        var tittleTextView              : TextView = view.findViewById(R.id.title)
        var imageView                   : ImageView = view.findViewById(R.id.image_view)
        var priceTextView               : TextView = view.findViewById(R.id.price)
        var shortDescriptionTextView    : TextView = view.findViewById(R.id.short_description)
        val imgLoader = ImageLoader.Builder(view.context)
            .memoryCachePolicy(CachePolicy.ENABLED)
            .diskCachePolicy(CachePolicy.ENABLED)
            .respectCacheHeaders(false)
            .build()
        val productCountView            : View        = view.findViewById(R.id.product_count_view)
        val addToBasketButton           : ImageButton = view.findViewById(R.id.add_to_card)
        val incProductButton            : ImageButton = view.findViewById(R.id.product_count_inc)
        val decProductButton            : ImageButton = view.findViewById(R.id.product_count_dec)
        val productCountTextView        : TextView    = view.findViewById(R.id.product_count)
    }

    interface UpdateUICallBack {
        fun updateUI()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemViewHolder {
        val itemView = LayoutInflater.from(parent.context).inflate(R.layout.item, parent, false)
        return ItemViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: ItemViewHolder, position: Int) {

        val item = items[ position ]

        item.let { item->
            //update item from basket
            viewModel.getForItem(item).observe(holder.itemView.context as AppCompatActivity){
                when(it.status){
                    Status.SUCCESS_LOCAL -> {
                        updateViewHolder(holder, it.data)
                    }
                    else -> {}
                }
            }

            if( item.titlePicUrl != null && item.titlePicUrl != ""){
                val picUrl =
                    ( if(BuildConfig.DEBUG) RetrofitBuilder.DEBUG_URL else RetrofitBuilder.RELEASE_URL ) + item.titlePicUrl

                holder.imageView.load( picUrl, holder.imgLoader ){
                    transformations(RoundedCornersTransformation(25F))
                }
            } else {
                holder.imageView.load(R.drawable.ic_baseline_art_track_24){
                    transformations(RoundedCornersTransformation(25F))
                }
            }

            holder.tittleTextView.text              = item.title
            holder.priceTextView.text               = item.price.toDouble().toString() + " ₽"
            holder.shortDescriptionTextView.text    = item.descriptionShort

            holder.itemView.tag = item
            holder.itemView.setOnClickListener(onClickListener)

            holder.addToBasketButton.setOnClickListener{ button ->
                viewModel.addItem(item, 1).observe( button.context as AppCompatActivity) {
                    when(it.status) {
                        Status.SUCCESS_LOCAL -> {
                            updateViewHolder(holder, it.data!!)
                        }

                        else -> {}
                    }
                }
            }

            holder.incProductButton.setOnClickListener { button ->
                viewModel.addItem(item, 1).observe( button.context as AppCompatActivity) {
                    when(it.status) {
                        Status.SUCCESS_LOCAL -> {
                            updateViewHolder(holder, it.data)
                            callback?.updateUI()
                        }

                        else -> {}
                    }
                }
            }

            holder.decProductButton.setOnClickListener { button ->
                viewModel.removeItem(item, 1).observe(button.context as AppCompatActivity) {
                    when(it.status) {
                        Status.SUCCESS_LOCAL -> {
                            updateViewHolder(holder, it.data)
                            callback?.updateUI()
                        }

                        else -> {}
                    }
                }
            }
        }
    }

    override fun getItemCount(): Int {
        return items.size
    }

    private fun updateViewHolder(holder: ItemViewHolder, item: ItemBasket?) {
        if(item != null && item.count > 0) {
            holder.addToBasketButton.visibility = View.GONE
            holder.productCountView.visibility = View.VISIBLE

            val count = item.count

            holder.productCountTextView.text = count.toString()
        } else {
            holder.addToBasketButton.visibility = View.VISIBLE
            holder.productCountView.visibility = View.GONE
        }
    }
}
