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
import android.util.LogPrinter;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.ImageView;

import com.google.android.gms.common.internal.service.Common;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.io.File;
import java.util.Set;

import tw.org.iii.appps.firebase_storage_camera.Model.Camera;

public class CameraActivity extends AppCompatActivity {
    private Vibrator vibrator; //震動物件
    private SwitchCompat fSwitch; //開關元件
    private CameraManager cameraManager; //相機閃光燈經理人
    private Button brnSetValue;

    private FirebaseDatabase firebaseDatabase;
    private DatabaseReference mCamera;

    private ImageView img;//相機照片呈現的imageView
    private File sdroot;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);
        //沒有權限的話,去要
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                        != PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                        != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{
                            Manifest.permission.CAMERA,
                            Manifest.permission.WRITE_EXTERNAL_STORAGE,
                            Manifest.permission.READ_EXTERNAL_STORAGE
                    },
                    12);
            Log.v("brad","要權限");
        }else{//有權限的話
            init();
            Log.v("brad","有權限init()");
        }

         brnSetValue.setOnClickListener(new View.OnClickListener() {
             @Override
             public void onClick(View view) {
                 setFireBaseValue();
             }
         });



         //6.按下切換按鈕完閃光燈(true/false)
         fSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
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


    //9.遠端Firebase控制燈光,0開燈,1關燈
    @Override
    protected void onResume() {
        super.onResume();
        mCamera.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
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


            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });

        Log.v("brad","onResume");
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

    //5.當使用者在權限下按允許或拒絕時來到這
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        init();
    }

    //4.直接有權限的話init();
    //CameraManager.getCameraIdList():取得相機鏡頭有幾個,0代表前鏡頭,1代表後鏡頭(回傳String[])
    private void init() {
        vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        fSwitch = findViewById(R.id.fSwitch);
        brnSetValue = findViewById(R.id.btn_setValue);

        firebaseDatabase = FirebaseDatabase.getInstance();
        mCamera = firebaseDatabase.getReference("Camera");//取得資料庫節點

        img = findViewById(R.id.camera_img);//取得顯示拍照照片的Img
        sdroot = Environment.getExternalStorageDirectory().getAbsoluteFile(); //取得sd卡外部檔案物件




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


    //1.產生一次震動按鈕
    //VibrationEffect.createOneShot(long milliseconds, int amplitude)//產生一次性震動(1.震動的秒數2.震動的強度)
    public void test1(View view) {
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
    //vibrate(long[] pattern, int repeat)://震動(1.震動的頻率 2.-1代表停止無限循環,0代表無限循環)
    public void test2(View view) {
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

    //3.重複頻率且可以調整強度
    //VibrationEffect.createWaveform(
    // long[] timings,//1.控制開始與暫停
    // int[] amplitudes,//2.控制強度
    // int repeat)//3.控制是否重複
    public void test3(View view){
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
    //送Camera物件到firebase上
    private void setFireBaseValue() {
        Camera camera = new Camera();
        mCamera.setValue(camera);
        Log.v("brad","setFirebaseValuet成功" +camera.getStatus());
    }
    //10.用intent方式叫出別人的相機
    public void test4(View view) {
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        startActivityForResult(intent,3);
    }
    //11.接受startActivityForResult處理,將拍好的照片data資料用縮圖方式呈現
    //getExtras():取得額外資料(回傳值Bundle)
    //get(String key):取得key,value資料(回傳值Object)
    //ImageView.setImageBitmap(Bitmap bm):將圖片顯示出來(bitmap)
    //bundle.keySet():取得Bundle裡的所有key(回傳Set<String>)
    // Object.getClass().getName()://取得物件的反射類別.裡面的類別名稱(回傳String)
    @Override
    protected void onActivityResult(int requestCode, //1.接受startActivityForResult(intent,3)的3回應馬
                                    int resultCode,  //2.使用者點選ok或拒絕的code(ok == -1)
                                    @Nullable Intent data) {//3.使用者拍照完的相機data資料
        super.onActivityResult(requestCode, resultCode, data);
        if(resultCode == RESULT_OK && requestCode == 3){
                //1.(徹底了解data資料裡的東西)從data裡面取得Bundle的所有key資料,再轉成Object型別取得反射類別的物件名
                   Bundle bundle = data.getExtras();
                   Set<String> keys = bundle.keySet();
                        for(String key : keys){
                            Log.v("brad","key:" + key);//得知裡面的key只有data
                            Object obj = bundle.get(key);
                            Log.v("brad","obj:" + obj.getClass().getName());//利用反射類別取得物件得類別名稱android.graphics.Bitmap,由此得知data是Bitmap類別
                        }


                //2.將拍下的照片取得資料,Bitmap型別給imageView呈現這招是縮圖而已
                  Bitmap bitmap = (Bitmap) data.getExtras().get("data");//從data取得.額外的資料.從key去取得,將照片的資料強制轉型成Bitmap
                  img.setImageBitmap(bitmap);//將照片顯示出來
        }

        if(resultCode == RESULT_OK && requestCode ==4){
            Bundle bundle = data.getExtras();
            Set<String> keys = bundle.keySet();
            for(String key : keys){
                Log.v("brad","key:" + key);
            }
            Bitmap bmp = BitmapFactory.decodeFile(sdroot.getAbsolutePath()+"/iii.jpg");//取得你檔案的(路徑圖片)，取得全圖
            img.setImageBitmap(bmp); //顯示圖片
            Log.v("brad","decodeFile:" + sdroot.getAbsolutePath()+"/cuz.jpg");

        }
    }
    //12.照相機儲存FileProvider,這種方式接受就直接存檔,
    //Uri getUriForFile(Context context,x
    //                  String authority,
    //                  File file
    //                  )://回傳值(Uri)
    //putExtra(String name, @RecentlyNullable Parcelable value)
    public void test5(View view) {
        Uri photoURI = FileProvider.getUriForFile(
                this, //1.這個activity
                getPackageName() + ".provider",//2.授權
                new File(sdroot, "czu.jpg")); //3.存放的file路徑("1.sdroot路徑實體","2.檔案照片名")

        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);//呼叫照片拍著時
//        intent.putExtra(android.provider.MediaStore.EXTRA_OUTPUT, photoURI);//掛上素質(1.外布檔案,2寫好的fileProvider路徑)
        startActivityForResult(intent, 4);
        Log.v("brad","test5" + sdroot.toString());
    }


}
