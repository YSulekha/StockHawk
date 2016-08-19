package com.sam_chordas.android.stockhawk.service;

import android.content.ContentProviderOperation;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.OperationApplicationException;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.os.RemoteException;
import android.preference.PreferenceManager;
import android.support.annotation.IntDef;
import android.util.Log;

import com.google.android.gms.gcm.GcmNetworkManager;
import com.google.android.gms.gcm.GcmTaskService;
import com.google.android.gms.gcm.TaskParams;
import com.sam_chordas.android.stockhawk.R;
import com.sam_chordas.android.stockhawk.data.QuoteColumns;
import com.sam_chordas.android.stockhawk.data.QuoteProvider;
import com.sam_chordas.android.stockhawk.rest.Utils;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.net.URLEncoder;
import java.util.ArrayList;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * Created by sam_chordas on 9/30/15.
 * The GCMTask service is primarily for periodic tasks. However, OnRunTask can be called directly
 * and is used for the initialization and adding task as well.
 */
public class StockTaskService extends GcmTaskService{


  @IntDef({STATUS_OK,STATUS_SERVER_DOWN,STATUS_SERVER_INVALID,STATUS_UNKNOWN,UNKNOWN_SYMBOL})
  @Retention(RetentionPolicy.SOURCE)
  public @interface NetworkCallStatus{}

  public static final int STATUS_OK = 0;
  public static final int STATUS_SERVER_DOWN = 1;
  public static final int STATUS_SERVER_INVALID = 2;
  public static final int STATUS_UNKNOWN = 3;
  public static final int UNKNOWN_SYMBOL = 4;

  private String LOG_TAG = StockTaskService.class.getSimpleName();
  public static final String ACTION_DATA_UPDATED = "com.sam_chordas.android.stockhawk.service.ACTION_DATA_UPDATED";


  private OkHttpClient client = new OkHttpClient();
  private Context mContext;
  private StringBuilder mStoredSymbols = new StringBuilder();
  private boolean isUpdate;

  public StockTaskService(){}

  public StockTaskService(Context context){
    mContext = context;
  }
  String fetchData(String url) throws IOException{

    Request request = new Request.Builder()
        .url(url)
        .build();
    Log.v("BeforeAPICall","Before");
    Response response = client.newCall(request).execute();
    Log.v("AfterAPICall","After");
    return response.body().string();
  }

  @Override
  public int onRunTask(TaskParams params){
    Cursor initQueryCursor;
    if (mContext == null){
      mContext = this;
    }
    StringBuilder urlStringBuilder = new StringBuilder();
    try{
      // Base URL for the Yahoo query
      urlStringBuilder.append("https://query.yahooapis.com/v1/public/yql?q=");
      urlStringBuilder.append(URLEncoder.encode("select * from yahoo.finance.quotes where symbol " + "in (", "UTF-8"));
     // urlStringBuilder.append("https://query.yahooapk.com/v1/public/yql?q=");
      //urlStringBuilder.append(URLEncoder.encode("select * from yahoo.finance.quotes where symbol " + "in (", "UTF-8"));
    } catch (UnsupportedEncodingException e) {
      e.printStackTrace();
    }
    if (params.getTag().equals("init") || params.getTag().equals("periodic")){
      isUpdate = true;
      Log.v("TaskService",params.getTag());
      initQueryCursor = mContext.getContentResolver().query(QuoteProvider.Quotes.CONTENT_URI,
          new String[] { "Distinct " + QuoteColumns.SYMBOL }, null,
          null, null);
      if (initQueryCursor.getCount() == 0 || initQueryCursor == null){
        // Init task. Populates DB with quotes for the symbols seen below
        try {
          urlStringBuilder.append(
              URLEncoder.encode("\"YHOO\",\"AAPL\",\"GOOG\",\"MSFT\")", "UTF-8"));
        } catch (UnsupportedEncodingException e) {
          e.printStackTrace();
        }
      } else if (initQueryCursor != null){
        DatabaseUtils.dumpCursor(initQueryCursor);
        initQueryCursor.moveToFirst();
        for (int i = 0; i < initQueryCursor.getCount(); i++){
          mStoredSymbols.append("\""+
              initQueryCursor.getString(initQueryCursor.getColumnIndex("symbol"))+"\",");
          initQueryCursor.moveToNext();
        }
        mStoredSymbols.replace(mStoredSymbols.length() - 1, mStoredSymbols.length(), ")");
        try {
          urlStringBuilder.append(URLEncoder.encode(mStoredSymbols.toString(), "UTF-8"));
        } catch (UnsupportedEncodingException e) {
          e.printStackTrace();
        }
      }
    } else if (params.getTag().equals("add")){
      Log.v("TaskService","add");
      isUpdate = false;
      // get symbol from params.getExtra and build query
      String stockInput = params.getExtras().getString("symbol");
      try {
        urlStringBuilder.append(URLEncoder.encode("\""+stockInput+"\")", "UTF-8"));
      } catch (UnsupportedEncodingException e){
        e.printStackTrace();
      }
    }
    // finalize the URL for the API query.
    urlStringBuilder.append("&format=json&diagnostics=true&env=store%3A%2F%2Fdatatables."
        + "org%2Falltableswithkeys&callback=");

    String urlString;
    String getResponse;
    int result = GcmNetworkManager.RESULT_FAILURE;

    if (urlStringBuilder != null){
      urlString = urlStringBuilder.toString();
      try{
        getResponse = fetchData(urlString);
        result = GcmNetworkManager.RESULT_SUCCESS;
        try {
          ContentValues contentValues = new ContentValues();
          // update ISCURRENT to 0 (false) so new data is current
          if (isUpdate){
            contentValues.put(QuoteColumns.ISCURRENT, 0);
            mContext.getContentResolver().update(QuoteProvider.Quotes.CONTENT_URI, contentValues,
                null, null);
          }
          ArrayList<ContentProviderOperation> operations = Utils.quoteJsonToContentVals(getResponse,mContext);
          if(operations.size()==1 && (operations.get(0)==null || operations.get(0).equals(null))){
            setLocationStatus(mContext,UNKNOWN_SYMBOL);
            Log.v("TaskService","NullValue");
            return -1;
          }
          mContext.getContentResolver().applyBatch(QuoteProvider.AUTHORITY,
              operations);
          setLocationStatus(mContext, STATUS_OK);
          mContext.getContentResolver().delete(QuoteProvider.Quotes.CONTENT_URI,QuoteColumns.ISCURRENT + " = ?",
                  new String[]{"0"});
          notifyWidgets();
        }catch (RemoteException | OperationApplicationException e) {
          Log.e(LOG_TAG, "Error applying batch insert", e);
        }
      } catch (IOException e){
        Log.v("InsideIOException","fjj");
        if(!Utils.checkNetworkState(mContext)){
          setLocationStatus(mContext,STATUS_UNKNOWN);
        }
        else
           setLocationStatus(mContext,STATUS_SERVER_DOWN);
        e.printStackTrace();

      }
    }

    return result;
  }
  public void notifyWidgets(){
    Intent intent = new Intent(ACTION_DATA_UPDATED);
    mContext.sendBroadcast(intent);
  }

  public  static void setLocationStatus(Context context, @NetworkCallStatus int status){
    SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
    SharedPreferences.Editor edit = preferences.edit();
    edit.putInt(context.getString(R.string.preference_status_key),status);
    edit.commit();
  }
}