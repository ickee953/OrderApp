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
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AutoCompleteTextView
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.fragment.app.commit
import androidx.lifecycle.ViewModelProviders
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import ru.fl.marketplace.app.R
import ru.fl.marketplace.app.data.model.Item
import ru.fl.marketplace.app.data.model.ItemBasket
import ru.fl.marketplace.app.data.viewmodel.BasketViewModel
import ru.fl.marketplace.app.databinding.FragmentBasketBinding
import ru.fl.marketplace.app.utils.Status

class BasketFragment : Fragment(), ItemsAdapter.UpdateUICallBack {

    companion object {
        const val ARG_ITEMS_LIST = "items_basket_list"
    }

    private lateinit var viewModel: BasketViewModel

    private var basketItems : List<ItemBasket> = ArrayList()
    private var items = ArrayList<Item>()

    private var _binding : FragmentBasketBinding? = null

    private val binding get() = _binding!!

    private var firstActivityResultLauncher: ActivityResultLauncher<Intent> = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val data: Intent? = result.data
            if (data != null) {
                val address = data.getStringExtra("address")
                binding.address.setText(address, false)
                binding.currentAddressLabel.visibility = View.VISIBLE
            }
        }else if (result.resultCode == Activity.RESULT_CANCELED) {
            //Log.e("Cancelled", "Cancelled")
            //Toast.makeText(this@MainActivity,"Result Cancelled",Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentBasketBinding.inflate(inflater, container, false)

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.currentAddressLabel.visibility = View.GONE

        (binding.address as? AutoCompleteTextView)?.setOnClickListener {
            //val intent = Intent(context, ChangeAddressActivity::class.java)
            //startActivity(intent)
            val intent = Intent(context, ChangeAddressActivity::class.java)
            firstActivityResultLauncher.launch(intent)
        }

        binding.gotoCheckoutBtn.setOnClickListener {
            //observer.selectImage()
            val intent = Intent(context, ChangeAddressActivity::class.java)
            firstActivityResultLauncher.launch(intent)
        }
    }

    override fun onResume() {
        super.onResume()

        viewModel = ViewModelProviders.of(this)[BasketViewModel::class.java]

        //val items = listOf("Material", "Design", "Components", "Android")
        //val adapter = ArrayAdapter(requireContext(), R.layout.address_list_item, items)
        //(binding.address as? AutoCompleteTextView)?.setAdapter(adapter)
    }

    override fun onViewStateRestored(savedInstanceState: Bundle?) {
        super.onViewStateRestored(savedInstanceState)

        if(savedInstanceState == null || basketItems.isEmpty()){
            MainScope().launch ( Dispatchers.Main ) {
                loadBasket()
            }
        } else if(savedInstanceState.containsKey(ARG_ITEMS_LIST)){
            basketItems = savedInstanceState.getParcelableArrayList(ARG_ITEMS_LIST)!!

            basketItems.stream().forEach {
                it.item?.let { it1 -> items.add(it1) }
            }

            val fragment = ItemListFragment.getInstance(items)

            if(basketItems.isNotEmpty()){
                binding.emptyBasketView.visibility = View.GONE
                //add list fragment
                activity?.supportFragmentManager?.commit {
                    replace(R.id.fragment_container_view, fragment)
                }
            } else {
                binding.emptyBasketView.visibility = View.VISIBLE
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putParcelableArrayList(CategoryItemListFragment.ARG_ITEMS_LIST, basketItems as ArrayList<Item>)
        super.onSaveInstanceState(outState)
    }

    private fun updateSummaryPrice() {
        viewModel.getBasket().observe(viewLifecycleOwner) { resource ->
            resource?.let { resource ->

                when (resource.status) {
                    Status.SUCCESS_REMOTE, Status.SUCCESS_LOCAL -> {
                        resource.data?.let {
                            var summaryPrice = 0.0
                            it.forEach { basketItem ->
                                summaryPrice += basketItem.item!!.price.toDouble() * basketItem.count
                            }
                            binding.summaryPrice.text = "Итого: $summaryPrice  ₽"
                        }
                    }
                    Status.ERROR -> {
                        binding.summaryPrice.text = "Error loading basket!"
                    }
                    Status.LOADING -> {
                        binding.summaryPrice.text = "Итого: ..."
                    }
                }
            }
        }
    }

    private fun loadBasket(){

        viewModel.getBasket().observe(viewLifecycleOwner) { resource ->
            resource?.let { resource ->

                when (resource.status) {
                    Status.SUCCESS_REMOTE, Status.SUCCESS_LOCAL -> {
                        //update ui
                        //binding.progressBar.visibility = View.GONE

                        resource.data?.let {
                            basketItems = it
                            items.clear()
                            basketItems.stream().forEach { basketItem ->
                                basketItem.item?.let { it1 -> items.add(it1) }
                            }

                            if(basketItems.isNotEmpty()){
                                val fragment = ItemListFragment.getInstance(items, this)

                                binding.emptyBasketView.visibility = View.GONE
                                updateSummaryPrice()
                                //add list fragment
                                activity?.supportFragmentManager?.commit {
                                    replace(R.id.fragment_container_view, fragment)
                                }
                            } else {
                                binding.emptyBasketView.visibility = View.VISIBLE
                            }
                        }
                    }
                    Status.ERROR -> {
                        //binding.progressBar.visibility = View.GONE

                        if (basketItems.isNullOrEmpty()) (activity as MainActivity).onNetworkError(
                            resource.message
                        )

                    }
                    Status.LOADING -> {
                        //binding.progressBar.visibility = View.VISIBLE
                    }
                }
            }
        }
    }

    override fun updateUI() {
        MainScope().launch ( Dispatchers.Main ) {
            updateSummaryPrice()
        }
    }
}
