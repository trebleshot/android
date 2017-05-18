package com.genonbeta.TrebleShot.dialog;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;

import com.genonbeta.TrebleShot.R;
import com.genonbeta.TrebleShot.adapter.PendingTransferListAdapter;
import com.genonbeta.TrebleShot.database.MainDatabase;
import com.genonbeta.TrebleShot.database.Transaction;
import com.genonbeta.TrebleShot.helper.AwaitedFileReceiver;
import com.genonbeta.TrebleShot.service.CommunicationService;
import com.genonbeta.TrebleShot.service.ServerService;
import com.genonbeta.android.database.CursorItem;

/**
 * Created by: veli
 * Date: 5/18/17 6:09 PM
 */

public class PendingTransferListDialog extends Dialog
{
	public PendingTransferListDialog(final Context context, final Transaction transaction, int groupId)
	{
		super(context, android.R.style.Theme_Light_NoTitleBar);
		initialize(context, transaction, new PendingTransferListAdapter(getContext(), groupId));
	}

	public PendingTransferListDialog(final Context context, final Transaction transaction, String deviceId)
	{
		super(context, android.R.style.Theme_Light_NoTitleBar);
		initialize(context, transaction, new PendingTransferListAdapter(getContext(), deviceId));
	}

	private void initialize(final Context context, final Transaction transaction, final PendingTransferListAdapter adapter)
	{
		requestWindowFeature(Window.FEATURE_NO_TITLE);

		View view = LayoutInflater.from(getContext()).inflate(R.layout.layout_transaction_editor, null);
		TextView closeTextView = (TextView) view.findViewById(R.id.layout_transaction_editor_close_text);
		ListView listListView = (ListView) view.findViewById(R.id.layout_transaction_editor_list_main);

		listListView.setAdapter(adapter);
		listListView.setDividerHeight(0);

		closeTextView.setOnClickListener(new View.OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				cancel();
			}
		});

		listListView.setOnItemClickListener(new AdapterView.OnItemClickListener()
		{
			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id)
			{
				CursorItem thisItem = (CursorItem) adapter.getItem(position);

				if (thisItem.getInt(MainDatabase.FIELD_TRANSFER_TYPE) == MainDatabase.TYPE_TRANSFER_TYPE_INCOMING)
				{
					AwaitedFileReceiver receiver = new AwaitedFileReceiver(thisItem);

					if (receiver.flag.equals(Transaction.Flag.INTERRUPTED))
					{
						receiver.flag = Transaction.Flag.RESUME;
						transaction.updateTransaction(receiver);
					}

					context.startService(new Intent(context, ServerService.class)
							.setAction(ServerService.ACTION_START_RECEIVING)
							.putExtra(CommunicationService.EXTRA_GROUP_ID, receiver.groupId));
				}
			}
		});

		adapter.update();
		adapter.notifyDataSetChanged();

		setContentView(view);
	}
}
