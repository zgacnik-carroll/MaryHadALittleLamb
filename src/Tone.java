import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;

public class Tone {

    public static void main(String[] args) throws Exception {
        if (args.length < 1) {
            System.err.println("Usage: ant run -Dfile=<song-file.txt>");
            System.exit(1);
        }

        final AudioFormat af =
                new AudioFormat(Note.SAMPLE_RATE, 8, 1, true, false);
        Tone t = new Tone(af);

        List<BellNote> song = t.loadSong(args[0]);
        t.playSong(song);
    }

    private final AudioFormat af;

    Tone(AudioFormat af) {
        this.af = af;
    }

    /**
     * Loads a song from a text file. Each line should contain a note name
     * and a numeric length separated by whitespace, e.g.:
     *
     *   A5 4    (A5 quarter note)
     *   G4 2    (G4 half note)
     *   REST 1  (whole rest)
     *
     * Valid lengths: 1=WHOLE, 2=HALF, 4=QUARTER, 8=EIGTH
     * Blank lines and lines starting with '#' are ignored as comments.
     */
    List<BellNote> loadSong(String filename) throws IOException {
        List<BellNote> notes = new ArrayList<>();

        // Open the file using try-with-resources so it's automatically closed when done
        try (BufferedReader reader = new BufferedReader(new FileReader(filename))) {
            String line;
            int lineNumber = 0; // Track line number for useful error messages

            while ((line = reader.readLine()) != null) {
                lineNumber++;
                line = line.trim(); // Strip leading/trailing whitespace

                // Skip blank lines and comment lines (starting with '#')
                if (line.isEmpty()) {
                    continue;
                }

                // Split on any whitespace — expects exactly two tokens: NOTE and LENGTH
                String[] parts = line.split("\\s+");
                if (parts.length != 2) {
                    throw new IllegalArgumentException(
                            "Invalid format on line " + lineNumber + ": \"" + line +
                                    "\". Expected: <NOTE> <LENGTH>"
                    );
                }

                Note note;
                NoteLength length;

                // Parse the first token as a Note enum value (case-insensitive)
                try {
                    note = Note.valueOf(parts[0].toUpperCase());
                } catch (IllegalArgumentException e) {
                    throw new IllegalArgumentException(
                            "Unknown note \"" + parts[0] + "\" on line " + lineNumber +
                                    ". Valid notes: " + java.util.Arrays.toString(Note.values())
                    );
                }

                // Parse the second token as a numeric length: 1=WHOLE, 2=HALF, 4=QUARTER, 8=EIGTH
                try {
                    length = NoteLength.fromNumeric(Integer.parseInt(parts[1]));
                } catch (NumberFormatException e) {
                    throw new IllegalArgumentException(
                            "Invalid length \"" + parts[1] + "\" on line " + lineNumber +
                                    ". Expected a number: 1, 2, 4, or 8"
                    );
                }

                // Both tokens valid — add the note to the song
                notes.add(new BellNote(note, length));
            }
        }

        // Reject empty files so playSong is never called with nothing to play
        if (notes.isEmpty()) {
            throw new IllegalArgumentException("Song file \"" + filename + "\" contains no notes.");
        }

        return notes;
    }

    void playSong(List<BellNote> song) throws LineUnavailableException {
        try (final SourceDataLine line = AudioSystem.getSourceDataLine(af)) {
            line.open();
            line.start();

            for (BellNote bn : song) {
                playNote(line, bn);
            }
            line.drain();
        }
    }

    private void playNote(SourceDataLine line, BellNote bn) {
        final int ms = Math.min(bn.length.timeMs(), Note.MEASURE_LENGTH_SEC * 1000);
        final int length = Note.SAMPLE_RATE * ms / 1000;
        line.write(bn.note.sample(), 0, length);
        line.write(Note.REST.sample(), 0, 50);
    }
}

class BellNote {
    final Note note;
    final NoteLength length;

    BellNote(Note note, NoteLength length) {
        this.note = note;
        this.length = length;
    }
}

enum NoteLength {
    WHOLE(1.0f),
    HALF(0.5f),
    QUARTER(0.25f),
    EIGTH(0.125f);

    private final int timeMs;

    private NoteLength(float length) {
        timeMs = (int)(length * Note.MEASURE_LENGTH_SEC * 1000);
    }

    public int timeMs() {
        return timeMs;
    }

    // Maps a numeric denominator (1, 2, 4, 8) to the corresponding NoteLength
    public static NoteLength fromNumeric(int n) {
        switch (n) {
            case 1: return WHOLE;
            case 2: return HALF;
            case 4: return QUARTER;
            case 8: return EIGTH;
            default: throw new IllegalArgumentException(
                    "Invalid note length: " + n + ". Valid values: 1, 2, 4, 8"
            );
        }
    }
}

enum Note {
    // REST Must be the first 'Note'
    REST,
    A4,
    A4S,
    B4,
    C4,
    C4S,
    D4,
    D4S,
    E4,
    F4,
    F4S,
    G4,
    G4S,
    A5;

    public static final int SAMPLE_RATE = 48 * 1024; // ~48KHz
    public static final int MEASURE_LENGTH_SEC = 1;

    // Circumference of a circle divided by # of samples
    private static final double step_alpha = (2.0d * Math.PI) / SAMPLE_RATE;

    private final double FREQUENCY_A_HZ = 440.0d;
    private final double MAX_VOLUME = 127.0d;

    private final byte[] sinSample = new byte[MEASURE_LENGTH_SEC * SAMPLE_RATE];

    private Note() {
        int n = this.ordinal();
        if (n > 0) {
            // Calculate the frequency!
            final double halfStepUpFromA = n - 1;
            final double exp = halfStepUpFromA / 12.0d;
            final double freq = FREQUENCY_A_HZ * Math.pow(2.0d, exp);

            // Create sinusoidal data sample for the desired frequency
            final double sinStep = freq * step_alpha;
            for (int i = 0; i < sinSample.length; i++) {
                sinSample[i] = (byte)(Math.sin(i * sinStep) * MAX_VOLUME);
            }
        }
    }

    public byte[] sample() {
        return sinSample;
    }
}