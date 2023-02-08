package de.androidcrypto.imagesplayground;

import static android.os.Build.VERSION.SDK_INT;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.app.Activity;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageDecoder;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.util.Log;
import android.util.Size;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Objects;

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

    Button saveBitmapToExternalSharedStorage;
    TextView saveBitmapToExternalSharedStorageLogFile;

    private Bitmap bitmap;
    private ActivityResultLauncher<String> storageResultActivity;

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

        saveBitmapToExternalSharedStorage = findViewById(R.id.btnSaveBitmapToExternalSharedStorage);
        saveBitmapToExternalSharedStorageLogFile = findViewById(R.id.tvSaveBitmapToExternalSharedStorage);

        registerWriteExternalStoragePermission();

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

                bitmap = original;

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

                Bitmap bitmap = ((BitmapDrawable) imageOriginal.getDrawable()).getBitmap();
                ByteArrayOutputStream stream = new ByteArrayOutputStream();
                bitmap.compress(Bitmap.CompressFormat.JPEG,10,stream);
                byte[] byteArray = stream.toByteArray();
                Bitmap compressedBitmap = BitmapFactory.decodeByteArray(byteArray,0,byteArray.length);
                imageConversion.setImageBitmap(compressedBitmap);
                String sizesData = "imageByteArray length: " + byteArray.length + "\n";
                sizesData += "Image sizes width: " + compressedBitmap.getWidth() +
                        " height: " + compressedBitmap.getHeight() +
                        " pixel: " + (compressedBitmap.getWidth() * compressedBitmap.getHeight());
                imageConversionSizes.setText(sizesData);
