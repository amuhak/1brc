Tested with Java 22 on https://www.graalvm.org/

Requires GraalVM due to the pair class.

This code runs in about 5886 ms on my machine.

The fastest code I could run on my system ran in 4066 ms
[Link](https://github.com/gunnarmorling/1brc/blob/main/src/main/java/dev/morling/onebrc/CalculateAverage_gonix.java)

Data gernated using [this repo](https://github.com/dannyvankooten/1brc#submitting) because I could not get the
java maven build to work.

Look [here](https://1brc.dev/) for all the languages.

# How to run

After installing GraalVM and cloning the repo, run the following commands:

```bash
javac Main.java
java Main
```

Make sure to have the `measurements.txt` file in the same directory as the Main.java file.
