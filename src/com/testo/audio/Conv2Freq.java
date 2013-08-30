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
	private static final int iPeriodPerBit = 12;  	// One bit has 12 carrier sinus periodes 
	private static final int iCarrierFreq = 2400; 	// Carrier Frequency in Hz
	private static final int iHalfFilterWidth = 500;  	// Half band path filter width in Hz
	
	private short sChannels = 1;					// Number of audio channels (mono=1, stereo=2) 
	private int iSampleRate = 44100;				// Audio sample rate
	private short sSamples = 16;					// Size of one sample in bit
	private int iSigQual;
	
	private byte byPattern = 0;
	private int iSamples = 0; 
	private byte[] bySrcBuf = null;
	private double dThresHold = 0;
	
	TypeConverter typeConverter = null;
	WriteWav writeWav = null;
	
	private static final boolean enableDebug = true; // Set to false if no debug is needed
	
	private PatternFilter patFilter = null;
	
	private Mat mSrc = null;		// Matrix Time 16 bit signed
	private Mat mFreq = null;		// Matrix frequency 32 bit format
	
	short[] sSrcBuf = null;		// Buffer stream containing sound samples (short 16 bit signed)
	
	Conv2Freq(int payloadSize, byte[] byBuf, int iSampRate, short sSamp, short sChan)  
	{
		Initialize( payloadSize, byBuf, iSampRate, sSamp, sChan);
		
		typeConverter = new TypeConverter();
	}
	
	private void Initialize(int payloadSize, byte[] byBuf, int iSampRate, short sSamp, short sChan)
	{
		iSamples = payloadSize/2;
		bySrcBuf = byBuf;
		iSampleRate = iSampRate;
		sSamples = sSamp;
		sChannels = sChan;
		iSigQual = 0;
	}
	
	public void CalcConv2Freq() throws Exception
	{
		int i2k4Index = 0;
		int iBandWidth = 0;
		int iRightBoarder = 0;
		int iLeftBoarder = 0;
		int[] ibuffTime = null;
		byte[] byStream = null;
		float fSampleRate = 0.0f;
		int[] buff = null;
		int[] buffMag = null;
		Size sSize;
		
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
			writeWav.writeWavShort(sSrcBuf,iSamples,
					Environment.getExternalStorageDirectory().getPath() + "/1.wav");
		}
		
		for(int i = 1; i < ((iSamples)); i++)
		{
			// Set start and end of signal to zero
			if(i<=((iSamples)/5)||i>=(4*(iSamples)/5))
			{
				sSrcBuf[i] = 0;
			}
			else
			{
				sSrcBuf[i] = sSrcBuf[i];
			}
		}
		
		if(enableDebug)
		{
			writeWav.writeWavShort(sSrcBuf,iSamples,
					Environment.getExternalStorageDirectory().getPath() + "/1_2.wav");
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
		
		// Calculate index of the 2,4kHz Signal =
		// ( Length of the signal ) / ( sf * iCarrierFreq Hz ) 
		i2k4Index = (iSamples * iCarrierFreq) / iSampleRate;
					
		// Put a 1kHz wide window over the frequency signal at the max amplitude 
		// Band width is samples times filter width divided by half SampleRate 
		// Band width = ((Samples)*500Hz)/(44100/2)
		iBandWidth = ((iSamples)*iHalfFilterWidth)/(iSampleRate/2);
		iRightBoarder = (i2k4Index*2)+iBandWidth;
		iLeftBoarder = (i2k4Index*2)-iBandWidth;
		
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
		ibuffTime = new int[iSamples];

		mF2T.get( 0, 0, ibuffTime);
		
		if(enableDebug)
		{
			writeWav.writeWavInt(ibuffTime,iSamples,
					Environment.getExternalStorageDirectory().getPath() + "/2.wav");
		}
				
		// calculate the absolute values
		for(int i = 0; i < ((iSamples)); i++)
		{
			ibuffTime[i]=Math.abs(ibuffTime[i]);
			//ibuffTime[i]=(int) Math.sqrt((double)ibuffTime[i]*(double)ibuffTime[i]);
		}
		
		if(enableDebug)
		{
			writeWav.writeWavInt(ibuffTime,iSamples,
					Environment.getExternalStorageDirectory().getPath() + "/3.wav");
		}
				
		mF2T.put( 0, 0, ibuffTime); 
		
						
		// -------- calculate the moving average (gleitender Mittelwert)
		sSize = new Size(9,1);
		Imgproc.boxFilter(mF2T, mF2T, -1, sSize);
				
		mF2T.get(0,0,ibuffTime);
		
		if(enableDebug)
		{
			// Write the buffTime into a wav file
			writeWav.writeWavInt(ibuffTime,iSamples,
					Environment.getExternalStorageDirectory().getPath() + "/4.wav");
		}

		// -------- calculate the threshold, half maximum value
		Core.MinMaxLocResult mRes = Core.minMaxLoc(mF2T);
		
		dThresHold = mRes.maxVal/3;
		
		// Every value bigger threshold is one, every value smaller is zero.
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
			writeWav.writeWavInt(ibuffTime,iSamples,
					Environment.getExternalStorageDirectory().getPath() + "/5.wav");
		}
		
		// ----------  Filter the pattern
		byStream = new byte[ibuffTime.length]; 
		
		for(int iIdx= 0; iIdx<ibuffTime.length;iIdx++)
		{
			byStream[iIdx] = (byte) ibuffTime[iIdx];
		}
		
		fSampleRate = (float)((float)iSampleRate/(float)iCarrierFreq);
		fSampleRate = fSampleRate * iPeriodPerBit; 
		
		patFilter = new PatternFilter(byStream, iSamples, 0, fSampleRate);
		
		if(true==patFilter.CalcPattern())
		{
			byPattern = patFilter.GetPattern();	
		}
		else
		{
			byPattern = 0;
			//throw new Exception("Pattern Calculation went wrong!");
			// The calculation of the pattern went wrong.
		}
		
		iSigQual = patFilter.GetHighPercental();
	}
	
	
	public byte GetPattern()
	{
		return byPattern;
	}
	
	public int GetSignalQuality()
	{
		return iSigQual;
	}
	
}
