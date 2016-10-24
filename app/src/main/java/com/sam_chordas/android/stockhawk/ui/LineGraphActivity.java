package com.sam_chordas.android.stockhawk.ui;

import android.app.LoaderManager;
import android.content.CursorLoader;
import android.content.Loader;
import android.database.Cursor;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.widget.TextView;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.Description;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.sam_chordas.android.stockhawk.R;
import com.sam_chordas.android.stockhawk.data.HistoricalQuoteColumns;
import com.sam_chordas.android.stockhawk.data.QuoteColumns;
import com.sam_chordas.android.stockhawk.data.QuoteProvider;
import com.sam_chordas.android.stockhawk.rest.DayAxisValueFormatter;
import com.sam_chordas.android.stockhawk.rest.Utils;

import java.util.ArrayList;
import java.util.List;

public class LineGraphActivity extends AppCompatActivity implements LoaderManager.LoaderCallbacks<Cursor> {

    private String mStockSymbol;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_line_graph);

        // Preset the list item view from the previous activity using intent data
        if (getIntent().hasExtra(MyStocksActivity.STOCK_SYMBOL_KEY)) {
            mStockSymbol = getIntent().getStringExtra(MyStocksActivity.STOCK_SYMBOL_KEY);

            // Initiate loaders to fill in data
            getLoaderManager().initLoader(MyStocksActivity.HISTORICAL_LOADER_ID, null, this);
            getLoaderManager().initLoader(MyStocksActivity.CURSOR_LOADER_ID, null, this);
        }
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args){
        if (id == MyStocksActivity.HISTORICAL_LOADER_ID) {
            // This returns all stocks in historical quotes table matching the stock symbol
            return new CursorLoader(this, // Context
                    QuoteProvider.HistoricalQuotes.CONTENT_URI, // Uri to data
                    new String[]{QuoteColumns._ID, // Projection
                            HistoricalQuoteColumns.SYMBOL,
                            HistoricalQuoteColumns.DATE,
                            HistoricalQuoteColumns.CLOSE},
                    HistoricalQuoteColumns.SYMBOL + " = ?", // Selection criteria
                    new String[]{mStockSymbol}, // Selection arguments
                    HistoricalQuoteColumns.DATE + " ASC"); // Sort order
        }
        if (id == MyStocksActivity.CURSOR_LOADER_ID){
            // This returns the current stock to fill in the details
            return new CursorLoader(this,
                    QuoteProvider.Quotes.CONTENT_URI, // Uri to data
                    new String[]{QuoteColumns._ID, // Projection (columns to return)
                            QuoteColumns.SYMBOL,
                            QuoteColumns.BIDPRICE,
                            QuoteColumns.PERCENT_CHANGE,
                            QuoteColumns.CHANGE,
                            QuoteColumns.ISUP},
                    QuoteColumns.SYMBOL + " = ?", // Selection criteria
                    new String[]{mStockSymbol}, // Selection arguments
                    null); // Sort order
        }
        return null;
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data){
        if (loader.getId() == MyStocksActivity.HISTORICAL_LOADER_ID) {
            // Create lists to hold the date and price values as floats.
            List<Entry> entries = new ArrayList<Entry>();

            data.moveToFirst();

            for (int i = 0; i < data.getCount(); i++) {
                String dateStr = data.getString(data.getColumnIndex(HistoricalQuoteColumns.DATE));
                String priceStr = data.getString(data.getColumnIndex(HistoricalQuoteColumns.CLOSE));

                Float date = Utils.convertDateToFloat(dateStr);
                Float price = Float.parseFloat(priceStr);

                entries.add(new Entry(date, price));

                data.moveToNext();
            }

            LineDataSet dataSet = new LineDataSet(entries, mStockSymbol); // add entries to dataset
            dataSet.setColor(getResources().getColor(R.color.material_blue_500));
            dataSet.setValueTextColor(getResources().getColor(R.color.material_blue_500)); // styling, ...
            dataSet.setDrawValues(false);

            LineChart chart = (LineChart) findViewById(R.id.chart);

            LineData lineData = new LineData(dataSet);
            chart.setData(lineData);
            chart.getLegend().setTextColor(Color.WHITE);
            Description description = new Description();
            description.setTextColor(Color.WHITE);
            description.setText(getString(R.string.history));
            chart.setDescription(description);
            chart.setTouchEnabled(false);


            // Set properties for the x axis
            XAxis xAxis = chart.getXAxis();
            xAxis.setTextColor(Color.WHITE);
            xAxis.setValueFormatter(new DayAxisValueFormatter(chart));

            // Set properties for the left and right axis
            YAxis leftAxis = chart.getAxisLeft();
            YAxis rightAxis = chart.getAxisRight();
            leftAxis.setTextColor(Color.WHITE);
            rightAxis.setTextColor(Color.WHITE);

            chart.invalidate(); // refresh
        }
        if (loader.getId() == MyStocksActivity.CURSOR_LOADER_ID){
            data.moveToFirst();
            ((TextView) findViewById(R.id.stock_symbol)).setText(data.getString(data.getColumnIndex(QuoteColumns.SYMBOL)));
            ((TextView) findViewById(R.id.bid_price)).setText(data.getString(data.getColumnIndex(QuoteColumns.BIDPRICE)));
            TextView change = (TextView) findViewById(R.id.change);
            int sdk = Build.VERSION.SDK_INT;
            if (data.getInt(data.getColumnIndex(QuoteColumns.ISUP)) == 1){
                if (sdk < Build.VERSION_CODES.JELLY_BEAN){
                    change.setBackgroundDrawable(
                            getResources().getDrawable(R.drawable.percent_change_pill_green));
                }else {
                    change.setBackground(
                            getResources().getDrawable(R.drawable.percent_change_pill_green));
                }
            } else{
                if (sdk < Build.VERSION_CODES.JELLY_BEAN) {
                    change.setBackgroundDrawable(
                            getResources().getDrawable(R.drawable.percent_change_pill_red));
                } else{
                    change.setBackground(
                            getResources().getDrawable(R.drawable.percent_change_pill_red));
                }
            }
            if (Utils.showPercent){
                change.setText(data.getString(data.getColumnIndex(QuoteColumns.PERCENT_CHANGE)));
            } else{
                change.setText(data.getString(data.getColumnIndex(QuoteColumns.CHANGE)));
            }
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader){

    }
}
