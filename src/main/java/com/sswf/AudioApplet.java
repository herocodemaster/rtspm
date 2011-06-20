package com.sswf;

/**
 *This is a simple Sound Recorder program written in java.
 *@author Tapas kumar jena
 *@mail tapas.friends@gmail.com
 */

import java.awt.FileDialog;
import java.awt.Frame;
import java.awt.Robot;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioFormat.Encoding;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.BooleanControl;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.DataLine.Info;
import javax.sound.sampled.FloatControl;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.Mixer;
import javax.sound.sampled.Port;
import javax.sound.sampled.SourceDataLine;
import javax.sound.sampled.TargetDataLine;
import javax.swing.JApplet;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JSlider;
import javax.swing.JTextField;
import javax.swing.JToggleButton;
import javax.swing.Timer;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.jma.encoder.IEncoder;
import org.red5.server.stream.codec.AudioCodec;
import org.red5.server.stream.codec.VideoCodec;

import com.sswf.io.encoder.Encoder;
import com.sswf.rtmp.ClientManager;

//Main class
public class AudioApplet extends JApplet implements ActionListener,	ChangeListener{

	private static final long serialVersionUID = 1L;
	// Global declarations
	protected boolean running;
	ByteArrayOutputStream out = null;
	AudioFileFormat.Type fileType;
	Object lock = new Object();
	TargetDataLine line = null;
	SourceDataLine sline = null;
	volatile boolean paused = false;
	boolean first;

	JButton publish;
	JButton play;
	JButton pause;
	JButton stop;
	JButton send;
	JButton save;

	JTextField streamName;
	JTextField statustxt;

	JSlider progress;
	JLabel time;
	Timer timer;
	int audioLength;
	int audioPosition = 0;
	JLabel vol1 = null;
	JLabel vol2 = null;
	JSlider volslider = null;
	JToggleButton mute = null;
	FloatControl volCtrl = null;
	Port lineIn = null;
	String list[];
	volatile String msg;
	private Info info;
	private Mixer mixer;
	private AudioFormat sourceFormat;
	private HashMap<String, Mixer> mixerName;
	private static final String userHome = System.getProperty("user.home");
	private static final File configFile = new File(userHome
			+ (userHome.lastIndexOf("/") != -1 ? "/" : "\\")
			+ "conf.properties");;
	ClientManager clientManager = new ClientManager();
	private URL serverUrl;

	public void init() {
		setLayout(null);
		JLabel recorder = new JLabel("Publisher");
		JLabel streamNameLabel = new JLabel("Server URL");
		JLabel status = new JLabel("Status...");

		streamName = new JTextField("rtmp://localhost:1935/live");
		statustxt = new JTextField("Check your status here...");

		publish = new JButton("Publish");
		play = new JButton("Play");
		pause = new JButton("Pause");
		stop = new JButton("Stop");
		send = new JButton("...");
		save = new JButton("Save");

		progress = new JSlider(0, audioLength, 0);
		time = new JLabel("0:00");
		mute = new JToggleButton("Mute");
		vol1 = new JLabel("Volume  -");
		vol2 = new JLabel("+");
		volslider = new JSlider(0, 100);
		volslider.setToolTipText("Volume");
		volslider.setPaintTicks(true);
		volslider.setMinorTickSpacing(10);

		recorder.setBounds(10, 10, 70, 25);
		publish.setBounds(70, 10, 80, 25);
		play.setBounds(155, 10, 80, 25);
		pause.setBounds(240, 10, 80, 25);
		stop.setBounds(325, 10, 80, 25);
		streamNameLabel.setBounds(10, 40, 130, 25);
		streamName.setBounds(80, 40, 240, 25);
		send.setBounds(325, 40, 80, 25);
		status.setBounds(10, 100, 70, 25);
		statustxt.setBounds(100, 100, 222, 25);
		save.setBounds(325, 100, 80, 25);

		progress.setBounds(50, 140, 300, 20);
		time.setBounds(360, 140, 30, 20);
		vol1.setBounds(75, 170, 100, 20);
		volslider.setBounds(130, 180, 150, 20);
		vol2.setBounds(280, 172, 30, 20);
		mute.setBounds(330, 170, 65, 30);

		add(recorder);
		add(publish);
		add(play);
		add(pause);
		add(stop);
		add(save);

		add(streamNameLabel);
		add(streamName);
		add(send);
		add(status);
		add(statustxt);

		add(progress);
		add(time);
		add(vol1);
		add(volslider);
		add(vol2);
		add(mute);

		publish.setEnabled(true);
		pause.setEnabled(true);
		play.setEnabled(true);
		stop.setEnabled(true);
		save.setEnabled(true);
		send.setEnabled(true);

		publish.addActionListener(this);
		play.addActionListener(this);
		pause.addActionListener(this);
		stop.addActionListener(this);
		save.addActionListener(this);
		send.addActionListener(this);
		mute.addActionListener(this);
		progress.addChangeListener(this);
		volslider.addChangeListener(this);
		doesDefaultInputDeviceExist();
		clientManager.setMode(ClientManager.NETONLY);
		clientManager.setRunning(true);
		Thread cmThread = new Thread(clientManager);
		try {
			serverUrl = new URL(streamName.getText().replace("rtmp", "http"));
			clientManager.setServerUrl(serverUrl);
		} catch (MalformedURLException e) {
			e.printStackTrace();
		}
		cmThread.start();
	}// End of init method

