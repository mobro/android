package com.testo.audio;

import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import android.os.Environment;
import android.util.Log;

public class PcmWriter {
	
	// Debugging
	private static final String TAG = "PcmWriter";
	private static final boolean D = true;
	
	private File pcmFile;
//	private final static String PATH_REC = "/data/data/com.testo.audio/";
	private final static String PATH_REC = (Environment.getExternalStorageDirectory().getPath() + "/sdcard");
	
	private Short[] data;

	public PcmWriter(String fileName, Short[] data) throws IOException {
		this.pcmFile = new File(PATH_REC + fileName);
		if (D) Log.i(TAG, "Absolute Path: " + pcmFile.getAbsolutePath());
		this.data = data;
	}
	
	public void saveFile() throws IOException {
		DataOutputStream dos = new DataOutputStream(
				new BufferedOutputStream(new FileOutputStream(pcmFile)));
		for(Short s : data)
			dos.writeShort(s);
		dos.close();
		if (D) Log.i(TAG, "File Saved: " + pcmFile.getName());
	}	
}