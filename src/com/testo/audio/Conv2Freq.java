package com.testo.audio;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.List;
import android.util.FloatMath;

import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.utils.Converters;
import org.opencv.imgproc.Imgproc;
import org.opencv.core.Size;
import org.opencv.core.Core.MinMaxLocResult;

import android.media.AudioFormat;
import android.media.AudioTrack;
import android.os.Environment;





public class Conv2Freq {
	private RandomAccessFile randomAccessWriter;
	private short nChannels = 1;
	private int sRate = 44100;
	private short bSamples = 16;
	byte byPattern = 0;
	//private int bufferSize = ;
	//private int aSource = ;
	//private int aFormat = ; 
	
	public Conv2Freq(int payloadSize, byte[] bySrcBuf) throws IOException {
		int iSamples = payloadSize/2; 
		int iValue1 = 0, iValue2 = 0;
		int iMaxValue = 0, iMaxIndex = 0;
		int iMinValue = 0, iMinIndex = 0;
		int iFilterWidth = 500;  // Value in Hz
		
		short[] sSrcBuf = new short[(int)(iSamples)]; // Sound Buffer sample stream (short 16 bit signed)		
		
		Mat mSrc = new Mat(1,iSamples,CvType.CV_16SC1); // Matrix Time 16 bit signed
		Mat mTime = new Mat(1,iSamples,CvType.CV_32FC1); // Matrix Time 32 bit format
		Mat mFreq = new Mat(1,iSamples,CvType.CV_32FC1); // Matrix Frequency 32 bit format
		Mat mFreq32SC1 = new Mat(1,iSamples,CvType.CV_32FC1);
							
		//for (int i=0; i<bySrcBuf.length/2; i++)
		// Build the short values (samples out of byte stream buffer)
		for (int i=0; i<iSamples; i++)
		{ // 16bit sample size
			byte b1,b2;
			b1 = bySrcBuf[i*2];
			b2 = bySrcBuf[i*2+1];
			sSrcBuf[i] = getShort(bySrcBuf[i*2], bySrcBuf[i*2+1]);
		}
		
		writeWav(sSrcBuf,iSamples,Environment.getExternalStorageDirectory().getPath() + "/1.wav");
		
		// Write the short samples into the 16bit matrix
		mSrc.put(0, 0, sSrcBuf);
		// Convert and copy the 16 bit matrix into a 32 bit matrix 
		mSrc.convertTo(mTime, CvType.CV_32FC1);
		
		// Time to frequency converting 
		Core.dft(mTime, mFreq, Core.DFT_SCALE, 0);
        
		
        // Matrix type after dft is CV_32FC1 
		mFreq.convertTo(mFreq32SC1, CvType.CV_32SC1);
		int imType = mFreq32SC1.type();
		
		//int[] iBuf = new int[iSamples];
		
		//List<Integer> is = new ArrayList<Integer>();
		
	
		int imCols = mFreq.cols();
        imCols = mFreq32SC1.cols();
		//int[] buff = new int[imCols];
        int[] buff = new int[iSamples];
        int[] buffMag = new int[iSamples/2];
		mFreq32SC1.get(0, 0, buff);
		//Converters.Mat_to_vector_int(mFreq32SC1, is);
		
		/*int iChannels = mFreq32SC1.channels();
		long iElemSize = mFreq32SC1.elemSize();		
		
		for(int i = 0; i < ((iSamples)/2); i++)
		{			
			double dValues[] = mFreq32SC1.get(0, i);
			iValue1 = (int)(dValues[0]/(iSamples));
			iValue2 = (int)(dValues[1]/(iSamples));
			if(iValue1 > iMaxValue)
			{
				iMaxIndex = i*2;
				iMaxValue = iValue1;
			}
			else if (iValue2 > iMaxValue)
			{
				iMaxIndex = i*2;
				iMaxValue = iValue2;
			}
		}*/
		// Calculate the magnitude = sqrt(Re²+Im²)
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
		
		
		iMaxValue = 0;
		iMaxIndex = 0;
		iMinValue = 0;
		iMinIndex = 0;
		
		// Search for the maximum value in the frequency data stream 
		for(int i = 0; i < ((iSamples/2)); i++)
		{
		    int iValue = buffMag[i];
						
			if(iValue > iMaxValue)
			{
				iMaxIndex = i;
				iMaxValue = iValue;
			}
			else if(iValue < iMinValue)
			{
				iMinIndex = i;
				iMinValue = iValue;
			}
		}
				
		
		
		// Put a 1kHz wide window over the frequency signal at the max amplitude 
		// Band width is samples times filter width divided by half SampleRate 
		// Band width = ((Samples)*500Hz)/(44100/2)
		int iBandWidth = ((iSamples)*iFilterWidth)/(44100/2);
		int iRightBoarder = (iMaxIndex*2)+iBandWidth;
		int iLeftBoarder = (iMaxIndex*2)-iBandWidth;
		
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
		
		writeWav(ibuffTime,iSamples,Environment.getExternalStorageDirectory().getPath() + "/2.wav");
		
		// calculate the absolute values
		for(int i = 0; i < ((iSamples)); i++)
		{
			ibuffTime[i]=Math.abs(ibuffTime[i]);
		}
		
		writeWav(ibuffTime,iSamples,Environment.getExternalStorageDirectory().getPath() + "/3.wav");
		
		mF2T.put( 0, 0, ibuffTime); 
		
		Size sSize = new Size(9,1);
		
		// Calculate the moving average (gleitender Mittelwert)
		Imgproc.boxFilter(mF2T, mF2T, -1, sSize);
		
		// Calculate the threshold, half maximum value
		Core.MinMaxLocResult mRes = Core.minMaxLoc(mF2T);
		
		double dThresHold = mRes.maxVal/2;
		
		mF2T.get(0,0,ibuffTime);
		
		// Write the buffTime into a wav file
		writeWav(ibuffTime,iSamples,Environment.getExternalStorageDirectory().getPath() + "/4.wav");
		
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
		
		writeWav(ibuffTime,iSamples,Environment.getExternalStorageDirectory().getPath() + "/5.wav");
		
		// Filter the pattern
		
		byte[] byStream = new byte[ibuffTime.length]; 
		
		for(int iIdx= 0; iIdx<ibuffTime.length;iIdx++)
		{
			byStream[iIdx] = (byte) ibuffTime[iIdx];
		}
					
		PatternFilter patFilter = new PatternFilter(byStream, iSamples, 0, 220);
		
		if(true==patFilter.CalcPattern())
		{
			byPattern = patFilter.GetPattern();	
		}
		
	
		iMaxValue = 0;
		iMaxIndex = 0;
	}
	
