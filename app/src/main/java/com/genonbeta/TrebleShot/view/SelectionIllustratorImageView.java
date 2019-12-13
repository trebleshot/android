/*
 * Copyright (C) 2019 Veli Tasalı
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */

package com.genonbeta.TrebleShot.view;

import android.content.Context;
import android.util.AttributeSet;
import androidx.appcompat.widget.AppCompatImageView;

/**
 * created by: veli
 * date: 11.04.2018 00:00
 */
public class SelectionIllustratorImageView extends AppCompatImageView
{
    public SelectionIllustratorImageView(Context context)
    {
        super(context);
    }

    public SelectionIllustratorImageView(Context context, AttributeSet attrs)
    {
        super(context, attrs);
    }

    public SelectionIllustratorImageView(Context context, AttributeSet attrs, int defStyleAttr)
    {
        super(context, attrs, defStyleAttr);
    }

    @Override
    public void setSelected(boolean selected)
    {
        super.setSelected(selected);

        if (selected) {
            getDrawable().setVisible(false, false);
        } else
            getDrawable().setVisible(true, false);
    }
}
