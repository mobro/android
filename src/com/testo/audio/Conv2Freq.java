package com.testo.audio;

import java.io.IOException;
import java.io.RandomAccessFile;

import android.util.FloatMath;

import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.imgproc.Imgproc;
import org.opencv.core.Size;

import android.os.Environment;


public class Conv2Freq {
	private short sChannels = 1;
	private int iSampleRate = 44100;
	private short sSamples = 16;
	byte byPattern = 0;
	int iSamples = 0; 
	byte[] bySrcBuf = null;
	double dThresHold = 0;
	
	TypeConverter typeConverter = null;
	WriteWav writeWav = null;
	
	private static final boolean enableDebug = true; // Set to false if no debug is needed
	
	PatternFilter patFilter = null;
	
	Mat mSrc = null;		// Matrix Time 16 bit signed
	Mat mFreq = null;		// Matrix frequency 32 bit format
	
	int[] buff = null;
	int[] buffMag = null;
	
	Size sSize;
	
	int iMaxValue = 0; 
	int iMaxIndex = 0;
	int iFilterWidth = 200;  // Value in Hz
	
	short[] sSrcBuf = null;		// Buffer stream containing sound samples (short 16 bit signed)
	
	Conv2Freq(int payloadSize, byte[] byBuf, int iSampRate, short sSamp, short sChan)  
	{
		iSamples = payloadSize/2;
		bySrcBuf = byBuf;
		iSampleRate = iSampRate;
		sSamples = sSamp;
		sChannels = sChan;
		
		typeConverter = new TypeConverter();
	}
	
