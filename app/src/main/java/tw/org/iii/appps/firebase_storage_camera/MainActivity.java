package tw.org.iii.appps.firebase_storage_camera;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

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
import android.widget.VideoView;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.squareup.picasso.Picasso;

import java.io.File;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {
    private Button mUploadBtn,page2_btn;
    private ImageView mImageView;
    private static final int CAMERA_REQUEST_CODE = 1;//自己設定的相機回應馬
    private static final int VIDEO_REQUEST = 100;//自己設定的相機回應馬

    private  FirebaseStorage firebaseStorage;
    private StorageReference  mStorage;

    private ProgressDialog mDialog;
    private Uri saveUri;

    private File sdroot;

    private Uri videoUri;
    private VideoView videoView;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //1.相機權限
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

        videoView = findViewById(R.id.main_video);


        //1.照相按鈕,存檔
        mUploadBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
             saveUri =  FileProvider.getUriForFile(//照相機拍照,存檔
                        MainActivity.this,
                        getPackageName()+".provider",
                        new File(sdroot+"/DCIM/Camera","zzz.jpg")
                        );
                Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);//Intent相機出來
                intent.putExtra(android.provider.MediaStore.EXTRA_OUTPUT,saveUri);//掛上素質(1.外布檔案,2寫好的fileProvider路徑)
                startActivityForResult(intent,CAMERA_REQUEST_CODE);

                Log.v("brad","拍照成功,savuri:" +saveUri);
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




    //2.接收照片ok,上傳到FirebaseStorage
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if(requestCode == CAMERA_REQUEST_CODE && resultCode == RESULT_OK){//如果回應的code==設定向紀的code,而且結果回應 ==按下ok的畫
            mDialog.setMessage("Upload..");
            mDialog.show();
           Log.v("brad","onActivityResult=> requestCode:" +requestCode +" ,resultCode:" + resultCode +" ,uri:" + saveUri);

            String imageName = UUID.randomUUID().toString();

           final StorageReference filepath = mStorage.child("images/" + imageName);

           //拍照檔案上傳成功時
           filepath.putFile(saveUri).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
               @Override
               public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                   mDialog.dismiss();

                   //上傳檔案下載成功時
                   filepath.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                       @Override
                       public void onSuccess(Uri uri) {
                           Picasso.with(MainActivity.this).load(uri).into(mImageView);
                           Log.v("brad","getDownloadrUrl.onSuccess => uri:" + uri);
                       }
                   });

                   Toast.makeText(MainActivity.this,"上傳成功",Toast.LENGTH_SHORT).show();
                   Log.v("brad","onSuccess,taskSnapsoht:" + taskSnapshot.toString());
               }
           })
                   //檔案下載失敗時:
                   .addOnFailureListener(new OnFailureListener() {
               @Override
               public void onFailure(@NonNull Exception e) {
                   Log.v("brad","OnFailureListner:" + e.toString());
               }
           });
        }
        //接受錄影程式,取得錄影的uri,顯示出來
        else if(requestCode == VIDEO_REQUEST && resultCode == RESULT_OK){
            videoView.setVideoURI(videoUri);
            videoView.start();
            Log.v("brad","onActivityResult => videoUri:" + videoUri);
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

    //3.呼叫錄影程式,錄影存檔
    public void captureVideoBtn(View view) {
        Intent videoIntent = new Intent(MediaStore.ACTION_VIDEO_CAPTURE);
        if(videoIntent.resolveActivity(getPackageManager())!= null){
            videoUri =  FileProvider.getUriForFile(//照相機拍照,存檔
                    MainActivity.this,
                    getPackageName()+".provider",
                    new File(sdroot+"/DCIM/Camera","zzz.jpg")
            );
            videoIntent.putExtra(android.provider.MediaStore.EXTRA_OUTPUT,videoUri);//掛上素質(1.外布檔案,2寫好的fileProvider路徑)
            startActivityForResult(videoIntent,VIDEO_REQUEST);
            Log.v("brad","captureVideoBtn => PackgetManager:" + getPackageManager().toString());
        }
    }
    //4.將取得的VideoUri送到第二頁作呈現
    public void playVideoBtn(View view) {
        Intent playIntent = new Intent(MainActivity.this,PlayVideoActivity.class);
        playIntent.putExtra("videoUri",videoUri);
        startActivity(playIntent);
    }


}
