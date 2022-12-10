package com.lino.shiablsfmk;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.support.v7.widget.AppCompatEditText;
import android.util.AttributeSet;
import android.util.Log;

public class LineNumberedEditText extends AppCompatEditText {
    private final Context context;
    private Rect rect;
    private Paint paint;

    public LineNumberedEditText(Context context) {
        super(context);
        this.context = context;
        init();
    }

    public LineNumberedEditText(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.context = context;
        init();
    }

    public LineNumberedEditText(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        this.context = context;
        init();
    }

    private void init() {
        rect = new Rect();
        paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.GRAY);
        paint.setTextSize(40);
        paint.setTypeface(Typeface.MONOSPACE);
        setPadding(80, getPaddingTop(), getPaddingRight(), getPaddingBottom());
    }

    @Override
    protected void onDraw(Canvas canvas) {
        int lineNumber = 2;
        int lineCount= getLineCount();
        int baseline = getLineBounds(0, null);
        canvas.drawText("1", rect.left, baseline, paint);
        for (int i = 1; i < lineCount; ++i) {
            baseline = getLineBounds(i, null);
            if (getText().charAt(getLayout().getLineStart(i) - 1) == '\n') {
                canvas.drawText("" + lineNumber, rect.left, baseline, paint);
                ++lineNumber;
            }
        }
        super.onDraw(canvas);
    }
//
//    int baseline;
//    int lineCount = getLineCount();
//    int lineNumber = 1;
//
//        for (int i = 0; i < lineCount; ++i)
//    {
//        baseline=getLineBounds(i, null);
//        if (i == 0)
//        {
//            canvas.drawText(""+lineNumber, rect.left, baseline, paint);
//            ++lineNumber;
//        }
//        else if (getText().charAt(getLayout().getLineStart(i) - 1) == '\n')
//        {
//            canvas.drawText(""+lineNumber, rect.left, baseline, paint);
//            ++lineNumber;
//        }
//     } if(lineCount<100)
//        {
//            setPadding(60,getPaddingTop(),getPaddingRight(),getPaddingBottom());
//        }
//        else if(lineCount<1000)
//        {
//            setPadding(70,getPaddingTop(),getPaddingRight(),getPaddingBottom());
//        }
//        else if(lineCount<10000)
//        {
//            setPadding(80,getPaddingTop(),getPaddingRight(),getPaddingBottom());
//        }
//        else if(lineCount<100000)
//        {
//            setPadding(90,getPaddingTop(),getPaddingRight(),getPaddingBottom());
//        }


}