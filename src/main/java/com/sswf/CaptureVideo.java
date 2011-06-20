package com.sswf;

import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.image.BufferedImage;
import java.awt.image.DataBuffer;

import org.jma.DataType;
import org.jma.encoder.video.IVideoEncoder;

import com.sswf.rtmp.Consumer;

public class CaptureVideo implements Runnable{
	
	private Consumer consumer;
	private Robot robot;
	private boolean running;
	private IVideoEncoder videoEncoder;
	
	final int blockWidth = 32;
    final int blockHeight = 32;
    
    final int timeBetweenFrames = 100; // 1000 / frameRate
    int frameCounter = 0;
	private int x = 0;
	private int y = 0;
	private int height = 240;
	private int width = 320;
	

	public CaptureVideo(Consumer consumer, Robot robot) {
		this.consumer = consumer;
		this.robot = robot;
		this.videoEncoder = consumer.getEncoder().getVideoEncoder();
	}

	public void run() {
		running = true;
		byte[] previous = null;
        
        while (running)
        {
            final long ctime = System.currentTimeMillis();
            
            BufferedImage image = robot.createScreenCapture(new Rectangle(x, y, width, height));
            
            byte[] current = toBGR(image);
            
            try
            {
                final byte[] encoded = videoEncoder.encode(current, previous, width, height);
                if (previous == null)
	            {
	                	consumer.putData(DataType.KEY_FRAME, System.currentTimeMillis(), encoded, encoded.length);
	                } else
	                {
	                	consumer.putData(DataType.INTER_FRAME, System.currentTimeMillis(), encoded, encoded.length);
	                }
                
                previous = current;
                
                if (++frameCounter % 10 == 0) previous = null;
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }
            
            final int spent = (int) (System.currentTimeMillis() - ctime);
            
            try {
				Thread.sleep(Math.max(0, timeBetweenFrames - spent));
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
        }
	}
	
	 /**
     * @param image
     * @return BGR image content
     */
    public byte[] toBGR(BufferedImage image)
    {
        final int width = image.getWidth();
        final int height = image.getHeight();
        
        byte[] buf = new byte[3 * width * height];
        
        final DataBuffer buffer = image.getData().getDataBuffer();
        
        for (int y = 0; y < height; y++)
        {
            for (int x = 0; x < width; x++)
            {
                final int rgb = buffer.getElem(y * width + x);
                final int offset = 3 * (y * width + x);
                
                buf[offset + 0] = (byte) (rgb & 0xFF);
                buf[offset + 1] = (byte) ((rgb >> 8) & 0xFF);
                buf[offset + 2] = (byte) ((rgb >> 16) & 0xFF);
            }
        }
        
        return buf;
    }

}
