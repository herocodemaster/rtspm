package com.sswf.rtmp;

import org.jma.DataType;
import org.jma.encoder.IEncoder;

public interface Consumer {
	
	public void putData(DataType dataType, long ts, byte[] buf, int size);
	
	public void setRecording(boolean isRecording);
	
	public boolean isRecording();	
	
	public void setEncoder(IEncoder encoder);
	
	public IEncoder getEncoder();
}
