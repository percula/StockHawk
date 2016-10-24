package com.sam_chordas.android.stockhawk.widget;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.widget.RemoteViews;
import android.widget.RemoteViewsService;

import com.sam_chordas.android.stockhawk.R;
import com.sam_chordas.android.stockhawk.data.QuoteColumns;
import com.sam_chordas.android.stockhawk.data.QuoteProvider;
import com.sam_chordas.android.stockhawk.ui.MyStocksActivity;

/**
 * Created by peter on 10/22/16.
 */

public class DetailWidgetRemoteViewsFactory implements RemoteViewsService.RemoteViewsFactory {

    private Context mContext;
    private Cursor mCursor;

    public DetailWidgetRemoteViewsFactory(Context context, Intent intent) {
        mContext = context;
    }

    @Override
    public void onCreate() {

    }

    @Override
    public int getCount() {
        return mCursor.getCount();
    }

    @Override
    public void onDataSetChanged() {
        if (mCursor != null) {
            mCursor.close();
        }

        mCursor = mContext.getContentResolver().query(
                QuoteProvider.Quotes.CONTENT_URI,
                new String[]{QuoteColumns._ID,
                        QuoteColumns.SYMBOL,
                        QuoteColumns.BIDPRICE,
                        QuoteColumns.PERCENT_CHANGE,
                        QuoteColumns.CHANGE,
                        QuoteColumns.ISUP},
                QuoteColumns.ISCURRENT + " = ?",
                new String[]{"1"},
                null);
    }

    @Override
    public int getViewTypeCount() {
        return 1;
    }

    @Override
    public RemoteViews getViewAt(int i) {
        RemoteViews remoteViews = new RemoteViews(mContext.getPackageName(), R.layout.list_item_quote);
        if (mCursor.moveToPosition(i)) {
            remoteViews.setTextViewText(R.id.stock_symbol, mCursor.getString(mCursor.getColumnIndex(QuoteColumns.SYMBOL)));
            remoteViews.setTextViewText(R.id.bid_price, mCursor.getString(mCursor.getColumnIndex(QuoteColumns.BIDPRICE)));
            remoteViews.setTextViewText(R.id.change, mCursor.getString(mCursor.getColumnIndex(QuoteColumns.CHANGE)));


            if (mCursor.getInt(mCursor.getColumnIndex(QuoteColumns.ISUP)) == 1) {
                remoteViews.setInt(R.id.change, "setBackgroundResource", R.drawable.percent_change_pill_green);
            } else {
                remoteViews.setInt(R.id.change, "setBackgroundResource", R.drawable.percent_change_pill_red);

            }

            // Create intent to open the detail view if clicked
            Intent intent = new Intent();
            intent.putExtra(MyStocksActivity.STOCK_SYMBOL_KEY, mCursor.getString(mCursor.getColumnIndex(QuoteColumns.SYMBOL)));
            remoteViews.setOnClickFillInIntent(R.id.list_item,intent);
        }

        return remoteViews;
    }

    @Override
    public long getItemId(int i) {
        return i;
    }

    @Override
    public void onDestroy() {
        if (null != mCursor) {
            mCursor.close();
        }
    }

    @Override
    public RemoteViews getLoadingView() {
        return null;
    }

    @Override
    public boolean hasStableIds() {
        return true;
    }
}
