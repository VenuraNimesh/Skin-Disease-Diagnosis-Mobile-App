package com.example.skindiseasediagnosis;

import android.Manifest;
import android.app.Activity;
import android.app.Dialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.CursorIndexOutOfBoundsException;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import java.io.EOFException;
import java.io.File;
import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import top.zibin.luban.CompressionPredicate;
import top.zibin.luban.Luban;
import top.zibin.luban.OnCompressListener;
import top.zibin.luban.OnRenameListener;

public class MainActivity extends AppCompatActivity {

    private final OkHttpClient client = new OkHttpClient().newBuilder().build();

    private static final String TAG = "MainActivity";
    private LinearLayout selectLayout, uploadLayout;
    private ImageView previewImage;
    private Button selectImageButton, cancelButton, uploadButton;
    private TextView treatmentsButton, helpButton;

    private boolean isFromCamera = false;
    private boolean isFromGallery = false;
    private boolean hasSelectedImage = false;

    private final int PICK_FROM_GALLERY = 1001;
    private final int TAKE_FROM_CAMERA = 2002;
    private final int ASK_PERMISSIONS = 3003;

    private final String[] REQUIRED_PERMISSIONS = {
            Manifest.permission.CAMERA,
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
    };

    private String temporaryPath, imageName, finalPath;
    private File folder, temporaryImage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        folder = new File(getExternalFilesDirectoryPath());

        selectLayout = findViewById(R.id.selectLayout);
        uploadLayout = findViewById(R.id.uploadLayout);
        previewImage = findViewById(R.id.previewImage);
        selectImageButton = findViewById(R.id.selectImageButton);
        cancelButton = findViewById(R.id.cancelButton);
        uploadButton = findViewById(R.id.uploadButton);
        treatmentsButton = findViewById(R.id.treatmentsButton);
        helpButton = findViewById(R.id.helpButton);

        uploadLayout.setVisibility(View.GONE);

        selectImageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showImagePickOptions();
            }
        });

        cancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                selectLayout.setVisibility(View.VISIBLE);
                uploadLayout.setVisibility(View.GONE);
                hasSelectedImage = false;
                isFromCamera = false;
                isFromGallery = false;
                previewImage.setImageResource(R.drawable.image);
            }
        });

        uploadButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                uploadImage();
            }
        });

        treatmentsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MainActivity.this, TreatmentsActivity.class));
            }
        });

        helpButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MainActivity.this, HelpActivity.class));
            }
        });
    }

    private void uploadImage() {
        post(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                // Something went wrong
                e.printStackTrace();
                showDialog(true, "Failed to connect to server.");
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    try {
                        String responseStr = response.peekBody(2048).string(); // Ref - https://stackoverflow.com/a/60750929
                        Log.d(TAG,response.body().string());
                        showDialog(false, responseStr);
                    } catch (EOFException e) {
                        e.printStackTrace();
                    }
                } else {
                    Log.e(TAG,response.body().string());
                    showDialog(true, "Server error occurred.");
                }
            }
        });
    }

    Call post(Callback callback) { // Ref - https://stackoverflow.com/a/28135573
        MediaType mediaType = MediaType.parse("text/plain");
        RequestBody body = new MultipartBody.Builder().setType(MultipartBody.FORM)
                .addFormDataPart("file", imageName,
                        RequestBody.create(MediaType.parse("application/octet-stream"),
                                new File(finalPath)))
                .build();
        String url = "http://35.185.179.181";
        Request request = new Request.Builder()
                .url(url)
                .method("POST", body)
                .build();

        Call call = client.newCall(request);
        call.enqueue(callback);
        return call;
    }

    private void showDialog(boolean isError, String msg) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                final Dialog dialog = new Dialog(MainActivity.this);
                dialog.setCanceledOnTouchOutside(false);
                dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
                //following statement is essential to make corners of dialog box round
                // used transparent instead R.drawable.dialog_box to move close button out of dialog box
                dialog.getWindow().setBackgroundDrawable(ContextCompat.getDrawable(MainActivity.this, R.color.transparent));
                dialog.setContentView(R.layout.dialog_box);

                ImageView closeIcon = dialog.findViewById(R.id.closeIcon);
                TextView hintTextView = dialog.findViewById(R.id.hintTextView);
                TextView diseaseTextView = dialog.findViewById(R.id.diseaseTextView);
                Button treatmentBtn = dialog.findViewById(R.id.treatmentBtn);

                if (isError) {
                    hintTextView.setText("Error");
                    hintTextView.setTextColor(ContextCompat.getColor(MainActivity.this, R.color.red));
                    treatmentBtn.setVisibility(View.GONE);
                } else {
                    hintTextView.setVisibility(View.VISIBLE);
                    treatmentBtn.setVisibility(View.VISIBLE);
                    treatmentBtn.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            startActivity(new Intent(MainActivity.this, TreatmentsActivity.class));
                        }
                    });
                }

                diseaseTextView.setText(msg);
                closeIcon.bringToFront();
                closeIcon.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        dialog.dismiss();
                    }
                });

                dialog.show();
            }
        });
    }

    private void showImagePickOptions() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Select Image");

        final String[] options = new String[]{"From Camera", "From Gallery"};

        builder.setItems(options, (dialog, which) -> {
            switch (options[which]) {
                case "From Camera":
                    isFromCamera = true;
                    isFromGallery = false;
                    checkPermissions(this);

                    break;
                case "From Gallery":
                    isFromCamera = false;
                    isFromGallery = true;
                    checkPermissions(this);

                    break;
            }
        }).create().show();
    }

    private void checkPermissions(Activity activity) {
        boolean isGranted = true;
        for (String permission : REQUIRED_PERMISSIONS) {
            isGranted = isGranted && (ContextCompat.checkSelfPermission(activity, permission) == PackageManager.PERMISSION_GRANTED);
        }

        if (!isGranted) { // permission required
            ActivityCompat.requestPermissions(activity, REQUIRED_PERMISSIONS, ASK_PERMISSIONS);
        } else {
            if (isFromCamera) {
                takeFromCamera();
            } else if (isFromGallery) {
                pickFromGallery();
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == ASK_PERMISSIONS && grantResults.length > 0) {
            boolean hasPermissions = (grantResults[0] == PackageManager.PERMISSION_GRANTED)
                    && (grantResults[1] == PackageManager.PERMISSION_GRANTED)
                    && (grantResults[2] == PackageManager.PERMISSION_GRANTED);
            if (hasPermissions) {
                if (isFromCamera) {
                    takeFromCamera(); // start camera once the permission has been granted
                } else if (isFromGallery) {
                    pickFromGallery(); // open image picker once the permission has been granted
                }
            }
        }
    }

    private void takeFromCamera() {
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        imageName = String.valueOf(System.currentTimeMillis());

        try {
            temporaryImage = File.createTempFile(imageName, ".jpg", folder);
            temporaryPath = "file:" + temporaryImage.getAbsolutePath(); // temp file path
            imageName = imageName + ".jpg";  // required name for resized output image

            Uri outUri = FileProvider.getUriForFile(this, BuildConfig.APPLICATION_ID + ".provider", temporaryImage);
            intent.putExtra(MediaStore.EXTRA_OUTPUT, outUri);
            startActivityForResult(intent, TAKE_FROM_CAMERA);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void pickFromGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        if (intent.resolveActivity(getPackageManager()) != null) {
            startActivityForResult(intent, PICK_FROM_GALLERY);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == TAKE_FROM_CAMERA && resultCode == Activity.RESULT_OK) {
            if (temporaryPath != null) {
                Uri imageUri = Uri.parse(temporaryPath);
                File file = new File(imageUri.getPath());
                compressImage(file, imageName);
            }
        } else if (requestCode == PICK_FROM_GALLERY && resultCode == Activity.RESULT_OK) {
            if (data != null) {
                Uri photoUri = data.getData();
                imageName = System.currentTimeMillis() + ".jpg";
                File file = new File(getImageFilePathFromUri(photoUri));
                compressImage(file, imageName);
            }
        }
    }

    private void compressImage(File file, String imageName) {
        Luban.with(this)
                .load(file)
                .ignoreBy(80)
                .setTargetDir(folder.getAbsolutePath())
                .filter(new CompressionPredicate() {
                    @Override
                    public boolean apply(String path) {
                        return !(TextUtils.isEmpty(path) || path.toLowerCase().endsWith(".gif"));
                    }
                })
                .setRenameListener(new OnRenameListener() {
                    @Override
                    public String rename(String filePath) {
                        return imageName; // rename image
                    }
                })
                .setCompressListener(new OnCompressListener() {
                    @Override
                    public void onStart() {
                    }

                    @Override
                    public void onSuccess(File file) {
                        hasSelectedImage = true;
                        finalPath = file.getAbsolutePath();
                        Bitmap bitmap = getCompressedBitmapFromFilePath(finalPath);
                        previewImage.setImageBitmap(bitmap);
                        selectLayout.setVisibility(View.GONE);
                        uploadLayout.setVisibility(View.VISIBLE);

                        if (isFromCamera) { // this check not required when bitmapToFile() method
                            // is used as it creates a temporary image for the image picked rom gallery
                            if (temporaryImage != null && temporaryImage.exists()) {
                                temporaryImage.delete(); // delete original image
                            }
                        }
                    }

                    @Override
                    public void onError(Throwable e) {
                        e.printStackTrace();
                    }
                }).launch();
    }

    private String getImageFilePathFromUri(Uri uri) {
        File file = new File(uri.getPath());
        String[] filePath = file.getPath().split("/");
        String image_id = filePath[filePath.length - 1];

        try {
            Cursor cursor = getContentResolver().query(android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI, null, MediaStore.Images.Media._ID + " = ? ", new String[]{image_id}, null);
            if (cursor != null) {
                cursor.moveToFirst();
                String imagePath = cursor.getString(cursor.getColumnIndex(MediaStore.Images.Media.DATA));
                cursor.close();
                return imagePath;
            }
        } catch (CursorIndexOutOfBoundsException e) {
            e.printStackTrace();
        }
        return null;
    }

    private String getExternalFilesDirectoryPath() {
        return new File(getExternalFilesDir(Environment.DIRECTORY_PICTURES) + "").getAbsolutePath();
    }

    private Bitmap getCompressedBitmapFromFilePath(String imagePath) {
        BitmapFactory.Options bmOptions = new BitmapFactory.Options();
        bmOptions.inSampleSize = 2; // resample
        return BitmapFactory.decodeFile(imagePath, bmOptions);
    }
}