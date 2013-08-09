package com.testo.audio;

import java.io.IOException;
import java.io.RandomAccessFile;

public class WriteWav {
	private RandomAccessFile randomAccessWriter;
	
	private int iSampleRate = 44100; 	// Sample rate
	private short sSamples = 16;		// Bits per sample
	private short sChannels = 1;		// Channels (mono = 1, stereo = 2)
	
	WriteWav(int iSampRate, short sSampl, short sChan)
	{
		iSampleRate = iSampRate; 	
		sSamples = sSampl;			
		sChannels = sChan;
	}
	
	public void writeWavShort(short[] sBuffer, int iSamples, String PathFileName) throws IOException
	{
		// write file header
		int iPayLoad = iSamples*2;
		//short[] sBuffer = new short[iSamples];
		byte[] bBuffer = new byte[iPayLoad];
		
		randomAccessWriter = new RandomAccessFile( PathFileName, "rw");
		randomAccessWriter.setLength(0); // Set file length to 0, to prevent unexpected behavior in case the file already existed
		randomAccessWriter.writeBytes("RIFF");
		randomAccessWriter.writeInt(Integer.reverseBytes(36+iPayLoad)); // File size -8 
		randomAccessWriter.writeBytes("WAVE");
		randomAccessWriter.writeBytes("fmt ");
		randomAccessWriter.writeInt(Integer.reverseBytes(16)); // Sub-chunk size, 16 for PCM
		randomAccessWriter.writeShort(Short.reverseBytes((short) 1)); // AudioFormat, 1 for PCM
		randomAccessWriter.writeShort(Short.reverseBytes((short) 1));// Number of channels, 1 for mono, 2 for stereo
		randomAccessWriter.writeInt(Integer.reverseBytes(iSampleRate)); // Sample rate
		randomAccessWriter.writeInt(Integer.reverseBytes(iSampleRate*sSamples*sChannels/8)); // Byte rate, SampleRate*NumberOfChannels*BitsPerSample/8
		randomAccessWriter.writeShort(Short.reverseBytes((short)(sChannels*sSamples/8))); // Block align, NumberOfChannels*BitsPerSample/8
		randomAccessWriter.writeShort(Short.reverseBytes(sSamples)); // Bits per sample
		randomAccessWriter.writeBytes("data"); // Header signature
		randomAccessWriter.writeInt(Integer.reverseBytes(iPayLoad)); // Data chunk size not known yet, write 0
										
		for(int i = 0; i<iSamples; i++)
		{
			bBuffer[i*2+1] = (byte) ((0xff00 & sBuffer[i]) >> 8);
			bBuffer[i*2] = (byte) (sBuffer[i] & 0x00ff);
		}
				
		randomAccessWriter.write(bBuffer); // Write buffer to file
		randomAccessWriter.close();

	}
	
	public void writeWavInt(int[] iBuffer, int iSamples, String PathFileName) throws IOException
	{
		// write file header
		int iPayLoad = iSamples*2;
		short[] sBuffer = new short[iSamples];
		byte[] bBuffer = new byte[iPayLoad];
		
		randomAccessWriter = new RandomAccessFile( PathFileName, "rw");
		randomAccessWriter.setLength(0); // Set file length to 0, to prevent unexpected behavior in case the file already existed
		randomAccessWriter.writeBytes("RIFF");
		randomAccessWriter.writeInt(Integer.reverseBytes(36+iPayLoad)); // Final file size not known yet, write 0 
		randomAccessWriter.writeBytes("WAVE");
		randomAccessWriter.writeBytes("fmt ");
		randomAccessWriter.writeInt(Integer.reverseBytes(16)); // Sub-chunk size, 16 for PCM
		randomAccessWriter.writeShort(Short.reverseBytes((short) 1)); // AudioFormat, 1 for PCM
		randomAccessWriter.writeShort(Short.reverseBytes((short) 1));// Number of channels, 1 for mono, 2 for stereo
		randomAccessWriter.writeInt(Integer.reverseBytes(iSampleRate)); // Sample rate
		randomAccessWriter.writeInt(Integer.reverseBytes(iSampleRate*sSamples*sChannels/8)); // Byte rate, SampleRate*NumberOfChannels*BitsPerSample/8
		randomAccessWriter.writeShort(Short.reverseBytes((short)(sChannels*sSamples/8))); // Block align, NumberOfChannels*BitsPerSample/8
		randomAccessWriter.writeShort(Short.reverseBytes(sSamples)); // Bits per sample
		randomAccessWriter.writeBytes("data");
		randomAccessWriter.writeInt(Integer.reverseBytes(iPayLoad)); // Data chunk size not known yet, write 0
										
		// Write the data into the wav file
		for(int i = 0; i<iSamples; i++)
		{
			sBuffer[i] = (short)iBuffer[i];
		}
		
		for(int i = 0; i<iSamples; i++)
		{
			bBuffer[i*2+1] = (byte) ((0xff00 & iBuffer[i]) >> 8);
			bBuffer[i*2] = (byte) (iBuffer[i] & 0x00ff);
		}
				
		randomAccessWriter.write(bBuffer); // Write buffer to file
		randomAccessWriter.close();

	}
	
}
