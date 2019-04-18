package com.zcy.renderdemo;

import android.app.Application;
import android.content.Context;

public class App extends Application {
    public static Context getContext() {
        return context;
    }

    public void setContext(Context context) {
        this.context = context;
    }

    private static Context context;
    @Override
    public void onCreate() {
        super.onCreate();
        context=this;
    }
}
