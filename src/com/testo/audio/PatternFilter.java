package com.testo.audio;

import android.annotation.TargetApi;
import android.os.Build;
import java.util.Arrays;


public class PatternFilter {
	private byte byPattern;				// The data pattern which was received
	
	private byte byPercentageHigh = 51;	// This value defines how much samples have to be high to decide a bit is 1 
	private byte byPercentageLow = 51;	// This value defines how much samples have to be low to decide a bit is 0
	private byte byPercentage = 51; 	// This value defines the minimum percentage to decide weather a bit is 0 or 1
	private byte byPercentageStartHigh = 70;  // This value defines the minimum percentage to decide weather the start bit is 0 or 1
	
	private int iIndex = 0;
	private int iSize = 0;
	private byte[] byStream;
	private boolean bFoundStart = false;
	private int iSPB = 220; 			// The formula for SamplesPerBit is (sample rate/carrier frequency)*periods per bit (44100/2400)*12
	
	private int iSignalQuality;
	
	private enum BitLevel {eLow,eHigh};
	private enum SearchState {eFound,eNotFound,ePending};
	private SearchState searchState = SearchState.ePending;
	
	// Constructor
	// byBuffer: Data vector
	// iSize: Number of bytes to search in
	// iIndex: Start Index
	// SPB: Samples per bit = (sample frequency/carrier frequency)*Number of periods per bit
	public PatternFilter(byte[] byBuffer, int iS, int iIdx, float fSamPerBit)
	{
		Initialize(byBuffer, iS, iIdx, fSamPerBit);
	}
	
	
	@TargetApi(Build.VERSION_CODES.GINGERBREAD)
	private void Initialize(byte[] byBuffer, int iS, int iIdx, float fSamPerBit)
	{
		// Initialize the byte buffer
		if(byBuffer != null)
		{
			if(iS<=byBuffer.length)
			{
				byStream = Arrays.copyOf(byBuffer,iS);	
			}
		}
		
		iIndex = iIdx;
		iSize = iS;
		iSPB = (int)fSamPerBit;
		byPattern = 0;
		iSignalQuality = 0;
	}

	// Function distill the data pattern (8-bit pattern) in the byte stream 
	public boolean CalcPattern()
	{
		boolean bSuccess = false;
		
		CalcSignalQuality();
		
		// Search the start bit in the byte data stream
		while(searchState == SearchState.ePending)
		{
			bFoundStart = FindHighSample();
			if(false != bFoundStart)
			{
				// the start bit was found, check the start byte
				if(true == CheckStartPattern() && (searchState!=SearchState.eNotFound))
				{	
					if(ReadDataPattern())
					{
						searchState = SearchState.eFound;	
						bSuccess = true;
					}
				}
			}
			else
			{
				// There was no start bit 
				searchState = SearchState.eNotFound;
				
			}
		}
		
		return bSuccess;  
	}
	
	// Get the pattern after successfully calling the CalcPattern function
	public byte GetPattern()
	{
	  return byPattern;
	}
	
	
	// Find the first "bit" in the byte stream which contains the pattern
	private boolean FindHighSample()
	{
		boolean bFound = false;
				
		while((bFound != true) && (iIndex<iSize))
		{
			if(byStream[iIndex]!=0) 
			{
				// The first one was found, this should be the start bit
				bFound = IsBit(BitLevel.eHigh,byPercentageStartHigh);
			}
			
			iIndex++;
		}
		
		if(!(iIndex<iSize))
		{
			searchState = SearchState.eNotFound;
		}
		
		return bFound; 
	}
	
	
	// check if the bit contains more then threshold ones in percent
	private boolean IsBit(BitLevel bitLevel, int iThreshold)
	{
		boolean bIs = false;
		int iNum = 0;
		int iPercental = 0;
		int iStartIndex = iIndex;
		
		// check if the bit contains a minimum amount of one's (samples where the signal is higher than a certain threshold)
		while((iIndex < (iSPB+iStartIndex)) && (iIndex < iSize))
		{
			if(bitLevel == BitLevel.eLow)
			{
				if(byStream[iIndex] != 1)
				{
					iNum++;
				}
			}
			else
			{
				if(byStream[iIndex] != 0)
				{
					iNum++;
				}
			}
				
			iIndex++;
		}
		
		if(!(iIndex < iSize))
		{
			searchState = SearchState.eNotFound;
		}
		
		if(searchState != SearchState.eNotFound)
		{
			// Calculate the percent value
			iPercental = 100*iNum/iSPB;
			
			// Decide if the bit is high or low
			if(iPercental >= iThreshold)
			{
				bIs = true;
			}			
		}
		
		return bIs; 		
	}
	
