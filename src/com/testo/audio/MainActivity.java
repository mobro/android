package com.testo.audio;


//import org.opencv.samples.puzzle15.Puzzle15Activity;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;

import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;

import android.app.Activity;
import android.content.Context;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.text.InputType;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.EditText;
import android.widget.ToggleButton;
import android.widget.Button;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.TimerTask;
import java.util.Timer;

//import android.media.MediaPlayer.OnCompletionListener;
// MediaPlayer


public class MainActivity extends Activity {	

	// Debugging
	private static final String TAG = "MainActivity";
	private static final boolean D = true;

	// AudioRecorder Data
	private static AudioRecord aRecorder;
	private static int audioSource = MediaRecorder.AudioSource.MIC;
	private static int sampleRateInHz = 44100; // guaranteed that it works on all devices
	private static int channelConfig = AudioFormat.CHANNEL_IN_STEREO;
	private static int audioFormat = AudioFormat.ENCODING_PCM_16BIT;
	private static int bufferSizeInBytes = AudioRecord.getMinBufferSize(sampleRateInHz, channelConfig, audioFormat);
	
	// UI
	@SuppressWarnings("unused")
	private ToggleButton toggleStartButton;
	private ToggleButton togglePlayButton;
	private Button buttonAuthentication;
	    
	// Audio recording
	//private static Thread recordingThread;
	private static boolean isRecording = false;
	
	// File Name
	private String FILE_NAME_RECORD = "record.pcm";

	// Audio playing
	private static Thread playingThread;
	
	
	boolean isRunning = true;
	
		
	// ExtAudioRecorder
	ExtAudioRecorder extAudioRecorder;
	AudioPlayer audioPlayer;
	
	static String PATH_REC = (Environment.getExternalStorageDirectory().getPath() + "/record.wav");

	// Initialize variables for timer
	TimerTask timerTask = null;
	Timer timer = null;

    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {

    	// Load opencv
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS:
                {
                    Log.i(TAG, "OpenCV loaded successfully");                  
                    
                } break;
                default:
                {
                    super.onManagerConnected(status);
                } break;
            }
        }
    };
    
    @Override
    public void onResume()
    {
        super.onResume();
        OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_2_4_3, this, mLoaderCallback);
    }
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		toggleStartButton = (ToggleButton) findViewById(R.id.toggleButtonStartStop);
		togglePlayButton = (ToggleButton) findViewById(R.id.togglebuttonPlayStop);
		//buttonAuthentication = (Button) findViewById(R.id.buttonAuthenticate) ;

		if (D) Log.i(TAG, "AudioRecord Buffer Size in Bytes: " + bufferSizeInBytes);
		        
        initialize(0);
	}

    private void initialize(int type){
    	    	
    }
	 
    
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}
	
	@Override
	protected void onPause() {
		super.onPause();
		try {
			stopRecording();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		//recordingThread = null;
	}

	public void buttonStartStop(View v) throws Exception {
		// if Button active - start recording
		if (((ToggleButton) v).isChecked()) {
			if (D) Log.i(TAG, "Start");
			// starts new recording thread
			startRecording();
		} else { // not active - stop recording
			if (D) Log.i(TAG, "Stop");
			stopRecording();
		}
	}
	
	public void buttonPlayStop(View v) {
		// if Button active - start playing
		if (((ToggleButton) v).isChecked()) {
			if (D) Log.i(TAG, "PlayStart");
			startPlaying();
		} else { // not active - stop recording
			if (D) Log.i(TAG, "PlayStop");
			stopPlaying();
		}
	}
	
	public void buttonAuthent(View v) throws Exception {
		if(((Button) v).isEnabled())
		{
			// Create mechanism to stop the recording after 5 seconds
	        timer = new Timer();
	        
			timerTask = new TimerTask() {
	            public void run() 
	            {
					try {
						stopRecording();
						timerTask.wait();
					} catch (Exception e) {
						// TODO Auto-generated catch block
						timer.cancel();
						//e.printStackTrace();
						
					}
	            }
	        };
	        
	        timer.schedule(timerTask, 4000);

			// Start the recording
			startRecording();
		}
	}
	
	private void startPlaying(){
		audioPlayer = new AudioPlayer();
		audioPlayer.playAudio();
	}
	
	private void startRecording() {
		
	    StringBuilder sb = new StringBuilder();
        sb.append(String.format("%02X ", 0));		
		EditText patternNmb = (EditText)findViewById(R.id.patternNumber);
		patternNmb.setText(sb);
		
		// Start recording
		//extAudioRecorder = ExtAudioRecorder.getInstanse(true);	  // Compressed recording (AMR)
		extAudioRecorder = ExtAudioRecorder.getInstanse(false); // Uncompressed recording (WAV)

		extAudioRecorder.setOutputFile( PATH_REC );
		extAudioRecorder.prepare();
		extAudioRecorder.start();
	}
	
	private void stopPlaying(){
		// terminate thread
		audioPlayer.stopPlaying();
	}
	
	private void stopRecording() throws Exception {
		byte byPattern = 0;
		
		// Stop recording
		extAudioRecorder.stop();
		extAudioRecorder.release();
		
		byPattern = extAudioRecorder.GetPattern();
		
 	    StringBuilder sb = new StringBuilder();
        sb.append(String.format("%02X ", byPattern));		
		EditText patternNmb = (EditText)findViewById(R.id.patternNumber);
		patternNmb.setText(sb);
				
		// terminate recording thread
		isRecording = false;
		if (aRecorder != null) {
			if(aRecorder.getState() == AudioRecord.STATE_INITIALIZED)
			{
				aRecorder.stop();
			}
				
			aRecorder.release();
			aRecorder = null;
		}
		
	}
}