	private void getAvailableInputDevice() {
		getFormat();
		info = new DataLine.Info(TargetDataLine.class, sourceFormat);
		System.out
				.println("Searching a mixer that is supported by the Line...");
		mixerName = new HashMap<String, Mixer>();
		for (Mixer.Info mixerInfo : AudioSystem.getMixerInfo()) {
			mixer = AudioSystem.getMixer(mixerInfo);
			System.out.println("Available Mixer:" + mixer.getMixerInfo());
			if (mixer.isLineSupported(info)) {
				System.out.println("Found a Mixer: " + mixer.getMixerInfo());
				mixerName.put(mixer.getMixerInfo().getName(), mixer);
			}
		}
	}

	private void chooseDefaultInputDevice() throws IOException {
		if (mixerName.size() == 0) {
			System.err.println("No suitable mixers found!");
			JOptionPane.showMessageDialog(this, "No suitable mixers found!");
			return;
		} else {
			Object[] mixerList = mixerName.keySet().toArray();
			String stringMixerName = (String) JOptionPane.showInputDialog(this,
					"Available devices:\n",
					"Choose your Sound Card Input Device",
					JOptionPane.PLAIN_MESSAGE, null, mixerList,
					mixer == null ? "" : mixer.getMixerInfo().getName());
			if (stringMixerName == null)
				return;
			System.out
					.println("Mixer " + stringMixerName + " has been chousen");
			mixer = mixerName.get(stringMixerName);

		}

		FileOutputStream fstream = new FileOutputStream(configFile);
		DataOutputStream out = new DataOutputStream(fstream);
		BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(out));
		bw.write(mixer.getMixerInfo().getName());
		bw.close();
		out.close();
		fstream.close();

	}

	private void doesDefaultInputDeviceExist() {

		getAvailableInputDevice();
		if (!configFile.exists()) {
			try {
				configFile.createNewFile();
				chooseDefaultInputDevice();

			} catch (IOException e) {
				e.printStackTrace();
			}
		} else {
			try {
				FileInputStream fstream = new FileInputStream(configFile);
				DataInputStream in = new DataInputStream(fstream);
				BufferedReader br = new BufferedReader(
						new InputStreamReader(in));
				String strMixer = br.readLine();
				if (strMixer == null) {
					br.close();
					in.close();
					fstream.close();
					chooseDefaultInputDevice();
					fstream = new FileInputStream(configFile);
					in = new DataInputStream(fstream);
					br = new BufferedReader(new InputStreamReader(in));
					mixer = mixerName.get(br.readLine().toString());
				} else {
					mixer = mixerName.get(strMixer);
				}

				br.close();
				in.close();
				fstream.close();
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	// ***************************************************/
	// ******* StateChanged method for ChangeListener*****/
	// ***************************************************/

	public void stateChanged(ChangeEvent e) {
		if (e.getSource() == volslider) {
			volumeControl();
		} else {
			int value = progress.getValue();
			time.setText(value / 1000 + "." + (value % 1000) / 100);
		}
	}


	// ***************************************************/
	// ***** ActionPerformed method for ActionListener****/
	// ***************************************************/

	public void actionPerformed(ActionEvent e) {
		if (e.getSource() == publish) {
			msg = "  Capturing audio .....";
			statustxt.setText(msg);
			publish.setEnabled(false);
			pause.setEnabled(true);
			stop.setEnabled(true);
			play.setEnabled(false);
			save.setEnabled(true);
			if (paused) {
				resumeRecord();
			} else {
				publish();
			}
		} else if (e.getSource() == play) {
			msg = "  Playing recorded audio.....";
			statustxt.setText(msg);
			stop.setEnabled(true);
			if (first) {
				playAudio();
			} else {
				resumePlay();
			}
		} else if (e.getSource() == pause) {
			msg = "Paused....";
			statustxt.setText(msg);
			publish.setEnabled(true);
			pause.setEnabled(true);
			pauseAudio();
			first = false;
		} else if (e.getSource() == stop) {
			msg = "  Action stopped by user.....";
			statustxt.setText(msg);
			progress.setValue(0);
			publish.setEnabled(true);
			stop.setEnabled(false);
			play.setEnabled(true);
			running = false;
			stopAudio();

		} else if (e.getSource() == save) {
			msg = "  Saving file to user's System....";
			statustxt.setText(msg);
			saveAudio();
		} else if (e.getSource() == send) {
			msg = "  Changing input line...";
			statustxt.setText(msg);
			try {
				chooseDefaultInputDevice();
			} catch (IOException e1) {
				e1.printStackTrace();
			}
			// uploadAudio();
		} else {
			muteControl();
		}
	}

	// ******************************************/
	// ************** Method Declarations ****/
	// ******************************************/

	private void publish() {
		first = true;
		try {
			if (!AudioSystem.isLineSupported(info)) {
				System.err.println("Line is not supported by the Audio System");
				JOptionPane.showMessageDialog(this,
						"Line is not supported by the Audio System");
				return;
			}
			serverUrl = new URL(streamName.getText().replace("rtmp", "http"));
			clientManager.setServerUrl(serverUrl);
			line = (TargetDataLine) mixer.getLine(info);
			line.open(sourceFormat);
			line.start();
			IEncoder encoder = new Encoder(); 
			encoder.add(AudioCodec.MP3, sourceFormat);
			encoder.add(VideoCodec.SCREEN_VIDEO, null);
			clientManager.setEncoder(encoder);
			clientManager.setRecording(true);
			CaptureAudio captureAudio = new CaptureAudio(clientManager, line);
			CaptureVideo captureVideo = new CaptureVideo(clientManager, new Robot());
			Thread publishAudio = new Thread(captureAudio);
			publishAudio.start();
			Thread publishVideo = new Thread(captureVideo);
			publishVideo.start();
			
		} catch (LineUnavailableException e) {
			System.err.println("Line Unavailable:" + e);
			e.printStackTrace();
			System.exit(-2);
		} catch (Exception e) {
			System.out.println("Direct Upload Error");
			e.printStackTrace();
		}
	}// End of RecordAudio method

	private void playAudio() {
		try {
			byte audio[] = out.toByteArray();
			InputStream input = new ByteArrayInputStream(audio);
			final AudioInputStream ais = new AudioInputStream(input, sourceFormat,
					audio.length / sourceFormat.getFrameSize());
			DataLine.Info info = new DataLine.Info(SourceDataLine.class, sourceFormat);
			sline = (SourceDataLine) AudioSystem.getLine(info);
			sline.open(sourceFormat);
			sline.start();

			Runnable runner = new Runnable() {
				int bufferSize = (int) sourceFormat.getSampleRate()
						* sourceFormat.getFrameSize();
				byte buffer[] = new byte[bufferSize];

				public void run() {
					try {
						int count;
						synchronized (lock) {
							while ((count = ais.read(buffer, 0, buffer.length)) != -1) {
								while (paused) {
									if (sline.isRunning()) {
										sline.stop();
									}
									try {
										lock.wait();
									} catch (InterruptedException e) {
									}
								}
								if (!sline.isRunning()) {
									sline.start();
								}
								if (count > 0) {
									sline.write(buffer, 0, count);
								}
							}
						}
						first = true;
						sline.drain();
						sline.close();
					} catch (IOException e) {
						System.err.println("I/O problems:" + e);
						System.exit(-3);
					}
				}
			};

			Thread playThread = new Thread(runner);
			playThread.start();
		} catch (LineUnavailableException e) {
			System.exit(-4);
		}
	}// End of PlayAudio method

	private void resumeRecord() {
		synchronized (lock) {
			paused = false;
			lock.notifyAll();
			first = true;
		}
	}// End of ResumeRecord method

	private void stopAudio() {
		if (sline != null) {
			sline.stop();
			sline.close();
		} else {
			line.stop();
			line.close();
		}
		clientManager.setRecording(false);
	}// End of StopAudio method

	private void resumePlay() {
		synchronized (lock) {
			paused = false;
			lock.notifyAll();
			System.out.println("inside resumeplay method");
		}
	}// End of ResumePlay method

	private void pauseAudio() {
		paused = true;
	}

	private void saveAudio() {
		Thread thread = new saveBytesThread();
		thread.start();
	}

	/*private void uploadAudio() {
		Thread th = new uploadThread();
		th.start();
	}*/

	private void getFormat() {
		Encoding encoding = AudioFormat.Encoding.PCM_SIGNED;
		float sampleRate = 44100.0F;
		int sampleSizeInBits = 16;
		int channels = 2;
		int frameSize = sampleSizeInBits / 8 * channels;
		float frameRate = 44100.0F;
		boolean bigEndian = false;
		sourceFormat = new AudioFormat(encoding, sampleRate, sampleSizeInBits,
				channels, frameSize, frameRate, bigEndian);
	}// End of getAudioFormat method

	class saveThread extends Thread {
		public void run() {
			AudioFileFormat.Type fileType = AudioFileFormat.Type.WAVE;
			FileDialog fd = new FileDialog(new Frame(), "Save as WAVE",
					FileDialog.SAVE);
			fd.setFile("*.wav");
			fd.setVisible(true);
			String name = fd.getDirectory() + fd.getFile();
			File file = new File(name);

			try {
				byte audio[] = out.toByteArray();
				InputStream input = new ByteArrayInputStream(audio);
				// final AudioFormat format = getFormat();
				final AudioInputStream ais = new AudioInputStream(input,
						sourceFormat, audio.length / sourceFormat.getFrameSize());
				AudioSystem.write(ais, fileType, file);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}// End of inner class saveThread

	class saveBytesThread extends Thread {
		public void run() {
			FileDialog fd = new FileDialog(new Frame(), "Save as mp3",
					FileDialog.SAVE);
			fd.setFile("*.mp3");
			fd.setVisible(true);

			String name = fd.getDirectory() + fd.getFile();
			File file = new File(name);
			try {
				OutputStream outputStream = new FileOutputStream(file);
				out.writeTo(outputStream);
				outputStream.flush();
				outputStream.close();
			} catch (FileNotFoundException e1) {
				e1.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}

		}
	}// End of inner class saveThread

	public void volumeControl() {
		try {
			if (AudioSystem.isLineSupported(Port.Info.LINE_OUT)) {
				lineIn = (Port) AudioSystem.getLine(Port.Info.LINE_OUT);
				lineIn.open();
			} else if (AudioSystem.isLineSupported(Port.Info.HEADPHONE)) {
				lineIn = (Port) AudioSystem.getLine(Port.Info.HEADPHONE);
				lineIn.open();
			} else if (AudioSystem.isLineSupported(Port.Info.SPEAKER)) {
				lineIn = (Port) AudioSystem.getLine(Port.Info.SPEAKER);
				lineIn.open();
			} else {
				System.out.println("Unable to get Output Port");
				return;
			}
			final FloatControl controlIn = (FloatControl) lineIn
					.getControl(FloatControl.Type.VOLUME);
			final float volume = 100 * (controlIn.getValue() / controlIn
					.getMaximum());
			System.out.println(volume);
			int sliderValue = volslider.getValue();
			controlIn.setValue((float) sliderValue / 100);

		} catch (Exception e) {
			System.out.println(" VOLUME control: exception = " + e);
		}
	}// End of volumeControl method

	private void muteControl() {
		BooleanControl mControl;
		try {
			if (sline!=null){
				mControl = (BooleanControl) sline.getControl(BooleanControl.Type.MUTE);
				if (mControl.getValue() == true) {
					mControl.setValue(false);
				} else {
					mControl.setValue(true);
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}// End of main class AudioBroadcast
