package com.sam_chordas.android.stockhawk.service;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.OperationApplicationException;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.os.RemoteException;
import android.util.Log;

import com.google.android.gms.gcm.GcmNetworkManager;
import com.google.android.gms.gcm.GcmTaskService;
import com.google.android.gms.gcm.TaskParams;
import com.sam_chordas.android.stockhawk.data.HistoricalQuoteColumns;
import com.sam_chordas.android.stockhawk.data.QuoteColumns;
import com.sam_chordas.android.stockhawk.data.QuoteProvider;
import com.sam_chordas.android.stockhawk.rest.Utils;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

/**
 * Created by sam_chordas on 9/30/15.
 * The GCMTask service is primarily for periodic tasks. However, OnRunTask can be called directly
 * and is used for the initialization and adding task as well.
 */
public class StockTaskService extends GcmTaskService {
    private String LOG_TAG = StockTaskService.class.getSimpleName();

    private OkHttpClient client = new OkHttpClient();
    private Context mContext;
    private StringBuilder mStoredSymbols = new StringBuilder();
    private boolean isUpdate;

    public StockTaskService() {
    }

    public StockTaskService(Context context) {
        mContext = context;
    }

    String fetchData(String url) throws IOException {
        Request request = new Request.Builder()
                .url(url)
                .build();

        Response response = client.newCall(request).execute();
        return response.body().string();
    }