	public void CalcConv2Freq() throws Exception
	{
		if(enableDebug)
		{
			writeWav = new WriteWav( iSampleRate, sSamples, sChannels);
		}
				
		sSrcBuf = new short[(int)(iSamples)];      		
		
		mSrc = new Mat(1,iSamples,CvType.CV_16SC1);    
		mFreq = new Mat(1,iSamples,CvType.CV_32FC1);   
							
		// Build the short values (samples out of byte stream buffer)
		for (int i=0; i<iSamples; i++)
		{ // 16bit sample size
			sSrcBuf[i] = typeConverter.getShort(bySrcBuf[i*2], bySrcBuf[i*2+1]);
		}
		
		if(enableDebug)
		{
			writeWav.writeWavShort(sSrcBuf,iSamples,Environment.getExternalStorageDirectory().getPath() + "/1.wav");
		}
				
		// Write the short samples into the 16bit matrix
		mSrc.put(0, 0, sSrcBuf);
		// Convert the 16 bit matrix into a 32 bit matrix 
		mSrc.convertTo(mSrc, CvType.CV_32FC1);
		
		
		// ---------- Time to frequency converting 
		Core.dft(mSrc, mFreq, Core.DFT_SCALE, 0);
        		
        // Matrix type after dft is CV_32FC1 
		mFreq.convertTo(mFreq, CvType.CV_32SC1);

        buff = new int[iSamples];
        buffMag = new int[iSamples/2];
        mFreq.get(0, 0, buff);

		// ---------- calculate the magnitude = sqrt(Re²+Im²)
		buff[0] = 0; // Set the dc value to zero
		buffMag[0]=buff[0];
		
		for(int i = 1; i < ((iSamples/2)); i++)
		{
			// Re²
			float fTemp = buff[i*2]*buff[i*2];
			// Im²
			fTemp =+ buff[i*2+1]*buff[i*2+1];
			// magnitude 
			buffMag[i] = (int)FloatMath.sqrt(fTemp);
		}
		
		// Search for the maximum value in the frequency data stream 
		MaxValIndex maxValIndex = new MaxValIndex();
		maxValIndex.CalcMaxValIndex( buffMag, iSamples/2);
		iMaxIndex = maxValIndex.GetMaxIndex();
		iMaxValue = maxValIndex.GetMaxValue();
		
		// Calculate index of the 2,4kHz Signal =
		// ( Length of the signal ) / ( sf * 2400 Hz ) 
		int i2k4Index = (iSamples * 2400) / 44100;
					
		// Put a 1kHz wide window over the frequency signal at the max amplitude 
		// Band width is samples times filter width divided by half SampleRate 
		// Band width = ((Samples)*500Hz)/(44100/2)
		int iBandWidth = ((iSamples)*iFilterWidth)/(iSampleRate/2);
		int iRightBoarder = (i2k4Index*2)+iBandWidth;
		int iLeftBoarder = (i2k4Index*2)-iBandWidth;
		
		for(int i = 0; i < ((iSamples)); i++)
		{
			if((i > iRightBoarder)||(i < iLeftBoarder))
			{
				// Values outside the filter window
				buff[i] = 0;
			}
			else
			{
				// Values inside the filter window
				// the values are multiplied by one, so nothing to do
			}
		}
		
		Mat mF2T = new Mat( 1, iSamples, CvType.CV_32S); // Matrix Time 32 bit format
		
		mF2T.put( 0, 0, buff);
		mF2T.convertTo( mF2T, CvType.CV_32FC1);
		
		// Inverse discrete fourier transformation
		Core.dft( mF2T, mF2T, Core.DFT_INVERSE, 0);
		
		mF2T.convertTo( mF2T, CvType.CV_32S);
		// Get an array out of the matrix mF2T
        int[] ibuffTime = new int[iSamples];
        		
		mF2T.get( 0, 0, ibuffTime);
		
		if(enableDebug)
		{
			writeWav.writeWavInt(ibuffTime,iSamples,Environment.getExternalStorageDirectory().getPath() + "/2.wav");
		}
				
		// calculate the absolute values
		for(int i = 0; i < ((iSamples)); i++)
		{
			ibuffTime[i]=Math.abs(ibuffTime[i]);
		}
		
		if(enableDebug)
		{
			writeWav.writeWavInt(ibuffTime,iSamples,Environment.getExternalStorageDirectory().getPath() + "/3.wav");
		}
				
		mF2T.put( 0, 0, ibuffTime); 
		
						
		// -------- calculate the moving average (gleitender Mittelwert)
		sSize = new Size(9,1);
		Imgproc.boxFilter(mF2T, mF2T, -1, sSize);
				
		mF2T.get(0,0,ibuffTime);
		
		if(enableDebug)
		{
			// Write the buffTime into a wav file
			writeWav.writeWavInt(ibuffTime,iSamples,Environment.getExternalStorageDirectory().getPath() + "/4.wav");			
		}

		// -------- calculate the threshold, half maximum value
		Core.MinMaxLocResult mRes = Core.minMaxLoc(mF2T);
		
		dThresHold = mRes.maxVal/2;
		
		for(int i=0;i<iSamples;i++)
		{
			if(dThresHold > ibuffTime[i])
			{
				ibuffTime[i] = 0;
			}
			else
			{
				ibuffTime[i] = 1;
			}	
		}
		
		if(enableDebug)
		{
			writeWav.writeWavInt(ibuffTime,iSamples,Environment.getExternalStorageDirectory().getPath() + "/5.wav");
		}
			
		// ----------  Filter the pattern
		
		byte[] byStream = new byte[ibuffTime.length]; 
		
		for(int iIdx= 0; iIdx<ibuffTime.length;iIdx++)
		{
			byStream[iIdx] = (byte) ibuffTime[iIdx];
		}
					
		patFilter = new PatternFilter(byStream, iSamples, 0, 220);
		
		if(true==patFilter.CalcPattern())
		{
			byPattern = patFilter.GetPattern();	
		}
		else
		{
			throw new Exception("Pattern Calculation went wrong!");
			// The calculation of the pattern went wrong.
		}
	}
	
	
	public byte GetPattern()
	{
		return byPattern;
	}
	
}