/*
                byte[] imageByteArray = getBytes(imageOriginal);
                String sizesData = "imageByteArray length: " + imageByteArray.length + "\n";

                Bitmap bmp = BitmapFactory.decodeByteArray(imageByteArray, 0, imageByteArray.length);
                //ImageView image = (ImageView) findViewById(R.id.imageView2);
                imageConversion.setImageBitmap(Bitmap.createScaledBitmap(bmp, bmp.getWidth(), bmp.getHeight(), false));
                sizesData += "Image sizes width: " + bmp.getWidth() +
                        " height: " + bmp.getHeight() +
                        " pixel: " + (bmp.getWidth() * bmp.getHeight());
                imageConversionSizes.setText(sizesData);
*/
            }
        });

        saveBitmapToExternalSharedStorage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.i(TAG, "save bitmap to external shared storage");

                Bitmap bitmap = ((BitmapDrawable) imageOriginal.getDrawable()).getBitmap();

                if (Build.VERSION.SDK_INT < 29) {
                    System.out.println("SDK < 29");
                    boolean success = saveImageInAndroidApi28AndBelow(bitmap);
                    Log.i(TAG, "the image storing was successful: " + success);
                } else {
                    System.out.println("SDK >= 29");
                    try {
                        Uri uri = saveImageInAndroidApi29AndAbove(bitmap);
                        Log.i(TAG, "the image was stored with this URI: " + uri);
                    } catch (IOException e) {
                        Log.e(TAG, "error on storing image: " + e.getMessage());
                        throw new RuntimeException(e);
                    }

                }

                // more better:
                // https://www.youtube.com/watch?v=tYQ8AO58Aj0

                // better:
                // https://www.youtube.com/watch?v=Ul4hum3y0J8
                // https://www.youtube.com/watch?v=XSlvGizGxEs&t=0s


                // https://www.youtube.com/watch?v=nA4XWsG9IPM
                // How to Save Image to External Storage using Java API 30+ || Scoped Storage android Q R || Java

                //registerWriteExternalStoragePermission();
                //storageResultActivity.launch("");
            }
        });
    }

    /**
     * section for storage of image files
     * source https://stackoverflow.com/a/70766323/8166854
     * author: answered Jan 19, 2022 at 6:47 by OneDev
     */

    private void registerWriteExternalStoragePermission() {
        storageResultActivity = registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
            if (isGranted) {
                checkStoragePermissionAndSaveImage(bitmap);
            }
        });
    }

    private void checkStoragePermissionAndSaveImage(Bitmap bitmap) {
        Log.i(TAG, "checkStoragePermissionAndSaveImage");
        if (ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
            Log.i(TAG, "now: saveImageInAndroidApi28AndBelow(bitmap)");
            saveImageInAndroidApi28AndBelow(bitmap);
        } else if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
            //show user app can't save image for storage permission denied
        } else {
            Log.i(TAG, "storageResultActivity.launch");
            storageResultActivity.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE);
        }

    }

    ActivityResultLauncher<Intent> activityResultLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
        if (result.getResultCode() == RESULT_OK) {
            Intent data = result.getData();
            if (data == null || data.getData() == null) {
                //showError
            }
            Uri uri = data.getData();
            if (Build.VERSION.SDK_INT < 29) {
                try {
                    bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), uri);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            } else {
                ImageDecoder.Source source = ImageDecoder.createSource(getContentResolver(), uri);
                try {
                    bitmap = ImageDecoder.decodeBitmap(source);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }
    });


        /*
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
            saveImageInAndroidApi28AndBelow(bitmap);
        } else if (ActivityCompat.shouldShowRequestPermissionRationale(requireActivity(), Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
            //show user app can't save image for storage permission denied
        } else {
            storageResultActivity.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE);
        }

         */

    private boolean saveImageInAndroidApi28AndBelow(Bitmap bitmap) {
        Log.i(TAG, "saveImageInAndroidApi28AndBelow");
        OutputStream fos;
        String imagesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM).toString();
        // DIRECTORY_DCIM = the image is placed in "external storage/DCIM, not in DCIM/Camera !

        //String imagesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES).toString();

        File image = new File(imagesDir, "IMG_" + System.currentTimeMillis() + ".png");
        try {
            fos = new FileOutputStream(image);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fos);
            Objects.requireNonNull(fos).close();
        } catch (IOException e) {
            e.printStackTrace();
            //isSuccess = false;
            return false;
        }
        //isSuccess = true;
        return true;
    }

    public Uri saveImageInAndroidApi29AndAbove(@NonNull final Bitmap bitmap) throws IOException {
        Log.i(TAG, "saveImageInAndroidApi29AndAbove");
        final ContentValues values = new ContentValues();
        values.put(MediaStore.MediaColumns.DISPLAY_NAME, "IMG_" + System.currentTimeMillis());
        //values.put(MediaStore.MediaColumns.MIME_TYPE, "image/png"); // automatic file extension = .png
        values.put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg"); // automatic file extension = .jpg
        if (SDK_INT >= Build.VERSION_CODES.Q) {
            values.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DCIM);
            // DIRECTORY_DCIM = the image is placed in "external storage/DCIM, not in DCIM/Camera !

            //values.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES);
            // DIRECTORY_PICTURES = the image is placed in "external storage/Pictures

            //values.put(MediaStore.MediaColumns.RELATIVE_PATH, "TestAbc");
            // java.lang.IllegalArgumentException: Primary directory TestAbc not allowed for content://media/external/images/media; allowed directories are [DCIM, Pictures]
        }
        final ContentResolver resolver = getContentResolver();
        Uri uri = null;
        try {
            final Uri contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
            uri = resolver.insert(contentUri, values);
            if (uri == null) {
                //isSuccess = false;
                throw new IOException("Failed to create new MediaStore record.");
            }
            try (final OutputStream stream = resolver.openOutputStream(uri)) {
                if (stream == null) {
                    //isSuccess = false;
                    throw new IOException("Failed to open output stream.");
                }
                //if (!bitmap.compress(Bitmap.CompressFormat.PNG, 95, stream)) {
                if (!bitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream)) {
                    //isSuccess = false;
                    throw new IOException("Failed to save bitmap.");
                }
            }
            //isSuccess = true;
            Log.i(TAG, "the URI is: " + uri);
            return uri;
        } catch (IOException e) {
            if (uri != null) {
                resolver.delete(uri, null, null);
            }
            throw e;
        }
    }

    private boolean saveImageToExternalStorage(String imgName, Bitmap bmp) {
        // https://www.youtube.com/watch?v=nA4XWsG9IPM
        Uri imageCollection = null;
        ContentResolver resolver = getContentResolver();
        // > SDK 28
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            imageCollection = MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY);
        } else {
            imageCollection = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
        }
        ContentValues contentValues = new ContentValues();
        contentValues.put(MediaStore.Images.Media.DISPLAY_NAME, imgName + ".jpg");
        contentValues.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg");
        Uri imageUri = resolver.insert(imageCollection, contentValues);
        try {
            OutputStream outputStream = resolver.openOutputStream(Objects.requireNonNull(imageUri));
            bmp.compress(Bitmap.CompressFormat.JPEG, 100, outputStream);
            Objects.requireNonNull(outputStream);
            return true;
        } catch (Exception e)  {
            Toast.makeText(this, "Image not saved: \n" + e, Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }
        return false;
    }

    /**
     * section for storage of image files END
     */

    public byte[] getBytes(ImageView imageView) {
        try {
            Bitmap bitmap = ((BitmapDrawable) imageView.getDrawable()).getBitmap();
            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            //bitmap.compress(Bitmap.CompressFormat.JPEG, 40, stream);
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);
            byte[] bytesData = stream.toByteArray();
            stream.close();
            return bytesData;
        } catch(Exception e) {
            e.printStackTrace();
            return null;
        }
        //return null;
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