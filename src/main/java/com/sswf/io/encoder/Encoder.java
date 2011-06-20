package com.sswf.io.encoder;

import javax.sound.sampled.AudioFormat;

import org.jma.ChannelType;
import org.jma.MetaData;
import org.jma.RateType;
import org.jma.encoder.IEncoder;
import org.jma.encoder.audio.IAudioEncoder;
import org.jma.encoder.video.IVideoEncoder;
import org.jma.video.VideoFormat;
import org.red5.server.stream.codec.AudioCodec;
import org.red5.server.stream.codec.VideoCodec;
import org.tritonus.lowlevel.lame.LameImp;

public class Encoder implements IEncoder {

	public Encoder() {
		this.metaData = new MetaData();
	}

	private IAudioEncoder audioEncoder;
	private IVideoEncoder videoEncoder;
	private MetaData metaData;
	public void add(AudioCodec targetAudioCodec, AudioFormat sourceFormat) {
		metaData.setAudioCodec(targetAudioCodec);
		metaData.setChannelType(ChannelType.lookup(sourceFormat.getChannels()));
		metaData.setRateType(RateType.lookup((int) sourceFormat.getSampleRate()));
		switch (targetAudioCodec) {
		case MP3:
			audioEncoder = new LameImp(sourceFormat);
			break;

		default:
			break;
		}

	}

	public void add(VideoCodec targetVideoCodec, VideoFormat format) {
		metaData.setVideoCodec(targetVideoCodec);
		switch (targetVideoCodec) {
		case SCREEN_VIDEO:
			videoEncoder = new ScreenVideo();
			break;

		default:
			break;
		}

	}

	public IAudioEncoder getAudioEncoder() {
		return audioEncoder;
	}
	
	public IVideoEncoder getVideoEncoder() {
		return videoEncoder;
	}

	public MetaData getMetaData() {
		return metaData;
	}
}
