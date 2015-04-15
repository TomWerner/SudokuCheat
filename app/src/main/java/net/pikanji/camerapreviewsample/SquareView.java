package net.pikanji.camerapreviewsample;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.View;

/**
 * TODO: document your custom view class.
 */
public class SquareView extends View {
    private Paint blackPaint;
    private Paint tranparentPaint;

    public SquareView(Context context) {
        super(context);
        init(null, 0);
    }

    public SquareView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(attrs, 0);
    }

    public SquareView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(attrs, defStyle);
    }

    private void init(AttributeSet attrs, int defStyle) {
        // Load attributes
        final TypedArray a = getContext().obtainStyledAttributes(
                attrs, R.styleable.SquareView, defStyle, 0);


        a.recycle();

        // Set up a default TextPaint object
        blackPaint = new Paint();
        blackPaint.setFlags(Paint.ANTI_ALIAS_FLAG);
        blackPaint.setColor(Color.BLACK);
        blackPaint.setStyle(Paint.Style.STROKE);
        blackPaint.setStrokeWidth(10);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        // TODO: consider storing these as member variables to reduce
        // allocations per draw cycle.
        int paddingLeft = getPaddingLeft();
        int paddingTop = getPaddingTop();
        int paddingRight = getPaddingRight();
        int paddingBottom = getPaddingBottom();

        int contentWidth = getWidth() - paddingLeft - paddingRight;
        int contentHeight = getHeight() - paddingTop - paddingBottom;

        float squareSize = Math.min(contentWidth, contentHeight);
        squareSize *= .85f;
        canvas.drawRect(paddingLeft + (contentWidth / 2 - squareSize / 2),
                paddingTop +  (contentHeight / 2 - squareSize / 2),
                paddingLeft + (contentWidth / 2 - squareSize / 2) + squareSize,
                paddingTop +  (contentHeight / 2 - squareSize / 2 + squareSize), blackPaint);
    }
}
