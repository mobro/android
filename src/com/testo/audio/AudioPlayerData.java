package com.testo.audio;

public class AudioPlayerData {
	public boolean isRunning;
	public int sr;
	public double fr;
	public int amp;
	public double ph;

	public AudioPlayerData(boolean isRunning, int sr, double fr, int amp,
			double ph) {
		this.isRunning = isRunning;
		this.sr = sr;
		this.fr = fr;
		this.amp = amp;
		this.ph = ph;
	}
}