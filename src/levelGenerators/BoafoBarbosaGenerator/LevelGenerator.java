package levelGenerators.BoafoBarbosaGenerator;

import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import engine.core.MarioLevelGenerator;
import engine.core.MarioLevelModel;
import engine.core.MarioTimer;


public class LevelGenerator implements MarioLevelGenerator {
    private final String LEVEL_FOLDER = "levels/ore/";
    // list of all slices this level generator knows of
    private List<Slice> slices;
    // indexes of all slices with a Mario start tile
    private List<Integer> starts;
    // list of slices with a flag tile
    private List<Integer> ends;

    /**
     * Basic constructor, reads all level files in LEVEL_FOLDER
     */
    public LevelGenerator() {
        slices = new ArrayList<Slice>();
        starts = new ArrayList<Integer>();
        ends = new ArrayList<Integer>();
        // Get all filenames in the lePath directory}
        for (final File fileEntry : new File(LEVEL_FOLDER).listFiles()) {
            if (!fileEntry.isDirectory()) {
                parseIn(fileEntry.getAbsolutePath());
            }
        }
    }

    /**
     * Reads in a level file from the filesystem, splitting it into 1-column
     * wide slices and adding them to a Markov transition table
     * @param filename the path to the file to read in
     */
    public void parseIn(String filename) {
        System.out.println("Reading " + filename);
        FileReader fr;
        List<char[]> cols = new ArrayList<char[]>();
        try {
            fr = new FileReader(filename);
            int i; // the character read by fr
            int column = 0; // the column number being read
            int row = 0; // the row number being read
            boolean firstLine = true;
            // read through the file and separate out each column into arrays of characters
            while ((i = fr.read()) != -1) {
                if ((char) i == '\n') {
                    if (firstLine) {
                        firstLine = false;
                    }
                    row++;
                    column = 0;
                } else {
                    if (firstLine) {
                        cols.add(new char[16]);
                    }
                    cols.get(column)[row] = (char) i;
                    column++;
                }
            }
            fr.close();

        } catch (Exception e) {
            System.err.println(e.toString());
            System.err.println("Error with file " + filename);
            return;
        }

        // convert char arrays into slices
        Slice lastSlice = null;
        for (char[] c : cols) {
            // see if we've seen this exact sequence of characters before
            Slice tempSlice = null;
            for (Slice s : slices) {
                if (s.toString().equals(String.valueOf(c))) {
                    tempSlice = s;
                    break;
                }
            }
            if (tempSlice == null) {
                // this is a new slice
                tempSlice = new Slice();
                for (int i = 0; i < 16; ++i) {
                    if ((char) c[i] == 'M') {
                        tempSlice.setMario(true);
                        starts.add(slices.size());
                    }
                    if ((char) c[i] == 'F') {
                        tempSlice.setFlag(true);
                        ends.add(slices.size());
                    }
                    tempSlice.addInChar(c[i], i);
                }
                slices.add(tempSlice);
            }
            // add this as a followup to the last slice we saw
            if (lastSlice != null) {
                lastSlice.addFollow(tempSlice);
            }
            lastSlice = tempSlice;
        }
    }

    /**
     * Generates a level
     * @param model a MarioLevelModel object
     * @parm timer a MarioTimer object
     * @return the string version of a Markov-generated level
     */
    @Override
    public String getGeneratedLevel(MarioLevelModel model, MarioTimer timer) {
        // initialize new random number generator
        Random rng = new Random();

        // start with a clear model
        model.clearMap();

        // pick a starting slice that has a Mario start block, or just a random
        // one if no slices have a Mario start
        Slice curSlice;
        if (starts.size() > 0) {
            curSlice = slices.get(starts.get(rng.nextInt(starts.size())));
        } else {
            curSlice = slices.get(rng.nextInt(slices.size()));
        }
        for (int i = 0; i < 16; ++i) {
            model.setBlock(0, i, curSlice.getPiece(i));
        }

        int x = 1;
        boolean alreadyFlag = false;
        int prevHeight = curSlice.getGroundHeight();
        while (x < model.getWidth() - 1) {
            do {
                curSlice = curSlice.getNext(rng);
                // make sure that the next slice has followups and isn't too high to jump to from here
            } while (curSlice.getTotalFollow() < 1 && curSlice.getGroundHeight() < prevHeight + 4);
            for (int i = 0; i < 16; ++i) {
                model.setBlock(x,  i, curSlice.getPiece(i));
            }
            if (curSlice.getFlag()) {
                alreadyFlag = true;
                break;
            }
            x++;
        }

        // if we haven't already added a flag, put a flag slice at the end
        if (!alreadyFlag) {
            // pick a random flag slice and add it to the end
            curSlice = slices.get(ends.get(rng.nextInt(ends.size())));
            for (int i = 0; i < 16; ++i) {
                model.setBlock(model.getWidth() - 1, i, curSlice.getPiece(i));
            }
        }

        return model.getMap();
    }

    /**
     * gets the name of this level generator
     * @return the name of this generator
     */
    @Override
    public String getGeneratorName() {
        return "MarkovChainGenerator";
    }
}
