package com.genonbeta.TrebleShot.graphics.drawable;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.OvalShape;
import android.graphics.drawable.shapes.RectShape;
import android.graphics.drawable.shapes.RoundRectShape;

import com.genonbeta.TrebleShot.util.TextUtils;

public class TextDrawable extends ShapeDrawable
{
    private static final float SHADE_FACTOR = 0.9f;

    private final Paint mTextPaint;
    private final Paint mBorderPaint;
    private final String mText;
    private final RectShape mShape;
    private final int mColor;
    private final int mHeight;
    private final int mWidth;
    private final int mFontSize;
    private final int mBorderThickness;
    private final float mRadius;

    private TextDrawable(Builder builder)
    {
        super(builder.mShape);

        // shape properties
        mShape = builder.mShape;
        mHeight = builder.mHeight;
        mWidth = builder.mWidth;
        mRadius = builder.mRadius;

        // text and color
        String processedText = builder.mToUpperCase ? builder.mText.toUpperCase() : builder.mText;

        if (builder.mTextMaxLength >= 0) {
            processedText = builder.mFirstLetters
                    ? TextUtils.getLetters(processedText, builder.mTextMaxLength)
                    : (processedText.length() > builder.mTextMaxLength) ? processedText.substring(0, builder.mTextMaxLength) : processedText;
        }

        mText = processedText;
        mColor = builder.mColor;

        // text paint settings
        mFontSize = builder.mFontSize;
        mTextPaint = new Paint();
        mTextPaint.setColor(builder.mTextColor);
        mTextPaint.setAntiAlias(true);
        mTextPaint.setFakeBoldText(builder.mIsBold);
        mTextPaint.setStyle(Paint.Style.FILL);
        mTextPaint.setTypeface(builder.mFont);
        mTextPaint.setTextAlign(Paint.Align.CENTER);
        mTextPaint.setStrokeWidth(builder.mBorderThickness);

        // border paint settings
        mBorderThickness = builder.mBorderThickness;
        mBorderPaint = new Paint();
        mBorderPaint.setColor(getDarkerShade(mColor));
        mBorderPaint.setStyle(Paint.Style.STROKE);
        mBorderPaint.setStrokeWidth(mBorderThickness);

        // drawable paint mColor
        Paint paint = getPaint();
        paint.setColor(mColor);
    }

    public static IShapeBuilder builder()
    {
        return new Builder();
    }

    private int getDarkerShade(int color)
    {
        return Color.rgb((int) (SHADE_FACTOR * Color.red(color)),
                (int) (SHADE_FACTOR * Color.green(color)),
                (int) (SHADE_FACTOR * Color.blue(color)));
    }

    @Override
    public void draw(Canvas canvas)
    {
        super.draw(canvas);
        Rect r = getBounds();

        // draw border
        if (mBorderThickness > 0)
            drawBorder(canvas);

        int count = canvas.save();
        canvas.translate(r.left, r.top);

        // draw text
        int width = mWidth < 0 ? r.width() : mWidth;
        int height = mHeight < 0 ? r.height() : mHeight;
        int fontSize = mFontSize < 0 ? (Math.min(width, height) / 2) : mFontSize;

        mTextPaint.setTextSize(fontSize);

        canvas.drawText(mText, width / 2, height / 2 - ((mTextPaint.descent() + mTextPaint.ascent()) / 2), mTextPaint);
        canvas.restoreToCount(count);
    }

    private void drawBorder(Canvas canvas)
    {
        RectF rect = new RectF(getBounds());
        rect.inset(mBorderThickness / 2, mBorderThickness / 2);

        if (mShape instanceof OvalShape) {
            canvas.drawOval(rect, mBorderPaint);
        } else if (mShape instanceof RoundRectShape) {
            canvas.drawRoundRect(rect, mRadius, mRadius, mBorderPaint);
        } else {
            canvas.drawRect(rect, mBorderPaint);
        }
    }

    @Override
    public void setAlpha(int alpha)
    {
        mTextPaint.setAlpha(alpha);
    }

    @Override
    public void setColorFilter(ColorFilter cf)
    {
        mTextPaint.setColorFilter(cf);
    }

    @Override
    public int getOpacity()
    {
        return PixelFormat.TRANSLUCENT;
    }

