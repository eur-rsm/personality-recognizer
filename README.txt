This is a slightly adapted version of the Personality Recognizer :

http://farm2.user.srcf.net/research/personality/recognizer

A few modifications has been made to the original code:

- Use Maven for building, runnning and deploying the application
- Include the dependencies in the source.
- Add an extra command line argument : -s
  This option allows a directory input with 1 subject per file,
  without standardizing the features over the whole corpus.
- Add an extra command line argument : -e 
  This option writes the output to JSON files instead of stdout.
- Fix a bug where not all models where used due to naming differences.

# To compile the application :

    $ mvn clean compile 

# To run the compiled application (1 text file per subject in a directory) :

    $ mvn exec:java -Dexec.args="-i <INDIR> -s -t 1 -m 4"

# Output the anaysis to JSON files (1 per subject) :

    $ mvn exec:java -Dexec.args="-i <INDIR> -r <OUTDIR> -s -t 1 -m 4"

# Package the application to a distributable JAR :
    
    $ mvn package

# The resulting JAR is ./target/personality-recognizer-1.0-jar-with-dependencies.jar :

    $ java -jar personality-recognizer-1.0-jar-with-dependencies.jar -i <INDIR> -s -t 1 -m 4

# Generate javadocs to ./target/apidocs/ :

    $ mvn javadoc:jar
