package view;

import java.awt.Button;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.TargetDataLine;
import javax.sound.sampled.UnsupportedAudioFileException;
import javax.swing.JFrame;
import javax.swing.JTextField;

import model.Complex;
import model.DataPoint;
import model.FFT;

import org.tritonus.sampled.convert.PCM2PCMConversionProvider;

public class AudioRecognizerWindow extends JFrame {

	public final int UPPER_LIMIT = 300;
	public final int LOWER_LIMIT = 40;
	public final int[] RANGE = new int[] { 40, 80, 120, 180, UPPER_LIMIT + 1 };
	private static final int FUZ_FACTOR = 2;

	
	private static final long serialVersionUID = -4446576755887058104L;
	boolean running = false;
	double highscores[][];
	double recordPoints[][];
	long points[][];
	Map<Long, List<DataPoint>> hashMap;
	 Map<Integer, Map<Integer, Integer>> matchMap; 
	long nrSong = 0;
	JTextField fileTextField = null;
	JTextField fileTextField2 = null;

	private AudioFormat getFormat() {
		float sampleRate = 44100;
		int sampleSizeInBits = 8;
		int channels = 1; // mono
		boolean signed = true;
		boolean bigEndian = true;
		return new AudioFormat(sampleRate, sampleSizeInBits, channels, signed,
				bigEndian);
	}
	private void listenSound(long songId, boolean isMatching)
			throws LineUnavailableException, IOException,
			UnsupportedAudioFileException {

		AudioFormat formatTmp = null;
		AudioFormat formatTmp2 = null;
		TargetDataLine lineTmp = null;
		TargetDataLine lineTmp2 = null;
		String filePath = fileTextField.getText();
		String filePath2 = fileTextField2.getText();
		AudioInputStream din = null;
		AudioInputStream din2 = null;
		AudioInputStream outDin = null;
		AudioInputStream outDin2 = null;
		PCM2PCMConversionProvider conversionProvider = new PCM2PCMConversionProvider();
		boolean isMicrophone = false;

		if (filePath == null || filePath.equals("") || isMatching) {

			formatTmp = getFormat(); // Fill AudioFormat with the wanted
			formatTmp2 = getFormat();							// settings
			DataLine.Info info = new DataLine.Info(TargetDataLine.class,
					formatTmp);
			lineTmp = (TargetDataLine) AudioSystem.getLine(info);
			DataLine.Info info2= new DataLine.Info(TargetDataLine.class,
					formatTmp2);
			lineTmp2 = (TargetDataLine) AudioSystem.getLine(info2);
			isMicrophone = true;
		} else {
			AudioInputStream in;
			AudioInputStream in2;
			if (filePath.contains("http")) {
				URL url = new URL(filePath);
				in = AudioSystem.getAudioInputStream(url);
			} else {
				File file = new File(filePath);
				in = AudioSystem.getAudioInputStream(file);
			}

			if (filePath2.contains("http")) {
				URL url = new URL(filePath2);
				in2 = AudioSystem.getAudioInputStream(url);
			}else{
				File file2 = new File(filePath2);
				in2 = AudioSystem.getAudioInputStream(file2);
			}
			AudioFormat baseFormat = in.getFormat();
			AudioFormat baseFormat2= in2.getFormat();
			System.out.println(baseFormat.toString());
			System.out.println(baseFormat2.toString());
			AudioFormat decodedFormat = new AudioFormat(
					AudioFormat.Encoding.PCM_SIGNED,
					baseFormat.getSampleRate(), 16, baseFormat.getChannels(),
					baseFormat.getChannels() * 2, baseFormat.getSampleRate(),
					false);
			AudioFormat decodedFormat2 = new AudioFormat(
					AudioFormat.Encoding.PCM_SIGNED,
					baseFormat.getSampleRate(), 16, baseFormat2.getChannels(),
					baseFormat.getChannels() * 2, baseFormat2.getSampleRate(),
					false);
			din2 = AudioSystem.getAudioInputStream(decodedFormat2, in);
			din = AudioSystem.getAudioInputStream(decodedFormat, in);

			if (!conversionProvider.isConversionSupported(getFormat(),
					decodedFormat)) {
				System.out.println("Conversion is not supported");
			}
			if (!conversionProvider.isConversionSupported(getFormat(),
					decodedFormat2)) {
				System.out.println("Conversion is not supported");
			}

			System.out.println(decodedFormat.toString());
			System.out.println(decodedFormat2.toString());
			outDin = conversionProvider.getAudioInputStream(getFormat(), din);
			outDin2 = conversionProvider.getAudioInputStream(getFormat(), din2);
			formatTmp = decodedFormat;
			formatTmp2 = decodedFormat2;
			DataLine.Info info = new DataLine.Info(TargetDataLine.class,
					formatTmp);
			DataLine.Info info2 = new DataLine.Info(TargetDataLine.class, formatTmp2);
			lineTmp2 = (TargetDataLine) AudioSystem.getLine(info2);
			lineTmp = (TargetDataLine) AudioSystem.getLine(info);
		}

		final AudioFormat format = formatTmp;
		final AudioFormat format2 = formatTmp2;
		final TargetDataLine line2= lineTmp2;
		final TargetDataLine line = lineTmp;
		final boolean isMicro = false;//isMicrophone;
		final AudioInputStream outDinSound = outDin;
		final AudioInputStream outDinSound2 = outDin2;

		if (isMicro) {
			try {
				line.open(format);
				line.start();
			} catch (LineUnavailableException e) {
				e.printStackTrace();
			}
		}

		final long sId = songId;
		final boolean isMatch = isMatching;

		Thread listeningThread = new Thread(new Runnable() {
			public void run() {
				ByteArrayOutputStream out = new ByteArrayOutputStream();
				running = true;
				int n = 0;
				byte[] buffer = new byte[(int) 1024];
				try {
					while (running) {
						n++;
						if (n > 1000)
							break;
						int count = 0;
						if (isMicro) {
							count = line.read(buffer, 0, 1024);
						} else {
							count = outDinSound.read(buffer, 0, 1024);
						}
						if (count > 0) {
							out.write(buffer, 0, count);
						}
					}

					byte b[] = out.toByteArray();
					for (int i = 0; i < b.length; i++) {
						System.out.println(b[i]);
					}

					try {
						makeSpectrum(out, sId, isMatch);

						FileWriter fstream = new FileWriter("out.txt");
						BufferedWriter outFile = new BufferedWriter(fstream);

						for (int i = 0; i < b.length; i++) {
							outFile.write("" + b[i] + ";");
						}
						outFile.close();

					} catch (Exception e) {
						System.err.println("Error: " + e.getMessage());
					}

					out.close();
					line.close();
				} catch (IOException e) {
					System.err.println("I/O problems: " + e);
					System.exit(-1);
				}

			}

		});

		listeningThread.start();
	}

