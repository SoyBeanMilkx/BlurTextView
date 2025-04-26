package com.yuuki.blurtextview.blur;

import android.app.Activity;
import android.content.Context;
import android.content.ContextWrapper;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Outline;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewOutlineProvider;
import android.view.ViewTreeObserver;

public class BlurTextView extends View {

    private float mDownscaleFactor = 2;
    private int mOverlayColor = Color.parseColor("#5CFFFFFF");
    private float mBlurRadius = 50;
    private final float mRoundRadius = 15f;
    private final SupportLibraryBlur mBlurImpl;
    private boolean mDirty = true;
    private Bitmap mBitmapToBlur, mBlurredBitmap;
    private Canvas mBlurringCanvas;
    private boolean mIsRendering;
    private final Paint mPaint;
    private final Rect mRectSrc = new Rect(), mRectDst = new Rect();
    private View mDecorView;
    private boolean mDifferentRoot;
    private static int RENDERING_COUNT;

    private final Paint textPaint;
    private final Path textPath;
    private String text = "Yuuki"; // 默认文字

    public BlurTextView(Context context) {
        this(context, null);
    }

    public BlurTextView(Context context, AttributeSet attrs) {
        super(context, attrs);

        mBlurImpl = getBlurImpl();
        mPaint = new Paint();
        setOutlineProvider(new ViewOutlineProvider() {
            @Override
            public void getOutline(View view, Outline outline) {
                outline.setRoundRect(0, 0, view.getWidth(), view.getHeight(), mRoundRadius);
            }
        });
        setClipToOutline(true);

        setBackgroundColor(Color.TRANSPARENT);
        textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        textPaint.setTextSize(300);
        textPaint.setTypeface(Typeface.DEFAULT_BOLD);
        textPaint.setColor(Color.TRANSPARENT);
        textPaint.setStyle(Paint.Style.STROKE);
        textPath = new Path();
    }

    protected SupportLibraryBlur getBlurImpl() {
        SupportLibraryBlur impl = new SupportLibraryBlur();
        Bitmap bmp = Bitmap.createBitmap(4, 4, Bitmap.Config.ARGB_8888);
        impl.prepare(getContext(), bmp, 4);
        impl.release();
        bmp.recycle();
        return new SupportLibraryBlur();
    }

    public void setBlurRadius(float radius) {
        if (mBlurRadius != radius) {
            mBlurRadius = radius;
            mDirty = true;
            invalidate();
        }
    }

    public void setDownscaleFactor(float factor) {
        if (factor <= 0) {
            throw new IllegalArgumentException("Downsample factor must be greater than 0.");
        }
        if (mDownscaleFactor != factor) {
            mDownscaleFactor = factor;
            mDirty = true;
            releaseBitmap();
            invalidate();
        }
    }

    public void setOverlayColor(int color) {
        if (mOverlayColor != color) {
            mOverlayColor = color;
            invalidate();
        }
    }

    public Bitmap getBlurredBitmapCopy() {
        if (mBlurredBitmap != null && !mBlurredBitmap.isRecycled()) {
            return Bitmap.createBitmap(mBlurredBitmap); // 深拷贝
        }
        return null;
    }

    private void releaseBitmap() {
        if (mBitmapToBlur != null) {
            mBitmapToBlur.recycle();
            mBitmapToBlur = null;
        }
        if (mBlurredBitmap != null) {
            mBlurredBitmap.recycle();
            mBlurredBitmap = null;
        }
    }

    protected void release() {
        releaseBitmap();
        mBlurImpl.release();
    }

    protected boolean prepare() {
        if (mBlurRadius == 0) {
            release();
            return false;
        }

        float downsampleFactor = mDownscaleFactor;
        float radius = mBlurRadius / downsampleFactor;
        if (radius > 25) {
            downsampleFactor = downsampleFactor * radius / 25;
            radius = 25;
        }

        final int width = getWidth();
        final int height = getHeight();

        int scaledWidth = Math.max(1, (int) (width / downsampleFactor));
        int scaledHeight = Math.max(1, (int) (height / downsampleFactor));

        boolean dirty = mDirty;

        if (mBlurringCanvas == null || mBlurredBitmap == null
                || mBlurredBitmap.getWidth() != scaledWidth
                || mBlurredBitmap.getHeight() != scaledHeight) {
            dirty = true;
            releaseBitmap();

            boolean r = false;
            try {
                mBitmapToBlur = Bitmap.createBitmap(scaledWidth, scaledHeight, Bitmap.Config.ARGB_8888);
                mBlurringCanvas = new Canvas(mBitmapToBlur);

                mBlurredBitmap = Bitmap.createBitmap(scaledWidth, scaledHeight, Bitmap.Config.ARGB_8888);

                r = true;
            } catch (OutOfMemoryError e) {
                e.printStackTrace();
            } finally {
                if (!r) {
                    release();
                    return false;
                }
            }
        }

        if (dirty) {
            if (mBlurImpl.prepare(getContext(), mBitmapToBlur, radius)) {
                mDirty = false;
            } else {
                return false;
            }
        }

        return true;
    }

