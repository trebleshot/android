package com.genonbeta.TrebleShot.database;

import android.content.ContentValues;

import com.genonbeta.android.database.CursorItem;
import com.genonbeta.android.database.SQLQuery;

/**
 * created by: Veli
 * date: 2.11.2017 21:31
 */

public interface FlexibleObject
{
	SQLQuery.Select getWhere();

	ContentValues getValues();

	void reconstruct(CursorItem item);

	void onCreateObject(AccessDatabase database);

	void onUpdateObject(AccessDatabase database);

	void onRemoveObject(AccessDatabase database);
}
