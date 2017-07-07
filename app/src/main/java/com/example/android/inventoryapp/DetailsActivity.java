package com.example.android.inventoryapp;

import android.app.AlertDialog;
import android.app.LoaderManager;
import android.content.ContentValues;
import android.content.CursorLoader;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.Loader;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.NavUtils;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.example.android.inventoryapp.data.InventoryContract.ItemEntry;

public class DetailsActivity extends AppCompatActivity implements LoaderManager.LoaderCallbacks<Cursor> {

    private static final int EXISTING_PET_LOADER = 0;
    private static final int SELECT_PICTURE = 1;
    private Uri mCurrentUri;
    private EditText mNameEditText;
    private EditText mSupplierEditText;
    private EditText mPriceEditText;
    private EditText mQuantityEditText;
    private int quantity;
    private Uri selectedImageUri;

    private boolean mItemHasChanged = false;
    private View.OnTouchListener mTouchListener = new View.OnTouchListener() {
        @Override
        public boolean onTouch(View view, MotionEvent motionEvent) {
            mItemHasChanged = true;
            return false;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_details);

        Intent intent = getIntent();
        mCurrentUri = intent.getData();
        if (mCurrentUri == null) {
            setTitle(getString(R.string.editor_activity_title_new_item));
            invalidateOptionsMenu();
        } else {
            setTitle(getString(R.string.editor_activity_title_edit_item));
            getLoaderManager().initLoader(EXISTING_PET_LOADER, null, this);
        }
        mNameEditText = (EditText) findViewById(R.id.editText_name);
        mSupplierEditText = (EditText) findViewById(R.id.editText_supplier);
        mPriceEditText = (EditText) findViewById(R.id.editText_price);
        mQuantityEditText = (EditText) findViewById(R.id.editText_quantity);
        Button mImageEdit = (Button) findViewById(R.id.edit_image);

        mNameEditText.setOnTouchListener(mTouchListener);
        mSupplierEditText.setOnTouchListener(mTouchListener);
        mPriceEditText.setOnTouchListener(mTouchListener);
        mQuantityEditText.setOnTouchListener(mTouchListener);

        mImageEdit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent();
                intent.setType("image/*");
                intent.setAction(Intent.ACTION_GET_CONTENT);
                startActivityForResult(Intent.createChooser(intent, getString(R.string.select_image)), SELECT_PICTURE);
            }
        });

        Button increment = (Button) findViewById(R.id.button_increment);
        increment.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (TextUtils.isEmpty(mQuantityEditText.getText())) {
                    mQuantityEditText.setText("1");
                } else {
                    quantity = Integer.parseInt(mQuantityEditText.getText().toString());
                    quantity++;
                    mQuantityEditText.setText(String.valueOf(quantity));

                }
            }
        });

        Button decrement = (Button) findViewById(R.id.button_decrement);
        decrement.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!TextUtils.isEmpty(mQuantityEditText.getText())) {
                    quantity = Integer.parseInt(mQuantityEditText.getText().toString());
                    if (quantity <= 0) {
                        //do nothing
                        return;
                    }
                    quantity--;
                    mQuantityEditText.setText(String.valueOf(quantity));
                }
            }
        });

        Button order = (Button) findViewById(R.id.order_more);
        if (mCurrentUri == null) {
            //hide order button if new item
            order.setVisibility(View.GONE);
        } else {
            order.setVisibility(View.VISIBLE);
            order.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    final String message = orderInfo();
                    Intent intent = new Intent(Intent.ACTION_SENDTO);
                    intent.setData(Uri.parse("mailto:" + mSupplierEditText.getText().toString()));
                    intent.putExtra(Intent.EXTRA_SUBJECT, mNameEditText.getText().toString());
                    intent.putExtra(Intent.EXTRA_TEXT, message);
                    if (intent.resolveActivity(getPackageManager()) != null) {
                        startActivity(intent);
                    }
                }
            });
        }
    }

    private String orderInfo() {
        StringBuilder sb = new StringBuilder();
        if (inputCheck(mNameEditText, mSupplierEditText, mPriceEditText)) {
            sb.append("\nProduct: " + mNameEditText.getText().toString());
            sb.append("\nPrice: " + mPriceEditText.getText().toString());
            sb.append("\nQuantity: " + mQuantityEditText.getText().toString());
        } else {
            sb.append("\nFailed to get product information.");
        }
        return sb.toString();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            if (requestCode == SELECT_PICTURE) {
                selectedImageUri = data.getData();
                if (selectedImageUri != null) {
                    Toast.makeText(this, R.string.upload_successful, Toast.LENGTH_SHORT).show();
                }
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_details, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
        if (mCurrentUri == null) {
            MenuItem menuItem = menu.findItem(R.id.action_delete);
            menuItem.setVisible(false);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_delete:
                showDeleteConfirmationDialog();
                return true;
            case R.id.action_clear:
                clearFields();
                Toast.makeText(this, R.string.field_cleared, Toast.LENGTH_SHORT).show();
                return true;
            case R.id.action_save:
                saveItem();
                return true;
            case android.R.id.home:
                if (!mItemHasChanged) {
                    NavUtils.navigateUpFromSameTask(DetailsActivity.this);
                    return true;
                } else {
                    DialogInterface.OnClickListener discardButtonClickListener =
                            new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {
                                    NavUtils.navigateUpFromSameTask(DetailsActivity.this);
                                }
                            };
                    showUnsavedChangesDialog(discardButtonClickListener);
                    return true;
                }
            default:
                Toast.makeText(this, R.string.invalid_menu, Toast.LENGTH_SHORT).show();
                return super.onOptionsItemSelected(item);
        }
    }

    private void saveItem() {
        String nameString = mNameEditText.getText().toString().trim();
        String supplierString = mSupplierEditText.getText().toString().trim();
        String priceString = mPriceEditText.getText().toString().trim();
        String quantityString = mQuantityEditText.getText().toString().trim();

        if (mCurrentUri == null &&
                TextUtils.isEmpty(nameString) && TextUtils.isEmpty(supplierString) &&
                TextUtils.isEmpty(priceString) && TextUtils.isEmpty(quantityString)) {
            return;
        }

        if (inputCheck(mNameEditText, mSupplierEditText, mPriceEditText)) {

            ContentValues values = new ContentValues();
            values.put(ItemEntry.COLUMN_ITEM_NAME, nameString);
            values.put(ItemEntry.COLUMN_ITEM_SUPPLIER, supplierString);
            values.put(ItemEntry.COLUMN_ITEM_PRICE, Double.parseDouble(priceString));
            int quantity = 0;
            if (!TextUtils.isEmpty(quantityString)) {
                quantity = Integer.parseInt(quantityString);
            }
            values.put(ItemEntry.COLUMN_ITEM_QUANTITY, quantity);
            values.put(ItemEntry.COLUMN_ITEM_IMAGE, String.valueOf(selectedImageUri));
            if (mCurrentUri == null) {
                Uri newUri = getContentResolver().insert(ItemEntry.CONTENT_URI, values);
                if (newUri == null) {
                    Toast.makeText(this, getString(R.string.editor_insert_item_failed), Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, getString(R.string.editor_insert_item_successful), Toast.LENGTH_SHORT).show();
                }
            } else {
                int rowsAffected = getContentResolver().update(mCurrentUri, values, null, null);
                if (rowsAffected == 0) {
                    Toast.makeText(this, getString(R.string.editor_update_item_failed), Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, getString(R.string.editor_update_item_successful), Toast.LENGTH_SHORT).show();
                }
            }
            finish();
        }
    }

    private boolean inputCheck(EditText name, EditText supplier, EditText price) {
        if (TextUtils.isEmpty(name.getText().toString())) {
            mNameEditText.setError(getString(R.string.input_error_name));
            Toast.makeText(this, getString(R.string.input_error_name), Toast.LENGTH_SHORT).show();
            return false;
        }
        if (TextUtils.isEmpty(supplier.getText().toString())) {
            mSupplierEditText.setError(getString(R.string.input_error_supplier));
            Toast.makeText(this, getString(R.string.input_error_supplier), Toast.LENGTH_SHORT).show();
            return false;
        }
        //input type number--no need to check for invalid characters
        if (TextUtils.isEmpty(price.getText().toString())) {
            mPriceEditText.setError(getString(R.string.input_error_price));
            Toast.makeText(this, getString(R.string.input_error_price), Toast.LENGTH_SHORT).show();
            return false;
        }
        return true;
    }

    private void clearFields() {
        mNameEditText.setText("");
        mSupplierEditText.setText("");
        mPriceEditText.setText("");
        mQuantityEditText.setText("0");
    }

    private void deleteItem() {
        if (mCurrentUri != null) {
            int rowsDeleted = getContentResolver().delete(mCurrentUri, null, null);
            if (rowsDeleted == 0) {
                Toast.makeText(this, getString(R.string.editor_delete_item_failed),
                        Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, getString(R.string.editor_delete_item_successful),
                        Toast.LENGTH_SHORT).show();
            }
        }
        finish();
    }

    private void showDeleteConfirmationDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(R.string.delete_dialog_msg);
        builder.setPositiveButton(R.string.delete, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                deleteItem();
            }
        });
        builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                if (dialog != null) {
                    dialog.dismiss();
                }
            }
        });
        AlertDialog alertDialog = builder.create();
        alertDialog.show();
    }

    @Override
    public void onBackPressed() {
        if (!mItemHasChanged) {
            super.onBackPressed();
            return;
        }
        DialogInterface.OnClickListener discardButtonClickListener =
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        finish();
                    }
                };
        showUnsavedChangesDialog(discardButtonClickListener);
    }

    private void showUnsavedChangesDialog(DialogInterface.OnClickListener discardButtonClickListener) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(R.string.unsaved_changes_dialog_msg);
        builder.setPositiveButton(R.string.discard, discardButtonClickListener);
        builder.setNegativeButton(R.string.keep_editing, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                if (dialog != null) {
                    dialog.dismiss();
                }
            }
        });
        AlertDialog alertDialog = builder.create();
        alertDialog.show();
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        String[] projection = {
                ItemEntry._ID,
                ItemEntry.COLUMN_ITEM_NAME,
                ItemEntry.COLUMN_ITEM_SUPPLIER,
                ItemEntry.COLUMN_ITEM_PRICE,
                ItemEntry.COLUMN_ITEM_QUANTITY};

        return new CursorLoader(this,   // Parent activity context
                mCurrentUri,            // Query the content URI for the current pet
                projection,             // Columns to include in the resulting Cursor
                null,                   // No selection clause
                null,                   // No selection arguments
                null);                  // Default sort order);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
        if (cursor == null || cursor.getCount() < 1) {
            return;
        }
        if (cursor.moveToFirst()) {
            int nameColumnIndex = cursor.getColumnIndex(ItemEntry.COLUMN_ITEM_NAME);
            int supplierColumnIndex = cursor.getColumnIndex(ItemEntry.COLUMN_ITEM_SUPPLIER);
            int priceColumnIndex = cursor.getColumnIndex(ItemEntry.COLUMN_ITEM_PRICE);
            int quantityColumnIndex = cursor.getColumnIndex(ItemEntry.COLUMN_ITEM_QUANTITY);

            String name = cursor.getString(nameColumnIndex);
            String supplier = cursor.getString(supplierColumnIndex);
            double price = cursor.getDouble(priceColumnIndex);
            int quantity = cursor.getInt(quantityColumnIndex);

            mNameEditText.setText(name);
            mSupplierEditText.setText(supplier);
            mPriceEditText.setText(Double.toString(price));
            mQuantityEditText.setText(Integer.toString(quantity));
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        clearFields();
    }
}
