package fretx.fretxapp;

import android.app.Activity;
import android.net.Uri;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.LinearLayout;
import android.widget.MediaController;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.Hashtable;

public class LessonTestActivity extends AppCompatActivity {

    ObservableVideoView vvMain;
    LinearLayout llMain;
    Activity mContext;
    MediaController mc;
    TextView tvCurTime;
    TextView tvNotice;
    int prevIndex = -1;
    float tickDuration = -1; // this is the time defined in midi files
    byte[] state = {};

    Hashtable lstTimeText = new Hashtable();
    int[] arrayKeys;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lesson_test);
        mContext = this;
        Log.v("status", "status: Initializing UI!");
        initUI();
        Log.v("status", "status: End of UI initialization!");
        Log.v("status", "status: Initializing Txt!");
        initTxt();
        Log.v("status", "status: End of Txt initialization!");
    }
    private void initUI() {

        tvCurTime = (TextView)findViewById(R.id.tvCurTime); // TextView with current reprod time
        tvNotice = (TextView)findViewById(R.id.tvNotice); // TextView with current position of MIDI

        llMain = (LinearLayout)findViewById(R.id.llVideoView);
        Uri videoUri = Uri.parse("android.resource://" + getPackageName() + "/" + R.raw.unit02a);
        vvMain = (ObservableVideoView)findViewById(R.id.vvMain);
        vvMain.setVideoURI(videoUri);
        mc = new MediaController(vvMain.getContext());
        mc.setMediaPlayer(vvMain);
        mc.setAnchorView(llMain);
        vvMain.setMediaController(mc);
        vvMain.start();


        vvMain.setVideoViewListener(new ObservableVideoView.IVideoViewActionListener() {
            @Override
            public void onPause() {
            }

            @Override
            public void onResume() {
                prevIndex = 0;
            }

            @Override
            public void onTimeBarSeekChanged(int currentTime) {
                tvCurTime.setText(String.format("%d", currentTime));
                changeText(currentTime);
                prevIndex = -1;
            }
        });
        new MyAsync().execute();
    }

    public void initTxt()
    {
        readRawTextFile();

        arrayKeys = new int[lstTimeText.size()];
        int i = 0;
        for ( Enumeration e = lstTimeText.keys(); e.hasMoreElements(); ) {
            arrayKeys[i] = (int) e.nextElement();
            i++;
        }
        Arrays.sort(arrayKeys);
    }
    private class MyAsync extends AsyncTask<Void, Integer, Void>
    {
        int currentTime = 0;
        @Override
        protected Void doInBackground(Void... params) {
            while(true) {
                currentTime = vvMain.getCurrentPosition();
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        changeText(currentTime);
                        tvCurTime.setText(String.format("%d", currentTime));
                    }
                });
            }
        }

        @Override
        protected void onProgressUpdate(Integer... values) {
            super.onProgressUpdate(values);
        }
    }

    public void readRawTextFile() {
        int ppqn = -1;
        int tempo = -1;
        int channel = -1;
        int note = -1;
        int ledId = -1;
        int time = -1;
        int len;
        InputStream inputStream = getResources().openRawResource(R.raw.test_data);

        InputStreamReader inputreader = new InputStreamReader(inputStream);
        BufferedReader buffreader = new BufferedReader(inputreader);
        String line;

        try {
            while ((line = buffreader.readLine()) != null) {
                Log.v("status", "processing line: " + line);

                String[] words = line.split( " " );

                len = words.length;
                if (words.length > 0) {
                    if (isInteger(words[0])){
                        if (len > 1){
                            switch (words[1])
                            {
                                case "Tempo":
                                    if (len > 2) {
                                        tempo = Integer.parseInt(words[2]);
                                        if (tempo != -1 && ppqn != -1) {
                                            tickDuration = (float) 600000000 / ppqn / tempo;
                                            Log.v("midi", "Tick Duration: " + String.valueOf(tickDuration));
                                        }
                                    }
                                    break;
                                case "On":
                                    if (len > 3) {
                                        channel = Integer.parseInt(words[2].substring(3));
                                        note = Integer.parseInt(words[3].substring(2));
                                        ledId = getArduinoData(channel, note);
                                        time = (int)(Integer.parseInt(words[0]) * tickDuration);
                                        if(lstTimeText.containsKey(time))
                                        {
                                            String strTemp = (String)lstTimeText.get(time);
                                            lstTimeText.put(time, strTemp + ",+" + Integer.toString(ledId));
                                        }else
                                            lstTimeText.put(time, "+" + Integer.toString(ledId));
                                    }
                                    break;
                                case "Off":
                                    if (len > 3) {
                                        channel = Integer.parseInt(words[2].substring(3));
                                        note = Integer.parseInt(words[3].substring(2));
                                        ledId = getArduinoData(channel, note);
                                        time = (int)(Integer.parseInt(words[0]) * tickDuration);
                                        if(lstTimeText.containsKey(time))
                                        {
                                            String strTemp = (String)lstTimeText.get(time);
                                            lstTimeText.put(time, strTemp + ",-" + Integer.toString(ledId));
                                        }else
                                            lstTimeText.put(time, "-" + Integer.toString(ledId));
                                    }
                                    break;
                            }
                        }
                    }
                    else{
                        switch (words[0]){
                            case "MFile":
                                if (len > 3) {
                                    ppqn = Integer.parseInt(words[3]);
                                    if (tempo != -1 && ppqn != -1)
                                        tickDuration = 600000000 / ppqn / tempo;
                                }
                                break;
                        }
                    }
                }
            }
        } catch (IOException e) {
            return;
        }
    }

    public void changeText(int currentTime)
    {
        if (prevIndex + 1 < arrayKeys.length && currentTime >= arrayKeys[prevIndex + 1])
        {
            prevIndex++;
            String mods[] = ((String)lstTimeText.get(arrayKeys[prevIndex])).split(",");
            for (int i = 0; i < mods.length; i++)
            {
                if (mods[i].charAt(0) == '+') {
                    state = AddToArray(state, (byte) (Integer.parseInt(mods[i].substring(1)) & 0xFF));
                }
                else if (mods[i].charAt(0) == '-') {
                    state = RemoveToArray(state, (byte) (Integer.parseInt(mods[i].substring(1)) & 0xFF));
                }
            }
            sendToArduino(state);
            tvNotice.setText((String) lstTimeText.get(arrayKeys[prevIndex]));
        }
    }

    private void sendToArduino(byte[] array)
    {
        byte end = 0;
        array = AddToArray(array, end);
        BluetoothClass.mHandler.obtainMessage(BluetoothClass.ARDUINO, array).sendToTarget();
        String s = "";
        for (int i = 0; i < array.length; i++)
        {
            s.concat("|" + String.valueOf((int)array[i]));
        }
        s.concat("|");
        Log.v("midi", "sending: " + s);
    }

    public static boolean isInteger(String str) {
        try {
            Integer.parseInt(str);
            return true;
        } catch (NumberFormatException nfe) {
            return false;
        }
    }

    private int getArduinoData(int channel, int note) {
        if (note >= 40 && note <= 44)
            return ((note - 40) * 10 + 6);
        else if (note == 45) {
            if (channel == 16)
                return (56);
            else if (channel == 15)
                return (5);
        }
        else if (note >= 46 && note <= 49)
            return ((note - 45) * 10 + 5);
        else if (note == 50) {
            if (channel == 15)
                return (55);
            else if (channel == 14)
                return (4);
        }
        else if (note >= 51 && note <= 54)
            return ((note - 50) * 10 + 4);
        else if (note == 55) {
            if (channel == 14)
                return (54);
            else if (channel == 13)
                return (3);
        }
        else if (note >= 56 && note <= 58)
            return ((note - 55) * 10 + 3);
        else if (note == 59) {
            if (channel == 12)
                return (2);
            else if (channel == 13)
                return (43);
        }
        else if (note == 60) {
            if (channel == 12)
                return (12);
            else if (channel == 13)
                return (53);
        }
        else if (note >= 61 && note <= 63)
            return ((note - 59) * 10 + 2);
        else if (note == 64) {
            if (channel == 11)
                return (1);
            else if (channel == 12)
                return (52);
        }
        else if (note >= 65 && note <= 69)
            return ((note - 64) * 10 + 1);
        return (0);
    }

    private byte[] RemoveToArray(byte array[], byte data)
    {
        if (!isInArray(array, data))
            return array;
        else{
            int size = array.length;
            byte[] newArray = new byte[size - 1];
            int newPos = 0;
            for (int pos = 0; pos < size; pos++)
            {
                if (array[pos] != data)
                    newArray[newPos++] = array[pos];
            }
            return newArray;
        }
    }

    private byte[] AddToArray(byte array[], byte data)
    {
        if (isInArray(array, data))
            return array;
        else{
            int size = array.length;
            byte[] newArray = new byte[size + 1];
            for (int pos = 0; pos < size; pos++)
            {
                newArray[pos] = array[pos];
            }
            newArray[size] = data;
            return newArray;
        }
    }

    private boolean isInArray(byte array[], byte data)
    {
        int size;

        if (array == null)
            return false;
        size = array.length;
        for (int pos = 0; pos < size; pos++)
        {
            if (array[pos] == data)
                return true;
        }
        return false;
    }
}
