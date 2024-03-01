/**
 * © Panov Vitaly 2023 - All Rights Reserved
 *
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 */

package ru.fl.marketplace.app.ui

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProviders
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import ru.fl.marketplace.app.databinding.FragmentItemDetailBinding
import ru.fl.marketplace.app.data.model.Item
import ru.fl.marketplace.app.data.viewmodel.ItemViewModel
import ru.fl.marketplace.app.utils.Refreshable
import ru.fl.marketplace.app.utils.Status
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import ru.fl.marketplace.app.R

class ItemDetailFragment : Fragment(), Refreshable {
    companion object {
        const val ARG_ITEM = "item"
    }

    private var item: Item? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(false)

        arguments?.let {
            if(it.containsKey(ARG_ITEM)){
                item = it.getParcelable(ARG_ITEM)
            }
        }
    }

    private var _binding: FragmentItemDetailBinding? = null
    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentItemDetailBinding.inflate(inflater, container, false)

        binding.fab.setOnClickListener{
            val shareIntent = Intent()
            shareIntent.action = Intent.ACTION_SEND
            shareIntent.type="text/plain"
            val text = item!!.title + " : " + item!!.price + "₽\n" + item!!.descriptionShort
            shareIntent.putExtra(Intent.EXTRA_TEXT, text)
            startActivity(Intent.createChooser(shareIntent, getString(R.string.sharing_title)))
        }

        val singleItems = arrayOf("1 star", "2 star", "3 star", "4 star", "5 star")
        val checkedItem = 3
        var rating = checkedItem + 1

        binding.rateBtn.setOnClickListener{
            MaterialAlertDialogBuilder(requireContext())
                .setTitle(resources.getString(R.string.rate_dlg_title))
                //.setMessage(resources.getString(R.string.rate_dlg_message))
                .setSingleChoiceItems( singleItems, checkedItem ){ dialog, which->
                    rating = which + 1
                }
                .setNeutralButton(resources.getString(R.string.later)) { dialog, which ->
                    // Respond to negative button press
                }
                .setPositiveButton(resources.getString(R.string.rate)) { dialog, which ->
                    val viewModel = ViewModelProviders.of(this).get(ItemViewModel::class.java)

                    viewModel.rate( item!!.id, rating ).observe( viewLifecycleOwner) { resource ->
                        when (resource.status) {
                            Status.SUCCESS_REMOTE -> updateUI(resource.data!!)
                            Status.ERROR -> Toast.makeText(
                                requireContext(),
                                resource.message,
                                Toast.LENGTH_LONG


                            ).show()
                            else -> {}
                        }
                    }
                }
                .show()
        }

        /*binding.addToShoppingCard.setOnClickListener {
            it.visibility = View.GONE
            binding.productCountView.visibility = View.VISIBLE
        }*/

        return binding.root
    }

    override fun onResume() {
        super.onResume()

        (activity as MainActivity).showUpToolbar()
    }

    override fun onViewStateRestored(savedInstanceState: Bundle?) {
        super.onViewStateRestored(savedInstanceState)
        if(savedInstanceState == null){
            MainScope().launch ( Dispatchers.Main ) {
                loadItem()
            }
        } else {
            if(savedInstanceState.containsKey(ARG_ITEM)){
                val item: Item? = savedInstanceState.getParcelable(ARG_ITEM)
                if (item != null) {
                    updateUI(item)
                }
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putParcelable(ARG_ITEM, item)
        super.onSaveInstanceState(outState)
    }

    private fun updateUI(item : Item){
        binding.itemPrice.text = "₽" + item.price.toDouble()
        binding.itemTitle.text = item.title
        binding.shortDescription.text = item.descriptionShort
        if ( item.description == null || "" == item.description) {
            binding.descriptionTitle.visibility = View.GONE
        } else {
            binding.description.text = item.description
        }

        var overageRating = item.rating?.overageRating()
        binding.itemRating.text = overageRating.toString()
        //calculate stars
        var starCount = overageRating?.toInt()
        when( starCount ){
            1 -> {
                binding.itemRatingStars.text = "●"
                setRatingVisibility(true)
            }
            2 -> {
                binding.itemRatingStars.text = "● ●"
                setRatingVisibility(true)
            }
            3 -> {
                binding.itemRatingStars.text = "● ● ●"
                setRatingVisibility(true)
            }
            4 -> {
                binding.itemRatingStars.text = "● ● ● ●"
                setRatingVisibility(true)
            }
            5 -> {
                binding.itemRatingStars.text = "● ● ● ● ●"
                setRatingVisibility(true)
            }
            else -> setRatingVisibility(false)
        }

        if( item.viewCount != null ) binding.itemViewCount.text = item.viewCount.toString()
    }

    private fun setRatingVisibility( isVisible: Boolean ){
        when(isVisible){
            true -> {
                binding.itemRating.visibility = View.VISIBLE
                binding.itemRatingStars.visibility = View.VISIBLE
            }
            else -> {
                binding.itemRating.visibility = View.GONE
                binding.itemRatingStars.visibility = View.GONE
            }
        }
    }

    private fun loadItem() {

        val viewModel = ViewModelProviders.of(this).get(ItemViewModel::class.java)

        //increment view count
        viewModel.viewCountInc( item!!.id ).observe(viewLifecycleOwner) { resource ->
            resource?.let { resource ->
                when (resource.status) {
                    Status.SUCCESS_REMOTE -> resource.data?.let {
                        item = it
                        updateUI(item!!)
                    }
                    else -> {}
                }
            }
        }

        viewModel.getItem( item!!.id ).observe(viewLifecycleOwner) { resource ->
            resource?.let { resource ->
                when (resource.status) {
                    Status.SUCCESS_REMOTE, Status.SUCCESS_LOCAL -> {
                        resource.data?.let {
                            item = it
                            updateUI(item!!)
                        }
                    }
                    Status.ERROR -> {
                        if (resource.data == null) {
                            (activity as MainActivity).onNetworkError(resource.message)
                        } else {
                            updateUI(resource.data)
                        }
                    }
                    Status.LOADING -> {

                    }
                }
            }
        }

    }

    override fun refresh() {
        MainScope().launch ( Dispatchers.Main ) {
            loadItem()
        }
    }
}
