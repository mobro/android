package com.testo.audio;

public class MaxValIndex {
	int iMaxIndex = 0;
	int iMaxValue = 0;
	
	MaxValIndex()
	{
		
	}
	
	public void CalcMaxValIndex(int[] iBuffer, int iValues)
	{
		for(int i = 0; i < ((iValues/2)); i++)
		{
		    int iValue = iBuffer[i];
						
			if(iValue > iMaxValue)
			{
				iMaxIndex = i;
				iMaxValue = iValue;
			}
		}
		
	}
	
	public int GetMaxValue()
	{
		return iMaxValue;
	}
	
	public int GetMaxIndex()
	{
		return iMaxIndex;
	}
}