    protected void blur(Bitmap bitmapToBlur, Bitmap blurredBitmap) {
        mBlurImpl.blur(bitmapToBlur, blurredBitmap);
    }

    private final ViewTreeObserver.OnPreDrawListener preDrawListener = new ViewTreeObserver.OnPreDrawListener() {
        @Override
        public boolean onPreDraw() {
            final int[] locations = new int[2];
            Bitmap oldBmp = mBlurredBitmap;
            View decor = mDecorView;
            if (decor != null && isShown() && prepare()) {
                boolean redrawBitmap = mBlurredBitmap != oldBmp;
                decor.getLocationOnScreen(locations);
                int x = -locations[0];
                int y = -locations[1];

                getLocationOnScreen(locations);
                x += locations[0];
                y += locations[1];

                mBitmapToBlur.eraseColor(mOverlayColor & 0xffffff);

                int rc = mBlurringCanvas.save();
                mIsRendering = true;
                RENDERING_COUNT++;
                try {
                    mBlurringCanvas.scale(1.f * mBitmapToBlur.getWidth() / getWidth(), 1.f * mBitmapToBlur.getHeight() / getHeight());
                    mBlurringCanvas.translate(-x, -y);
                    if (decor.getBackground() != null) {
                        decor.getBackground().draw(mBlurringCanvas);
                    }
                    decor.draw(mBlurringCanvas);
                } catch (RuntimeException ignored) {
                } finally {
                    mIsRendering = false;
                    RENDERING_COUNT--;
                    mBlurringCanvas.restoreToCount(rc);
                }

                blur(mBitmapToBlur, mBlurredBitmap);

                if (redrawBitmap || mDifferentRoot) {
                    invalidate();
                }
            }

            return true;
        }
    };

    protected View getActivityDecorView() {
        Context ctx = getContext();
        for (int i = 0; i < 4 && !(ctx instanceof Activity) && ctx instanceof ContextWrapper; i++) {
            ctx = ((ContextWrapper) ctx).getBaseContext();
        }
        if (ctx instanceof Activity) {
            return ((Activity) ctx).getWindow().getDecorView();
        } else {
            return null;
        }
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        mDecorView = getActivityDecorView();
        if (mDecorView != null) {
            mDecorView.getViewTreeObserver().addOnPreDrawListener(preDrawListener);
            mDifferentRoot = mDecorView.getRootView() != getRootView();
            if (mDifferentRoot) {
                mDecorView.postInvalidate();
            }
        } else {
            mDifferentRoot = false;
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        if (mDecorView != null) {
            mDecorView.getViewTreeObserver().removeOnPreDrawListener(preDrawListener);
        }
        release();
        super.onDetachedFromWindow();
    }

    @Override
    public void draw(Canvas canvas) {
        if (mIsRendering) {
            throw new RuntimeException();
        } else if (RENDERING_COUNT > 0) {
        } else {
            super.draw(canvas);
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        textPath.reset();
        textPaint.getTextPath(text, 0, text.length(), getWidth() / 2f - textPaint.measureText(text) / 2f, getHeight() / 2f + getTextHeightOffset(), textPath);

        canvas.save();

        canvas.clipPath(textPath);

        if (mBlurredBitmap != null) {
            mRectSrc.right = mBlurredBitmap.getWidth();
            mRectSrc.bottom = mBlurredBitmap.getHeight();
            mRectDst.right = getWidth();
            mRectDst.bottom = getHeight();
            canvas.drawBitmap(mBlurredBitmap, mRectSrc, mRectDst, null);
        }
        mPaint.setColor(mOverlayColor);
        canvas.drawRect(mRectDst, mPaint);

        canvas.restore();

    }

    private float getTextHeightOffset() {
        Paint.FontMetrics fontMetrics = textPaint.getFontMetrics();
        return (Math.abs(fontMetrics.ascent) - fontMetrics.descent) / 2;
    }

    public void setText(String newText) {
        if (newText != null && !newText.equals(text)) {
            text = newText;
            invalidate();
        }
    }

    public void setTextSize(float size) {
        if (textPaint.getTextSize() != size) {
            textPaint.setTextSize(size);
            invalidate();
        }
    }

    public void setTypeface(Typeface typeface) {
        if (textPaint.getTypeface() != typeface) {
            textPaint.setTypeface(typeface);
            invalidate();
        }
    }

}