	void makeSpectrum(ByteArrayOutputStream out, long songId, boolean isMatching) {
		byte audio[] = out.toByteArray();

		final int totalSize = audio.length;

		int amountPossible = totalSize / 4096;

		// When turning into frequency domain we'll need complex numbers:
		Complex[][] results = new Complex[amountPossible][];

		// For all the chunks:
		for (int times = 0; times < amountPossible; times++) {
			Complex[] complex = new Complex[4096];
			for (int i = 0; i < 4096; i++) {
				// Put the time domain data into a complex number with imaginary
				// part as 0:
				complex[i] = new Complex(audio[(times * 4096) + i], 0);
			}
			// Perform FFT analysis on the chunk:
			results[times] = FFT.fft(complex);
		}
		determineKeyPoints(results, songId, isMatching);
		JFrame spectrumView = new SpectrumView(results, 4096, highscores,
				recordPoints);
		spectrumView.setVisible(true);
	}



	// Find out in which range
	public int getIndex(int freq) {
		int i = 0;
		while (RANGE[i] < freq)
			i++;
		return i;
	}

	void determineKeyPoints(Complex[][] results, long songId, boolean isMatching) {
		this.matchMap = new HashMap<Integer, Map<Integer, Integer>>();

		FileWriter fstream = null;
		try {
			fstream = new FileWriter("result.txt");
		} catch (IOException e1) {
			e1.printStackTrace();
		}
		BufferedWriter outFile = new BufferedWriter(fstream);

		highscores = new double[results.length][5];
		for (int i = 0; i < results.length; i++) {
			for (int j = 0; j < 5; j++) {
				highscores[i][j] = 0;
			}
		}

		recordPoints = new double[results.length][UPPER_LIMIT];
		for (int i = 0; i < results.length; i++) {
			for (int j = 0; j < UPPER_LIMIT; j++) {
				recordPoints[i][j] = 0;
			}
		}

		points = new long[results.length][5];
		for (int i = 0; i < results.length; i++) {
			for (int j = 0; j < 5; j++) {
				points[i][j] = 0;
			}
		}

		for (int t = 0; t < results.length; t++) {
			for (int freq = LOWER_LIMIT; freq < UPPER_LIMIT - 1; freq++) {
				// Get the magnitude:
				double mag = Math.log(results[t][freq].abs() + 1);

				// Find out which range we are in:
				int index = getIndex(freq);

				// Save the highest magnitude and corresponding frequency:
				if (mag > highscores[t][index]) {
					highscores[t][index] = mag;
					recordPoints[t][freq] = 1;
					points[t][index] = freq;
				}
			}

			try {
				for (int k = 0; k < 5; k++) {
					outFile.write("" + highscores[t][k] + ";"
							+ recordPoints[t][k] + "\t");
				}
				outFile.write("\n");

			} catch (IOException e) {
				e.printStackTrace();
			}

			long h = hash(points[t][0], points[t][1], points[t][2],
					points[t][3]);

			if (isMatching) {
				List<DataPoint> listPoints;

				if ((listPoints = hashMap.get(h)) != null) {
					for (DataPoint dP : listPoints) {
						int offset = Math.abs(dP.getTime() - t);
						Map<Integer, Integer> tmpMap = null;
						if ((tmpMap = this.matchMap.get(dP.getSongId())) == null) {
							tmpMap = new HashMap<Integer, Integer>();
							tmpMap.put(offset, 1);
							matchMap.put(dP.getSongId(), tmpMap);
						} else {
							Integer count = tmpMap.get(offset);
							if (count == null) {
								tmpMap.put(offset, new Integer(1));
							} else {
								tmpMap.put(offset, new Integer(count + 1));
							}
						}
					}
				}
			} else {
				List<DataPoint> listPoints = null;
				if ((listPoints = hashMap.get(h)) == null) {
					listPoints = new ArrayList<DataPoint>();
					DataPoint point = new DataPoint((int) songId, t);
					listPoints.add(point);
					hashMap.put(h, listPoints);
				} else {
					DataPoint point = new DataPoint((int) songId, t);
					listPoints.add(point);
				}
			}
		}
		try {
			outFile.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	AudioRecognizerWindow(String windowName) {
		this.setTitle(windowName);
	}


	private long hash(long p1, long p2, long p3, long p4) {
		return (p4 - (p4 % FUZ_FACTOR)) * 100000000 + (p3 - (p3 % FUZ_FACTOR))
				* 100000 + (p2 - (p2 % FUZ_FACTOR)) * 100
				+ (p1 - (p1 % FUZ_FACTOR));
	}

	public void createWindow() {

		this.hashMap = new HashMap<Long, List<DataPoint>>();
		this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		Button buttonStart = new Button("Start");
		Button buttonStop = new Button("Stop");
		Button buttonMatch = new Button("Match");
		Button buttonStartMatch = new Button("Start Match");
		Button buttonStopMatch = new Button("Stop Match");
		fileTextField = new JTextField(20);
		fileTextField.setText("/Users/juandavidhenao/a.mp3");
		fileTextField2 = new JTextField(30);
		fileTextField2.setText("/Users/juandavidhenao/a.mp3");
		buttonStart.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				try {
					try {
						listenSound(nrSong, false);
					} catch (IOException e1) {
						e1.printStackTrace();
					} catch (UnsupportedAudioFileException e1) {
						e1.printStackTrace();
					}
					nrSong++;
				} catch (LineUnavailableException ex) {
					ex.printStackTrace();
				}
			}
		});

