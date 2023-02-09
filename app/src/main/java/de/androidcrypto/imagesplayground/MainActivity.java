package de.androidcrypto.imagesplayground;

import static android.os.Build.VERSION.SDK_INT;
import static android.os.ext.SdkExtensions.getExtensionVersion;

import static androidx.core.content.PermissionChecker.PERMISSION_DENIED;
import static androidx.core.content.PermissionChecker.PERMISSION_GRANTED;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.PickVisualMediaRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.annotation.SuppressLint;
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
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

public class MainActivity extends AppCompatActivity {

    private final String TAG = "ImagesPlayground";

    com.google.android.material.textfield.TextInputEditText showAndroidVersion;

    Button loadImageFromGallery, loadImageFromGalleryPhotoPicker;
    ImageView imageOriginal;
    TextView imageOriginalSizes;

    Button scaledDownImage;
    ImageView imageScaledDown;
    TextView imageScaledDownSizes;

    Button bitmapToByteArrayConversion;
    ImageView imageConversion;
    TextView imageConversionSizes;

    Button checkForGrantedStoragePermission;
    boolean storagePermissionIsGranted = false;
    Button grantStoragePermission;
    Button saveBitmapToExternalSharedStorage;
    TextView saveBitmapToExternalSharedStorageLogFile;
    private static final int REQUEST_PERMISSION_WRITE_EXTERNAL_STORAGE = 100;

    private Bitmap bitmap;
    private ActivityResultLauncher<String> storageResultActivity;

    private int androidVersion = 0;
    private boolean photoPickerIsAvailable = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        // encrypt an image file (OWN !) https://stackoverflow.com/questions/66101007/java-aes-gcm-aead-tag-mismatch

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        showAndroidVersion = findViewById(R.id.etShowAndroidVersion);

        loadImageFromGallery = findViewById(R.id.btnLoadImageFromGallery);
        loadImageFromGalleryPhotoPicker = findViewById(R.id.btnLoadImageFromGalleryPhotoPicker);
        imageOriginal = findViewById(R.id.ivOriginal);
        imageOriginalSizes = findViewById(R.id.tvOriginalSizes);

        scaledDownImage = findViewById(R.id.btnScaledDown);
        imageScaledDown = findViewById(R.id.ivScaledDown);
        imageScaledDownSizes = findViewById(R.id.tvScaledDownSizes);

        bitmapToByteArrayConversion = findViewById(R.id.btnBitmapConversionByteArray);
        imageConversion = findViewById(R.id.ivBitmapConversion);
        imageConversionSizes = findViewById(R.id.tvBitmapConversionSizes);

        checkForGrantedStoragePermission = findViewById(R.id.btnCheckGrantedStoragePermission);
        grantStoragePermission = findViewById(R.id.btnGrantStoragePermission);
        saveBitmapToExternalSharedStorage = findViewById(R.id.btnSaveBitmapToExternalSharedStorage);
        saveBitmapToExternalSharedStorageLogFile = findViewById(R.id.tvSaveBitmapToExternalSharedStorage);

        //  registerWriteExternalStoragePermission();

        androidVersion = getAndroidVersion();
        Log.i(TAG, "the app is running on Android version " + androidVersion);
        showAndroidVersion.setText(String.valueOf(androidVersion));

        photoPickerIsAvailable = isPhotoPickerAvailable();

