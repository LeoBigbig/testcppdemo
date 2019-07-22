package com.example.testcppdemo;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

/**
 * 聚焦框
 */

public class DrawFocusRect extends View {

    public DrawFocusRect(Context context){
        this(context,null);
    }

    public DrawFocusRect(Context context, AttributeSet attributeSet){
        super(context,attributeSet);
    }

    private int mcolorfill;
    private int mleft, mtop, mwidth, mheight;
    public DrawFocusRect(Context context, int colorfill) {
        super(context);
        // TODO Auto-generated constructor stub
        this.mcolorfill = colorfill;

    }
    @Override
    protected void onDraw(Canvas canvas) {
        // TODO Auto-generated method stub

        mwidth = getWidth()-10;
        mheight = getHeight()-10;

        Paint mpaint = new Paint();
        mpaint.setColor(mcolorfill);
        mpaint.setStyle(Paint.Style.FILL);
        mpaint.setStrokeWidth(1.0f);
        canvas.drawLine(mleft, mtop, mleft+mwidth, mtop, mpaint);
        canvas.drawLine(mleft+mwidth, mtop, mleft+mwidth, mtop+mheight, mpaint);
        canvas.drawLine(mleft, mtop, mleft, mtop+mheight, mpaint);
        canvas.drawLine(mleft, mtop+mheight, mleft+mwidth, mtop+mheight, mpaint);
        super.onDraw(canvas);
    }

}
