package com.example.audioapp;

import android.app.Activity;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import com.example.cs571x_app.R;

import com.example.audioapp.utils.FileUtil;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class ListActivity extends Activity {
    ListView listView;
    List<File> list = new ArrayList<>();
    FileListAdapter adapter;
    MediaPlayer player;
    private TextView mState;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_list);
        listView = findViewById(R.id.listView);
        mState = findViewById(R.id.playStatus); // Ensure this exists in your XML layout

        // Initialize MediaPlayer
        player = new MediaPlayer();

        if ("csv".equals(getIntent().getStringExtra("type"))) {
            list = FileUtil.getCSVFiles();
        } else if ("pcm".equals(getIntent().getStringExtra("type"))) {
            list = FileUtil.getPcmFiles();
        } else if ("wav".equals(getIntent().getStringExtra("type"))) {
            list = FileUtil.getWavFiles();
        } else {
            list = FileUtil.getVideoFiles();
        }

        adapter = new FileListAdapter(this, list);
        listView.setAdapter(adapter);

        initEvent();
    }

    private void initEvent() {
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                String file = String.valueOf(list.get(position));
                int index = file.lastIndexOf("/");
                String fileName = file.substring(index + 1);
                Toast.makeText(getApplicationContext(), "Clicked " + fileName, Toast.LENGTH_SHORT).show();

                // Handle playing of the selected file
                playSelectedFile(file, fileName);
            }
        });
    }

    private void playSelectedFile(String file, String fileName) {
        try {
            if (player.isPlaying()) {
                player.stop();
            }
            player.reset(); // Always reset before setting a new data source
            player.setDataSource(file);
            player.prepare();
            player.start();
            mState.setText("Playing: " + fileName);

        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(getApplicationContext(), "Error playing file", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (player != null) {
            player.release(); // Release MediaPlayer when activity is destroyed
            player = null;
        }
    }
}
