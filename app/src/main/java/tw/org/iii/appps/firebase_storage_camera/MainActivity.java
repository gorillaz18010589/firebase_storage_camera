package tw.org.iii.appps.firebase_storage_camera;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.File;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {
    private Button mUploadBtn,page2_btn;
    private ImageView mImageView;
    private static final int CAMERA_REQUEST_CODE = 1;//自己設定的相機回應馬

    private  FirebaseStorage firebaseStorage;
    private StorageReference  mStorage;

    private ProgressDialog mDialog;
    private Uri saveUri;

    private File sdroot;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.CAMERA) //改相機權限
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.CAMERA}, //改相機權限
                    12);
        }
        mUploadBtn = findViewById(R.id.camera_btn);
        mImageView =findViewById(R.id.camera_image);
        page2_btn =findViewById(R.id.page2_btn);

        firebaseStorage = FirebaseStorage.getInstance();
        mStorage = firebaseStorage.getReference();

        mDialog = new ProgressDialog(this);

        sdroot =  Environment.getExternalStorageDirectory();

        //照相按鈕
        mUploadBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);//Intent相機出來
                startActivityForResult(intent,CAMERA_REQUEST_CODE);


            }
        });
        //第二頁按鈕
        page2_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(MainActivity.this,CameraActivity.class);
                startActivity(intent);
            }
        });
    }
    //getLastPathSegment()
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if(requestCode == CAMERA_REQUEST_CODE && resultCode == RESULT_OK){//如果回應的code==設定向紀的code,而且結果回應 ==按下ok的畫
            mDialog.setMessage("Upload..");
            mDialog.show();
           Log.v("brad","onActivityResult=> requestCode:" +requestCode +" ,resultCode:" + resultCode +" ,uri:" + saveUri);

            String imageName = UUID.randomUUID().toString();
            saveUri = data.getData();

           StorageReference filepath = mStorage.child("images/" + imageName);
           filepath.putFile(saveUri).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
               @Override
               public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                   mDialog.dismiss();
                   Toast.makeText(MainActivity.this,"上傳成功",Toast.LENGTH_SHORT).show();
               }
           }).addOnFailureListener(new OnFailureListener() {
               @Override
               public void onFailure(@NonNull Exception e) {
                   Log.v("brad","OnFailureListner:" + e.toString());
               }
           });
        }
    }

    public static Uri getMediaUriFromPath(Context context, String path) {
        Uri mediaUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
        Cursor cursor = context.getContentResolver().query(mediaUri,
                null,
                MediaStore.Images.Media.DISPLAY_NAME + "= ?",
                new String[] {path.substring(path.lastIndexOf("/") + 1)},
                null);

        Uri uri = null;
        if(cursor.moveToFirst()) {
            uri = ContentUris.withAppendedId(mediaUri,
                    cursor.getLong(cursor.getColumnIndex(MediaStore.Images.Media._ID)));
        }
        cursor.close();
        return uri;
    }
}
