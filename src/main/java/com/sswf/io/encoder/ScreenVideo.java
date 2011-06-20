package com.sswf.io.encoder;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.util.zip.Deflater;
import java.util.zip.DeflaterOutputStream;

import org.jma.encoder.video.IVideoEncoder;


public class ScreenVideo implements IVideoEncoder {

	private Deflater deflater = new Deflater();
	
	final int blockWidth = 32;
	final int blockHeight = 32;
	
	// inner use methods
   
    
    /**
     * Performs 'ScreenVideo' encode.
     * 
     * @param current
     * @param previous
     * @param blockWidth
     * @param blockHeight
     * @param width
     * @param height
     * @return buffer
     * @throws Exception
     */
    public byte[] encode(final byte[] current, final byte[] previous, final int width, final int height) throws Exception
    {
        ByteArrayOutputStream baos = new ByteArrayOutputStream(16 * 1024);
        
        // write header
        final int wh = width + ((blockWidth / 16 - 1) << 12);
        final int hh = height + ((blockHeight / 16 - 1) << 12);
        
        writeShort(baos, wh);
        writeShort(baos, hh);
        
        // write content
        int y0 = height;
        int x0 = 0;
        int bwidth = blockWidth;
        int bheight = blockHeight;
        
        while (y0 > 0)
        {
            bheight = Math.min(y0, blockHeight);
            y0 -= bheight;
            
            bwidth = blockWidth;
            x0 = 0;
            
            while (x0 < width)
            {
                bwidth = (x0 + blockWidth > width) ? width - x0 : blockWidth;
                
                final boolean changed = isChanged(current, previous, x0, y0, bwidth, bheight, width, height);
                
                if (changed)
                {
                    ByteArrayOutputStream blaos = new ByteArrayOutputStream(4 * 1024);
                    
                    DeflaterOutputStream dos = new DeflaterOutputStream(blaos, deflater);
                    
                    for (int y = 0; y < bheight; y++)
                    {
                        dos.write(current, 3 * ((y0 + bheight - y - 1) * width + x0), 3 * bwidth);
                    }
                    
                    dos.finish();
                    deflater.reset();
                    
                    final byte[] bbuf = blaos.toByteArray();
                    final int written = bbuf.length;
                    
                    // write DataSize
                    writeShort(baos, written);
                    // write Data
                    baos.write(bbuf, 0, written);
                }
                else
                {
                    // write DataSize
                    writeShort(baos, 0);
                }
                
                x0 += bwidth;
            }
        }
        
        return baos.toByteArray();
    }
    
    /**
     * Writes short value to the {@link OutputStream <tt>os</tt>}.
     * 
     * @param os
     * @param n
     * @throws Exception if an exception occurred
     */
    private void writeShort(OutputStream os, final int n) throws Exception
    {
        os.write((n >> 8) & 0xFF);
        os.write((n >> 0) & 0xFF);
    }
    
    /**
     * Checks if image block is changed.
     * 
     * @param current
     * @param previous
     * @param x0
     * @param y0
     * @param blockWidth
     * @param blockHeight
     * @param width
     * @param height
     * @return <code>true</code> if changed, otherwise <code>false</code>
     */
    public boolean isChanged(final byte[] current, final byte[] previous, final int x0, final int y0, final int blockWidth, final int blockHeight, final int width, final int height)
    {
        if (previous == null) return true;
        
        for (int y = y0, ny = y0 + blockHeight; y < ny; y++)
        {
            final int foff = 3 * (x0 + width * y);
            final int poff = 3 * (x0 + width * y);
            
            for (int i = 0, ni = 3 * blockWidth; i < ni; i++)
            {
                if (current[foff + i] != previous[poff + i]) return true;
            }
        }
        
        return false;
    }

    /**
     * @param frame
     * @param codec
     * @return tag
     */
    public int getTag(final int frame, final int codec)
    {
        return ((frame & 0x0F) << 4) + ((codec & 0x0F) << 0);
    }

}
