package com.sam_chordas.android.stockhawk.widgets;

import android.content.Intent;
import android.database.Cursor;
import android.os.Binder;
import android.os.Build;
import android.widget.AdapterView;
import android.widget.RemoteViews;
import android.widget.RemoteViewsService;

import com.sam_chordas.android.stockhawk.R;
import com.sam_chordas.android.stockhawk.data.QuoteColumns;
import com.sam_chordas.android.stockhawk.data.QuoteProvider;
import com.sam_chordas.android.stockhawk.rest.Utils;
import com.sam_chordas.android.stockhawk.ui.DetailFragment;

public class DetailWidgetRemoteViewService extends RemoteViewsService {
    @Override
    public RemoteViewsFactory onGetViewFactory(Intent intent) {
        return new RemoteViewsFactory() {
            Cursor data = null;

            @Override
            public void onCreate() {

            }

            @Override
            public void onDataSetChanged() {
                if (data != null) {
                    data.close();
                    data = null;
                }
                final long identityToken = Binder.clearCallingIdentity();
                data = getContentResolver().query(QuoteProvider.Quotes.CONTENT_URI, null, QuoteColumns.ISCURRENT + " = ?",
                        new String[]{"1"}, null);
                Binder.restoreCallingIdentity(identityToken);

            }

            @Override
            public void onDestroy() {
                if (data != null) {
                    data.close();
                    data = null;
                }
            }

            @Override
            public int getCount() {
                return data == null ? 0 : data.getCount();
            }

            @Override
            public RemoteViews getViewAt(int position) {
                if (position == AdapterView.INVALID_POSITION || data == null | !data.moveToPosition(position)) {
                    return null;
                }
                RemoteViews remoteViews = new RemoteViews(getPackageName(), R.layout.collection_list_item);
                String symbol = data.getString(data.getColumnIndex(QuoteColumns.SYMBOL));
                String bidPrice = data.getString(data.getColumnIndex(QuoteColumns.BIDPRICE));
                String name = data.getString(data.getColumnIndex(QuoteColumns.NAME));
                String change;
                if (Utils.showPercent) {
                    change = data.getString(data.getColumnIndex("percent_change"));
                } else {
                    change = data.getString(data.getColumnIndex("change"));
                }
                remoteViews.setTextViewText(R.id.widget_stock_symbol, symbol);
                remoteViews.setTextViewText(R.id.widget_stock_bidprice, bidPrice);
                remoteViews.setTextViewText(R.id.widget_stock_name, name);
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
                final Intent fillinIntent = new Intent();
                fillinIntent.putExtra(DetailFragment.SYMBOL, symbol);
                fillinIntent.putExtra(DetailFragment.NAME, name);
                remoteViews.setOnClickFillInIntent(R.id.widget_list_item, fillinIntent);
                return remoteViews;
            }

            @Override
            public RemoteViews getLoadingView() {
                return new RemoteViews(getPackageName(), R.layout.collection_list_item);
            }

            @Override
            public int getViewTypeCount() {
                return 1;
            }

            @Override
            public long getItemId(int position) {
                if (data.moveToPosition(position)) {
                    return data.getLong(data.getColumnIndex(QuoteColumns._ID));
                }
                return 0;
            }

            @Override
            public boolean hasStableIds() {
                return true;
            }
        };
    }
}
