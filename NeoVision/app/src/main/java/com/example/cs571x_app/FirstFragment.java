package com.example.audioapp;


import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.os.Build;
import android.os.Bundle;
import android.text.format.DateFormat;
import android.util.ArrayMap;
import com.example.cs571x_app.R;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.VideoView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.fragment.NavHostFragment;


import com.example.audioapp.entity.SensorData;
import com.example.audioapp.utils.AudioRecorder;
import com.example.audioapp.utils.FileUtil;

import org.supercsv.cellprocessor.FmtBool;
import org.supercsv.cellprocessor.FmtDate;
import org.supercsv.cellprocessor.Optional;
import org.supercsv.cellprocessor.constraint.LMinMax;
import org.supercsv.cellprocessor.constraint.NotNull;
import org.supercsv.cellprocessor.constraint.UniqueHashCode;
import org.supercsv.cellprocessor.ift.CellProcessor;
import org.supercsv.io.CsvBeanWriter;
import org.supercsv.io.CsvMapWriter;
import org.supercsv.io.ICsvBeanWriter;
import org.supercsv.io.ICsvMapWriter;
import org.supercsv.prefs.CsvPreference;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

//@RequiresApi(api = Build.VERSION_CODES.KITKAT)
public class FirstFragment extends Fragment {
    //Motion
    private TextView motionText = null;

    //Acceleration sensor: TYPE_ACCELEROMETER: include gravity
    private SensorManager mSensorManager;
    private TextView mSensorText = null;
    private Sensor mSensor;
    private SensorData mData = new SensorData();

    //Gyroscope sensor: TYPE_GYROSCOPE
    private SensorManager mSensorManager2;
    private Sensor mSensor2;

    //CSV file to store data from accelerator and gyroscope
    //private String[] HEADER = new String[] { " Time ", "acc -x ", "acc-y", "acc-Z", "accuracy"};
    private String[] HEADER          = new String[] { "Time", "X-acc", "Y-acc", "Z-acc", "Accuracy", "X-Rot", "Y-Rot", "Z-Rot"};
    private ICsvMapWriter beanWriter = null;

    //Recorder and player part
    private TextView phrases;
    private Button clickShow;
    private int phrase_type;
    private Button start,pause,play, stop, type, pause2, next, previous, loop, switch_btn;
    private TextView wavplaying;
    private AudioRecorder mAudioRecorder = null;
    MediaPlayer player = new MediaPlayer();
    private SurfaceView mvideo;
    String[] musiclist;
    int listIndex;
    int listLength;