        loadImageFromGallery.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.i(TAG, "load image from Gallery");
                readImageFromExternalSharedStorage();
            }
        });

        loadImageFromGalleryPhotoPicker.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.i(TAG, "load image from Gallery with PhotoPicker");
                if (!photoPickerIsAvailable) {
                    Log.e(TAG, "PhotoPicker is NOT available on system, aborted");
                    Toast.makeText(view.getContext(), "PhotoPicker is NOT available on system, aborted", Toast.LENGTH_SHORT).show();
                    return;
                }

                // note: this line causes a warning but the code will compile and running...
                ActivityResultContracts.PickVisualMedia.VisualMediaType mediaType = (ActivityResultContracts.PickVisualMedia.VisualMediaType) ActivityResultContracts.PickVisualMedia.ImageOnly.INSTANCE;
                PickVisualMediaRequest request = new PickVisualMediaRequest.Builder()
                        .setMediaType(mediaType)
                        .build();
                pickMediaResultLauncher.launch(request);
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
                bitmap.compress(Bitmap.CompressFormat.JPEG, 10, stream);
                byte[] byteArray = stream.toByteArray();
                Bitmap compressedBitmap = BitmapFactory.decodeByteArray(byteArray, 0, byteArray.length);
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

        checkForGrantedStoragePermission.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.i(TAG, "check for granted storage permission");
                int permissionCheck = ContextCompat.checkSelfPermission(view.getContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE);
                saveBitmapToExternalSharedStorageLogFile.setText("check for granted storage permission result: " + permissionCheck +
                        " (granted=" + PERMISSION_GRANTED + " , denied=" + PERMISSION_DENIED + ")");
            }
        });


        grantStoragePermission.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.i(TAG, "grant storage permission");
                verifyPermissionsWriteImage();
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
                    saveBitmapToExternalSharedStorageLogFile.setText("the image storing was successful: " + success);
                } else {
                    System.out.println("SDK >= 29");
                    try {
                        Uri uri = saveImageInAndroidApi29AndAbove(bitmap);
                        Log.i(TAG, "the image was stored with this URI: " + uri);
                        saveBitmapToExternalSharedStorageLogFile.setText("the image was stored with this URI: " + uri);
                    } catch (IOException e) {
                        Log.e(TAG, "error on storing image: " + e.getMessage());
                        throw new RuntimeException(e);
                    }
                }

            }
        });
    }

    /**
     * section for storage of image files
     */

    private void verifyPermissionsWriteImage() {
        String[] permissions = {Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE};
        if (ContextCompat.checkSelfPermission(this.getApplicationContext(),
                permissions[0]) == PackageManager.PERMISSION_GRANTED
                && ContextCompat.checkSelfPermission(this.getApplicationContext(),
                permissions[1]) == PackageManager.PERMISSION_GRANTED) {
            saveBitmapToExternalSharedStorageLogFile.setText("write external storage permissions granted");
            Log.i(TAG, "write external storage permissions granted");
            //writeImageToExternalSharedStorage();
        } else {
            ActivityCompat.requestPermissions(this,
                    permissions,
                    REQUEST_PERMISSION_WRITE_EXTERNAL_STORAGE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_PERMISSION_WRITE_EXTERNAL_STORAGE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                saveBitmapToExternalSharedStorageLogFile.setText("write permission granted");
                Log.i(TAG, "write permission granted");
            } else {
                Toast.makeText(this, "Grant Storage Permission is Required to use this function.", Toast.LENGTH_SHORT).show();
                Log.e(TAG, "write permission NOT granted");
            }
        }
    }

    private boolean saveImageInAndroidApi28AndBelow(Bitmap bitmap) {
        Log.i(TAG, "saveImageInAndroidApi28AndBelow");
        final AtomicBoolean returnResult = new AtomicBoolean(false);
        Thread DoSave = new Thread() {
            public void run() {
                Log.i(TAG, "running Thread DoBasicListFolder");

                OutputStream fos;
                String imagesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM).toString();
                // DIRECTORY_DCIM = the image is placed in "external storage/DCIM, not in DCIM/Camera !

                //String imagesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES).toString();

                File image = new File(imagesDir, "IMG_" + System.currentTimeMillis() + ".png");
                try {
                    fos = new FileOutputStream(image);
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fos);
                    Objects.requireNonNull(fos).close();
                    returnResult.set(true);
                } catch (IOException e) {
                    e.printStackTrace();
                    //isSuccess = false;
                    //return false;
                }
                //isSuccess = true;
                //return true;
            }
        };
        DoSave.start();
        try {
            DoSave.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return returnResult.get();
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
        } catch (Exception e) {
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
        if (ratio >= 1.0) {
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

    private int getAndroidVersion() {
        return Build.VERSION.SDK_INT;
    }

    @SuppressLint("NewApi")
    private boolean isPhotoPickerAvailable() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return true;
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            return getExtensionVersion(Build.VERSION_CODES.R) >= 2;
        } else
            return false;
    }

    // Registers a photo picker activity launcher in single-select mode.
    // Note: When using PickVisualMedia, the photo picker opens in half-screen mode.
    ActivityResultLauncher<PickVisualMediaRequest> pickMediaResultLauncher =
            registerForActivityResult(new ActivityResultContracts.PickVisualMedia(), uri -> {
                // Callback is invoked after the user selects a media item or closes the
                // photo picker.
                if (uri != null) {
                    Log.d("PhotoPicker", "Selected URI: " + uri);
                    // same method as in imageFileLoaderActivityResultLauncher
                    imageOriginal.setImageURI(uri);
                    // get real sizes
                    BitmapDrawable draw = (BitmapDrawable) imageOriginal.getDrawable();
                    Bitmap original = draw.getBitmap();
                    Size size = new Size(original.getWidth(), original.getHeight());
                    String sizesData = "Image sizes width: " + original.getWidth() +
                            " height: " + original.getHeight() +
                            " pixel: " + (original.getWidth() * original.getHeight());
                    imageOriginalSizes.setText(sizesData);
                } else {
                    Log.d("PhotoPicker", "No media selected");
                }
            });
}