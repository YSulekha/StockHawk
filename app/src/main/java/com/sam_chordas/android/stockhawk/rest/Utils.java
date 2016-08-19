package com.sam_chordas.android.stockhawk.rest;

import android.content.ContentProviderOperation;
import android.content.Context;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.preference.PreferenceManager;
import android.util.Log;

import com.sam_chordas.android.stockhawk.R;
import com.sam_chordas.android.stockhawk.data.QuoteColumns;
import com.sam_chordas.android.stockhawk.data.QuoteProvider;
import com.sam_chordas.android.stockhawk.service.StockTaskService;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

/**
 * Created by sam_chordas on 10/8/15.
 */
public class Utils {

  private static String LOG_TAG = Utils.class.getSimpleName();

  public static boolean showPercent = true;

  public static ArrayList quoteJsonToContentVals(String JSON, Context mContext){
      Log.v("Utils",JSON);
    ArrayList<ContentProviderOperation> batchOperations = new ArrayList<>();
    JSONObject jsonObject = null;
    JSONArray resultsArray = null;
    try{
      jsonObject = new JSONObject(JSON);
      if (jsonObject != null && jsonObject.length() != 0){
        jsonObject = jsonObject.getJSONObject("query");
        int count = Integer.parseInt(jsonObject.getString("count"));
          Log.v("Utils+Count",String.valueOf(count));
        if (count == 1){
          jsonObject = jsonObject.getJSONObject("results")
              .getJSONObject("quote");
          ContentProviderOperation cop = buildBatchOperation(jsonObject);
      //      Log.v("Utils",cop.toString());
          batchOperations.add(buildBatchOperation(jsonObject));
       //     Log.v("Utils", String.valueOf(batchOperations.size())+batchOperations.get(0));
        } else{
          resultsArray = jsonObject.getJSONObject("results").getJSONArray("quote");

          if (resultsArray != null && resultsArray.length() != 0){
            for (int i = 0; i < resultsArray.length(); i++){
                Log.v("UtilsInsidefor","dsf");
              jsonObject = resultsArray.getJSONObject(i);
              batchOperations.add(buildBatchOperation(jsonObject));
            }
          }
        }
      }
        else{
          StockTaskService.setLocationStatus(mContext,StockTaskService.STATUS_SERVER_DOWN);
      }
    } catch (JSONException e){
        StockTaskService.setLocationStatus(mContext,StockTaskService.STATUS_SERVER_INVALID);
      Log.e(LOG_TAG, "String to JSON failed: " + e);
    }
    return batchOperations;
  }

  public static String truncateBidPrice(String bidPrice){
    bidPrice = String.format("%.2f", Float.parseFloat(bidPrice));
    return bidPrice;
  }

  public static String truncateChange(String change, boolean isPercentChange){
    String weight = change.substring(0,1);
    String ampersand = "";
    if (isPercentChange){
      ampersand = change.substring(change.length() - 1, change.length());
      change = change.substring(0, change.length() - 1);
    }
    change = change.substring(1, change.length());
    double round = (double) Math.round(Double.parseDouble(change) * 100) / 100;
    change = String.format("%.2f", round);
    StringBuffer changeBuffer = new StringBuffer(change);
    changeBuffer.insert(0, weight);
    changeBuffer.append(ampersand);
    change = changeBuffer.toString();
    return change;
  }

  public static ContentProviderOperation buildBatchOperation(JSONObject jsonObject){
    ContentProviderOperation.Builder builder = ContentProviderOperation.newInsert(
            QuoteProvider.Quotes.CONTENT_URI);
    //  Log.v("BatchOperation",jsonObject.toString());
    try {
      String change = jsonObject.getString("Change");
      String symbol =   jsonObject.getString("symbol");
        Log.v("symbol",symbol);
      String bid = jsonObject.getString("Bid") ;

      if((change==null || (change.equals("null")) && ( bid == null) || (bid.equals("null")))){
          Log.v("Utils","NullBid");
          return null;
      }
      builder.withValue(QuoteColumns.SYMBOL, jsonObject.getString("symbol"));
      builder.withValue(QuoteColumns.NAME, jsonObject.getString("Name"));
        Log.v("Name", jsonObject.getString("Name"));
      builder.withValue(QuoteColumns.BIDPRICE, truncateBidPrice(jsonObject.getString("Bid")));
        Log.v("changeInPercent", jsonObject.getString("ChangeinPercent"));
      String changeInPercent =   jsonObject.getString("ChangeinPercent");
        if(changeInPercent == null || changeInPercent.equals("null")){
            Log.v("ChangeInPercentNull",symbol);
            changeInPercent = "+0.00%";
        }
        if(change==null || change.equals("null")){
            Log.v("InsideChangeNull",symbol);
            Log.v("InsideChangeNull",jsonObject.toString());
            change =  "+0.0";

        }
     // builder.withValue(QuoteColumns.PERCENT_CHANGE, truncateChange(
       //   jsonObject.getString("ChangeinPercent"), true));

        builder.withValue(QuoteColumns.PERCENT_CHANGE, truncateChange(
                  changeInPercent, true));
      builder.withValue(QuoteColumns.CHANGE, truncateChange(change, false));
      builder.withValue(QuoteColumns.ISCURRENT, 1);
      if (change.charAt(0) == '-'){
        builder.withValue(QuoteColumns.ISUP, 0);
      }else{
        builder.withValue(QuoteColumns.ISUP, 1);
      }

    } catch (JSONException e){
      e.printStackTrace();
    }
    return builder.build();
  }

  public static ArrayList<ArrayList<String>>  jsonToArrayList(String response){
      ArrayList<ArrayList<String>>  values = new ArrayList<ArrayList<String>>(2);
      ArrayList<String> highPoint = new ArrayList<String>();
      ArrayList<String> date = new ArrayList<String>();
       JSONObject jsonObject = null;
      JSONArray resultsArray = null;
      try {
          jsonObject = new JSONObject(response);
          JSONObject query = null;
          if (jsonObject != null && jsonObject.length() != 0) {
              query = jsonObject.getJSONObject("query");
          }
          JSONObject result = query.getJSONObject("results");
          resultsArray = result.getJSONArray("quote");
          for(int i=0;i<resultsArray.length();i++){
              JSONObject resultObject = resultsArray.getJSONObject(i);
              String highValue = resultObject.getString("Close");
              String dateValue = resultObject.getString("Date");
              highPoint.add(highValue);
              date.add(dateValue);
          }
      }
   catch (JSONException e) {
          e.printStackTrace();
      }
      values.add(highPoint);
      values.add(date);
      return values;
  }
    public static int getNetworkStatus(Context mContext){
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(mContext);
        int status = preferences.getInt(mContext.getString(R.string.preference_status_key),StockTaskService.STATUS_UNKNOWN);
        return status;
    }
    public static boolean checkNetworkState(Context context){
        ConnectivityManager con = (ConnectivityManager)context.getSystemService(context.CONNECTIVITY_SERVICE);
        NetworkInfo ni = con.getActiveNetworkInfo();
        if(ni != null && ni.isConnectedOrConnecting())
            return true;
        return false;
    }
}
