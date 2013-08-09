package com.testo.audio;

public class TypeConverter {

	TypeConverter()
	{
		
	}
	
	/* 
	 * 
	 * Converts a byte[2] to a short, in LITTLE_ENDIAN format
	 * 
	 */
	public short getShort(byte argB1, byte argB2)
	{
		short sTest = (short)0x0000;
		sTest = (short)(0x00ff & argB1);
		sTest = (short)(sTest | (short)(0xff00 & (argB2 << 8)));
				
		return sTest; 
	}

	public int getInt(byte argB1, byte argB2, byte argB3, byte argB4)
	{
		int iInt = (int)0x00000000;
		
		iInt = (int)(0x000000ff & argB1);
		iInt = (int)(iInt | (int)(0x0000ff00 & (argB2<<8)));
		iInt = (int)(iInt | (int)(0x00ff0000 & (argB3<<16)));
		iInt = (int)(iInt | (int)(0xff000000 & (argB4<<24)));
		
		return iInt;
	}
}
