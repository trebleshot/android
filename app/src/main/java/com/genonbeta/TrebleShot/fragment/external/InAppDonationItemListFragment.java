package com.genonbeta.TrebleShot.fragment.external;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.anjlab.android.iab.v3.BillingProcessor;
import com.anjlab.android.iab.v3.SkuDetails;
import com.anjlab.android.iab.v3.TransactionDetails;
import com.genonbeta.TrebleShot.R;
import com.genonbeta.TrebleShot.app.DynamicRecyclerViewFragment;
import com.genonbeta.TrebleShot.config.AppConfig;
import com.genonbeta.TrebleShot.widget.RecyclerViewAdapter;

import java.util.ArrayList;

/**
 * created by: veli
 * date: 7/15/18 10:12 PM
 */
public class InAppDonationItemListFragment
		extends DynamicRecyclerViewFragment<SkuDetails, RecyclerViewAdapter.ViewHolder, InAppDonationItemListFragment.DefaultAdapter>
		implements BillingProcessor.IBillingHandler
{
	private static final String TAG = InAppDonationItemListFragment.class.getSimpleName();

	private BillingProcessor mBillingProcessor;

	@Override
	public void onCreate(@Nullable Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);

		mBillingProcessor = BillingProcessor.newBillingProcessor(getActivity(), AppConfig.KEY_GOOLE_PUBLIC, this);
		mBillingProcessor.initialize();
	}

	@Override
	public RecyclerView.LayoutManager onLayoutManager()
	{
		int sizeScale = isScreenLarge() ? 4 : 2;
		int rotationScale = isScreenLandscape() ? 2 : 1;

		return new GridLayoutManager(getContext(), sizeScale * rotationScale);
	}

	@Override
	public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState)
	{
		super.onViewCreated(view, savedInstanceState);

		useEmptyActionButton(true);
		getEmptyActionButton().setText(R.string.butn_retry_all);
		getEmptyActionButton().setOnClickListener(new View.OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				refreshList();
			}
		});
	}

	@Override
	public DefaultAdapter onAdapter()
	{
		return new DefaultAdapter(getContext());
	}

	@Override
	public void onProductPurchased(@NonNull String productId, @Nullable TransactionDetails details)
	{

	}

	@Override
	public void onPurchaseHistoryRestored()
	{

	}

	@Override
	public void onBillingError(int errorCode, @Nullable Throwable error)
	{

	}

	@Override
	public void onBillingInitialized()
	{
		refreshList();
	}

	public class DefaultAdapter extends RecyclerViewAdapter<SkuDetails, RecyclerViewAdapter.ViewHolder>
	{
		private ArrayList<SkuDetails> mList = new ArrayList<>();

		public DefaultAdapter(Context context)
		{
			super(context);
		}

		@NonNull
		@Override
		public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType)
		{
			return new ViewHolder(getInflater().inflate(R.layout.list_donations, parent, false));
		}

		@Override
		public void onBindViewHolder(@NonNull ViewHolder holder, int position)
		{
			SkuDetails details = getList().get(position);
			TextView text1 = holder.getView().findViewById(R.id.text);

			text1.setText(details.title);
		}

		@Override
		public int getItemCount()
		{
			return mList.size();
		}

		@Override
		public ArrayList<SkuDetails> onLoad()
		{
			ArrayList<SkuDetails> returnedList = new ArrayList<>();

			if (mBillingProcessor.isInitialized() && mBillingProcessor.isOneTimePurchaseSupported()) {
				Log.d(TAG, "Configuration is OK");

				ArrayList<String> items = new ArrayList<>();

				items.add("trebleshot.donation.1");

				returnedList.addAll(mBillingProcessor.getPurchaseListingDetails(items));
			}

			return returnedList;
		}

		@Override
		public void onUpdate(ArrayList<SkuDetails> passedItem)
		{
			synchronized (getList()) {
				getList().clear();
				getList().addAll(passedItem);
			}
		}

		@Override
		public ArrayList<SkuDetails> getList()
		{
			return mList;
		}
	}
}
