package tw.org.iii.appps.firebase_storage_camera;
//手機是電燈硬體,結合在相機裡面運用
//震動權限要加入 <uses-permission android:name="android.permission.VIBRATE"/>
//閃光燈是相機在控制要相機權限:    <uses-permission android:name="android.permission.CAMERA" />
//手機版本歸版本,但硬體有沒有這種功能,版本並不知道,所以及時版本合,但硬體不合,上架時會顯示需要相機硬體功能等
//摄像头：android.hardware.camera
//1.呼叫別人的相機,權限要開(FileProvider)做存取動作
//2.在res底下Android創Resuourse Directory,類別選xml
//3.將照片路徑設置在
// <paths xmlns:android="http://schemas.android.com/apk/res/android">
//    <external-path name="name"  //name="名稱"
//        path="Android/data/tw.org.iii.appps.firebase_storage_camera/files/Pictures" /> //path="目錄"
//</paths>

//4.在檔案總館底下Appiction裡聲明
//androidx.core.content.FileProvider這是新版的類別名,必須去google
//<provider
//        android:name="androidx.core.content.FileProvider" //name="配置當前FileProvider的類別名"
//        android:authorities="${applicationId}.provider"        //authorities(當前)="(授權字串)配置一個FileProvider的名字,${applicationId}類似packgename唯一直.在當前系統內是唯一的值
//        android:exported="false"                               //exported(出口)="FileProvider是否需要公開(true/false)"
//        android:grantUriPermissions="true">                    //grantUriPermissions(授予.uri.權限)="是否允許進入受權(true/false),要開放允許才能存取照片"
//        <meta-data
//        android:name="android.support.FILE_PROVIDER_PATHS"
//        android:resource="@xml/file_paths" />
//</provider>
//*讀寫權限都開,照片要可存可讀
//Intenet透過Bumdle做資料交換可從Bundle裡挖資料
//        各种传感器：
//        加速计：android.hardware.sensor.accelerometer
//        气压计：android.hardware.sensor.barometer
//        指南针：android.hardware.sensor.compass
//        陀螺仪：android.hardware.sensor.gyroscope
//        感光：android.hardware.sensor.light
//        近距离感测：android.hardware.sensor.proximity
//        麦克风：android.hardware.microphone
//        定位：android.hardware.location
//        USB：
//        USB Host：android.hardware.usb.host
//        WIFI：android.hardware.wifi
//        蓝牙：android.hardware.bluetooth
//        软件方面的：
//
//        Bluetooth Low Energy：android.software.bluetooth_le
//        VOIP：android.software.sip.voip
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.ImageView;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.io.File;
import java.util.Set;

import tw.org.iii.appps.firebase_storage_camera.Model.Camera;


public class CameraActivity extends AppCompatActivity {
    private Vibrator vibrator ;//震動器
    private SwitchCompat fswitch;//閃光燈按鈕
    private CameraManager cameraManager;//相機關裡員
    private ImageView img;
    private File sdroot;

    private FirebaseDatabase firebaseDatabase;
    private DatabaseReference mCamera;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);

        //閃光燈所需的相機權限
