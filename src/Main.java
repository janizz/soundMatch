import java.awt.Graphics;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.SourceDataLine;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.TargetDataLine;

public class Main {

	static List<String> songs = new ArrayList<String>();
	static Map<Long, List<DataPoint>> HashDB = new HashMap<Long, List<DataPoint>>();
	static Map<Long, List<DataPoint>> HashPrim = new HashMap<Long, List<DataPoint>>();

	final static int CHUNK_SIZE = 1024;
	static byte buffer[];
	static byte audio[];
	static Graphics g2d;
	static Complex[][] results;
	static int cas = 0;
	static boolean stejCas = false;
	static int blockSizeY = 1, blockSizeX = 1;
	private static final int LOWER_LIMIT = 40;
	private static final int UPPER_LIMIT = 200;
	public static final int[] RANGE = new int[] { 40, 80, 120, 180,
			UPPER_LIMIT + 1 };
	private static final int AMOUNT_OF_POINTS = 4;
	static int totalFramesRead;
	// static String somePathName = "Avicii - Silhouettes.mp3";
	static String[] files = { "Pitbull - Don't Stop The Party ft. TJR.mp3",
			"Avicii - Silhouettes.mp3", "RITA ORA - Shine Ya Light.mp3" };
	// static String somePathName = "whatYoureSmoking.wav";
	static String somePathName = "RITA ORA -  Shine Your Light (Official Video)-cutmp3.net.mp3";
	// Mp3Encoder mp3encoder = new Mp3Encoder();

	static File fileIn = new File(somePathName);
	static PrintStream fw;

	static boolean writeToFile = false;
	static boolean draw = false;

	private static AudioFormat getFormat() {
		float sampleRate = 44100;
		int sampleSizeInBits = 8;
		int channels = 1; // mono
		boolean signed = true;
		boolean bigEndian = true;
		return new AudioFormat(sampleRate, sampleSizeInBits, channels, signed,
				bigEndian);
	}

