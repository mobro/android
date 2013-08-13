package com.testo.audio;

import java.util.Arrays;


public class PatternFilter {
	// Constrains 
	
	
	private int iIndex = 0;
	private int iStartBitIndex = 0; // Index of the first sample of the start bit
	private int iSize = 0;
	private byte[] byStream;
	boolean bFoundStart = false;
	boolean bIsBit = false;
	int iSPB = 220; // (44100/2400)*12
	private byte byPattern;
	enum BitLevel {eLow,eHigh};
	enum SearchState {eFound,eNotFound,ePending};
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
	}

	public byte GetPattern()
	{
	  return byPattern;
	}
	
	public boolean CalcPattern()
	{
		boolean bSuccess = false;
		
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
	

	
	// Find the first "bit" in the byte stream which contains the pattern
	private boolean FindHighSample()
	{
		boolean bFound = false;
				
		while((bFound != true) && (iIndex<iSize))
		{
			if(byStream[iIndex]!=0) 
			{
				// The first one was found, this should be the start bit
				bFound = IsBit(BitLevel.eHigh,55);
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
		//int iThreshold = 50; // Value in percent
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
				iStartBitIndex = iStartIndex;
			}			
		}
		
		return bIs; 		
	}
	
	private boolean ReadDataPattern()
	{
		boolean bSucceeded = false;
		byte[] byPattBuff = {0,0,0,0,0,0,0,0};
		
		if(byPattBuff != null)
		{
			if(true==FindHighSample())
			{
				// The start bit was detected
				for(int iIdx = 7; iIdx >= 0; iIdx--)
				{
					// Read the value of the bit in the pattern
					// byPattBuff[iIdx] = byStream[(iStartBitIndex+(iSPB/2)+(iIdx*iSPB))];
					if(BitLevel.eHigh != BitIs(65))
					{
						byPattBuff[iIdx] = 0;
					}
					else
					{
						byPattBuff[iIdx] = 1;
					}
				}

				// check the parity bit
				if(IsBit(BitLevel.eHigh, 50))
				{
					bSucceeded = true;
					
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
	
	// Check the pattern of the first eight bit (01010101)
	boolean CheckStartPattern()
	{
		boolean bFound = true; 
		byte byLoop = 0;

		while((byLoop < 8) && (bFound != false))
		{
			if(0 != (byLoop%2))
			{ 
				// All odd values are low
				bFound = IsBit(BitLevel.eLow,60);
			}
			else
			{
				// All even values are high
				bFound = IsBit(BitLevel.eHigh,60);
			}
			byLoop++;
		}
		
		if(false != bFound)
		{
			// Check the parity bit
			bFound = IsBit(BitLevel.eLow,55);
		}
				
		return bFound;
	}
	
	
	// Check if the bit has high or low level
	BitLevel BitIs(int iThreshold)
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
			iStartBitIndex = iStartIndex;
		}
		
		
		return bL;
	}
}