	private boolean ReadDataPattern()
	{
		boolean bSucceeded = false;
		byte[] byPattBuff = {0,0,0,0,0,0,0,0};
		int iNumOnes = 0;
		int iNumZeros = 0;
		byte byParityBit = 0;
		
		if(byPattBuff != null)
		{
			if(true==FindHighSample())
			{
				// The start bit was detected
				for(int iIdx = 7; iIdx >= 0; iIdx--)
				{
					// Read the value of the bit in the pattern
					if(BitLevel.eHigh != BitIs(byPercentage))
					{
						byPattBuff[iIdx] = 0;
						iNumZeros++;
					}
					else
					{
						byPattBuff[iIdx] = 1;
						iNumOnes++;
					}
				}

				if(IsBit(BitLevel.eHigh, byPercentageHigh))
				{
					// The parity bit has to be one
					if(iNumOnes%2!=0)
					{
						bSucceeded = true;
					}
				}
				else
				{
					// The parity bit has to be one
					if(iNumOnes%2==0)
					{
						bSucceeded = true;
					}
				}
				
				if(bSucceeded == true)
				{
					// Code the eight bits into one byte
					for(int iLoop = 0; iLoop < 8; iLoop++)
					{
						if(byPattBuff[iLoop]==1)
						{
							byPattern = (byte) (byPattern | (0x01 << iLoop));
						}
						else
						{
							byPattern = (byte) (byPattern | (0x00) << iLoop);
						}
					}
				}
			}
		}
		
		return bSucceeded;
	}
	

	
	// Following pattern is expected!
	//  _ _   _   _   _   
	// | | |_| |_| |_| |_ _
	// Start	pattern		parity
	
	// Check the pattern of the first eight bit
	private boolean CheckStartPattern()
	{
		boolean bFound = true; 
		byte byLoop = 0;

		while((byLoop < 8) && (bFound != false))
		{
			if(0 != (byLoop%2))
			{ 
				// All odd values are low
				bFound = IsBit(BitLevel.eLow,byPercentageLow);
			}
			else
			{
				// All even values are high
				bFound = IsBit(BitLevel.eHigh,byPercentageHigh);
			}
			byLoop++;
		}
		
		if(false != bFound)
		{
			// Check the parity bit
			bFound = IsBit(BitLevel.eLow,byPercentageLow);
		}
				
		return bFound;
	}
	
	
	// Check if the bit has high or low level
	private BitLevel BitIs(int iThreshold)
	{
		BitLevel bL = BitLevel.eHigh;
		int iNumZeros = 0;
		int iPercentalZeros = 0;
		int iStartIndex = iIndex;
		
		// check if the bit contains a minimum amount of one's (samples where the signal is higher than a certain threshold)
		while((iIndex < (iSPB+iStartIndex)) && (iIndex < iSize))
		{
			if(byStream[iIndex] != 1)
			{
				iNumZeros++;
			}
					
			iIndex++;
		}
		
		// Calculate the percent value of zero samples
		iPercentalZeros = 100*iNumZeros/iSPB;
		
		// Decide if the bit is high or low
		if(iPercentalZeros >= iThreshold)
		{
			bL = BitLevel.eLow;
		}
		
		return bL;
	}
	
	private void CalcSignalQuality()
	{
		int iNumOfZeros = 0;
		int iNumOfOnes = 0;
		
		for (int iLoop = 0; iLoop < iSize; iLoop++)
		{
			if(byStream[iLoop]==0)
			{
				iNumOfZeros++;
			}
			else
			{
				iNumOfOnes++;
			}
		}
		if(iNumOfOnes!=0)
		{
			iSignalQuality = (100*iNumOfOnes)/iSize;
		}
	}
	
	public int GetHighPercental()
	{
		return iSignalQuality;
	}
}
