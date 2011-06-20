package com.sswf;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import javax.sound.sampled.TargetDataLine;

import org.jma.DataType;
import org.jma.encoder.IEncoder;
import org.jma.encoder.audio.IAudioEncoder;

import com.sswf.rtmp.Consumer;

public class CaptureAudio implements Runnable {

	private ByteArrayOutputStream outputStream;
	private boolean running;
	private TargetDataLine line;
	private IAudioEncoder audioEncoder;
	private Consumer consumer;

	public CaptureAudio(ByteArrayOutputStream outputStream,	TargetDataLine line, IEncoder encoder) {
		this.outputStream = outputStream;
		this.line = line;
		this.audioEncoder = encoder.getAudioEncoder();
	}

	public CaptureAudio(Consumer consumer, TargetDataLine line) {
		this.consumer = consumer;
		this.line = line;
		this.audioEncoder = consumer.getEncoder().getAudioEncoder();
	}

	public void run() {
		running = true;
		int bufferSize = audioEncoder.getInputBufferSize();
		byte buffer[] = new byte[bufferSize];
		byte[] encoded = new byte[audioEncoder.getOutputBufferSize()];

		try {
			while (running) {
				int count = line.read(buffer, 0, buffer.length);
				if (count > 0) {
					int encodedCount = audioEncoder.encodeBuffer(buffer, 0,	count, encoded);
					// encoded mp3 bytes
					
					if (outputStream != null)
						outputStream.write(encoded, 0, encodedCount);
					if (consumer != null)
						consumer.putData(DataType.AUDIO, System.currentTimeMillis(), encoded, encodedCount);
				}
			}
			int encodedCount = audioEncoder.encodeFinish(encoded);
			if (outputStream != null)
				outputStream.write(encoded, 0, encodedCount);
			if (consumer != null)
				consumer.putData(DataType.AUDIO, System.currentTimeMillis(), encoded,
						encodedCount);
			audioEncoder.close();
			outputStream.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public synchronized void setRunning(boolean running){
		this.running = running;
	}

}
