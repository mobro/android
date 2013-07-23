package com.testo.audio;

import java.util.Arrays;


public class PatternFilter {
	// Constrains 
	
	
	private int iIndex = 0;
	private int iStartBitIndex = 0;
	private int iSize = 0;
	private byte[] byStream;
	boolean bFoundStart = false;
	boolean bIsBit = false;
	int iSPB = 220; // (44100/2400)*12
	byte byPattern;
	
	// Constructor
	// byBuffer: Data vector
	// iSize: Number of bytes to search in
	// iIndex: Start Index
	// SPB: Samples per bit = (sample frequency/carrier frequency)*Number of periods per bit
	public PatternFilter(byte[] byBuffer, int iS, int iIdx, int SamPerBit)
	{
		Initialize(byBuffer, iS, iIdx, SamPerBit);
	}
	
	
	private void Initialize(byte[] byBuffer, int iS, int iIdx, int SamPerBit)
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
		iSPB = SamPerBit;
	}

	public byte GetPattern()
	{
	  return byPattern;
	}
	
	public boolean CalcPattern()
	{
		boolean bSuccess = false;
		byte[] byPattBuff = {0,0,0,0,0,0,0,0};
		
		if(byPattBuff != null)
		{
			// Search a one in the byte data stream
			bFoundStart = FindOne();
			if(false!=bFoundStart)
			{
				// Check if the first bit contains only one's
				bIsBit = IsBit();
				
				if(false != bIsBit)
				{
					// the start bit was found
					byPattBuff[0] = 1;
					
					for(int iIdx = 1; iIdx < 8; iIdx++)
					{
						// Read the value of the bit in the pattern
						byPattBuff[iIdx] = byStream[(iStartBitIndex+(iSPB/2)+(iIdx*iSPB))];
					}
								
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
					
					
					bSuccess = true;
				}
			}			
		}

		
		return bSuccess;  
	}
	

	
	// Find the first "bit" in the byte stream which contains the pattern
	private boolean FindOne()
	{
		boolean bFound = false;
				
		while((bFound != true) && (iIndex<iSize))
		{
			if(byStream[iIndex]!=0) 
			{
				// The first one was found, this should be the start bit
				bFound = true;
				iStartBitIndex = iIndex; 
			}
			
			iIndex++;
		}
		
		return bFound; 
	}
	
	
	// check if the bit contains just one's
	private boolean IsBit()
	{
		boolean bIs = true;
		
		// check the whole bit width contains one's
		while((bIs != true) && (iIndex < iSPB))
		{
			if(1 != byStream[iIndex]) 
			{
				bIs = false;
			}
					
			iIndex++;
		}
		
		return bIs; 		
	}
	
}
