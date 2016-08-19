package com.sam_chordas.android.stockhawk.ui;


import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.sam_chordas.android.stockhawk.R;
import com.sam_chordas.android.stockhawk.rest.Utils;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * A simple {@link Fragment} subclass.
 */
public class DetailFragment extends android.support.v4.app.Fragment {

    public static final String SYMBOL = "symbol";
    public static final String NAME = "name";
    private OkHttpClient client = new OkHttpClient();
    ArrayList<Entry> entries;
    ArrayList<String> labels;
    LineChart lineChart;
    private Handler mHandler;
    ArrayList<ArrayList<String>> result;
    Context mContext;
    String symbolValue;
    String nameValue;

    public DetailFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        Bundle bundle = getArguments();
        if (bundle != null) {
            symbolValue = bundle.getString(SYMBOL);
            nameValue = bundle.getString(NAME);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View rootView = inflater.inflate(R.layout.fragment_detail, container, false);
        mContext = getActivity();
        Intent intent = getActivity().getIntent();
        getActivity().setTitle(nameValue);
        mHandler = new Handler(Looper.getMainLooper());
        lineChart = (LineChart) rootView.findViewById(R.id.linechart);
        entries = new ArrayList<Entry>();
        labels = new ArrayList<String>();
        lineChart.setDescriptionColor(getResources().getColor(R.color.material_blue_500));
        lineChart.setNoDataText("Fetching Data..");
        if (savedInstanceState == null) {
            buildUrl(symbolValue);
        } else {
            if (savedInstanceState.containsKey("Label")) {
                result = new ArrayList<ArrayList<String>>();
                result.add(0, savedInstanceState.getStringArrayList("Label"));
                result.add(1, savedInstanceState.getStringArrayList("Entry"));
                setGraphParameters(result);
                updateChart();
            } else {
                buildUrl(symbolValue);
            }
        }
        return rootView;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (result != null) {
            outState.putStringArrayList("Entry", result.get(1));
            outState.putStringArrayList("Label", result.get(0));
        }
    }

    void fetchData(String url) throws IOException {

        Request request = new Request.Builder()
                .url(url)
                .build();
        Log.v("Before API Call", "Graph");
        client.newCall(request).enqueue(new Callback() {

            @Override
            public void onFailure(Call call, IOException e) {
                Log.v("OnFailure", e.toString());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String responsedata = response.body().string();
                result = Utils.jsonToArrayList(responsedata);
                setGraphParameters(result);
                updateChart();
            }

        });

    }

    public void updateChart() {
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                LineDataSet dataset = new LineDataSet(entries, "Close value");
                LineData data = new LineData(labels, dataset);
                lineChart.setData(data);
                dataset.setHighLightColor(Color.rgb(244, 117, 117));
                dataset.setDrawCircles(true);
                dataset.setDrawFilled(true);
                lineChart.setDescription(symbolValue);
                lineChart.setAutoScaleMinMaxEnabled(true);
                lineChart.animateXY(2000, 3000);
                lineChart.setKeepPositionOnRotation(true);
                lineChart.invalidate();
            }
        });
    }

    public void setGraphParameters(ArrayList<ArrayList<String>> result) {
        ArrayList<String> highValue = result.get(0);
        labels = result.get(1);
        for (int i = 0; i < highValue.size(); i++) {
            String high = highValue.get(i);
            Entry entry = new Entry(Float.parseFloat(high), i);
            entries.add(entry);
        }

    }

    public void buildUrl(String symbol) {
        StringBuilder urlStringBuilder = new StringBuilder();
        Calendar c = Calendar.getInstance();
        SimpleDateFormat f = new SimpleDateFormat("yyyy-MM-dd");
        Date endDate = c.getTime();
        String endDateStr = f.format(endDate);
        c.add(Calendar.DAY_OF_YEAR, -365);
        Date startDate = c.getTime();
        String startDateStr = f.format(startDate);
        try {
            // Base URL for the Yahoo query
            urlStringBuilder.append("https://query.yahooapis.com/v1/public/yql?q=");
            urlStringBuilder.append(URLEncoder.encode(" select * from yahoo.finance.historicaldata " +
                    "where symbol = \"" + symbol + "\" and startDate = \"" + startDateStr + "\" and endDate = \"" + endDateStr + "\" ", "UTF-8"));
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        urlStringBuilder.append("&format=json&diagnostics=true&env=store%3A%2F%2Fdatatables."
                + "org%2Falltableswithkeys");
        String urlString = urlStringBuilder.toString();

        String response = "";
        try {
            fetchData(urlString);
        } catch (IOException e) {
            e.printStackTrace();
        }

    }
}


