package com.genonbeta.TrebleShot.fragment.external;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.util.Base64;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.genonbeta.TrebleShot.R;
import com.genonbeta.TrebleShot.app.RecyclerViewFragment;
import com.genonbeta.TrebleShot.config.AppConfig;
import com.genonbeta.TrebleShot.widget.RecyclerViewAdapter;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;

import velitasali.updatewithgithub.RemoteServer;

/**
 * created by: Veli
 * date: 16.03.2018 15:46
 */

public class GitHubContributorsListFragment extends RecyclerViewFragment<GitHubContributorsListFragment.ContributorObject, GitHubContributorsListFragment.TestViewHolder, GitHubContributorsListFragment.ContributorListAdapter>
{
	@Override
	public ContributorListAdapter onAdapter()
	{
		return new ContributorListAdapter(getContext());
	}

	@Override
	public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState)
	{
		super.onViewCreated(view, savedInstanceState);

		setEmptyImage(R.drawable.ic_github_circle_white_24dp);
		setEmptyText(getString(R.string.mesg_somethingWentWrong));

		getEmptyImage().setOnLongClickListener(new View.OnLongClickListener()
		{
			@Override
			public boolean onLongClick(View v)
			{
				// Here we leave a message to those who are concerned
				String ultimateMessage = new String(Base64.decode("Ik5lYnVsYSDwn4yMIiBtdXN0IHJlYWNoIEB2ZWxpdGFzYWxp", Base64.DEFAULT));
				Toast.makeText(getContext(), ultimateMessage, Toast.LENGTH_SHORT).show();

				return true;
			}
		});
	}

	public static class ContributorObject
	{
		public String name;
		public String url;
		public String urlAvatar;

		public ContributorObject(String name, String url, String urlAvatar)
		{
			this.name = name;
			this.url = url;
			this.urlAvatar = urlAvatar;
		}
	}

	public static class ContributorListAdapter extends RecyclerViewAdapter<ContributorObject, TestViewHolder>
	{
		private ArrayList<ContributorObject> mList = new ArrayList<>();

		public ContributorListAdapter(Context context)
		{
			super(context);
		}

		@NonNull
		@Override
		public TestViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType)
		{
			return new TestViewHolder(getInflater().inflate(R.layout.list_contributors, parent, false));
		}

		@Override
		public void onBindViewHolder(@NonNull TestViewHolder holder, int position)
		{
			ContributorObject contributorObject = getList().get(position);

			TextView textView = holder.getView().findViewById(R.id.text);

			textView.setText(contributorObject.name);
		}

		@Override
		public ArrayList<ContributorObject> onLoad()
		{
			ArrayList<ContributorObject> contributorObjects = new ArrayList<>();
			RemoteServer server = new RemoteServer(AppConfig.URI_REPO_APP_CONTRIBUTORS);

			try {
				String result = server.connect(null, null);

				JSONArray releases = new JSONArray(result);

				if (releases.length() > 0) {
					for (int iterator = 0; iterator < releases.length(); iterator++) {
						JSONObject currentObject = releases.getJSONObject(iterator);

						contributorObjects.add(new ContributorObject(currentObject.getString("login"),
								currentObject.getString("url"),
								currentObject.getString("avatar_url")));
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
			}

			return contributorObjects;
		}

		@Override
		public void onUpdate(ArrayList<ContributorObject> passedItem)
		{
			synchronized (getList()) {
				getList().clear();
				getList().addAll(passedItem);
			}
		}

		@Override
		public long getItemId(int i)
		{
			return 0;
		}

		@Override
		public int getItemCount()
		{
			return getList().size();
		}

		@Override
		public ArrayList<ContributorObject> getList()
		{
			return mList;
		}
	}

	public static class TestViewHolder extends RecyclerView.ViewHolder
	{
		private View mView;

		public TestViewHolder(View itemView)
		{
			super(itemView);
			mView = itemView;
		}

		public View getView()
		{
			return mView;
		}
	}

	public class NonScrollRecyclerView extends RecyclerView
	{
		public NonScrollRecyclerView(Context context)
		{
			super(context);
		}

		public NonScrollRecyclerView(Context context, AttributeSet attrs)
		{
			super(context, attrs);
		}

		public NonScrollRecyclerView(Context context, AttributeSet attrs, int defStyle)
		{
			super(context, attrs, defStyle);
		}

		@Override
		public void onMeasure(int widthMeasureSpec, int heightMeasureSpec)
		{
			int heightMeasureSpec_custom = MeasureSpec.makeMeasureSpec(
					Integer.MAX_VALUE >> 2, MeasureSpec.AT_MOST);
			super.onMeasure(widthMeasureSpec, heightMeasureSpec_custom);
			ViewGroup.LayoutParams params = getLayoutParams();
			params.height = getMeasuredHeight();
		}
	}
}
