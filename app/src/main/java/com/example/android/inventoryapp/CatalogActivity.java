package com.example.android.inventoryapp;

import android.app.LoaderManager;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.CursorLoader;
import android.content.Intent;
import android.content.Loader;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.Toast;

import com.example.android.inventoryapp.data.InventoryContract.ItemEntry;

public class CatalogActivity extends AppCompatActivity implements LoaderManager.LoaderCallbacks<Cursor> {

    private static final int ITEM_LOADER = 0;
    InventoryCursorAdapter mCursorAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_catalog);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(CatalogActivity.this, DetailsActivity.class);
                startActivity(intent);
            }
        });

        ListView itemListView = (ListView) findViewById(R.id.list);
        View emptyView = findViewById(R.id.empty_view);
        itemListView.setEmptyView(emptyView);
        mCursorAdapter = new InventoryCursorAdapter(this, null);
        itemListView.setAdapter(mCursorAdapter);
        itemListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
                Intent intent = new Intent(CatalogActivity.this, DetailsActivity.class);
                Uri currentItemUri = ContentUris.withAppendedId(ItemEntry.CONTENT_URI, id);
                intent.setData(currentItemUri);
                startActivity(intent);
            }
        });
        getLoaderManager().initLoader(ITEM_LOADER, null, this);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_catalog, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_delete_all:
                int rowsDeleted = getContentResolver().delete(ItemEntry.CONTENT_URI, null, null);
                Toast.makeText(this, rowsDeleted + " items deleted", Toast.LENGTH_SHORT).show();
                return true;
            case R.id.action_test_data:
                insertTestData();
                Toast.makeText(this, R.string.insert_test_data, Toast.LENGTH_SHORT).show();
                return true;
            default:
                Toast.makeText(this, R.string.invalid_menu, Toast.LENGTH_SHORT).show();
                return super.onOptionsItemSelected(item);
        }
    }

    private void insertTestData() {
        ContentValues values = new ContentValues();
        values.put(ItemEntry.COLUMN_ITEM_NAME, "Ball-Point Pen");
        values.put(ItemEntry.COLUMN_ITEM_SUPPLIER, "Pens Inc.");
        values.put(ItemEntry.COLUMN_ITEM_PRICE, 1.99);
        values.put(ItemEntry.COLUMN_ITEM_QUANTITY, 5);
        values.put(ItemEntry.COLUMN_ITEM_IMAGE, "android.resource://com.example.android.inventoryapp/drawable/pen_clipart");
        getContentResolver().insert(ItemEntry.CONTENT_URI, values);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        String[] projection = {
                ItemEntry._ID,
                ItemEntry.COLUMN_ITEM_NAME,
                ItemEntry.COLUMN_ITEM_QUANTITY,
                ItemEntry.COLUMN_ITEM_IMAGE};

        // This loader will execute the ContentProvider's query method on a background thread
        return new CursorLoader(this,   // Parent activity context
                ItemEntry.CONTENT_URI,  // Provider content URI to query
                projection,             // Columns to include in the resulting Cursor
                null,                   // No selection clause
                null,                   // No selection arguments
                null);                  // Default sort order
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        mCursorAdapter.swapCursor(data);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        mCursorAdapter.swapCursor(null);
    }
}
