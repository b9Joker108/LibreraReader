package com.foobnix.android.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.webkit.JavascriptInterface;
import android.webkit.WebSettings;
import android.webkit.WebView;

import java.io.OutputStream;


public class WebViewUtils {

    public static class WebAppInterface {
        Runnable run;

        WebAppInterface(Runnable run) {
            this.run = run;
        }

        @JavascriptInterface
        public void finish() {
            run.run();
        }
    }

    static public WebView web;

    public static android.os.Handler handler = new Handler(Looper.getMainLooper());

    public static void init(Context activity) {
        web = new WebView(activity);
        //web.setPadding(0, 0, 0, 0);
        web.getSettings().setJavaScriptEnabled(true);
        web.getSettings().setSupportZoom(false);
        web.getSettings().setLoadWithOverviewMode(true);
        web.getSettings().setUseWideViewPort(true);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            WebView.enableSlowWholeDocumentDraw();
        }
        web.getSettings().setCacheMode(WebSettings.LOAD_DEFAULT);

        //web.setScrollBarStyle(WebView.SCROLLBARS_OUTSIDE_OVERLAY);
        //web.setScrollbarFadingEnabled(false);
        //web.setInitialScale(1);


        web.layout(0, 0, Dips.screenMinWH(), Dips.screenMinWH());
    }


    public static void renterToPng(String name, String content, OutputStream os, Object lock) {
        String h, f;
        boolean isMath = false;
        if (content.trim().startsWith("<math")) {
            h = "<html><head>\n" +
                    "<script type=\"text/javascript\"src=\"https://cdnjs.cloudflare.com/ajax/libs/mathjax/2.7.5/MathJax.js?config=MML_CHTML\"></script>\n" +
                    "<script type=\"text/javascript\"> MathJax.Hub.Register.StartupHook(\"End\",function () { android.finish() }); </script>\n" +
                    "</head><body>";


            f = "</body></html>";
            isMath = true;
        } else {
            h = "<html><head>\n" +
                    "</head><body onload=\" android.finish() \">";
            f = "</body></html>";
            isMath = false;
        }

        final String contentWrapper = h + content + f;


        Runnable execute = new Runnable() {
            @Override
            public void run() {
                try {
                    LOG.d("web.getContentHeight()", web.getContentHeight());

                    Bitmap bitmap = Bitmap.createBitmap(Dips.screenMinWH(), (int) (web.getContentHeight() * 1.1), Bitmap.Config.ARGB_8888);
                    Canvas c = new Canvas(bitmap);
                    web.draw(c);


                    //bitmap = ImageExtractor.cropBitmap(bitmap, bitmap);


                    Bitmap.CompressFormat format = Bitmap.CompressFormat.PNG;
                    bitmap.compress(format, 100, os);
                    bitmap.recycle();


                } finally {
                    synchronized (lock) {
                        lock.notify();
                    }
                }

            }
        };

        handler.post(() -> {
            LOG.d("loadData-content", contentWrapper);
            web.loadData(contentWrapper, "text/html", "utf-8");
            web.addJavascriptInterface(new WebAppInterface(() -> handler.postDelayed(execute, 50)), "android");

        });


    }


}