    @Override
    public int onRunTask(TaskParams params) {
        Cursor initQueryCursor;
        if (mContext == null) {
            mContext = this;
        }
        StringBuilder urlStringBuilder = new StringBuilder();
        try {
            // Base URL for the Yahoo query
            urlStringBuilder.append("https://query.yahooapis.com/v1/public/yql?q=");
            urlStringBuilder.append(URLEncoder.encode("select * from yahoo.finance.quotes where symbol "
                    + "in (", "UTF-8"));
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        if (params.getTag().equals("init") || params.getTag().equals("periodic")) {
            isUpdate = true;
            initQueryCursor = mContext.getContentResolver().query(QuoteProvider.Quotes.CONTENT_URI,
                    new String[]{"Distinct " + QuoteColumns.SYMBOL}, null,
                    null, null);
            if (initQueryCursor.getCount() == 0 || initQueryCursor == null) {
                // Init task. Populates DB with quotes for the symbols seen below
                try {
                    urlStringBuilder.append(
                            URLEncoder.encode("\"YHOO\",\"AAPL\",\"GOOG\",\"MSFT\")", "UTF-8"));
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
            } else if (initQueryCursor != null) {
                DatabaseUtils.dumpCursor(initQueryCursor);
                initQueryCursor.moveToFirst();
                for (int i = 0; i < initQueryCursor.getCount(); i++) {
                    mStoredSymbols.append("\"" +
                            initQueryCursor.getString(initQueryCursor.getColumnIndex(QuoteColumns.SYMBOL)) + "\",");
                    initQueryCursor.moveToNext();
                }
                mStoredSymbols.replace(mStoredSymbols.length() - 1, mStoredSymbols.length(), ")");
                try {
                    urlStringBuilder.append(URLEncoder.encode(mStoredSymbols.toString(), "UTF-8"));
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
            }
        } else if (params.getTag().equals("add")) {
            isUpdate = false;
            // get symbol from params.getExtra and build query
            String stockInput = params.getExtras().getString("symbol");
            try {
                urlStringBuilder.append(URLEncoder.encode("\"" + stockInput + "\")", "UTF-8"));
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
        }
        // finalize the URL for the API query.
        urlStringBuilder.append("&format=json&diagnostics=true&env=store%3A%2F%2Fdatatables."
                + "org%2Falltableswithkeys&callback=");

        String urlString;
        String getResponse;
        int result = GcmNetworkManager.RESULT_FAILURE;

        if (urlStringBuilder != null) {
            urlString = urlStringBuilder.toString();
            try {
                getResponse = fetchData(urlString);
                result = GcmNetworkManager.RESULT_SUCCESS;
                try {
                    ContentValues contentValues = new ContentValues();
                    // update ISCURRENT to 0 (false) so new data is current
                    if (isUpdate) {
                        contentValues.put(QuoteColumns.ISCURRENT, 0);
                        mContext.getContentResolver().update(QuoteProvider.Quotes.CONTENT_URI, contentValues,
                                null, null);
                    }
                    mContext.getContentResolver().applyBatch(QuoteProvider.AUTHORITY,
                            Utils.quoteJsonToContentVals(getResponse));
                } catch (RemoteException | OperationApplicationException e) {
                    Log.e(LOG_TAG, "Error applying batch insert", e);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }


        // Get calendar object for today
        Calendar calendarToday = Calendar.getInstance();
        // Set calendar date to today (Date objects initialize to current time)
        calendarToday.setTime(new Date());

        // Get calendar object for beginning of historical data graph
        Calendar calendarBegin = Calendar.getInstance();
        calendarBegin.setTime(new Date());
        // Set calendar date to 3 months ago, future feature can modify this
        calendarBegin.add(Calendar.MONTH, -3);

        // Using english locale so that the date format will not change and break the API call
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH);

        // Get strings to represent the begin and today dates for the historical chart
        String beginDate = simpleDateFormat.format(calendarBegin.getTime());
        String todayDate = simpleDateFormat.format(calendarToday.getTime());

        // Create new cursor for the historical data and get data from QuoteProvider
        Cursor historicalQueryCursor;
        historicalQueryCursor = mContext.getContentResolver().query(QuoteProvider.Quotes.CONTENT_URI,
                new String[]{"Distinct " + QuoteColumns.SYMBOL}, null,
                null, null);

        // Make sure it's empty
        DatabaseUtils.dumpCursor(historicalQueryCursor);
        historicalQueryCursor.moveToFirst();

        // For each stock in the list
        for (int i = 0; i < historicalQueryCursor.getCount(); i++) {
            // Keep track if the result is good, default to failure
            int historicalResult = GcmNetworkManager.RESULT_FAILURE;

            // Get stock symbol from cursor
            String stockSymbol = historicalQueryCursor.getString(historicalQueryCursor.getColumnIndex(QuoteColumns.SYMBOL));

            // Start building the string
            StringBuilder urlHistoricalStringBuilder = new StringBuilder();
            try {
                // Base URL for the Yahoo query
                urlHistoricalStringBuilder.append("https://query.yahooapis.com/v1/public/yql?q=");
                // New search string for the historical data
                urlHistoricalStringBuilder.append(URLEncoder.encode("select * from yahoo.finance.historicaldata where symbol " + "= ", "UTF-8"));
                urlHistoricalStringBuilder.append(URLEncoder.encode("\"" + stockSymbol + "\" ", "UTF-8"));
                urlHistoricalStringBuilder.append(URLEncoder.encode("and startDate " + "= ", "UTF-8"));
                urlHistoricalStringBuilder.append(URLEncoder.encode("\"" + beginDate + "\" and endDate " + "= ", "UTF-8"));
                urlHistoricalStringBuilder.append(URLEncoder.encode("\"" + todayDate + "\""));
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
            // Finish url
            urlHistoricalStringBuilder.append("&format=json&diagnostics=true&env=store%3A%2F%2Fdatatables."
                    + "org%2Falltableswithkeys&callback=");

            String urlHistoricalString;

            String getHistoricalResponse;

            if (urlHistoricalStringBuilder != null) {
                urlHistoricalString = urlHistoricalStringBuilder.toString();
                try {
                    getHistoricalResponse = fetchData(urlHistoricalString);
                    historicalResult = GcmNetworkManager.RESULT_SUCCESS;
                    try {
                        // Get content resolver
                        ContentResolver contentResolver = mContext.getContentResolver();

                        // Delete any previous data matching the stock symbol
                        contentResolver.delete(QuoteProvider.HistoricalQuotes.CONTENT_URI,
                        HistoricalQuoteColumns.SYMBOL + " = \"" + stockSymbol + "\"", null);

                        // Add the data to the database
                        mContext.getContentResolver().applyBatch(QuoteProvider.AUTHORITY,
                                Utils.historicalQuoteJsonToContentVals(getHistoricalResponse));

                    } catch (RemoteException | OperationApplicationException e) {
                        Log.e(LOG_TAG, "Error applying batch insert", e);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            // Check if the network was able to complete the task, and if not make the main result fail
            if (historicalResult != GcmNetworkManager.RESULT_SUCCESS) {
                result = GcmNetworkManager.RESULT_FAILURE;
            }

            // Move on to the next stock symbol
            historicalQueryCursor.moveToNext();
        }

        historicalQueryCursor.close();

        return result;
    }

}
