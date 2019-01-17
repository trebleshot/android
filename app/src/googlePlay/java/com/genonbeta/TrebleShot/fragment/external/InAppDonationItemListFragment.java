package com.genonbeta.TrebleShot.fragment.external;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.anjlab.android.iab.v3.BillingProcessor;
import com.anjlab.android.iab.v3.SkuDetails;
import com.anjlab.android.iab.v3.TransactionDetails;
import com.genonbeta.TrebleShot.R;
import com.genonbeta.TrebleShot.config.AppConfig;
import com.genonbeta.TrebleShot.util.AppUtils;
import com.genonbeta.android.framework.app.DynamicRecyclerViewFragment;
import com.genonbeta.android.framework.util.MathUtils;
import com.genonbeta.android.framework.widget.RecyclerViewAdapter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

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

        mBillingProcessor = BillingProcessor.newBillingProcessor(getActivity(), AppConfig.KEY_GOOGLE_PUBLIC, this);
        mBillingProcessor.initialize();
    }

    @Override
    public RecyclerView.LayoutManager onLayoutManager()
    {
        int sizeScale = isScreenLarge() ? 3 : 2;
        int rotationScale = isScreenLandscape() ? 2 : 1;

        return new GridLayoutManager(getContext(), sizeScale * rotationScale);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState)
    {
        super.onViewCreated(view, savedInstanceState);

        useEmptyActionButton(getString(R.string.butn_retry), new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                refreshList();
            }
        });
        setEmptyImage(R.drawable.ic_favorite_white_24dp);
        setEmptyText(getString(R.string.mesg_noInternetConnection));
    }

    @Override
    public DefaultAdapter onAdapter()
    {
        final AppUtils.QuickActions<RecyclerViewAdapter.ViewHolder> quickActions = new AppUtils.QuickActions<RecyclerViewAdapter.ViewHolder>()
        {
            @Override
            public void onQuickActions(final RecyclerViewAdapter.ViewHolder clazz)
            {
                clazz.getView().setOnClickListener(new View.OnClickListener()
                {
                    @Override
                    public void onClick(View v)
                    {
                        final SkuDetails skuDetails = getAdapter().getList().get(clazz.getAdapterPosition());

                        if (getContext() == null)
                            return;

                        mBillingProcessor.purchase(getActivity(), skuDetails.productId, "Donations are not real world items");
                    }
                });
            }
        };


        return new DefaultAdapter(getContext())
        {
            @NonNull
            @Override
            public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType)
            {
                return AppUtils.quickAction(super.onCreateViewHolder(parent, viewType), quickActions);
            }
        };
    }

    @Override
    public void onProductPurchased(@NonNull String productId, @Nullable TransactionDetails details)
    {
        showToast(R.string.mesg_donationSuccessful);
    }

    @Override
    public void onPurchaseHistoryRestored()
    {

    }

    @Override
    public void onBillingError(int errorCode, @Nullable Throwable error)
    {
        showToast(R.string.mesg_somethingWentWrong);
    }

    @Override
    public void onBillingInitialized()
    {
        refreshList();
    }

    private void showToast(final int stringRes)
    {
        new Handler(Looper.getMainLooper()).post(new Runnable()
        {
            @Override
            public void run()
            {
                if (getContext() != null)
                    Toast.makeText(getContext(), stringRes, Toast.LENGTH_LONG).show();
            }
        });
    }

    public class DefaultAdapter extends RecyclerViewAdapter<SkuDetails, RecyclerViewAdapter.ViewHolder>
    {
        private List<SkuDetails> mList = new ArrayList<>();

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
            TextView text2 = holder.getView().findViewById(R.id.text2);
            TextView text3 = holder.getView().findViewById(R.id.text3);

            int lastParenthesis = details.title.lastIndexOf(" (");

            text1.setText(lastParenthesis != -1 ? details.title.substring(0, lastParenthesis) : details.title);
            text2.setText(details.description);
            text3.setText(details.priceText);
        }

        @Override
        public int getItemCount()
        {
            return mList.size();
        }

        @Override
        public List<SkuDetails> onLoad()
        {
            ArrayList<SkuDetails> returnedList = new ArrayList<>();

            if (mBillingProcessor.isInitialized() && mBillingProcessor.isOneTimePurchaseSupported()) {
                Log.d(TAG, "Configuration is OK");

                ArrayList<String> items = new ArrayList<>();

                items.add("trebleshot.donation.1");
                items.add("trebleshot.donation.2");
                items.add("trebleshot.donation.3");
                items.add("trebleshot.donation.4");
                items.add("trebleshot.donation.5");
                items.add("trebleshot.donation.6");

                // For testing purposes
                //items.add("android.test.purchased");

                List<SkuDetails> skuDetails = null;

                try {
                    skuDetails = mBillingProcessor.getPurchaseListingDetails(items);
                } catch (Exception e) {
                    e.printStackTrace();
                }

                if (skuDetails != null) {
                    Collections.sort(skuDetails, new Comparator<SkuDetails>()
                    {
                        @Override
                        public int compare(SkuDetails o1, SkuDetails o2)
                        {
                            return MathUtils.compare(o1.priceLong, o2.priceLong);
                        }
                    });

                    returnedList.addAll(skuDetails);
                }
            }

            return returnedList;
        }

        @Override
        public void onUpdate(List<SkuDetails> passedItem)
        {
            synchronized (getList()) {
                getList().clear();
                getList().addAll(passedItem);
            }
        }

        @Override
        public List<SkuDetails> getList()
        {
            return mList;
        }
    }
}
