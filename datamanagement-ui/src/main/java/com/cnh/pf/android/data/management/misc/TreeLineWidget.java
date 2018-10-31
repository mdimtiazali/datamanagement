/*
 * Copyright (C) 2018 CNH Industrial NV. All rights reserved.
 *
 * This software contains proprietary information of CNH Industrial NV. Neither
 * receipt nor possession thereof confers any right to reproduce, use, or
 * disclose in whole or in part any such information without written
 * authorization from CNH Industrial NV.
 *
 */
package com.cnh.pf.android.data.management.misc;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;
import com.android.annotations.Nullable;
import com.cnh.pf.android.data.management.R;

/**
 * view to provide a tree line drawing
 */
public class TreeLineWidget extends View {
    private static final String TAG = "CrossingLineView";
    private Paint paint = new Paint();
    private int mColor = Color.rgb(0xb9,0xb9,0xb9);
    private float mWidth = 2f;
    private boolean corner = false;
    public TreeLineWidget(Context context) {
        super(context);
        setWillNotDraw(false);
        paint.setStrokeWidth(mWidth);
        paint.setColor(mColor);

    }

    public TreeLineWidget(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        setWillNotDraw(false);
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.TreeLineWidget);
        int indexCount = a.getIndexCount();
        for (int i = 0; i < indexCount; i++) {
            int attr = a.getIndex(i);
            switch (attr) {
                case R.styleable.TreeLineWidget_tree_line_color:
                    mColor = a.getColor(attr, mColor);
                    break;
                case R.styleable.TreeLineWidget_tree_line_width:
                    mWidth = a.getFloat(attr,mWidth);
                    break;
                default:
                    break;
            }
        }
        a.recycle();
        paint.setStrokeWidth(mWidth);
        paint.setColor(mColor);
    }


    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float middletopx = getMeasuredWidth()/2;
        float middletopy = 0;
        float middlebottomx = middletopx;
        float middlebottomy = getMeasuredHeight();
        float middlex = middletopx;
        float middley = middlebottomy/2;
        float middlerightx = getMeasuredWidth();
        float middlerighty = middley;

        if(corner) {
            canvas.drawLine(middletopx,middletopy,middlex,middley,paint);

        }
        else{
            canvas.drawLine(middletopx,middletopy,middlebottomx,middlebottomy, paint);
        }
        canvas.drawLine(middlex, middley, middlerightx, middlerighty, paint);
    }

    /**
     * if need to draw a corner line only
     * @param flag
     */
    public void setCornerFlag(boolean flag){
        if(flag != corner) {
            corner = flag;
            invalidate();
        }
    }

    /**
     * Getter for corner
     * @return corner
     */
    public boolean isCorner() {
        return corner;
    }
}