		buttonStop.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				running = false;
			}
		});

		buttonStartMatch.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				try {
					try {
						listenSound(nrSong, true);
					} catch (IOException e1) {
						e1.printStackTrace();
					} catch (UnsupportedAudioFileException e1) {
						e1.printStackTrace();
					}
				} catch (LineUnavailableException ex) {
					ex.printStackTrace();
				}
			}
		});

		buttonStopMatch.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				running = false;
			}
		});

		buttonMatch.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				int bestCount = 0;
				int bestSong = -1;

				for (int id = 0; id < nrSong; id++) {

					try {
						System.out.println("For song id: " + id);
						Map<Integer, Integer> tmpMap = matchMap.get(id);
						int bestCountForSong = 0;

						for (Map.Entry<Integer, Integer> entry : tmpMap.entrySet()) {
							if (entry.getValue() > bestCountForSong) {
								bestCountForSong = entry.getValue();
							}
							System.out.println("Time offset = " + entry.getKey()
									+ ", Count = " + entry.getValue());
						}
						if (bestCountForSong > bestCount) {
							bestCount = bestCountForSong;
							bestSong = id;
						}
					} catch (NullPointerException exception) {
						exception.printStackTrace();
					}
				}

				System.out.println("Best song id: " + bestSong);
			}
		});

		this.add(buttonStart);
		this.add(buttonStop);
		this.add(buttonStartMatch);
		this.add(buttonStopMatch);
		this.add(buttonMatch);
		this.add(fileTextField);
		this.add(fileTextField2);
		this.setLayout(new FlowLayout());
		this.setSize(300, 300);
		this.setLocationRelativeTo(null);
		this.setVisible(true);
	}
}