    private SensorEventListener mListener =  new SensorEventListener() {
        @Override
        public void onSensorChanged(SensorEvent event) {
            mData.setX(event.values[0]);
            mData.setY(event.values[1]);
            mData.setZ(event.values[2]);
            updateSensorStateText();
            Log.d("sensor", event.sensor.getName());
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {
            mData.setAccuracy(accuracy);
            updateSensorStateText();
        }
    };

    private SensorEventListener mListener2 =  new SensorEventListener() {
        @Override
        public void onSensorChanged(SensorEvent event) {
            mData.setRotx(event.values[0]);
            mData.setRoty(event.values[1]);
            mData.setRotz(event.values[2]);
            updateSensorStateText();
            Log.d("sensor", event.sensor.getName());
        }
        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {
            mData.setAccuracy(accuracy);
            updateSensorStateText();
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //motion
        //setOnTouchListener(new myOnTouchListener());

        //Acceleration sensor
        mSensorManager = (SensorManager) getActivity().getSystemService(Context.SENSOR_SERVICE);
        mSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);

        //Gyroscope sensor
        mSensorManager2 = (SensorManager) getActivity().getSystemService(Context.SENSOR_SERVICE);
        mSensor2        = mSensorManager2.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
        mSensorManager.registerListener(mListener, mSensor, 50000);
        mSensorManager2.registerListener(mListener2, mSensor2, 50000);
    }

    private class myOnTouchListener implements View.OnTouchListener {
        @Override
        public boolean onTouch(View v, MotionEvent event) {
            motionText.setText("Motion: " + event.getAction() + ", " + event.getPressure());
            return true;
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d("sensor", "sensor destroyed");
        mSensorManager.unregisterListener(mListener);
        mSensorManager2.unregisterListener(mListener2);
    }

    @Override
    public View onCreateView(
            LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_first, container, false);
    }

    @Override
    public void onStart() {
        super.onStart();
    }

    @Override
    public void onStop() {
        super.onStop();
    }

    private void updateSensorStateText()
    {
        if (null != mSensorText) {
            mSensorText.setText(mData.getText());
            try{
                writeWithCsvBeanWriter();
            }catch (Exception exception) {

            }
        }
    }

    private static CellProcessor[] getProcessors() {
        final CellProcessor[] processors = new CellProcessor[] {
                new NotNull(), // Time 
                new NotNull(), // x-accelerometer 
                new NotNull(), // y-accelerometer
                new NotNull(), // z-accelerometer
                new NotNull(), // accuracy
                new NotNull(), // x-rotation
                new NotNull(), // y-rotation
                new NotNull()  // z-rotation
        };

        return processors;
    }

    private void initCSVFile(String fileName)
    {
        //File file = new File(fileName +".csv");
        File file = new File(fileName);
        try{
            if (!file.exists()) {
                file.createNewFile();
            }
            final CellProcessor[] processors = getProcessors();

            // write the header
            //beanWriter = new CsvMapWriter(new FileWriter(fileName +".csv"),
            beanWriter = new CsvMapWriter(new FileWriter(fileName),
                    CsvPreference.STANDARD_PREFERENCE);
            beanWriter.writeHeader(HEADER);
        }catch (IOException exception) {
        }
    }

    private void writeWithCsvBeanWriter() throws Exception {
        try {
            final CellProcessor[] processors = getProcessors();
            Map<String,String> map = new HashMap<>();
            SimpleDateFormat formater = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
            map.put(HEADER[0],formater.format(new Date()));
            map.put(HEADER[1], String.valueOf(mData.getX()));
            map.put(HEADER[2], String.valueOf(mData.getY()));
            map.put(HEADER[3], String.valueOf(mData.getZ()));
            map.put(HEADER[4], String.valueOf(mData.getAccuracy()));
            map.put(HEADER[5], String.valueOf(mData.getRotx()));
            map.put(HEADER[6], String.valueOf(mData.getRoty()));
            map.put(HEADER[7], String.valueOf(mData.getRotz()));
            beanWriter.write(map, HEADER, processors);
        }
        finally {
        }
    }

    private void closeCSVWriter() {
        if( beanWriter != null ) {
            try{
                beanWriter.close();
            }catch (Exception exception) {

            }
        }
    }

    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        Log.e("111", "onViewCreated");
        FileUtil.setBasePath(getActivity().getExternalFilesDir(null).toString());
        view.setOnTouchListener(new myOnTouchListener());
        motionText = view.findViewById(R.id.textView2);
        mSensorText = view.findViewById(R.id.textview_first);
        mAudioRecorder = AudioRecorder.getInstance();
        start = view.findViewById(R.id.start);
        pause = view.findViewById(R.id.pause);
        pause.setEnabled(false);
        play = view.findViewById(R.id.play);
        stop = view.findViewById(R.id.stop);
        stop.setEnabled(false);
        phrases = view.findViewById(R.id.phrases);
        clickShow = view.findViewById(R.id.click_display);
        phrase_type = 0;
        wavplaying = view.findViewById(R.id.textView);
        mvideo = view.findViewById(R.id.surfaceView);
        type = view.findViewById(R.id.type);
        loop = view.findViewById(R.id.loop);
        pause2 = view.findViewById(R.id.pause2);
        pause2.setEnabled(false);
        next = view.findViewById(R.id.next);
        previous = view.findViewById(R.id.previous);
        mvideo.getHolder().setKeepScreenOn(true);//keep screen lighting 
        musiclist = FileUtil.getWavFilesStrings();
        listIndex = 0;
        listLength = musiclist.length;

        previous.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (listLength == 0) {
                    Toast.makeText(getContext(), "No files available.", Toast.LENGTH_SHORT).show();
                } else {
                    listIndex = (listIndex - 1 + listLength) % listLength;
                    play.performClick();
                    Toast.makeText(getContext(), "Previous", Toast.LENGTH_SHORT).show();
                }
            }
        });

        next.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (listLength == 0) {
                    Toast.makeText(getContext(), "No files available.", Toast.LENGTH_SHORT).show();
                } else {
                    listIndex = (listIndex + 1) % listLength;
                    play.performClick();
                    Toast.makeText(getContext(), "Next", Toast.LENGTH_SHORT).show();
                }
            }
        });

        pause2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(pause2.getText().toString().equals("pause")){
                    player.pause();
                    pause2.setText("resume");
                    Toast.makeText(getContext(), "Pause", Toast.LENGTH_SHORT).show();
                } else {
                    player.start();
                    pause2.setText("pause");
                    Toast.makeText(getContext(), "Resume", Toast.LENGTH_SHORT).show();
                }
            }
        });

        clickShow.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(phrase_type == 0) {
                    phrases.setText("Can I help you?");
                } else if(phrase_type == 1) {
                    phrases.setText("What's your name?");
                } else if(phrase_type == 2) {
                    phrases.setText("Nice to meet you.");
                } else if(phrase_type == 3) {
                    phrases.setText("I need your help.");
                }
                phrase_type = (phrase_type + 1) % 4;
            }
        });

        type.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(type.getText().toString().equals(".wav")){
                    type.setText(".video");
                    musiclist = FileUtil.getVideoFilesStrings();
                    listLength = musiclist.length;
                    Log.d("file", "onClickvideo: "+ musiclist.length);
                    Toast.makeText(getContext(), "Videos selected", Toast.LENGTH_SHORT).show();
                } else {
                    type.setText(".wav");
                    musiclist = FileUtil.getWavFilesStrings();
                    listLength = musiclist.length;
                    Log.d("file", "onClickwav: "+ musiclist.length);
                    Toast.makeText(getContext(), "Wav selected", Toast.LENGTH_SHORT).show();
                }
            }
        });

        play.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (listLength == 0) {
                    Toast.makeText(getContext(), "No files available.", Toast.LENGTH_SHORT).show();
                } else {
                    pause2.setEnabled(true);
                    pause2.setText("pause");
                    stop.setEnabled(true);
                    int index = musiclist[listIndex].lastIndexOf("/");
                    String fileName = musiclist[listIndex].substring(index + 1);
                    if (wavplaying == null || wavplaying.getText().toString().isEmpty()) {
                        wavplaying.setText("Playing: " + fileName);
                        try {
                            player.setDisplay(mvideo.getHolder());
                            player.setDataSource(musiclist[listIndex]);
                            Log.d("indexplaying", "onClick: " + listIndex);
                            player.prepare();
                            player.start();
//                            player.setLooping(true);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    } else {
                        player.stop();
                        player.reset();
                        wavplaying.setText("Playing: " + fileName);
                        try {
                            player.setDisplay(mvideo.getHolder());
                            player.setDataSource(musiclist[listIndex]);
                            Log.d("indexplaying", "onClick: " + listIndex);
                            player.prepare();
                            player.start();
//                            player.setLooping(true);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        });

        loop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(loop.getText().toString().equals("loop off")){
                    loop.setText("loop on");
                    Toast.makeText(getContext(), "LOOP ON", Toast.LENGTH_SHORT).show();
                } else {
                    loop.setText("loop off");
                    Toast.makeText(getContext(), "LOOP OFF", Toast.LENGTH_SHORT).show();
                }
            }
        });

        player.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {
//                Log.d("completion", "onCompletion: ");
                if(loop.getText().toString().equals("loop on")){
                    listIndex -= 1;
                }
                next.performClick();
            }
        });

        stop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(wavplaying.getText().toString().equals("Stopped") || wavplaying.getText().toString().isEmpty()){
                } else {
                    player.stop();
                    pause2.setEnabled(false);
                    pause2.setText("pause");
                    stop.setEnabled(false);
                    wavplaying.setText("Stopped");
                    Toast.makeText(getContext(), "Stop", Toast.LENGTH_SHORT).show();
                }
            }
        });

        start.setOnClickListener(new View.OnClickListener() {
//            @RequiresApi(api = Build.VERSION_CODES.KITKAT)
            @Override
            public void onClick(View view) {
                Log.d("encheck", "onclick: "+mAudioRecorder.getStatus());
                try {
                    if (mAudioRecorder.getStatus() == AudioRecorder.Status.STATUS_NO_READY) {
                        Log.d("encheck", "after click1: "+mAudioRecorder.getStatus());
                        pause.setEnabled(true);
                        //
                        String fileName = new SimpleDateFormat("yyyyMMddhhmmss").format(new Date());
                        mAudioRecorder.createDefaultAudio(fileName);
                        mAudioRecorder.startRecord(null);
//                        start.setText("Stop Recording");
                        start.setText("Stop Recording");
//                        pause.setVisibility(View.VISIBLE);
                        initCSVFile(FileUtil.getCSVFileAbsolutePath(fileName));
                        Log.d("encheck", "after click2: "+mAudioRecorder.getStatus());
                    } else {
                        Log.d("encheck", "stop1: "+mAudioRecorder.getStatus());
                        pause.setEnabled(false);
                        // Stop Recording
                        mAudioRecorder.stopRecord();
                        closeCSVWriter();
                        start.setText("Start Recording");
                        pause.setText("Pause Recording");
//                        pause.setVisibility(View.GONE);
                        Log.d("encheck", "stop2: "+mAudioRecorder.getStatus());
                    }
                } catch (IllegalStateException e) {
                    Toast.makeText(getActivity(), e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            }
        });
        view.findViewById(R.id.btn_first_list).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getActivity(), ListActivity.class);
                intent.putExtra("type","wav");
                startActivity(intent);
            }
        });
        view.findViewById(R.id.btn_first_pcm).setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getActivity(), ListActivity.class);
                intent.putExtra("type", "pcm");
                startActivity(intent);
            }
        });
        view.findViewById(R.id.btn_first_json).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getActivity(), ListActivity.class);
                intent.putExtra("type", "csv");
                startActivity(intent);
            }
        });
        view.findViewById(R.id.btn_videolist).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getActivity(), ListActivity.class);
                intent.putExtra("type", "mp4");
                startActivity(intent);
            }
        });
        pause.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d("encheck", "click pause: "+mAudioRecorder.getStatus());
                try {
                    if (mAudioRecorder.getStatus() == AudioRecorder.Status.STATUS_START) {
                        //pause
                        mAudioRecorder.pauseRecord();
//                        pause.setText(" keep recording ");
                        pause.setText("Keep Recording");
                        Log.d("encheck", "after pause: "+mAudioRecorder.getStatus());
                    } else {
                        Log.d("encheck", "click keep: "+mAudioRecorder.getStatus());
                        mAudioRecorder.startRecord(null);
//                        pause.setText(" pause ");
                        pause.setText("Pause Recording");
                        Log.d("encheck", "after keep: "+mAudioRecorder.getStatus());
                    }
                } catch (IllegalStateException e) {
                    Toast.makeText(getActivity(), e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            }
        });

        verifyPermissions(getActivity());
    }

    //get the permission of recording

    private static final int GET_RECODE_AUDIO = 1;

    private static String[] PERMISSION_ALL = {
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.READ_EXTERNAL_STORAGE,
    };

    /** get the permission of recording **/
    public static void verifyPermissions(Activity activity) {
        boolean permission = (ActivityCompat.checkSelfPermission(activity, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED)
                || (ActivityCompat.checkSelfPermission(activity, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED)
                || (ActivityCompat.checkSelfPermission(activity, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED);
        if (permission) {
            ActivityCompat.requestPermissions(activity, PERMISSION_ALL,
                    GET_RECODE_AUDIO);
        }
    }
}