	public static void main(String args[]) {

		// Time matching process
		long startTimeProgram = System.nanoTime();

		// Do it for @files
		for (String file : files) {
			songs.add(file);
		}

		MinimumSize main = null;
		if (draw) {
			// Start drawing window
			main = new MinimumSize();
			main.display();
		}

		// Initialize audio capturing
		final AudioFormat format = getFormat(); // Fill AudioFormat with the
												// wanted settings
		DataLine.Info info = new DataLine.Info(TargetDataLine.class, format);
		TargetDataLine line = null;
		try {
			line = (TargetDataLine) AudioSystem.getLine(info);
			line.open(format);
		} catch (LineUnavailableException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		// If no errors, start recording line
		line.start();

		if (writeToFile) {
			// Open file for writing
			try {
				fw = new PrintStream("audioPoints");
			} catch (FileNotFoundException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
		}

		// In another thread I start:
		OutputStream out = new ByteArrayOutputStream();
		boolean running = true;
		buffer = new byte[line.getBufferSize() / 5];

		byte[] audioBytes = null;

		// Play sound function managed by a thread
		// playSound(somePathName);

		// Defs for audio playing (not working at the time of writting this)
		SourceDataLine linen = null;
		try {
			linen = AudioSystem.getSourceDataLine(getFormat());
			linen.open();
		} catch (LineUnavailableException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}

		// Do it for @files
		for (String song : songs) {

			// Read input audio file (mp3)
			try {
				// AudioInputStream audioInputStream =
				// Mp3Encoder.getInStream(somePathName);
				AudioInputStream audioInputStream = AudioSystem
						.getAudioInputStream(new File(song));
				int bytesPerFrame = audioInputStream.getFormat().getFrameSize();
				System.out.println("bytesPerFrame: " + bytesPerFrame);
				if (bytesPerFrame == AudioSystem.NOT_SPECIFIED) {
					// some audio formats may have unspecified frame size
					// in that case we may read any amount of bytes
					bytesPerFrame = 1;
					System.out
							.println("AudioSystem frame size is not specified.");
				}
				// Set an arbitrary buffer size of 1024 frames.
				int numBytes = 1024 * bytesPerFrame;
				audioBytes = new byte[numBytes];
				try {
					int numBytesRead = 0;
					int numFramesRead = 0;
					totalFramesRead = 0;
					results = null;
					cas = 0;
					// Try to read numBytes bytes from the file.
					while ((numBytesRead = audioInputStream.read(audioBytes)) != -1) {
						// Calculate the number of frames actually read.
						numFramesRead = numBytesRead / bytesPerFrame;
						totalFramesRead += numFramesRead;
						// Here, do something useful with the audio data that's
						// now in the audioBytes array...

						// read chunks from a stream and write them to a source
						// data
						linen.write(audioBytes, 0, numBytesRead);

						final int totalSize = audioBytes.length;
						int amountPossible = totalSize / CHUNK_SIZE;
						results = new Complex[amountPossible][];

						for (int times = 0; times < amountPossible; times++) {
							Complex[] complex = new Complex[CHUNK_SIZE];
							for (int i = 0; i < CHUNK_SIZE; i++) {
								// Put the time domain data into a complex
								// number
								// with imaginary
								// part as 0:
								// Write all points to file / test
								// fw.append(audioBytes[(times * CHUNK_SIZE) +
								// i] +
								// " ");
								complex[i] = new Complex(
										audioBytes[(times * CHUNK_SIZE) + i], 0);
							}
							// fw.append("\n");
							// Perform FFT analysis on the chunk:
							results[times] = FFT.fft(complex);
						}
						// Determine Key Points
						// TODO amountPossible may not always be 0
						keyPoints(results[amountPossible - 1], new DataPoint(
								songs.indexOf(song), cas), HashDB);

						// Done!
						// main.cc.paintLine(cas, results);

						cas++;
					}
				} catch (Exception ex) {
					// Handle the error...
					ex.printStackTrace();
				}
			} catch (Exception e) {
				// Handle the error...
				e.printStackTrace();
			}
		}

		// Time matching process
		long startTimeReadMatchSong = System.nanoTime();

		// Read comparing mp3 file and compare / other database
		try {
			// AudioInputStream audioInputStream =
			// Mp3Encoder.getInStream(somePathName);
			AudioInputStream audioInputStream = AudioSystem
					.getAudioInputStream(fileIn);
			int bytesPerFrame = audioInputStream.getFormat().getFrameSize();
			System.out.println("bytesPerFrame: " + bytesPerFrame);
			if (bytesPerFrame == AudioSystem.NOT_SPECIFIED) {
				// some audio formats may have unspecified frame size
				// in that case we may read any amount of bytes
				bytesPerFrame = 1;
				System.out.println("AudioSystem frame size is not specified.");
			}
			// Set an arbitrary buffer size of 1024 frames.
			int numBytes = 1024 * bytesPerFrame;
			audioBytes = new byte[numBytes];
			try {
				int numBytesRead = 0;
				int numFramesRead = 0;
				totalFramesRead = 0;
				results = null;
				cas = 0;
				// Try to read numBytes bytes from the file.
				while ((numBytesRead = audioInputStream.read(audioBytes)) != -1) {
					// Calculate the number of frames actually read.
					numFramesRead = numBytesRead / bytesPerFrame;
					totalFramesRead += numFramesRead;
					// Here, do something useful with the audio data that's
					// now in the audioBytes array...

					// read chunks from a stream and write them to a source data
					linen.write(audioBytes, 0, numBytesRead);

					final int totalSize = audioBytes.length;
					int amountPossible = totalSize / CHUNK_SIZE;
					results = new Complex[amountPossible][];

					for (int times = 0; times < amountPossible; times++) {
						Complex[] complex = new Complex[CHUNK_SIZE];
						for (int i = 0; i < CHUNK_SIZE; i++) {
							// Put the time domain data into a complex number
							// with imaginary
							// part as 0:
							// Write all points to file / test
							// fw.append(audioBytes[(times * CHUNK_SIZE) + i] +
							// " ");
							complex[i] = new Complex(
									audioBytes[(times * CHUNK_SIZE) + i], 0);
						}
						// fw.append("\n");
						// Perform FFT analysis on the chunk:
						results[times] = FFT.fft(complex);
					}
					// Determine Key Points
					// TODO amountPossible may not always be 0
					keyPoints(results[amountPossible - 1],
							new DataPoint(songs.indexOf(somePathName), cas),
							HashPrim);

					// Done!
					if (draw) {
						main.cc.paintLine(cas, results);
					}

					cas++;
				}
			} catch (Exception ex) {
				// Handle the error...
				ex.printStackTrace();
			}
		} catch (Exception e) {
			// Handle the error...
			e.printStackTrace();
		}

		// Use Long.valueOf
		// int ff = HashPrim.get(Long.valueOf("17008405040")).get(0).getTime();
		// System.out.println("Get hash: " + ff);

		int[] matches = new int[songs.size()];
		int keyMatch = 0;

		// Time matching process
		long startTime = System.nanoTime();
		// Pattern matching!
		Iterator<Entry<Long, List<DataPoint>>> it = HashPrim.entrySet()
				.iterator();
		while (it.hasNext()) {
			Map.Entry pairs = (Map.Entry) it.next();
			// System.out.println(pairs.getKey() + " = " + pairs.getValue());
			Long key = (Long) pairs.getKey();
			List<DataPoint> points = (List<DataPoint>) pairs.getValue();
			List<DataPoint> dbPoints = HashDB.get(key);
			if (HashDB.containsKey(key) == true) {
				keyMatch++;
				for (DataPoint dbPoint : dbPoints) {
					for (DataPoint instancePoint : points) {
						// if (dbPoint.getTime() == instancePoint.getTime()) {
						matches[dbPoint.getSongId()]++;
						// }
					}
				}
			}
			it.remove(); // avoids a ConcurrentModificationException
		}
		// matching time
		long endTime = System.nanoTime();

		long duration = endTime - startTime;
		long durationProgram = endTime - startTimeProgram;
		long durationReadMatch = endTime - startTimeReadMatchSong;
		System.out.println("Read + match: " + durationReadMatch / 1000 / 1000
				+ " ms");
		System.out
				.println("Matching (hash): " + duration / 1000 / 1000 + " ms");
		System.out.println("Program time: " + durationProgram / 1000 / 1000
				+ " ms" + "(" + durationProgram / 1000 / 1000 / 1000 + "s)");

		for (int i = 0; i < matches.length; i++) {
			System.out.println(matches[i] + "\t" + songs.get(i));
		}
		System.out.println("Key matches: " + keyMatch);

		/*
		 * try { while (running) { // if (cas < 0) { // break; // } int count =
		 * 0; // count = line.read(buffer, 0, buffer.length); // audio = buffer;
		 * buffer = readAudioFile(); count = buffer.length;
		 * System.out.println("Count: " + count); if (count > 0) { // count =
		 * 4410 final int totalSize = buffer.length;
		 * System.out.println("Total size: " + totalSize); int amountPossible =
		 * totalSize / CHUNK_SIZE; System.out.println("Amount possible: " +
		 * amountPossible); // When turning into frequency domain we'll need
		 * complex // numbers: results = new Complex[amountPossible][];
		 * 
		 * // For all the chunks: for (int times = 0; times < amountPossible;
		 * times++) { Complex[] complex = new Complex[CHUNK_SIZE]; for (int i =
		 * 0; i < CHUNK_SIZE; i++) { // Put the time domain data into a complex
		 * number // with imaginary // part as 0: complex[i] = new Complex(
		 * buffer[(times * CHUNK_SIZE) + i], 0); } // Perform FFT analysis on
		 * the chunk: results[times] = FFT.fft(complex);
		 * System.out.println("Performing FFT...");
		 * 
		 * } System.out.println("Results: " + results.length); // Determine Key
		 * Points // TODO amountPossible may not always be 0
		 * keyPoints(results[amountPossible - 1]);
		 * 
		 * // Done! main.cc.paintLine(cas, results);
		 * 
		 * } cas++;
		 * 
		 * if (stejCas && cas < 20 - 1) { cas++; } else { main.cc.reDraw(); //
		 * main.cc.repaint(); //trying to get the oval to move left // 20 }
		 * 
		 * } out.close(); } catch (IOException e) {
		 * System.err.println("I/O problems: " + e); System.exit(-1); }
		 */
	} // main method

	// Play sound file (only .wav)
	public static synchronized void playSound(final String url) {
		new Thread(new Runnable() { // the wrapper thread is unnecessary, unless
					// it blocks on the Clip finishing, see
					// comments
					public void run() {
						try {
							Clip clip = AudioSystem.getClip();
							AudioInputStream inputStream = AudioSystem
									.getAudioInputStream(Main.class
											.getResourceAsStream("" + url));
							clip.open(inputStream);
							clip.start();
						} catch (Exception e) {
							System.err.println(e.getMessage());
						}
					}
				}).start();
	}

	private static void keyPoints(Complex[] results, final DataPoint dp,
			Map<Long, List<DataPoint>> database) {
		// For every line of data:

		int[] recordPoints = new int[CHUNK_SIZE];
		double[] highscores = new double[CHUNK_SIZE];

		for (int freq = LOWER_LIMIT; freq < UPPER_LIMIT - 1; freq++) {
			// Get the magnitude:
			double mag = Math.log(results[freq].abs() + 1);

			// Find out which range we are in:
			int index = getIndex(freq);

			// Save the highest magnitude and corresponding frequency:
			if (mag > highscores[index]) {
				highscores[index] = mag;
				recordPoints[index] = freq;
			}
		}

		String line = "";
		// Write the points to a file:
		for (int i = 0; i < AMOUNT_OF_POINTS; i++) {
			if (writeToFile) {
				fw.append(recordPoints[i] + "\t");
			}
			line += recordPoints[i] + "\t";
		}

		// Make hash
		long hh = hash(line);
		System.out.println(line + "\t" + hh);

		if (writeToFile) {
			fw.append(String.valueOf(hh));
			fw.append("\n");
		}

		// Insert hash in database
		if (database.containsKey(hh) == true) {
			database.get(hh).add(dp);
		} else {
			List<DataPoint> ll = new ArrayList<DataPoint>();
			ll.add(dp);
			database.put(hh, ll);
		}
		// ... snip ...

	}

	// Find out in which range
	public static int getIndex(int freq) {
		int i = 0;
		while (RANGE[i] < freq)
			i++;
		return i;
	}

	// Using a little bit of error-correction, damping
	private static final int FUZ_FACTOR = 2;

	private static long hash(String line) {
		String[] p = line.split("\t");
		long p1 = Long.parseLong(p[0]);
		long p2 = Long.parseLong(p[1]);
		long p3 = Long.parseLong(p[2]);
		long p4 = Long.parseLong(p[3]);
		return (p4 - (p4 % FUZ_FACTOR)) * 100000000 + (p3 - (p3 % FUZ_FACTOR))
				* 100000 + (p2 - (p2 % FUZ_FACTOR)) * 100
				+ (p1 - (p1 % FUZ_FACTOR));
	}

	private static class DataPoint {

		private int time;
		private int songId;

		public DataPoint(int songId, int time) {
			this.songId = songId;
			this.time = time;
		}

		public int getTime() {
			return time;
		}

		public int getSongId() {
			return songId;
		}
	}

}
