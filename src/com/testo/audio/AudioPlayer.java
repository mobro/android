package com.testo.audio;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;

public class AudioPlayer {
	// Audio playing
	private static Thread playingThread;
	boolean isRunning = true;

	private int sr = 44100;  	// Sampling rate
	private double fr = 2400.f; // Carrier frequency 
	int amp = 10000;				// Amplitude
	double ph = 0.0;			// Signal phase
	
	public AudioPlayer()
	{
		initialize();
	}
	
	private void initialize()
	{
		
	}
	
	public void playAudio()												
	{
		playingThread = new Thread() {
			public void run(){
				isRunning = true;
				// set process priority
				setPriority(Thread.MAX_PRIORITY);
				int buffsize = AudioTrack.getMinBufferSize(sr, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT);
				
				// create an audio track object
				AudioTrack audioTrack = new AudioTrack(AudioManager.STREAM_MUSIC,sr,AudioFormat.CHANNEL_OUT_MONO,AudioFormat.ENCODING_PCM_16BIT,buffsize,AudioTrack.MODE_STREAM);
				
				short samples[] = new short[buffsize];
				double twopi = 8.*Math.atan(1.);
								
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
	
	public void stopPlaying()
	{
		isRunning = false;
		try{
			playingThread.join();
		}catch (InterruptedException e){
			e.printStackTrace();
		}
		playingThread = null;
	}
}