//        if (ContextCompat.checkSelfPermission(this,
//                Manifest.permission.CAMERA) //改相機權限
//                != PackageManager.PERMISSION_GRANTED) {
//            ActivityCompat.requestPermissions(this,
//                    new String[]{Manifest.permission.CAMERA}, //改相機權限
//                    12);
//        }else {
//            init();
//        }

        //讀寫sdcard權限,要修正
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.CAMERA) //寫的權限
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.CAMERA,
                            Manifest.permission.READ_EXTERNAL_STORAGE,//讀的權限
                            Manifest.permission.WRITE_EXTERNAL_STORAGE,//寫的權限
                    },
                    12);
            Log.v("brad","沒權限去要");
        }else {
            Log.v("brad","有權限");
            init();
        }

        vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);//這個震動期是由 getSystemService
        fswitch = findViewById(R.id.fswitch);
        img = findViewById(R.id.img);

        //6.按下切換按鈕完閃光燈(true/false)
        fswitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {//打開為true/關掉false
                Log.v("brad" ,"b:" + b);
                if(b){//如果開關打開,閃光燈打開
                    onFlashLight();
                }else{//如果開關閉,閃光燈關閉
                    offFlashLight();
                }
            }
        });

    }


    //5.當使用者在權限下按允許或拒絕時來到這
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        init();
        Log.v("brad","onRequestPermissionsResult:" + ",permissions:" + permissions);
    }

    //4.直接有權限的話init();
    //CameraManager.getCameraIdList():取得相機鏡頭有幾個,0代表前鏡頭,1代表後鏡頭(回傳String[])
    private  void init(){
        cameraManager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);//取得相機管理員

        sdroot = Environment.getExternalStorageDirectory(); //取得sd卡外部檔案物件
        firebaseDatabase = FirebaseDatabase.getInstance();
        mCamera = firebaseDatabase.getReference("Camera");//取得資料庫節點


        //查詢相機有幾顆鏡頭
        cameraManager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        try {
            String[] ids = cameraManager.getCameraIdList();//取得相機鏡頭有幾個,0代表前鏡頭,1代表後鏡頭
            for(String id : ids){
                Log.v("brad","相機鏡頭有:" + id);
            }
        } catch (CameraAccessException e) {
            Log.v("brad","相機鏡頭取得錯誤:" + e.toString());
        }
    }

    //7.開啟閃光燈
    //setTorchMode(@NonNull String cameraId, boolean enabled)
    private void onFlashLight() {
        try {
            cameraManager.setTorchMode("0",true);
        } catch (CameraAccessException e) {
            Log.v("brad","開啟閃光燈失敗:" + e.toString());
        }
    }
    //8.關閉閃光燈
    private void offFlashLight(){
        try {
            cameraManager.setTorchMode("0",false);
        } catch (CameraAccessException e) {
            Log.v("brad","關閉閃光燈失敗:" + e.toString());
        }
    }

    //9.遠端Firebase控制燈光,0開燈,1關燈
    @Override
    protected void onResume() {
        super.onResume();
        if(mCamera != null){ //如果有節點才進來
            mCamera.addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    if(dataSnapshot.exists()){//如果節點裡有資料才近來
                        Camera b = dataSnapshot.getValue(Camera.class); //取得Camera節點裡面的物件,存放到Camera類別裡
                        Log.v("brad",b.getStatus());

                        if(b.getStatus().equals("0")){//如果節點裡的資料狀態是0的話開燈
                            onFlashLight();
                            Log.v("brad","數字是0時開燈" +b.getStatus().toString()+"dataSnapshot:" +dataSnapshot);

                        }else if(b.getStatus().equals("1")){//如果節點裡的資料狀態是1的話關燈
                            offFlashLight();
                            Log.v("brad","數字是1時關燈" + b.getStatus().toString()+"dataSnapshot:" +dataSnapshot);
                        }
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {

                }
            });

            Log.v("brad","onResume");
        }

    }



    //1.產生一次震動按鈕
    public void test1(View view) {
      createOneShotVibrator();
    }

    //1.產生一次性手機震動方法
    //VibrationEffect.createOneShot(long milliseconds, int amplitude)//產生一次性震動(1.震動的秒數2.震動的強度)
    private void createOneShotVibrator(){
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){//如果使用者的版本,大於等於Ozeo版本的話
            vibrator.vibrate(
                    VibrationEffect.createOneShot(
                            1*1000,//震動秒數為一秒
                            VibrationEffect.DEFAULT_AMPLITUDE));//震動強度為默認強度
        }else{//小於ozero版本
            vibrator.vibrate(1*1000);
        }
    }

    //2.重複頻率的震動按鈕
    public void test2(View view) {
        vibratePatten();
    }

    //2.重複頻率的震動按鈕方法
    //vibrate(long[] pattern, int repeat)://震動(1.震動的頻率 2.-1代表停止無限循環,0代表無限循環)
    private void vibratePatten(){
        long[] patten ={
                3*1000, //1.開始執行得的時間為:3秒
                1*1000, //2.持續震動時間為:1秒
                1000*10};//3.執行下一次循環間隔時間為:10秒
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){
            vibrator.vibrate(patten,0);//震動(1.震動的頻率 2.-1代表停止無限循環,0代表無限循環)
        }else{
            vibrator.vibrate(patten,0);
        }
    }

    //10.用intent方式叫出別人的相機
    public void test3(View view) {
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);//intent(媒體商店裡的,相片動態捕捉)
        startActivityForResult(intent,3); //開始連接intent(intent,跟指定的要求code)
    }

    //11.接受startActivityForResult處理,將拍好的照片data資料用縮圖方式呈現
    //getExtras():取得額外資料(回傳值Bundle)
    //get(String key):取得key,value資料(回傳值Object)
    //ImageView.setImageBitmap(Bitmap bm):將圖片顯示出來(bitmap)
    //bundle.keySet():取得Bundle裡的所有key(回傳Set<String>)
    // Object.getClass().getName()://取得物件的反射類別.裡面的類別名稱(回傳String)
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if(resultCode ==RESULT_OK){//如果按下ok
            //1.(徹底了解data資料裡的東西)從data裡面取得Bundle的所有key資料,再轉成Object型別取得反射類別的物件名

            if(requestCode ==3){ //抓到code3的時候
                Bundle bundle = data.getExtras();//取得額外資料(回傳值Bundle)
                //了解為什麼是Bitmap用反射方法回推
                Set<String> keys = bundle.keySet(); //取得Bundle裡的所有key(回傳Set<String>)
                for(String key: keys){//尋訪
                    Log.v("brad","key =" + key);//得知裡面的key只有data
                    Object obj = bundle.get(key);
                    Log.v("brad",obj.getClass().getName());//印出這個物件的類別跟名字
                }

                //2.將拍下的照片取得資料,Bitmap型別給imageView呈現這招是縮圖而已
                Bitmap bmp = (Bitmap) data.getExtras().get("data");//從data取得.額外的資料.從key去取得,將照片的資料強制轉型成Bitmap
                img.setImageBitmap(bmp);//把照片顯示在程式上，縮圖

                //如果code是4的
            }else if(requestCode ==4){
                Bitmap bmp = BitmapFactory.decodeFile(sdroot.getAbsolutePath()+"/DCIM/Camera"+"/iii.jpg");//取得你檔案的(路徑圖片)，取得全圖
                Log.v("brad","sdroot:" + sdroot.toString());
                img.setImageBitmap(bmp); //顯示圖片
            }
        }
    }
    //12.照相機儲存FileProvider,這種方式接受就直接存檔,
    //Uri getUriForFile(Context context,x
    //                  String authority,
    //                  File file
    //                  )://回傳值(Uri)
    //putExtra(String name, @RecentlyNullable Parcelable value)
    public void test4(View view) {
        Uri photoURI = FileProvider.getUriForFile(
                this,  //1.這個activity
                getPackageName() + ".provider", //2.授權
                new File(sdroot +"/DCIM/Camera", "iii.jpg")); //3.存放的file路徑("1.sdroot路徑實體","2.檔案照片名")

        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);//呼叫照片拍著時
        intent.putExtra(android.provider.MediaStore.EXTRA_OUTPUT, photoURI);//掛上素質(1.外布檔案,2寫好的fileProvider路徑)
        startActivityForResult(intent, 4);
        Log.v("brad","sdroot:" + sdroot.toString());
    }

    //引用自己寫的照相方法
    public void test5(View view) {
//        Intent intent = new Intent(this,MyCameraXActivity.class);//跳轉照相頁面
//        startActivity(intent);
    }
    //送Camera物件到firebase上
    public void btnAddFirebase(View view) {
        Camera camera = new Camera();
        mCamera.setValue(camera);
        Log.v("brad","setFirebaseValuet成功,狀態為:" +camera.getStatus());
    }

    //3.重複頻率且可以調整強度的按鈕
    public void test6(View view) {
        vibrationEffectCreateWaveform();
    }

    //3.重複頻率且可以調整強度的按鈕
    //VibrationEffect.createWaveform(
    // long[] timings,//1.控制開始與暫停
    // int[] amplitudes,//2.控制強度
    // int repeat)//3.控制是否重複
    private void vibrationEffectCreateWaveform(){
        long[] timings = {0, //延遲
                400, 800, //開始 //暫停
                600, 800, //開始 //暫停
                800, 800, //開始 //暫停
                1000}; //開始幾秒
        int[] mAmplitudes = new int[]{0, 255, 0, 255, 0, 255, 0, 255};
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createWaveform(
                    timings,//1.控制開始與暫停
                    mAmplitudes,//2.控制強度
                    -1//3.控制是否重複
            ));
        }
    }
}