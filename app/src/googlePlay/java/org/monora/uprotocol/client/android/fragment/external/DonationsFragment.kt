package org.monora.uprotocol.client.android.fragment.external

import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import com.anjlab.android.iab.v3.BillingProcessor
import com.anjlab.android.iab.v3.BillingProcessor.IBillingHandler
import com.anjlab.android.iab.v3.BillingProcessor.newBillingProcessor
import com.anjlab.android.iab.v3.SkuDetails
import com.anjlab.android.iab.v3.TransactionDetails
import com.genonbeta.android.framework.app.RecyclerViewFragment
import com.genonbeta.android.framework.widget.RecyclerViewAdapter
import com.genonbeta.android.framework.widget.RecyclerViewAdapter.ViewHolder
import org.monora.uprotocol.client.android.R
import org.monora.uprotocol.client.android.config.AppConfig
import org.monora.uprotocol.client.android.fragment.external.DonationsFragment.DefaultAdapter

/**
 * created by: veli
 * date: 7/15/18 10:12 PM
 */
class DonationsFragment : RecyclerViewFragment<SkuDetails, ViewHolder, DefaultAdapter>(), IBillingHandler {
    private val billingProcessor: BillingProcessor by lazy {
        newBillingProcessor(activity, AppConfig.KEY_GOOGLE_PUBLIC, this)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?,
    ): View? {
        return generateDefaultView(inflater, container, savedInstanceState)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        billingProcessor.initialize()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        adapter = DefaultAdapter(requireContext())
        emptyListImageView.setImageResource(R.drawable.ic_favorite_white_24dp)
        emptyListTextView.text = getString(R.string.mesg_noInternetConnection)
        useEmptyListActionButton(getString(R.string.butn_retry)) { refreshList() }
    }

    override fun onProductPurchased(productId: String, details: TransactionDetails?) {
        showToast(R.string.mesg_donationSuccessful)
    }

    override fun onPurchaseHistoryRestored() {

    }

    override fun onBillingError(errorCode: Int, error: Throwable?) {
        showToast(R.string.mesg_somethingWentWrong)
    }

    override fun onBillingInitialized() {
        refreshList()
    }

    private fun showToast(stringRes: Int) {
        Handler(Looper.getMainLooper()).post {
            context?.let {
                Toast.makeText(it, stringRes, Toast.LENGTH_LONG).show()
            }
        }
    }

    inner class DefaultAdapter(
        context: Context,
    ) : RecyclerViewAdapter<SkuDetails, ViewHolder>(context) {
        private val list: MutableList<SkuDetails> = ArrayList()

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            return ViewHolder(layoutInflater.inflate(R.layout.list_donations, parent, false)).also { vh ->
                vh.itemView.setOnClickListener {
                    val skuDetails: SkuDetails = list[vh.adapterPosition]
                    billingProcessor.purchase(
                        activity, skuDetails.productId, "Donations are not real world items"
                    )
                }
            }
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val details: SkuDetails = getList()[position]
            val text1: TextView = holder.itemView.findViewById(R.id.text)
            val text2: TextView = holder.itemView.findViewById(R.id.text2)
            val text3: TextView = holder.itemView.findViewById(R.id.text3)
            val lastParenthesis: Int = details.title.lastIndexOf(" (")
            text1.text = if (lastParenthesis != -1) details.title.substring(0, lastParenthesis) else details.title
            text2.text = details.description
            text3.text = details.priceText
        }

        override fun onLoad(): MutableList<SkuDetails> {
            if (list.isNotEmpty()) {
                Log.d(TAG, "Skipped reloading because the list already loaded.")
                return list
            }

            if (billingProcessor.isInitialized && billingProcessor.isOneTimePurchaseSupported) {
                Log.d(TAG, "Configuration is OK")

                val items = arrayListOf(
                    "trebleshot.donation.1",
                    "trebleshot.donation.2",
                    "trebleshot.donation.3",
                    "trebleshot.donation.4",
                    "trebleshot.donation.5",
                    "trebleshot.donation.6",
                )

                // For testing purposes
                //items.add("android.test.purchased");
                try {
                    return billingProcessor.getPurchaseListingDetails(items).toMutableList().also {
                        it.sortWith { o1: SkuDetails, o2: SkuDetails -> o1.priceLong.compareTo(o2.priceLong) }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            return mutableListOf()
        }

        override fun onUpdate(passedItem: MutableList<SkuDetails>) {
            synchronized(list) {
                list.clear()
                list.addAll(passedItem)
            }
        }

        override fun getItemCount(): Int = list.size

        override fun getList(): MutableList<SkuDetails> = list
    }

    companion object {
        private val TAG = DonationsFragment::class.simpleName
    }
}