	/* 
	 * 
	 * Converts a byte[2] to a short, in LITTLE_ENDIAN format
	 * 
	 */
	// original
	//private short getShort(byte argB1, byte argB2)
	//{
	//	return (short)(argB1 | (argB2 << 8));
	//}
	private short getShort(byte argB1, byte argB2)
	{
		short sTest = (short)0x0000;
		sTest = (short)(0x00ff & argB1);
		sTest = (short)(sTest | (short)(0xff00 & (argB2 << 8)));
				
		return sTest; 
	}
	
	private void writeWav(int[] iBuffer, int iSamples, String PathFileName) throws IOException
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
		randomAccessWriter.writeInt(Integer.reverseBytes(44100)); // Sample rate
		randomAccessWriter.writeInt(Integer.reverseBytes(sRate*bSamples*nChannels/8)); // Byte rate, SampleRate*NumberOfChannels*BitsPerSample/8
		randomAccessWriter.writeShort(Short.reverseBytes((short)(nChannels*bSamples/8))); // Block align, NumberOfChannels*BitsPerSample/8
		randomAccessWriter.writeShort(Short.reverseBytes(bSamples)); // Bits per sample
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
	
	private void writeWav(short[] sBuffer, int iSamples, String PathFileName) throws IOException
	{
		// write file header
		int iPayLoad = iSamples*2;
		//short[] sBuffer = new short[iSamples];
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
		randomAccessWriter.writeInt(Integer.reverseBytes(44100)); // Sample rate
		randomAccessWriter.writeInt(Integer.reverseBytes(sRate*bSamples*nChannels/8)); // Byte rate, SampleRate*NumberOfChannels*BitsPerSample/8
		randomAccessWriter.writeShort(Short.reverseBytes((short)(nChannels*bSamples/8))); // Block align, NumberOfChannels*BitsPerSample/8
		randomAccessWriter.writeShort(Short.reverseBytes(bSamples)); // Bits per sample
		randomAccessWriter.writeBytes("data");
		randomAccessWriter.writeInt(Integer.reverseBytes(iPayLoad)); // Data chunk size not known yet, write 0
										
		// Write the data into the wav file
		//for(int i = 0; i<iSamples; i++)
		//{
		//	sBuffer[i] = (short)iBuffer[i];
		//}
		
		for(int i = 0; i<iSamples; i++)
		{
			bBuffer[i*2+1] = (byte) ((0xff00 & sBuffer[i]) >> 8);
			bBuffer[i*2] = (byte) (sBuffer[i] & 0x00ff);
		}
				
		randomAccessWriter.write(bBuffer); // Write buffer to file
		randomAccessWriter.close();

	}
	
	public byte GetPattern()
	{
		return byPattern;
	}
	
}
