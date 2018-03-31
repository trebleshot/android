package com.genonbeta.android.database;

import android.app.ActionBar;

import java.util.HashMap;

/**
 * created by: Veli
 * date: 10.10.2017 17:31
 */

public class SQLValues
{
	private HashMap<String, Table> mIndex = new HashMap<>();

	public Table defineTable(String name)
	{
		Table table = new Table(name);
		getTables().put(name, table);

		return table;
	}

	public Table defineTable(String name, boolean mayExist)
	{
		Table table = new Table(name, mayExist);
		getTables().put(name, table);

		return table;
	}

	public HashMap<String, Table> getTables()
	{
		return mIndex;
	}

	public static class Column
	{
		private String mName;
		private String mExtra;
		private String mValue;
		private SQLType mType;
		private boolean mNullable;

		public Column(String columnName, Object value)
		{
			setName(columnName);
			setValue(String.valueOf(value));
		}

		public Column(String columnName, SQLType type, boolean mNullable)
		{
			setName(columnName);
			setType(type);
			setNullable(mNullable);
		}

		public Column(String columnName, SQLType type, boolean mNullable, String extra)
		{
			this(columnName, type, mNullable);
			setExtra(extra);
		}

		public String getExtra()
		{
			return mExtra;
		}

		public String getName()
		{
			return mName;
		}

		public SQLType getType()
		{
			return mType;
		}

		public String getValue()
		{
			return mValue;
		}

		public boolean isNullable()
		{
			return mNullable;
		}

		public Column setExtra(String mExtra)
		{
			this.mExtra = mExtra;
			return this;
		}

		public Column setName(String mName)
		{
			this.mName = mName;
			return this;
		}

		public Column setNullable(boolean mNullable)
		{
			this.mNullable = mNullable;
			return this;
		}

		public Column setType(SQLType mType)
		{
			this.mType = mType;
			return this;
		}

		public void setValue(String mValue)
		{
			this.mValue = mValue;
		}

		@Override
		public String toString()
		{
			return "`" + getName() + "` " + getType().toString() + " " + ((isNullable()) ? "null" : "not null") + ((getExtra() != null) ? " " + mExtra : "");
		}
	}

	public static class Table
	{
		private String mName;
		private HashMap<String, Column> mColumns = new HashMap<>();
		private boolean mMayExist;

		public Table(String name)
		{
			setName(name);
		}

		public Table(String name, boolean mayExist) {
			this(name);
			mMayExist = mayExist;
		}

		public boolean columnExist(String columnName)
		{
			return mColumns.containsKey(columnName);
		}

		public Table define(Column column)
		{
			getColumns().put(column.getName(), column);
			return this;
		}

		public Column getColumn(String name)
		{
			return getColumns().get(name);
		}

		public HashMap<String, Column> getColumns()
		{
			return mColumns;
		}

		public String getName()
		{
			return mName;
		}

		public boolean mayExist()
		{
			return mMayExist;
		}

		public void setName(String mName)
		{
			this.mName = mName;
		}
	}
}
