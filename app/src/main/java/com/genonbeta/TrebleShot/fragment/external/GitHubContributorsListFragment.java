package com.genonbeta.TrebleShot.fragment.external;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.util.Base64;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.genonbeta.TrebleShot.R;
import com.genonbeta.TrebleShot.app.ListViewFragment;
import com.genonbeta.TrebleShot.config.AppConfig;
import com.genonbeta.TrebleShot.widget.ListViewAdapter;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;

import velitasali.updatewithgithub.RemoteServer;

/**
 * created by: Veli
 * date: 16.03.2018 15:46
 */

public class GitHubContributorsListFragment extends ListViewFragment<GitHubContributorsListFragment.ContributorObject, GitHubContributorsListFragment.ContributorListAdapter>
{
	@Override
	public ContributorListAdapter onAdapter()
	{
		return new ContributorListAdapter(getContext());
	}

	@Override
	protected ListView onListView(View mainContainer, ViewGroup listViewContainer)
	{
		NonScrollListView listView = new NonScrollListView(getContext());

		listView.setId(R.id.customListFragment_listView);
		listView.setDividerHeight(0);

		listView.setLayoutParams(new AbsListView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
				ViewGroup.LayoutParams.MATCH_PARENT));

		listViewContainer.addView(listView);

		return listView;
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

	@Override
	public void onListItemClick(ListView l, View v, int position, long id)
	{
		super.onListItemClick(l, v, position, id);

		ContributorObject contributorObject = (ContributorObject) getAdapter().getItem(position);

		startActivity(new Intent(Intent.ACTION_VIEW)
				.setData(Uri.parse(String.format(AppConfig.URI_GITHUB_PROFILE, contributorObject.name))));
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

	public static class ContributorListAdapter extends ListViewAdapter<ContributorObject>
	{
		private ArrayList<ContributorObject> mList = new ArrayList<>();

		public ContributorListAdapter(Context context)
		{
			super(context);
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
		public ArrayList<ContributorObject> getList()
		{
			return mList;
		}

		@Override
		public int getCount()
		{
			return getList().size();
		}

		@Override
		public Object getItem(int i)
		{
			return getList().get(i);
		}

		@Override
		public long getItemId(int i)
		{
			return 0;
		}

		@Override
		public View getView(int i, View view, ViewGroup viewGroup)
		{
			if (view == null)
				view = getInflater().inflate(R.layout.list_contributors, viewGroup, false);

			ContributorObject contributorObject = (ContributorObject) getItem(i);

			TextView textView = view.findViewById(R.id.text);

			textView.setText(contributorObject.name);

			return view;
		}
	}

	public class NonScrollListView extends ListView
	{
		public NonScrollListView(Context context)
		{
			super(context);
		}

		public NonScrollListView(Context context, AttributeSet attrs)
		{
			super(context, attrs);
		}

		public NonScrollListView(Context context, AttributeSet attrs, int defStyle)
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
