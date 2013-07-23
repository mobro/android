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
import android.text.InputType;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.EditText;
import android.widget.ToggleButton;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;


//import android.media.MediaPlayer.OnCompletionListener;
// MediaPlayer

/**
 * @author Felix Born
 * @date 02.07.2013
 * 
 */
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
	//private EditText patternTxt;
    
    // Audio In Data
    //private static Short[] series1Numbers;
    
	// Audio recording
	private static Thread recordingThread;
	private static boolean isRecording = false;
	
	// File Name
	private String FILE_NAME_RECORD = "record.pcm";

	// Audio playing
	private static Thread playingThread;
	int sr = 44100;  // Sampling rate
	boolean isRunning = true;
	
	// ExtAudioRecorder
	ExtAudioRecorder extAudioRecorder;
	static String PATH_REC = (Environment.getExternalStorageDirectory().getPath() + "/record.wav");


    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {

        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS:
                {
                    Log.i(TAG, "OpenCV loaded successfully");

                    /* Now enable camera view to start receiving frames */
                    
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
		stopRecording();
		recordingThread = null;
	}

	public void buttonStartStop(View v) {
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
			// starts playing thread
			startPlaying();
		} else { // not active - stop recording
			if (D) Log.i(TAG, "PlayStop");
			stopPlaying();
		}
	}
	
	private void startPlaying(){
		playingThread = new Thread() {
			public void run(){
				isRunning = true;
				// set process priority
				setPriority(Thread.MAX_PRIORITY);
				int buffsize = AudioTrack.getMinBufferSize(sr, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT);
				// create an audiotrack object
				AudioTrack audioTrack = new AudioTrack(AudioManager.STREAM_MUSIC,sr,AudioFormat.CHANNEL_OUT_MONO,AudioFormat.ENCODING_PCM_16BIT,buffsize,AudioTrack.MODE_STREAM);
				short samples[] = new short[buffsize];
				int amp = 5000;
				double twopi = 8.*Math.atan(1.);
				double fr = 2400.f;
				double ph = 0.0;
				
				audioTrack.play();
				
				// synthesis loop
				while(isRunning){
					for(int i = 0; i<buffsize;i++){
						samples[i]= (short)(amp*Math.sin(ph));
						ph += twopi*fr/sr;
					}
					audioTrack.write(samples,0,buffsize);
				}
				audioTrack.stop();
				audioTrack.release();
			}
		};
		playingThread.start();	
	}
	
	private void startRecording() {
		// Start recording
		//extAudioRecorder = ExtAudioRecorder.getInstanse(true);	  // Compressed recording (AMR)
		extAudioRecorder = ExtAudioRecorder.getInstanse(false); // Uncompressed recording (WAV)

		extAudioRecorder.setOutputFile( PATH_REC );
		extAudioRecorder.prepare();
		extAudioRecorder.start();

		/*aRecorder = new AudioRecord(audioSource, sampleRateInHz, channelConfig,
		audioFormat, bufferSizeInBytes);
		aRecorder.startRecording();
		isRecording = true;
		
		// new Thread for recording
		recordingThread = new Thread(new Runnable() {
			
			@Override
			public void run() {
				Short[] recordData = recordAudioData();
				// if record is stopped, save data
				try {
					// create file
					FileOutputStream fos = openFileOutput(FILE_NAME_RECORD, Context.MODE_PRIVATE);
					fos.close();
					// save Data
					PcmWriter pcmWriter = new PcmWriter(FILE_NAME_RECORD, recordData);
					pcmWriter.saveFile();
				} catch (IOException e) {
					Log.e(TAG, "IOExeption in recording Thread");
				}
				recordingThread = null;
			}
		});
		recordingThread.start();*/
	}
	
	private void stopPlaying(){
		// terminate thread
		isRunning = false;
		try{
			playingThread.join();
		}catch (InterruptedException e){
			e.printStackTrace();
		}
		playingThread = null;
	}
	
	private void stopRecording() {
		byte byPattern = 0;
		
		// Stop recording
		extAudioRecorder.stop();
		extAudioRecorder.release();
		
		char[] chPattern = new char[5];  
		
		
		byPattern = extAudioRecorder.GetPattern();
		
		if(byPattern == -71)
		{
			chPattern[0] =	'O';
			chPattern[1] =	'K';
			
		}
		else
		{
			chPattern[0] =	'K';
			chPattern[1] =	'O';
		}
		
		
		
		EditText patternTxt = (EditText)findViewById(R.id.patternText);
		//patternTxt.setText(R.id.patternText);
		patternTxt.setText(chPattern, 0, 2);
		
	    StringBuilder sb = new StringBuilder();
        sb.append(String.format("%02X ", byPattern));		
		EditText patternNmb = (EditText)findViewById(R.id.patternNumber);
		//patternNmb.setRawInputType(InputType.TYPE_CLASS_NUMBER);		
		patternNmb.setText(sb);
		
		//setText(chPattern, 0, 1);
		/*try {
			FileInputStream fis = openFileInput(PATH_REC);
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}*/
		
		
		// terminate thread
		/*isRecording = false;
		if (aRecorder != null) {
			if(aRecorder.getState() == AudioRecord.STATE_INITIALIZED)
				aRecorder.stop();
			aRecorder.release();
			aRecorder = null;
		}*/
	}


	
	/*private Short[] recordAudioData() {
		ArrayList<Short> data = new ArrayList<Short>();
		short dataBuffer[] = new short[bufferSizeInBytes];
		// received bytes by recorder
		int read = 0;
		// counter to update display

		while(isRecording) {
			read = aRecorder.read(dataBuffer, 0, bufferSizeInBytes);
			if(AudioRecord.ERROR_INVALID_OPERATION != read) {
				// copy everything into data to get the whole record
				for (int i = 0; i < read; ++i) {
					data.add(dataBuffer[i]);
					//series1Numbers[i] =  dataBuffer[i];
				}
			}

			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		return data.toArray(new Short[data.size()]);
	}*/
}
