package com.sam_chordas.android.stockhawk.ui;

import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.widget.TextView;

import com.sam_chordas.android.stockhawk.R;

public class LineGraphActivity extends AppCompatActivity {

    private String mStockSymbol;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_line_graph);

        // Preset the list item view from the previous activity using intent data
        if (getIntent().hasExtra(MyStocksActivity.STOCK_SYMBOL_KEY)) {
            mStockSymbol = getIntent().getStringExtra(MyStocksActivity.STOCK_SYMBOL_KEY);
            ((TextView) findViewById(R.id.stock_symbol)).setText(mStockSymbol);
        }
        if (getIntent().hasExtra(MyStocksActivity.STOCK_PRICE_KEY)) {
            String stockPrice = getIntent().getStringExtra(MyStocksActivity.STOCK_PRICE_KEY);
            ((TextView) findViewById(R.id.bid_price)).setText(stockPrice);
        }
        if (getIntent().hasExtra(MyStocksActivity.STOCK_PERCENT_KEY)) {
            String stockPercent = getIntent().getStringExtra(MyStocksActivity.STOCK_PERCENT_KEY);
            TextView change = (TextView) findViewById(R.id.change);
            change.setText(stockPercent);

            boolean stockIncrease = stockPercent.substring(0,1).equals("+");

            int sdk = Build.VERSION.SDK_INT;

            if (stockIncrease){
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
        }


    }
}
