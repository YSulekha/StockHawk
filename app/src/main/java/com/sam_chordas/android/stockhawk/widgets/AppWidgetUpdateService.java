package com.sam_chordas.android.stockhawk.widgets;

import android.annotation.TargetApi;
import android.app.IntentService;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Intent;
import android.database.Cursor;
import android.os.Build;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.widget.RemoteViews;

import com.sam_chordas.android.stockhawk.R;
import com.sam_chordas.android.stockhawk.data.QuoteColumns;
import com.sam_chordas.android.stockhawk.data.QuoteProvider;
import com.sam_chordas.android.stockhawk.rest.Utils;
import com.sam_chordas.android.stockhawk.ui.MyStocksActivity;

public class AppWidgetUpdateService extends IntentService {

    //Basic widget for stockHawk
    public AppWidgetUpdateService(String name) {
        super(name);
    }

    public AppWidgetUpdateService() {
        super(AppWidgetUpdateService.class.getName());
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(this);
        int[] appWidgetIds = appWidgetManager.getAppWidgetIds(new ComponentName(this, StockHawkWidgetProvider.class));
        Cursor data = getContentResolver().query(QuoteProvider.Quotes.CONTENT_URI, null
                , null, null, null);
        if (data == null) {
            return;
        }
        if (!data.moveToFirst()) {
            return;
        }
        int defaultWidth = getResources().getDimensionPixelSize(R.dimen.widget_default_width);
        int minWidth = getResources().getDimensionPixelSize(R.dimen.widget_small_width);

        String symbol = data.getString(data.getColumnIndex(QuoteColumns.SYMBOL));
        String bidPrice = data.getString(data.getColumnIndex(QuoteColumns.BIDPRICE));
        String change;
        if (Utils.showPercent) {
            change = data.getString(data.getColumnIndex("percent_change"));
        } else {
            change = data.getString(data.getColumnIndex("change"));
        }
        int layoutId;
        for (int widgetId : appWidgetIds) {
            int currentWidth = getWidgetWidth(appWidgetManager, widgetId);
            if (currentWidth < defaultWidth) {
                layoutId = R.layout.small_widget;
            } else
                layoutId = R.layout.default_widget;
            RemoteViews remoteViews = new RemoteViews(getPackageName(), layoutId);
            remoteViews.setTextViewText(R.id.widget_stock_symbol, symbol);
            remoteViews.setTextViewText(R.id.widget_stock_bidprice, bidPrice);
            int sdk = Build.VERSION.SDK_INT;
            if (data.getInt(data.getColumnIndex("is_up")) == 1) {
                if (sdk < Build.VERSION_CODES.JELLY_BEAN) {
                    remoteViews.setInt(R.id.widget_stock_percent, "setBackgroundResource", R.drawable.percent_change_pill_green);
                } else {
                    remoteViews.setInt(R.id.widget_stock_percent, "setBackgroundResource", R.drawable.percent_change_pill_green);
                }
            } else {
                if (sdk < Build.VERSION_CODES.JELLY_BEAN) {
                    remoteViews.setInt(R.id.widget_stock_percent, "setBackgroundResource", R.drawable.percent_change_pill_red);
                } else {
                    remoteViews.setInt(R.id.widget_stock_percent, "setBackgroundResource", R.drawable.percent_change_pill_red);
                }
            }

            remoteViews.setTextViewText(R.id.widget_stock_percent, change);
            Intent stockIntent = new Intent(this, MyStocksActivity.class);
            PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, stockIntent, 0);
            remoteViews.setOnClickPendingIntent(R.id.widget, pendingIntent);
            appWidgetManager.updateAppWidget(widgetId, remoteViews);
        }

    }

    private int getWidgetWidth(AppWidgetManager manager, int widgetId) {
        int sdk = Build.VERSION.SDK_INT;
        if (sdk < Build.VERSION_CODES.JELLY_BEAN) {
            return getResources().getDimensionPixelSize(R.dimen.widget_default_width);
        }
        return getWidgetWidthFromOptions(manager, widgetId);
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    private int getWidgetWidthFromOptions(AppWidgetManager manager, int widgetId) {
        Bundle options = manager.getAppWidgetOptions(widgetId);
        if (options.containsKey(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH)) {
            int minidth = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH);
            DisplayMetrics displayMetrics = getResources().getDisplayMetrics();
            int minWidthdp = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, minidth, displayMetrics);
            return minWidthdp;
        }
        return getResources().getDimensionPixelSize(R.dimen.widget_default_width);
    }
}
