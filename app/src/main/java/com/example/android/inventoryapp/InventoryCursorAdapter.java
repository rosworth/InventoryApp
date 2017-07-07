package com.example.android.inventoryapp;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.support.v4.widget.CursorAdapter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.android.inventoryapp.data.InventoryContract.ItemEntry;

public class InventoryCursorAdapter extends CursorAdapter {

    public InventoryCursorAdapter(Context context, Cursor cursor) {
        super(context, cursor, 0);
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        return LayoutInflater.from(context).inflate(R.layout.list_item, parent, false);
    }

    @Override
    public void bindView(View view, final Context context, final Cursor cursor) {
        TextView nameTextView = (TextView) view.findViewById(R.id.textView_item);
        TextView quantityTextView = (TextView) view.findViewById(R.id.textView_quantity);
        ImageView itemImageView = (ImageView) view.findViewById(R.id.imageView_item);
        Button saleButton = (Button) view.findViewById(R.id.item_sale);

        int nameColumnIndex = cursor.getColumnIndex(ItemEntry.COLUMN_ITEM_NAME);
        int quantityColumnIndex = cursor.getColumnIndex(ItemEntry.COLUMN_ITEM_QUANTITY);
        String imageColumnIndex = cursor.getString(cursor.getColumnIndex(ItemEntry.COLUMN_ITEM_IMAGE));

        final String itemName = cursor.getString(nameColumnIndex);
        final int quantity = cursor.getInt(quantityColumnIndex);

        nameTextView.setText(itemName);
        quantityTextView.setText(context.getResources().getString(R.string.in_stock, String.valueOf(quantity)));
        if (imageColumnIndex != null) {
            Uri imageUri = Uri.parse(imageColumnIndex);
            itemImageView.setImageURI(imageUri);
        }
        saleButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (quantity == 0) {
                    Toast.makeText(context, context.getResources().getString(R.string.out_of_stock, itemName), Toast.LENGTH_SHORT).show();
                    return;
                }
                ContentValues values = new ContentValues();
                String[] itemId = {cursor.getString(cursor.getColumnIndex(ItemEntry._ID))};
                String where = ItemEntry._ID + "=?";
                values.put(ItemEntry.COLUMN_ITEM_QUANTITY, quantity - 1);
                context.getContentResolver().update(ItemEntry.CONTENT_URI, values, where, itemId);
            }
        });
    }
}
