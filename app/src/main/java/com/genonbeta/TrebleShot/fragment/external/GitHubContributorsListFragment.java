package com.genonbeta.TrebleShot.fragment.external;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.RecyclerView;
import android.util.Base64;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.genonbeta.TrebleShot.R;
import com.genonbeta.TrebleShot.app.DynamicRecyclerViewFragment;
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

public class GitHubContributorsListFragment
		extends DynamicRecyclerViewFragment<GitHubContributorsListFragment.ContributorObject, RecyclerViewAdapter.ViewHolder, GitHubContributorsListFragment.ContributorListAdapter>
{
	@Override
	public ContributorListAdapter onAdapter()
	{
		return new ContributorListAdapter(getContext())
		{
			@Override
			public void onBindViewHolder(@NonNull ViewHolder holder, int position)
			{
				super.onBindViewHolder(holder, position);

				final ContributorObject contributorObject = getAdapter().getList().get(position);

				holder.getView().setOnClickListener(new View.OnClickListener()
				{
					@Override
					public void onClick(View v)
					{
						if (getContext() == null)
							return;

						getContext().startActivity(new Intent(Intent.ACTION_VIEW)
								.setData(Uri.parse(String.format(AppConfig.URI_GITHUB_PROFILE, contributorObject.name))));
					}
				});
			}
		};
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

	public static class ContributorListAdapter extends RecyclerViewAdapter<ContributorObject, RecyclerViewAdapter.ViewHolder>
	{
		private ArrayList<ContributorObject> mList = new ArrayList<>();

		public ContributorListAdapter(Context context)
		{
			super(context);
		}

		@NonNull
		@Override
		public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType)
		{
			return new ViewHolder(getInflater().inflate(R.layout.list_contributors, parent, false));
		}

		@Override
		public void onBindViewHolder(@NonNull ViewHolder holder, int position)
		{
			final ContributorObject contributorObject = getList().get(position);
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
}
