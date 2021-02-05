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
package com.genonbeta.TrebleShot.graphics.drawable

import com.genonbeta.TrebleShot.dataobject.MappedSelectable.Companion.compileFrom
import com.genonbeta.TrebleShot.dataobject.Identity.Companion.withORs
import com.genonbeta.TrebleShot.dataobject.Identifier.Companion.from
import com.genonbeta.TrebleShot.dataobject.TransferIndex.bytesPending
import com.genonbeta.TrebleShot.dataobject.TransferItem.Flag.bytesValue
import com.genonbeta.TrebleShot.dataobject.TransferItem.flag
import com.genonbeta.TrebleShot.dataobject.TransferItem.putFlag
import com.genonbeta.TrebleShot.dataobject.Identity.Companion.withANDs
import com.genonbeta.TrebleShot.dataobject.TransferItem.Companion.from
import com.genonbeta.TrebleShot.dataobject.DeviceAddress.hostAddress
import com.genonbeta.TrebleShot.dataobject.Container.expand
import com.genonbeta.TrebleShot.dataobject.Device.equals
import com.genonbeta.TrebleShot.dataobject.TransferItem.flags
import com.genonbeta.TrebleShot.dataobject.TransferItem.getFlag
import com.genonbeta.TrebleShot.dataobject.TransferItem.Flag.toString
import com.genonbeta.TrebleShot.dataobject.TransferItem.reconstruct
import com.genonbeta.TrebleShot.dataobject.Device.generatePictureId
import com.genonbeta.TrebleShot.dataobject.TransferItem.setDeleteOnRemoval
import com.genonbeta.TrebleShot.dataobject.MappedSelectable.selectableTitle
import com.genonbeta.TrebleShot.dataobject.TransferIndex.hasOutgoing
import com.genonbeta.TrebleShot.dataobject.TransferIndex.hasIncoming
import com.genonbeta.TrebleShot.dataobject.Comparable.comparisonSupported
import com.genonbeta.TrebleShot.dataobject.Comparable.comparableDate
import com.genonbeta.TrebleShot.dataobject.Comparable.comparableSize
import com.genonbeta.TrebleShot.dataobject.Comparable.comparableName
import com.genonbeta.TrebleShot.dataobject.Editable.applyFilter
import com.genonbeta.TrebleShot.dataobject.Editable.id
import com.genonbeta.TrebleShot.dataobject.Shareable.setSelectableSelected
import com.genonbeta.TrebleShot.dataobject.Shareable.initialize
import com.genonbeta.TrebleShot.dataobject.Shareable.isSelectableSelected
import com.genonbeta.TrebleShot.dataobject.Shareable.comparisonSupported
import com.genonbeta.TrebleShot.dataobject.Shareable.comparableSize
import com.genonbeta.TrebleShot.dataobject.Shareable.applyFilter
import com.genonbeta.TrebleShot.dataobject.Device.hashCode
import com.genonbeta.TrebleShot.dataobject.TransferIndex.percentage
import com.genonbeta.TrebleShot.dataobject.TransferIndex.getMemberAsTitle
import com.genonbeta.TrebleShot.dataobject.TransferIndex.isSelectableSelected
import com.genonbeta.TrebleShot.dataobject.TransferIndex.numberOfCompleted
import com.genonbeta.TrebleShot.dataobject.TransferIndex.numberOfTotal
import com.genonbeta.TrebleShot.dataobject.TransferIndex.bytesTotal
import com.genonbeta.TrebleShot.dataobject.TransferItem.isSelectableSelected
import com.genonbeta.TrebleShot.dataobject.TransferItem.setSelectableSelected
import com.genonbeta.TrebleShot.dataobject.TransferItem.senderFlagList
import com.genonbeta.TrebleShot.dataobject.TransferItem.getPercentage
import com.genonbeta.TrebleShot.dataobject.TransferItem.setId
import com.genonbeta.TrebleShot.dataobject.TransferItem.comparableDate
import com.genonbeta.TrebleShot.dataobject.Identity.equals
import com.genonbeta.TrebleShot.dataobject.Transfer.equals
import com.genonbeta.TrebleShot.dataobject.TransferMember.reconstruct
import android.os.Parcelable
import android.os.Parcel
import com.genonbeta.TrebleShot.io.Containable
import android.os.Parcelable.Creator
import com.genonbeta.TrebleShot.R
import android.content.DialogInterface
import com.genonbeta.TrebleShot.activity.AddDeviceActivity.AvailableFragment
import android.content.Intent
import com.genonbeta.TrebleShot.activity.AddDeviceActivity
import androidx.annotation.DrawableRes
import com.genonbeta.TrebleShot.dataobject.Shareable
import com.genonbeta.android.framework.util.actionperformer.PerformerEngineProvider
import com.genonbeta.TrebleShot.ui.callback.LocalSharingCallback
import com.genonbeta.android.framework.ui.PerformerMenu
import android.view.MenuInflater
import com.genonbeta.android.framework.util.actionperformer.IPerformerEngine
import com.genonbeta.TrebleShot.ui.callback.SharingPerformerMenuCallback
import com.genonbeta.TrebleShot.dataobject.MappedSelectable
import com.genonbeta.TrebleShot.dialog.ChooseSharingMethodDialog
import com.genonbeta.TrebleShot.dialog.ChooseSharingMethodDialog.PickListener
import com.genonbeta.TrebleShot.dialog.ChooseSharingMethodDialog.SharingMethod
import com.genonbeta.TrebleShot.task.OrganizeLocalSharingTask
import com.genonbeta.TrebleShot.App
import com.genonbeta.TrebleShot.util.NotificationUtils
import com.genonbeta.TrebleShot.database.Kuick
import com.genonbeta.TrebleShot.util.AppUtils
import android.content.SharedPreferences
import androidx.appcompat.app.AppCompatActivity
import com.genonbeta.TrebleShot.service.backgroundservice.BaseAttachableAsyncTask
import android.content.IntentFilter
import android.content.BroadcastReceiver
import android.os.Bundle
import androidx.annotation.StyleRes
import android.content.pm.PackageManager
import com.genonbeta.TrebleShot.activity.WelcomeActivity
import com.genonbeta.TrebleShot.GlideApp
import com.bumptech.glide.request.target.CustomTarget
import android.graphics.drawable.Drawable
import com.genonbeta.TrebleShot.config.AppConfig
import kotlin.jvm.Synchronized
import com.genonbeta.TrebleShot.service.BackgroundService
import android.os.PowerManager
import com.genonbeta.TrebleShot.dialog.RationalePermissionRequest
import com.genonbeta.TrebleShot.service.backgroundservice.AttachedTaskListener
import com.genonbeta.TrebleShot.service.backgroundservice.AttachableAsyncTask
import com.genonbeta.TrebleShot.dialog.ProfileEditorDialog
import android.widget.ProgressBar
import android.view.LayoutInflater
import kotlin.jvm.JvmOverloads
import com.genonbeta.android.framework.widget.RecyclerViewAdapter
import com.genonbeta.TrebleShot.widget.EditableListAdapter
import com.genonbeta.android.framework.app.DynamicRecyclerViewFragment
import com.genonbeta.TrebleShot.app.IEditableListFragment
import com.genonbeta.android.framework.util.actionperformer.IEngineConnection
import com.genonbeta.android.framework.util.actionperformer.EngineConnection
import com.genonbeta.android.framework.util.actionperformer.PerformerEngine
import com.genonbeta.TrebleShot.app.EditableListFragment.FilteringDelegate
import android.database.ContentObserver
import android.graphics.*
import com.genonbeta.TrebleShot.app.EditableListFragment.LayoutClickListener
import com.genonbeta.TrebleShot.app.EditableListFragmentBase
import com.genonbeta.TrebleShot.app.EditableListFragment
import android.view.ViewGroup
import com.genonbeta.TrebleShot.view.LongTextBubbleFastScrollViewProvider
import com.genonbeta.TrebleShot.widget.recyclerview.ItemOffsetDecoration
import com.genonbeta.TrebleShot.widget.EditableListAdapterBase
import android.os.Looper
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.GridLayoutManager.SpanSizeLookup
import android.view.View.OnLongClickListener
import com.genonbeta.android.framework.util.actionperformer.SelectableNotFoundException
import com.genonbeta.android.framework.util.actionperformer.CouldNotAlterException
import com.genonbeta.TrebleShot.widget.recyclerview.SwipeSelectionListener
import com.genonbeta.TrebleShot.util.SelectionUtils
import com.genonbeta.TrebleShot.dialog.SelectionEditorDialog
import com.genonbeta.TrebleShot.util.TextUtils
import com.genonbeta.android.framework.util.actionperformer.IBaseEngineConnection
import com.genonbeta.android.framework.``object`

class TextDrawable private constructor(builder: Builder) : ShapeDrawable(builder.mShape) {
    private val mTextPaint: Paint
    private val mBorderPaint: Paint
    private val mText: String?
    private val mShape: RectShape
    private val mColor: Int
    private val mHeight: Int
    private val mWidth: Int
    private val mFontSize: Int
    private val mBorderThickness: Int
    private val mRadius: Float
    private fun getDarkerShade(color: Int): Int {
        return Color.rgb(
            (SHADE_FACTOR * Color.red(color)).toInt(),
            (SHADE_FACTOR * Color.green(color)).toInt(),
            (SHADE_FACTOR * Color.blue(color)).toInt()
        )
    }

    override fun draw(canvas: Canvas) {
        super.draw(canvas)
        val r: Rect = getBounds()

        // draw border
        if (mBorderThickness > 0) drawBorder(canvas)
        val count = canvas.save()
        canvas.translate(r.left.toFloat(), r.top.toFloat())

        // draw text
        val width = if (mWidth < 0) r.width() else mWidth
        val height = if (mHeight < 0) r.height() else mHeight
        val fontSize = if (mFontSize < 0) Math.min(width, height) / 2 else mFontSize
        mTextPaint.textSize = fontSize.toFloat()
        canvas.drawText(
            mText!!,
            (width / 2).toFloat(),
            height / 2 - (mTextPaint.descent() + mTextPaint.ascent()) / 2,
            mTextPaint
        )
        canvas.restoreToCount(count)
    }

    private fun drawBorder(canvas: Canvas) {
        val rect = RectF(getBounds())
        rect.inset((mBorderThickness / 2).toFloat(), (mBorderThickness / 2).toFloat())
        if (mShape is OvalShape) {
            canvas.drawOval(rect, mBorderPaint)
        } else if (mShape is RoundRectShape) {
            canvas.drawRoundRect(rect, mRadius, mRadius, mBorderPaint)
        } else {
            canvas.drawRect(rect, mBorderPaint)
        }
    }

    override fun setAlpha(alpha: Int) {
        mTextPaint.alpha = alpha
    }

    override fun setColorFilter(cf: ColorFilter?) {
        mTextPaint.colorFilter = cf
    }

    override fun getOpacity(): Int {
        return PixelFormat.TRANSLUCENT
    }

    override fun getIntrinsicWidth(): Int {
        return mWidth
    }

    override fun getIntrinsicHeight(): Int {
        return mHeight
    }

    interface IConfigBuilder {
        fun bold(): IConfigBuilder
        fun firstLettersOnly(use: Boolean): IConfigBuilder
        fun fontSize(size: Int): IConfigBuilder
        fun height(height: Int): IConfigBuilder
        fun shapeColor(color: Int): IConfigBuilder
        fun textColor(color: Int): IConfigBuilder
        fun textMaxLength(length: Int): IConfigBuilder
        fun toUpperCase(): IConfigBuilder
        fun useFont(font: Typeface): IConfigBuilder
        fun width(width: Int): IConfigBuilder
        fun withBorder(thickness: Int): IConfigBuilder
        fun endConfig(): IShapeBuilder
    }

    interface IBuilder {
        fun build(text: String): TextDrawable
    }

    interface IShapeBuilder {
        fun beginConfig(): IConfigBuilder
        fun rect(): IBuilder
        fun round(): IBuilder
        fun roundRect(radius: Int): IBuilder
        fun buildRect(text: String): TextDrawable
        fun buildRound(text: String): TextDrawable
        fun buildRoundRect(text: String, radius: Int): TextDrawable
    }

    class Builder : IConfigBuilder, IShapeBuilder, IBuilder {
        var mColor: Int
        var mBorderThickness: Int
        var mWidth: Int
        var mHeight: Int
        var mTextColor: Int
        var mTextMaxLength: Int
        var mFontSize: Int
        var mRadius = 0f
        var mToUpperCase: Boolean
        var mIsBold: Boolean
        var mFirstLetters = false
        var mText = ""
        var mFont: Typeface
        var mShape: RectShape
        override fun width(width: Int): IConfigBuilder {
            mWidth = width
            return this
        }

        override fun height(height: Int): IConfigBuilder {
            mHeight = height
            return this
        }

        override fun shapeColor(color: Int): IConfigBuilder {
            mColor = color
            return this
        }

        override fun textColor(color: Int): IConfigBuilder {
            mTextColor = color
            return this
        }

        override fun textMaxLength(length: Int): IConfigBuilder {
            mTextMaxLength = length
            return this
        }

        override fun withBorder(thickness: Int): IConfigBuilder {
            mBorderThickness = thickness
            return this
        }

        override fun useFont(font: Typeface): IConfigBuilder {
            mFont = font
            return this
        }

        override fun fontSize(size: Int): IConfigBuilder {
            mFontSize = size
            return this
        }

        override fun bold(): IConfigBuilder {
            mIsBold = true
            return this
        }

        override fun toUpperCase(): IConfigBuilder {
            mToUpperCase = true
            return this
        }

        override fun firstLettersOnly(use: Boolean): IConfigBuilder {
            mFirstLetters = use
            return this
        }

        override fun beginConfig(): IConfigBuilder {
            return this
        }

        override fun endConfig(): IShapeBuilder {
            return this
        }

        override fun rect(): IBuilder {
            mShape = RectShape()
            return this
        }

        override fun round(): IBuilder {
            mShape = OvalShape()
            return this
        }

        override fun roundRect(radius: Int): IBuilder {
            mRadius = radius.toFloat()
            val radii = floatArrayOf(
                radius.toFloat(),
                radius.toFloat(),
                radius.toFloat(),
                radius.toFloat(),
                radius.toFloat(),
                radius.toFloat(),
                radius.toFloat(),
                radius.toFloat()
            )
            mShape = RoundRectShape(radii, null, null)
            return this
        }

        override fun buildRect(text: String): TextDrawable {
            rect()
            return build(text)
        }

        override fun buildRoundRect(text: String, radius: Int): TextDrawable {
            roundRect(radius)
            return build(text)
        }

        override fun buildRound(text: String): TextDrawable {
            round()
            return build(text)
        }

        override fun build(text: String): TextDrawable {
            mText = text
            return TextDrawable(this)
        }

        init {
            mColor = Color.GRAY
            mTextColor = Color.WHITE
            mBorderThickness = 0
            mWidth = -1
            mHeight = -1
            mShape = RectShape()
            mFont = Typeface.create("sans-serif", Typeface.NORMAL)
            mTextMaxLength = -1
            mFontSize = -1
            mIsBold = false
            mToUpperCase = false
        }
    }

    companion object {
        private const val SHADE_FACTOR = 0.9f
        fun builder(): IShapeBuilder {
            return Builder()
        }
    }

    init {

        // shape properties
        mShape = builder.mShape
        mHeight = builder.mHeight
        mWidth = builder.mWidth
        mRadius = builder.mRadius

        // text and color
        var processedText: String? = if (builder.mToUpperCase) builder.mText.toUpperCase() else builder.mText
        if (builder.mTextMaxLength >= 0) {
            processedText = if (builder.mFirstLetters) TextUtils.getLetters(
                processedText,
                builder.mTextMaxLength
            ) else if (processedText!!.length > builder.mTextMaxLength) processedText.substring(
                0,
                builder.mTextMaxLength
            ) else processedText
        }
        mText = processedText
        mColor = builder.mColor

        // text paint settings
        mFontSize = builder.mFontSize
        mTextPaint = Paint()
        mTextPaint.color = builder.mTextColor
        mTextPaint.isAntiAlias = true
        mTextPaint.isFakeBoldText = builder.mIsBold
        mTextPaint.style = Paint.Style.FILL
        mTextPaint.typeface = builder.mFont
        mTextPaint.textAlign = Paint.Align.CENTER
        mTextPaint.strokeWidth = builder.mBorderThickness.toFloat()

        // border paint settings
        mBorderThickness = builder.mBorderThickness
        mBorderPaint = Paint()
        mBorderPaint.color = getDarkerShade(mColor)
        mBorderPaint.style = Paint.Style.STROKE
        mBorderPaint.strokeWidth = mBorderThickness.toFloat()

        // drawable paint mColor
        val paint: Paint = getPaint()
        paint.color = mColor
    }
}