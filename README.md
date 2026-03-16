# Mary Had A Little Lamb

This is a simple Java program that reads musical notes from a text file and plays them as audio through your system's sound output.

---

## Requirements

- Java JDK 21.0.7 (check with `java --version`)
- Apache ANT 1.10.15 (check with `ant -version`)

---

## How It Works

`Tone.java` reads a song from a text file and plays each note by generating a audio sample at the appropriate frequency. Notes and their lengths are defined in a plain text file, one per line.

### Song File Format

Each line contains a note name and a length separated by whitespace:

```
A5 4
G4 2
F4 1
```

- Lines that are blank are ignored
- Note names and lengths are case-insensitive

---

## Running the Project

Build and run with ANT from the project root:

```bash
ant run clean -Dfile=mary.txt
```

This compiles `src/Tone.java` into the `build/` directory, plays `mary.txt`, and cleans the build directory upon program completion.

---

## Adding a New Song

If you get bored with Mary Had A Little Lamb, you can create your own song!

1. Create a new text file (e.g. `mysong.txt`) in the project root using the format described above
2. run the song file using ANT in the command-line:
```java
ant run clean -Dfile=mysong.txt
```

---

## Closing Remarks

This project was created to strengthen my understanding of
file-reading in Java and how to create music through notes written
in a text file. Have fun playing Mary Had A Little Lamb!