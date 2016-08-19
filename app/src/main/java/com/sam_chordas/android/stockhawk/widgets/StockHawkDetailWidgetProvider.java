package com.sam_chordas.android.stockhawk.widgets;

import android.annotation.TargetApi;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.support.v4.app.TaskStackBuilder;
import android.util.Log;
import android.widget.RemoteViews;

import com.sam_chordas.android.stockhawk.R;
import com.sam_chordas.android.stockhawk.service.StockTaskService;
import com.sam_chordas.android.stockhawk.ui.DetailActivity;
import com.sam_chordas.android.stockhawk.ui.MyStocksActivity;

/**
 * Created by aharyadi on 7/28/16.
 */
public class StockHawkDetailWidgetProvider extends AppWidgetProvider {
    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);
        if(intent.getAction().equals(StockTaskService.ACTION_DATA_UPDATED)){
            AppWidgetManager manager = AppWidgetManager.getInstance(context);
            int [] widgetIds = manager.getAppWidgetIds(new ComponentName(context,getClass()));
            manager.notifyAppWidgetViewDataChanged(widgetIds,R.id.widget_list_view);
        }
    }

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
       for(int widgetId:appWidgetIds){
           Log.v("InsideWidget","ada");
           RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.collection_widget);
           Intent intent = new Intent(context, MyStocksActivity.class);
           PendingIntent pendingIntent = PendingIntent.getActivity(context,0,intent,0);
           views.setOnClickPendingIntent(R.id.widget,pendingIntent);

           if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH){
               setRemoteAdapter(context,views);
           }
           else{
               setRemoteAdadpterv11(context,views);
           }

           Intent detailIntent = new Intent(context, DetailActivity.class);
           PendingIntent detailPendingIntent =
                   TaskStackBuilder.create(context).addNextIntentWithParentStack(detailIntent).
                           getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);

           views.setPendingIntentTemplate(R.id.widget_list_view,detailPendingIntent);
           views.setEmptyView(R.id.widget_list_view, R.id.widget_text_view);
           appWidgetManager.updateAppWidget(widgetId,views);

       }
    }

    @TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
    public void setRemoteAdapter(Context context,RemoteViews views){
        views.setRemoteAdapter(R.id.widget_list_view,new Intent(context,DetailWidgetRemoteViewService.class));
    }

    public void setRemoteAdadpterv11(Context context,RemoteViews views){
        views.setRemoteAdapter(0,R.id.widget_list_view,new Intent(context,DetailWidgetRemoteViewService.class));
    }
}
