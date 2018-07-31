package com.genonbeta.android.framework.preference;

import android.content.SharedPreferences;
import android.support.annotation.Nullable;

import java.util.Map;
import java.util.Set;

/**
 * created by: veli
 * date: 31.03.2018 16:36
 */
public class SuperPreferences
		implements SharedPreferences
{
	public static final String KEY_SYNC_TIME = "__SYNC_TIME";

	private SharedPreferences mWeakManager;
	private OnPreferenceUpdateListener mUpdateListener;

	public SuperPreferences(SharedPreferences preferences)
	{
		mWeakManager = preferences;
	}

	public SharedPreferences getWeakManager()
	{
		return mWeakManager;
	}

	@Override
	public Map<String, ?> getAll()
	{
		return mWeakManager.getAll();
	}

	@Nullable
	@Override
	public String getString(String key, @Nullable String defValue)
	{
		return mWeakManager.getString(key, defValue);
	}

	@Nullable
	@Override
	public Set<String> getStringSet(String key, @Nullable Set<String> defValues)
	{
		return mWeakManager.getStringSet(key, defValues);
	}

	@Override
	public int getInt(String key, int defValue)
	{
		return mWeakManager.getInt(key, defValue);
	}

	@Override
	public long getLong(String key, long defValue)
	{
		return mWeakManager.getLong(key, defValue);
	}

	@Override
	public float getFloat(String key, float defValue)
	{
		return mWeakManager.getFloat(key, defValue);
	}

	@Override
	public boolean getBoolean(String key, boolean defValue)
	{
		return mWeakManager.getBoolean(key, defValue);
	}

	@Override
	public boolean contains(String key)
	{
		return mWeakManager.contains(key);
	}

	@Override
	public Editor edit()
	{
		return new SuperEditor(mWeakManager.edit());
	}

	@Override
	public void registerOnSharedPreferenceChangeListener(OnSharedPreferenceChangeListener listener)
	{
		mWeakManager.registerOnSharedPreferenceChangeListener(listener);
	}

	@Override
	public void unregisterOnSharedPreferenceChangeListener(OnSharedPreferenceChangeListener listener)
	{
		mWeakManager.unregisterOnSharedPreferenceChangeListener(listener);
	}

	public void setOnPreferenceUpdateListener(OnPreferenceUpdateListener updateListener)
	{
		mUpdateListener = updateListener;
	}

	public class SuperEditor implements Editor
	{
		private Editor mEditor;

		public SuperEditor(Editor editor)
		{
			mEditor = editor;
		}

		@Override
		public Editor putString(String key, @Nullable String value)
		{
			mEditor.putString(key, value);
			return this;
		}

		@Override
		public Editor putStringSet(String key, @Nullable Set<String> values)
		{
			mEditor.putStringSet(key, values);
			return this;
		}

		@Override
		public Editor putInt(String key, int value)
		{
			mEditor.putInt(key, value);
			return this;
		}

		@Override
		public Editor putLong(String key, long value)
		{
			mEditor.putLong(key, value);
			return this;
		}

		@Override
		public Editor putFloat(String key, float value)
		{
			mEditor.putFloat(key, value);
			return this;
		}

		@Override
		public Editor putBoolean(String key, boolean value)
		{
			mEditor.putBoolean(key, value);
			return this;
		}

		@Override
		public Editor remove(String key)
		{
			mEditor.remove(key);
			return this;
		}

		@Override
		public Editor clear()
		{
			mEditor.clear();
			return this;
		}

		@Override
		public boolean commit()
		{
			mEditor.putLong(KEY_SYNC_TIME, System.currentTimeMillis());

			boolean result = mEditor.commit();

			if (mUpdateListener != null)
				mUpdateListener.onPreferenceUpdate(SuperPreferences.this, false);

			return result;
		}

		@Override
		public void apply()
		{
			mEditor.putLong(KEY_SYNC_TIME, System.currentTimeMillis());
			mEditor.apply();

			if (mUpdateListener != null)
				mUpdateListener.onPreferenceUpdate(SuperPreferences.this, false);
		}
	}

	public interface OnPreferenceUpdateListener
	{
		void onPreferenceUpdate(SuperPreferences superPreferences, boolean commit);
	}
}
