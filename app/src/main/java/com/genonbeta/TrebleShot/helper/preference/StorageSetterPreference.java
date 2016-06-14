package com.genonbeta.TrebleShot.helper.preference;

import android.preference.EditTextPreference;
import android.text.Editable;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import com.genonbeta.TrebleShot.R;
import com.genonbeta.TrebleShot.helper.ApplicationHelper;

import java.io.File;

public class StorageSetterPreference extends EditTextPreference
{
    public StorageSetterPreference(android.content.Context context, android.util.AttributeSet attrs, int defStyleAttr, int defStyleRes)
    {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    public StorageSetterPreference(android.content.Context context, android.util.AttributeSet attrs, int defStyleAttr)
    {
        super(context, attrs, defStyleAttr, defStyleAttr);
    }

    public StorageSetterPreference(android.content.Context context, android.util.AttributeSet attrs)
    {
        super(context, attrs);
    }

    public StorageSetterPreference(android.content.Context context)
    {
        super(context);
    }

    @Override
    protected void onAddEditTextToDialogView(View dialogView, EditText editText)
    {
        super.onAddEditTextToDialogView(dialogView, editText);

        Editable editor = editText.getText();

        if (editor.toString().length() == 0)
        {
            editor.append(ApplicationHelper.getApplicationDirectory(getContext()).getAbsolutePath());
        }
    }

    @Override
    protected void onDialogClosed(boolean positiveResult)
    {
        super.onDialogClosed(positiveResult);

        Editable editable = getEditText().getText();
        File appDir = ApplicationHelper.getApplicationDirectory(getContext());

        if (!editable.toString().equals(appDir.getAbsolutePath()) && editable.toString().length() > 0 && positiveResult)
            Toast.makeText(getContext(), R.string.error_rejected_path, Toast.LENGTH_LONG).show();
        else if (!appDir.isDirectory())
            Toast.makeText(getContext(), getContext().getString(R.string.error_default_path, appDir.getAbsolutePath()), Toast.LENGTH_LONG).show();
    }
}
