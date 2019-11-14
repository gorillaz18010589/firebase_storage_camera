package tw.org.iii.appps.firebase_storage_camera;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.widget.VideoView;

public class PlayVideoActivity extends AppCompatActivity {
    private VideoView videoView;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_play_video);

          videoView = findViewById(R.id.video_view);

          if (getIntent() != null){
              Uri videoUri = Uri.parse(getIntent().getExtras().getString("videoUri"));
              Log.v("brad","activity_play_video => videoUri:" + videoUri);
              videoView.setVideoURI(videoUri);
              videoView.start();
          }
    }
}
