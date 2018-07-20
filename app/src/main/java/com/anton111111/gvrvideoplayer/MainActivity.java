package com.anton111111.gvrvideoplayer;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.anton111111.vr.GLHelper;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.concurrent.ExecutionException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class MainActivity extends AppCompatActivity {

    private static final String TAG = "GVRVIDEOPLAYER";

    private EditText urlView;
    private Spinner videoFormatsView;
    private Button playBtn;
    private CheckBox stereoModeView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        urlView = findViewById(R.id.url);


        playBtn = findViewById(R.id.play_btn);
        findViewById(R.id.play_btn).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                play();
            }
        });


        videoFormatsView = findViewById(R.id.video_format);
        ArrayAdapter<CharSequence> adapterV =
                ArrayAdapter.createFromResource(this, R.array.video_formats, android.R.layout.simple_spinner_item);


        adapterV.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        videoFormatsView.setAdapter(adapterV);


        findViewById(R.id.play_sample_btn).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // play(s);
            }
        });

        stereoModeView = findViewById(R.id.stereo_mode);

        fillInfoBox();
    }


    private void fillInfoBox() {
        String info = "";
        info += "Max texture size: " + GLHelper.getMaxTextureSize() + "\n";
        ((TextView) findViewById(R.id.info_box)).setText(info);
    }


    private String[] getAssetsVideo() {
        ArrayList<String> result = new ArrayList<String>();
        String[] list;
        try {
            list = getAssets().list("");
            if (list.length > 0) {
                // This is a folder
                for (String file : list) {
                    if (file.endsWith(".mp4")) {
                        result.add(file.replace(".mp4", ""));
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return result.toArray(new String[result.size()]);
    }

    private void play() {
        playBtn.setEnabled(false);
        String source = urlView.getText().toString();
        if (source == null || source.length() <= 0) {
            playBtn.setEnabled(true);
            return;
        }
        playBtn.setEnabled(true);
        play(source);
    }


    private void play(String url) {
        Intent intent = new Intent(this, VRPlayerActivity.class);
        intent.putExtra(VRPlayerActivity.INTENT_EXTRA_URL_KEY, url);
        intent.putExtra(VRPlayerActivity.INTENT_EXTRA_VIDEO_FORMAT_KEY, videoFormatsView.getSelectedItem().toString());
        intent.putExtra(VRPlayerActivity.INTENT_EXTRA_STEREO_MODE_ENABLED, stereoModeView.isChecked());
        startActivity(intent);
    }

}
