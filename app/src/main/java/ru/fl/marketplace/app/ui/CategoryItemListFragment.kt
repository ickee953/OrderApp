/**
 * © Panov Vitaly 2023 - All Rights Reserved
 *
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 */

package ru.fl.marketplace.app.ui

import android.os.Bundle
import android.view.*
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProviders
import androidx.navigation.fragment.findNavController
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.adapter.FragmentViewHolder
import com.google.android.material.tabs.TabLayoutMediator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import ru.fl.marketplace.app.R
import ru.fl.marketplace.app.data.model.Item
import ru.fl.marketplace.app.data.model.ItemCategory
import ru.fl.marketplace.app.data.viewmodel.BasketViewModel
import ru.fl.marketplace.app.data.viewmodel.ItemViewModel
import ru.fl.marketplace.app.databinding.FragmentCategoryItemListBinding
import ru.fl.marketplace.app.utils.Refreshable
import ru.fl.marketplace.app.utils.Status

class CategoryItemListFragment : Fragment(), Refreshable {
    companion object {
        const val ARG_ITEMS_LIST = "items_list"
    }

    private lateinit var viewModel:         ItemViewModel
    private lateinit var basketViewModel:   BasketViewModel
    private lateinit var viewPagerAdapter:  ViewPagerAdapter

    private var items : List<Item> = ArrayList()

    private var _binding: FragmentCategoryItemListBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)

        /*val items: List<Item>? = requireArguments().getParcelableArrayList<Item>(ARG_ITEMS)

        items?.let{
            setupCategoriesAdapter(it)
            viewPagerAdapter.notifyDataSetChanged()
        }*/
    }

    override fun onResume() {
        super.onResume()

        viewModel       = ViewModelProviders.of(this)[ItemViewModel::class.java]
        basketViewModel = ViewModelProviders.of(this)[BasketViewModel::class.java]
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.top_app_bar, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem) = when (item.itemId) {
        R.id.basket_item -> {
            findNavController().navigate(R.id.show_item_basket)

            true
        }

        else -> {
            // If we got here, the user's action was not recognized.
            // Invoke the superclass to handle it.
            super.onOptionsItemSelected(item)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentCategoryItemListBinding.inflate(inflater, container, false)

        return binding.root
    }

    override fun onViewStateRestored(savedInstanceState: Bundle?) {
        super.onViewStateRestored(savedInstanceState)

        if(savedInstanceState == null || items.isEmpty()){
            MainScope().launch ( Dispatchers.Main ) {
                loadItems()
            }
        } else if(savedInstanceState.containsKey(ARG_ITEMS_LIST)){
            items = savedInstanceState.getParcelableArrayList(ARG_ITEMS_LIST)!!
            setupCategoriesAdapter(items)
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putParcelableArrayList(ARG_ITEMS_LIST, items as ArrayList<Item>)
        super.onSaveInstanceState(outState)
    }

    private fun setupCategoriesAdapter( list: List<Item> ){

        val itemsMap: Map<ItemCategory, MutableList<Item>> = convertResponse( list )
        viewPagerAdapter = ViewPagerAdapter(activity as MainActivity, itemsMap.toMutableMap())

        val categoryTab     = binding.tabLayout
        val tabViewPager    = binding.tabViewpager

        tabViewPager.adapter = viewPagerAdapter

        TabLayoutMediator(categoryTab, tabViewPager) { tab, position ->
            tab.text = itemsMap.keys.elementAt(position).name
        }.attach()

    }

    private fun loadItems(){

        var loaded = false
        viewModel.getItems().observe(viewLifecycleOwner) { resource ->
            resource?.let { resource ->
                when (resource.status) {
                    Status.SUCCESS_REMOTE, Status.SUCCESS_LOCAL -> {
                        //update ui
                        binding.progressBar.visibility = View.GONE

                        resource.data?.let {items->
                            loaded = true
                            setupCategoriesAdapter(items)
                            /*basketViewModel.getAllForItemList(items)
                                .observe(viewLifecycleOwner) {resource->
                                when(resource.status) {
                                    Status.SUCCESS_LOCAL ->{
                                        resource.data?.let {
                                            setupCategoriesAdapter(it)
                                        }
                                    }
                                    else -> {}
                                }
                            }*/
                        }
                    }
                    Status.ERROR -> {
                        if(!loaded){
                            binding.progressBar.visibility = View.GONE

                            if (items.isNullOrEmpty()) (activity as MainActivity).onNetworkError(
                                resource.message
                            )
                        }
                    }
                    Status.LOADING -> {
                        binding.progressBar.visibility = View.VISIBLE
                    }
                }
            }
        }

    }

    private fun convertResponse( itemList: List<Item> ): MutableMap<ItemCategory, MutableList<Item>>{

        val itemsMap : MutableMap<ItemCategory, MutableList<Item>> = HashMap()

        val iterator = itemList.sortedByDescending { item->
            item.rating
        }.listIterator()

        while(iterator.hasNext()){
            val item = iterator.next()
            item.let { item->
                if( item.category != null ){
                    if( itemsMap[item.category] == null )
                        itemsMap[item.category!!] = ArrayList()

                    itemsMap[item.category]!!.add( item )
                }
            }
        }

        return itemsMap
    }

    override fun refresh() {
        MainScope().launch ( Dispatchers.Main ) {
            loadItems()
        }
    }

    class ViewPagerAdapter(private var activity: AppCompatActivity, private var itemsMap: MutableMap<ItemCategory, MutableList<Item>>): FragmentStateAdapter( activity ){

        override fun getItemCount(): Int {
            itemsMap.let {
                return it.keys.size
            }
            return 0;
        }

        override fun createFragment(position: Int): Fragment {
            itemsMap.let {
                var category = it.keys.elementAt(position)
                var items = it[ category ]

                return ItemListFragment.getInstance(items as ArrayList<Item>?)
            }
            return ItemListFragment.getInstance(ArrayList<Item>())
        }

        override fun onBindViewHolder(
            holder: FragmentViewHolder,
            position: Int,
            payloads: MutableList<Any>
        ) {
            val tag = "f" + holder.itemId

            val fragment: Fragment? = activity.supportFragmentManager.findFragmentByTag(tag)

            if (fragment != null) {
                if(fragment is ItemListFragment){
                    (fragment as ItemListFragment).refresh()
                }
            } else {
                // fragment might be null, if it`s call of notifyDatasetChanged()
                // which is updates whole list, not specific fragment
                super.onBindViewHolder(holder, position, payloads)
            }
        }
    }

}
