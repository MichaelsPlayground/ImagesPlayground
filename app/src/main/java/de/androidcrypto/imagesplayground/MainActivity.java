package de.androidcrypto.imagesplayground;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.DocumentsContract;
import android.util.Log;
import android.util.Size;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

public class MainActivity extends AppCompatActivity {

    private final String TAG = "ImagesPlayground";

    Button loadImageFromGallery;
    ImageView imageOriginal;
    TextView imageOriginalSizes;

    Button scaledDownImage;
    ImageView imageScaledDown;
    TextView imageScaledDownSizes;

    Button bitmapToByteArrayConversion;
    ImageView imageConversion;
    TextView imageConversionSizes;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        loadImageFromGallery = findViewById(R.id.btnLoadImageFromGallery);
        imageOriginal = findViewById(R.id.ivOriginal);
        imageOriginalSizes = findViewById(R.id.tvOriginalSizes);

        scaledDownImage = findViewById(R.id.btnScaledDown);
        imageScaledDown = findViewById(R.id.ivScaledDown);
        imageScaledDownSizes = findViewById(R.id.tvScaledDownSizes);

        bitmapToByteArrayConversion = findViewById(R.id.btnBitmapConversionByteArray);
        imageConversion = findViewById(R.id.ivBitmapConversion);
        imageConversionSizes = findViewById(R.id.tvBitmapConversionSizes);

        loadImageFromGallery.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.i(TAG, "load image from Gallery");
                readImageFromExternalSharedStorage();
            }
        });

        scaledDownImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.i(TAG, "scale down image");
                BitmapDrawable draw = (BitmapDrawable) imageOriginal.getDrawable();
                Bitmap original = draw.getBitmap();

                Bitmap resized = scaleDown(original, 300, true);
                imageScaledDown.setImageBitmap(resized);
                BitmapDrawable draw2 = (BitmapDrawable) imageScaledDown.getDrawable();
                Bitmap newBitmap = draw2.getBitmap();
                Size size = new Size(newBitmap.getWidth(), newBitmap.getHeight());
                String sizesData = "Image sizes width: " + newBitmap.getWidth() +
                        " height: " + newBitmap.getHeight() +
                        " pixel: " + (newBitmap.getWidth() * newBitmap.getHeight());
                imageScaledDownSizes.setText(sizesData);


            }
        });

        bitmapToByteArrayConversion.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.i(TAG, "bitmap to bytearray conversion");

                /*
                Bitmap bmp = BitmapFactory.decodeByteArray(decryptedPicture, 0, decryptedPicture.length);
                ImageView image = (ImageView) findViewById(R.id.imageView2);
                image.setImageBitmap(Bitmap.createScaledBitmap(bmp, bmp.getWidth(), bmp.getHeight(), false));
                */
            }
        });
    }

    public static Bitmap scaleDown(Bitmap realImage, float maxImageSize,
                                   boolean filter) {
        float ratio = Math.min(
                (float) maxImageSize / realImage.getWidth(),
                (float) maxImageSize / realImage.getHeight());
        int width = Math.round((float) ratio * realImage.getWidth());
        int height = Math.round((float) ratio * realImage.getHeight());

        // to prevent upscaling
        if (ratio >= 1.0){
            return realImage;
        }

        Bitmap newBitmap = Bitmap.createScaledBitmap(realImage, width,
                height, filter);
        return newBitmap;
    }

    private void readImageFromExternalSharedStorage() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.setType("image/*");
        // todo delete routines following
        // does not work on Samsung A5
        //intent.putExtra("crop","true");
        //intent.putExtra("outputFormat", Bitmap.CompressFormat.JPEG.toString());
        // delete until here
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        // Only the system receives the ACTION_OPEN_DOCUMENT, so no need to test.
        //startActivityForResult(intent, REQUEST_IMAGE_OPEN);
        boolean pickerInitialUri = false;
        intent.putExtra(DocumentsContract.EXTRA_INITIAL_URI, pickerInitialUri);
        imageFileLoaderActivityResultLauncher.launch(intent);
    }

    ActivityResultLauncher<Intent> imageFileLoaderActivityResultLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            new ActivityResultCallback<ActivityResult>() {
                @Override
                public void onActivityResult(ActivityResult result) {
                    if (result.getResultCode() == Activity.RESULT_OK) {
                        // There are no request codes
                        Intent resultData = result.getData();
                        // The result data contains the full sized image data
                        // the user selected.
                        Uri uri = null;
                        if (resultData != null) {
                            uri = resultData.getData();
                            imageOriginal.setImageURI(uri);
                            // get real sizes

                            BitmapDrawable draw = (BitmapDrawable) imageOriginal.getDrawable();
                            Bitmap original = draw.getBitmap();
                            Size size = new Size(original.getWidth(), original.getHeight());
                            String sizesData = "Image sizes width: " + original.getWidth() +
                                    " height: " + original.getHeight() +
                                    " pixel: " + (original.getWidth() * original.getHeight());
                            imageOriginalSizes.setText(sizesData);
                        }
                    }
                }
            });
}