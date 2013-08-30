package com.testo.audio;

import android.content.Context;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.view.SoundEffectConstants;

public class AudioPlayer {
	// Audio playing
	private static Thread playingThread;
	AudioPlayerData data = new AudioPlayerData(true, 44100, 2400.f, 10000, 0.0);
	AudioManager amanager;
	Context mContext;
	int oldAudioMode;
	int oldRingerMode;
	boolean bisSpeakerPhoneOn;
	
	public AudioPlayer(Context mContext)
	{
		this.mContext = mContext;
		initialize();
	}
	
	private void initialize()
	{
		
	}
	
	public void playAudio()												
	{
		playingThread = new Thread() {
			public void run(){
				amanager = (AudioManager)mContext.getSystemService(Context.AUDIO_SERVICE);
				
				// Save the current audio manager settings
				oldAudioMode = amanager.getMode();
				oldRingerMode = amanager.getRingerMode();
				bisSpeakerPhoneOn = amanager.isSpeakerphoneOn();
				
				// Apply your audio manger settings
				amanager.requestAudioFocus(null, amanager.AUDIOFOCUS_GAIN_TRANSIENT, amanager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK);
				amanager.setMode(AudioManager.MODE_IN_CALL); // The sound have to be played over the speakers and not the earpeaces
				amanager.setSpeakerphoneOn(true);

				amanager.loadSoundEffects();
				amanager.playSoundEffect( SoundEffectConstants.CLICK,5);
				amanager.unloadSoundEffects();

				data.isRunning = true;
				// set process priority
				setPriority(Thread.MAX_PRIORITY);
				int buffsize = AudioTrack.getMinBufferSize(data.sr, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT);
				
				// create an audio track object
				AudioTrack audioTrack = new AudioTrack(AudioManager.STREAM_MUSIC,data.sr,AudioFormat.CHANNEL_OUT_MONO,AudioFormat.ENCODING_PCM_16BIT,buffsize,AudioTrack.MODE_STREAM);
				
				short samples[] = new short[buffsize];
				double twopi = 8.*Math.atan(1.);
								
				audioTrack.play();
				
				// synthesis loop
				while(data.isRunning){
					for(int i = 0; i<buffsize;i++){
						samples[i]= (short)(data.amp*Math.sin(data.ph));
						data.ph += twopi*data.fr/data.sr;
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
		data.isRunning = false;
		try{
			playingThread.join();
		}catch (InterruptedException e){
			e.printStackTrace();
		}
		playingThread = null;
		
		// Restore the audio manger settings
		amanager.setMode(oldAudioMode);
		amanager.setRingerMode(oldRingerMode);
		amanager.setSpeakerphoneOn(bisSpeakerPhoneOn);
		amanager.abandonAudioFocus(null);
	}
}