    @Override
    public int getIntrinsicWidth()
    {
        return mWidth;
    }

    @Override
    public int getIntrinsicHeight()
    {
        return mHeight;
    }

    public interface IConfigBuilder
    {
        IConfigBuilder bold();

        IConfigBuilder firstLettersOnly(boolean use);

        IConfigBuilder fontSize(int size);

        IConfigBuilder height(int height);

        IConfigBuilder shapeColor(int color);

        IConfigBuilder textColor(int color);

        IConfigBuilder textMaxLength(int length);

        IConfigBuilder toUpperCase();

        IConfigBuilder useFont(Typeface font);

        IConfigBuilder width(int width);

        IConfigBuilder withBorder(int thickness);

        IShapeBuilder endConfig();
    }

    public interface IBuilder
    {
        TextDrawable build(String text);
    }

    public interface IShapeBuilder
    {
        IConfigBuilder beginConfig();

        IBuilder rect();

        IBuilder round();

        IBuilder roundRect(int radius);

        TextDrawable buildRect(String text);

        TextDrawable buildRound(String text);

        TextDrawable buildRoundRect(String text, int radius);
    }

    public static class Builder implements IConfigBuilder, IShapeBuilder, IBuilder
    {
        private int mColor;
        private int mBorderThickness;
        private int mWidth;
        private int mHeight;
        private int mTextColor;
        private int mTextMaxLength;
        private int mFontSize;
        private float mRadius;
        private boolean mToUpperCase;
        private boolean mIsBold;
        private boolean mFirstLetters;
        private String mText;
        private Typeface mFont;
        private RectShape mShape;

        private Builder()
        {
            mText = "";
            mColor = Color.GRAY;
            mTextColor = Color.WHITE;
            mBorderThickness = 0;
            mWidth = -1;
            mHeight = -1;
            mShape = new RectShape();
            mFont = Typeface.create("sans-serif", Typeface.NORMAL);
            mTextMaxLength = -1;
            mFontSize = -1;
            mIsBold = false;
            mToUpperCase = false;
        }

        public IConfigBuilder width(int width)
        {
            mWidth = width;
            return this;
        }

        public IConfigBuilder height(int height)
        {
            mHeight = height;
            return this;
        }

        @Override
        public IConfigBuilder shapeColor(int color)
        {
            mColor = color;
            return this;
        }

        public IConfigBuilder textColor(int color)
        {
            mTextColor = color;
            return this;
        }

        @Override
        public IConfigBuilder textMaxLength(int length)
        {
            mTextMaxLength = length;
            return this;
        }

        public IConfigBuilder withBorder(int thickness)
        {
            mBorderThickness = thickness;
            return this;
        }

        public IConfigBuilder useFont(Typeface font)
        {
            mFont = font;
            return this;
        }

        public IConfigBuilder fontSize(int size)
        {
            mFontSize = size;
            return this;
        }

        public IConfigBuilder bold()
        {
            mIsBold = true;
            return this;
        }

        public IConfigBuilder toUpperCase()
        {
            mToUpperCase = true;
            return this;
        }

        @Override
        public IConfigBuilder firstLettersOnly(boolean use)
        {
            mFirstLetters = use;
            return this;
        }

        @Override
        public IConfigBuilder beginConfig()
        {
            return this;
        }

        @Override
        public IShapeBuilder endConfig()
        {
            return this;
        }

        @Override
        public IBuilder rect()
        {
            mShape = new RectShape();
            return this;
        }

        @Override
        public IBuilder round()
        {
            mShape = new OvalShape();
            return this;
        }

        @Override
        public IBuilder roundRect(int radius)
        {
            mRadius = radius;
            float[] radii = {radius, radius, radius, radius, radius, radius, radius, radius};
            mShape = new RoundRectShape(radii, null, null);
            return this;
        }

        @Override
        public TextDrawable buildRect(String text)
        {
            rect();
            return build(text);
        }

        @Override
        public TextDrawable buildRoundRect(String text, int radius)
        {
            roundRect(radius);
            return build(text);
        }

        @Override
        public TextDrawable buildRound(String text)
        {
            round();
            return build(text);
        }

        @Override
        public TextDrawable build(String text)
        {
            mText = text;
            return new TextDrawable(this);
        }
    }
}