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
	
}
