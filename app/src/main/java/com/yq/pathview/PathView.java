package com.yq.pathview;

import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PathMeasure;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.view.animation.LinearInterpolator;

/**
 * Created by Administrator on 2018/4/25.
 */

public class PathView extends View {

    String text = "2017";

    private int[] androidStyleAttrs = {android.R.attr.textSize, android.R.attr.textColor};

    private Paint textPaint;
    private PathMeasure pathMeasure = new PathMeasure();
    private Path textPath, drawingPath;
    private float pathLength;
    //测量Path具体范围
    private RectF mPathBounds = new RectF();
    private ValueAnimator drawAnimator;

    public PathView(Context context) {
        this(context, null);
    }

    public PathView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public PathView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.PathView);
        int textSize = typedArray.getDimensionPixelSize(R.styleable.PathView_android_textSize, 30);
        typedArray.recycle();
        //关闭硬件加速，否则 drawPath 不显示
        setLayerType(View.LAYER_TYPE_SOFTWARE, null);

        textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setColor(Color.CYAN);
        textPaint.setTextSize(textSize);
        textPaint.setStyle(Paint.Style.FILL);

        textPath = new Path();
        drawingPath = new Path();
        textPaint.getTextPath(text, 0, text.length(), getWidth() / 2, -textPaint.getFontMetrics().ascent, textPath);
        pathMeasure.setPath(textPath, false);
        pathLength = pathMeasure.getLength();
        while (pathMeasure.nextContour()) {
            pathLength += pathMeasure.getLength();
        }
        Log.i("pathView", "path length = " + pathMeasure.getLength() + ">>>" + pathLength);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        textPath.computeBounds(mPathBounds, true);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        textPaint.setColor(Color.CYAN);
        Paint.FontMetrics fontMetrics = textPaint.getFontMetrics();
//        int baseLineY = (int) (getHeight() / 2 - (fontMetrics.descent + fontMetrics.ascent) / 2);
////        canvas.drawText(text, getWidth() / 2, baseLineY, textPaint);
//
        textPaint.setColor(Color.RED);
        textPaint.setStyle(Paint.Style.STROKE);
//        canvas.save();
        //由于 path 绘制是从左上顶点坐标开始绘制的，所以画布平移距离
        canvas.translate(getWidth() / 2 - mPathBounds.left - mPathBounds.width() / 2, getHeight() / 2 - mPathBounds.top - mPathBounds.height() / 2);
        canvas.drawPath(drawingPath, textPaint);
        canvas.drawRect(mPathBounds, textPaint);
        canvas.drawCircle(position[0], position[1], 20, textPaint);
//        canvas.restore();
    }

    private float[] position = new float[2];

    public void starDrawPath() {
        drawAnimator = ValueAnimator.ofFloat(0, pathLength);
        drawAnimator.setDuration(20000);
        drawAnimator.setInterpolator(new LinearInterpolator());
        drawAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                float animatedValue = (Float) animation.getAnimatedValue();
                Log.i("pathView", "anim value = " + animatedValue);
                pathMeasure.setPath(textPath, false);
                drawingPath.reset();
                //判断执行动画的长度，大于当前路径片段（一个字为一个片段）的长度，就去下一路径片段
                //进行同样的判断，同时将上一片段的路径信息保存到 drawingPath 中，否则再次绘制前面
                //的路径会没有
                while (animatedValue > pathMeasure.getLength()) {
                    Log.i("pathView", "anim value = " + animatedValue + " ;length = " + pathMeasure.getLength());
                    animatedValue = animatedValue - pathMeasure.getLength();
                    //获取之前几个片段路径保存在 drawingPath 中
                    pathMeasure.getSegment(0, pathMeasure.getLength(), drawingPath, true);
                    if (!pathMeasure.nextContour()) {
                        break;
                    }
                }
                boolean clipRes = pathMeasure.getSegment(0, animatedValue, drawingPath, true);
                pathMeasure.getPosTan(animatedValue, position, null);
                Log.i("position", "clipRes = " + clipRes + "x = " + position[0] + "  y = " + position[1]);
//                invalidate();
                postInvalidate();
            }
        });
        drawAnimator.start();
    }

    public void stopDrawPath() {
        if (drawAnimator != null && drawAnimator.isRunning())
            drawAnimator.cancel();
    }

}
