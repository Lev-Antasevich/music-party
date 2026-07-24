package com.example.musicparty.ui;

import android.animation.ArgbEvaluator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RadialGradient;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.LinearInterpolator;

import androidx.annotation.ColorInt;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.example.musicparty.R;

/**
 * Soft edge glow over a near-black base — ambient atmosphere that reacts to music colors.
 */
public class AmbientGradientView extends View {

    private static final long COLOR_TRANSITION_MS = 900L;
    private static final long BREATH_DURATION_MS = 4800L;

    private final Paint basePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint blobPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint vignettePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final ArgbEvaluator argbEvaluator = new ArgbEvaluator();

    @ColorInt
    private int colorTop = Color.TRANSPARENT;
    @ColorInt
    private int colorBottom = Color.TRANSPARENT;
    @ColorInt
    private int colorSide = Color.TRANSPARENT;

    private float breathScale = 1f;
    @Nullable
    private ValueAnimator colorAnimator;
    @Nullable
    private ValueAnimator breathAnimator;

    public AmbientGradientView(Context context) {
        super(context);
        init();
    }

    public AmbientGradientView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public AmbientGradientView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        setWillNotDraw(false);
        basePaint.setColor(ContextCompat.getColor(getContext(), R.color.background_dark));
        colorTop = ContextCompat.getColor(getContext(), R.color.glow_pink);
        colorBottom = ContextCompat.getColor(getContext(), R.color.glow_blue);
        colorSide = ContextCompat.getColor(getContext(), R.color.glow_orange);
        startBreathing();
    }

    public void setIdleAtmosphere() {
        setAtmosphereColors(
                ContextCompat.getColor(getContext(), R.color.glow_orange),
                ContextCompat.getColor(getContext(), R.color.glow_pink),
                ContextCompat.getColor(getContext(), R.color.glow_blue),
                true
        );
    }

    public void setAtmosphereColors(
            @ColorInt int top,
            @ColorInt int bottom,
            @ColorInt int side,
            boolean animate
    ) {
        if (!animate) {
            colorTop = top;
            colorBottom = bottom;
            colorSide = side;
            invalidate();
            return;
        }

        if (colorAnimator != null) {
            colorAnimator.cancel();
        }

        final int fromTop = colorTop;
        final int fromBottom = colorBottom;
        final int fromSide = colorSide;

        colorAnimator = ValueAnimator.ofFloat(0f, 1f);
        colorAnimator.setDuration(COLOR_TRANSITION_MS);
        colorAnimator.addUpdateListener(animation -> {
            float fraction = (float) animation.getAnimatedValue();
            colorTop = (int) argbEvaluator.evaluate(fraction, fromTop, top);
            colorBottom = (int) argbEvaluator.evaluate(fraction, fromBottom, bottom);
            colorSide = (int) argbEvaluator.evaluate(fraction, fromSide, side);
            invalidate();
        });
        colorAnimator.start();
    }

    private void startBreathing() {
        if (breathAnimator != null) {
            return;
        }
        breathAnimator = ValueAnimator.ofFloat(0.92f, 1.08f);
        breathAnimator.setDuration(BREATH_DURATION_MS);
        breathAnimator.setRepeatMode(ValueAnimator.REVERSE);
        breathAnimator.setRepeatCount(ValueAnimator.INFINITE);
        breathAnimator.setInterpolator(new LinearInterpolator());
        breathAnimator.addUpdateListener(animation -> {
            breathScale = (float) animation.getAnimatedValue();
            invalidate();
        });
        breathAnimator.start();
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        startBreathing();
    }

    @Override
    protected void onDetachedFromWindow() {
        if (colorAnimator != null) {
            colorAnimator.cancel();
            colorAnimator = null;
        }
        if (breathAnimator != null) {
            breathAnimator.cancel();
            breathAnimator = null;
        }
        super.onDetachedFromWindow();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float width = getWidth();
        float height = getHeight();
        if (width <= 0 || height <= 0) {
            return;
        }

        canvas.drawRect(0, 0, width, height, basePaint);

        float radius = Math.max(width, height) * 0.72f * breathScale;
        drawBlob(canvas, width * 0.08f, height * 0.05f, radius, withAlpha(colorTop, 0.38f));
        drawBlob(canvas, width * 0.95f, height * 0.92f, radius * 0.95f, withAlpha(colorBottom, 0.34f));
        drawBlob(canvas, width * 1.05f, height * 0.35f, radius * 0.7f, withAlpha(colorSide, 0.28f));
        drawBlob(canvas, width * -0.05f, height * 0.7f, radius * 0.55f, withAlpha(colorSide, 0.18f));

        float vignetteRadius = Math.max(width, height) * 0.85f;
        vignettePaint.setShader(new RadialGradient(
                width * 0.5f,
                height * 0.45f,
                vignetteRadius,
                Color.TRANSPARENT,
                withAlpha(ContextCompat.getColor(getContext(), R.color.background_dark), 0.72f),
                Shader.TileMode.CLAMP
        ));
        canvas.drawRect(0, 0, width, height, vignettePaint);
        vignettePaint.setShader(null);
    }

    private void drawBlob(Canvas canvas, float cx, float cy, float radius, @ColorInt int color) {
        blobPaint.setShader(new RadialGradient(
                cx,
                cy,
                radius,
                color,
                Color.TRANSPARENT,
                Shader.TileMode.CLAMP
        ));
        canvas.drawCircle(cx, cy, radius, blobPaint);
        blobPaint.setShader(null);
    }

    @ColorInt
    private static int withAlpha(@ColorInt int color, float alpha) {
        int a = Math.round(255 * Math.max(0f, Math.min(1f, alpha)));
        return Color.argb(a, Color.red(color), Color.green(color), Color.blue(color));
    }
}
