package com.izhangqian.mysharedlib;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;

public class LibMain extends View {
    public LibMain(Context context) {
        super(context);
    }

    public LibMain(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public LibMain(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public LibMain(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    public int getMyNumber(int a) {
        return a * a;
    }

    public String getStringById(String id) {
        return getContext().getString(R.string.app_name);
    }
}
