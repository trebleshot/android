package com.genonbeta.TrebleShot.object;

import android.annotation.SuppressLint;
import android.content.ContentValues;

import com.genonbeta.TrebleShot.database.AccessDatabase;
import com.genonbeta.TrebleShot.util.FileUtils;
import com.genonbeta.android.database.CursorItem;
import com.genonbeta.android.database.DatabaseObject;
import com.genonbeta.android.database.SQLQuery;
import com.genonbeta.android.database.SQLiteDatabase;
import com.genonbeta.android.framework.io.DocumentFile;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.ANNOTATION_TYPE;
import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.LOCAL_VARIABLE;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.PACKAGE;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.RetentionPolicy.CLASS;

/**
 * Created by: veli
 * Date: 4/24/17 11:50 PM
 */

public class TransferObject
        implements DatabaseObject<TransferGroup>, Editable
{
    public String friendlyName;
    public String file;
    public String fileMimeType;
    public String directory;
    public String deviceId;
    public long requestId;
    public long groupId;
    public long skippedBytes;
    public long fileSize = 0;
    public int accessPort;
    public Type type = Type.INCOMING;
    public Flag flag = Flag.PENDING;

    private boolean mIsSelected = false;

    public TransferObject()
    {
    }

    public TransferObject(long requestId, long groupId, String friendlyName, String file, String fileMime, long fileSize, Type type)
    {
        this(requestId, groupId, null, friendlyName, file, fileMime, fileSize, type);
    }

    public TransferObject(long requestId, long groupId, String deviceId, String friendlyName, String file, String fileMime, long fileSize, Type type)
    {
        this.friendlyName = friendlyName;
        this.file = file;
        this.fileSize = fileSize;
        this.fileMimeType = fileMime;
        this.deviceId = deviceId;
        this.requestId = requestId;
        this.groupId = groupId;
        this.type = type;
    }

    public TransferObject(long requestId, String deviceId, Type type)
    {
        this.requestId = requestId;
        this.deviceId = deviceId;
        this.type = type;
    }

    public TransferObject(CursorItem item)
    {
        reconstruct(item);
    }

    @Override
    public boolean applyFilter(String[] filteringKeywords)
    {
        for (String keyword : filteringKeywords)
            if (friendlyName.contains(keyword))
                return true;

        return false;
    }

    @Override
    public boolean comparisonSupported()
    {
        return true;
    }

    @Override
    public boolean equals(Object obj)
    {
        if (!(obj instanceof TransferObject))
            return super.equals(obj);

        TransferObject otherObject = (TransferObject) obj;

        return otherObject.requestId == requestId
                && type.equals(otherObject.type)
                && ((deviceId == null && otherObject.deviceId == null) || (deviceId != null && deviceId.equals(otherObject.deviceId)));
    }

    public boolean isDivisionObject()
    {
        return deviceId == null;
    }

    @Override
    public SQLQuery.Select getWhere()
    {
        String whereClause = isDivisionObject()
                ? String.format("%s = ? AND %s = ?", AccessDatabase.FIELD_TRANSFER_ID, AccessDatabase.FIELD_TRANSFER_TYPE)
                : String.format("%s = ? AND %s = ? AND %s = ?", AccessDatabase.FIELD_TRANSFER_ID, AccessDatabase.FIELD_TRANSFER_TYPE, AccessDatabase.FIELD_TRANSFER_DEVICEID);

        return isDivisionObject()
                ? new SQLQuery.Select(AccessDatabase.DIVIS_TRANSFER).setWhere(whereClause, String.valueOf(requestId), type.toString())
                : new SQLQuery.Select(AccessDatabase.TABLE_TRANSFER).setWhere(whereClause, String.valueOf(requestId), type.toString(), deviceId);
    }

    @Override
    public ContentValues getValues()
    {
        ContentValues values = new ContentValues();

        values.put(AccessDatabase.FIELD_TRANSFER_ID, requestId);
        values.put(AccessDatabase.FIELD_TRANSFER_GROUPID, groupId);
        values.put(AccessDatabase.FIELD_TRANSFER_DEVICEID, deviceId);
        values.put(AccessDatabase.FIELD_TRANSFER_NAME, friendlyName);
        values.put(AccessDatabase.FIELD_TRANSFER_SIZE, fileSize);
        values.put(AccessDatabase.FIELD_TRANSFER_MIME, fileMimeType);
        values.put(AccessDatabase.FIELD_TRANSFER_FLAG, flag.toString());
        values.put(AccessDatabase.FIELD_TRANSFER_TYPE, type.toString());
        values.put(AccessDatabase.FIELD_TRANSFER_FILE, file);
        values.put(AccessDatabase.FIELD_TRANSFER_ACCESSPORT, accessPort);
        values.put(AccessDatabase.FIELD_TRANSFER_SKIPPEDBYTES, skippedBytes);
        values.put(AccessDatabase.FIELD_TRANSFER_DIRECTORY, directory);

        return values;
    }

    @Override
    public void reconstruct(CursorItem item)
    {
        this.friendlyName = item.getString(AccessDatabase.FIELD_TRANSFER_NAME);
        this.file = item.getString(AccessDatabase.FIELD_TRANSFER_FILE);
        this.fileSize = item.getLong(AccessDatabase.FIELD_TRANSFER_SIZE);
        this.fileMimeType = item.getString(AccessDatabase.FIELD_TRANSFER_MIME);
        this.requestId = item.getLong(AccessDatabase.FIELD_TRANSFER_ID);
        this.groupId = item.getLong(AccessDatabase.FIELD_TRANSFER_GROUPID);
        this.deviceId = item.getString(AccessDatabase.FIELD_TRANSFER_DEVICEID);
        this.type = Type.valueOf(item.getString(AccessDatabase.FIELD_TRANSFER_TYPE));

        // We may have put long in that field indicating that the file was / is in progress so generate
        try {
            this.flag = Flag.valueOf(item.getString(AccessDatabase.FIELD_TRANSFER_FLAG));
        } catch (Exception e) {
            this.flag = Flag.IN_PROGRESS;
            this.flag.setBytesValue(item.getLong(AccessDatabase.FIELD_TRANSFER_FLAG));
        }

        this.accessPort = item.getInt(AccessDatabase.FIELD_TRANSFER_ACCESSPORT);
        this.skippedBytes = item.getLong(AccessDatabase.FIELD_TRANSFER_SKIPPEDBYTES);
        this.directory = item.getString(AccessDatabase.FIELD_TRANSFER_DIRECTORY);
    }

    @Override
    public void onCreateObject(android.database.sqlite.SQLiteDatabase dbInstance, SQLiteDatabase database, TransferGroup parent)
    {

    }

    @Override
    public void onUpdateObject(android.database.sqlite.SQLiteDatabase dbInstance, SQLiteDatabase database, TransferGroup parent)
    {

    }

    @Override
    public void onRemoveObject(android.database.sqlite.SQLiteDatabase dbInstance, SQLiteDatabase database, TransferGroup parent)
    {
        // Normally we'd like to check every file, but I may take a while.
        if (!Flag.INTERRUPTED.equals(flag) || !Type.INCOMING.equals(type))
            return;

        try {
            if (parent == null) {
                parent = new TransferGroup(groupId);
                database.reconstruct(parent);
            }

            DocumentFile file = FileUtils.getIncomingPseudoFile(database.getContext(), this, parent, false);

            if (file != null && file.isFile())
                file.delete();
        } catch (Exception e) {

        }
    }

    @Override
    public String getComparableName()
    {
        return getSelectableTitle();
    }

    @Override
    public long getComparableDate()
    {
        return requestId;
    }

    @Override
    public long getComparableSize()
    {
        return fileSize;
    }

    @SuppressLint("DefaultLocale")
    @Override
    public long getId()
    {
        return String.format("%d_%d_%s", requestId, type.ordinal(), deviceId).hashCode();
    }

    @Override
    public void setId(long id)
    {
        // it will && should be effective on representative text items
        this.requestId = id;
    }

    @Override
    public String getSelectableTitle()
    {
        return friendlyName;
    }

    @Override
    public boolean isSelectableSelected()
    {
        return mIsSelected;
    }

    @Override
    public boolean setSelectableSelected(boolean selected)
    {
        mIsSelected = selected;
        return true;
    }

    public enum Type
    {
        INCOMING,
        OUTGOING
    }

    public enum Flag
    {
        INTERRUPTED,
        PENDING,
        REMOVED,
        IN_PROGRESS,
        DONE;

        private long bytesValue;

        public long getBytesValue()
        {
            return bytesValue;
        }

        public void setBytesValue(long bytesValue)
        {
            this.bytesValue = bytesValue;
        }

        @Override
        public String toString()
        {
            return getBytesValue() > 0
                    ? String.valueOf(getBytesValue())
                    : super.toString();
        }
    }

    @Retention(CLASS)
    @Target({METHOD, PARAMETER, FIELD, LOCAL_VARIABLE, ANNOTATION_TYPE, PACKAGE})
    public @interface Virtual
    {
    }
}