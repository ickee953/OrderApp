/**
 * © Panov Vitaly 2023 - All Rights Reserved
 *
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 */

package ru.fl.marketplace.app.ui

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProviders
import androidx.navigation.findNavController
import ru.fl.marketplace.app.R
import ru.fl.marketplace.app.databinding.FragmentItemListBinding
import ru.fl.marketplace.app.data.model.Item
import ru.fl.marketplace.app.data.model.ItemBasket
import ru.fl.marketplace.app.data.viewmodel.BasketViewModel

class ItemListFragment(private val updateUICallback: ItemsAdapter.UpdateUICallBack? = null): Fragment() {
    private lateinit var itemAdapter : ItemsAdapter

    companion object {
        const val ARG_ITEMS = "items"

        fun getInstance( items: List<Item>?, updateUICallback: ItemsAdapter.UpdateUICallBack? = null): Fragment{
            var bundle = Bundle()
            bundle.putParcelableArrayList( ARG_ITEMS, items as ArrayList<Item>)
            var fragment = ItemListFragment(updateUICallback)
            fragment.arguments = bundle

            return fragment
        }
    }

    private var _binding: FragmentItemListBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    @SuppressLint("NotifyDataSetChanged")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val items = requireArguments().getParcelableArrayList<Item>(ARG_ITEMS)

        items?.let{
            itemAdapter = ItemsAdapter(it, View.OnClickListener { itemView ->

                // Click Listener to trigger navigation based

                var item = itemView.tag as Item
                val bundle = Bundle()
                bundle.putParcelable(
                    ItemDetailFragment.ARG_ITEM,
                    item
                )
                itemView.findNavController().navigate(R.id.show_item_detail, bundle)
            }, ViewModelProviders.of(this)[BasketViewModel::class.java], updateUICallback)
        }
    }

    fun refresh(){
        itemAdapter.notifyDataSetChanged()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentItemListBinding.inflate(inflater, container, false)

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.recyclerItemsView.adapter = itemAdapter
    }
}
