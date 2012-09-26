package jclustering;

import ij.IJ;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;

/**
 * This class reads data from a text file with two columns separated by a
 * space: each row contains the starting time (first column) and ending time
 * (second column). Example:
 * 
 * <pre>
 * 1.0 2.0
 * 2.0 3.0
 * 3.0 4.0
 * ...
 * </pre>
 * 
 * This file needs to have as many rows as frames, or the time data will
 * not be used when saving data to file.
 * <p>
 * This class can also read PMOD's .acqtimes files.
 * 
 * @author <a href="mailto:jmmateos@mce.hggm.es">José María Mateos</a>.
 *
 */
public class TimeVectorReader {
    
    private String file_path;
    
    /**
     * Builds a new TimeVectorReader object.
     * @param file_path The path for the file that includes the time vector
     * data.
     */
    
    public TimeVectorReader (String file_path) {
        
        this.file_path = file_path;
        
    }
    
    /**
     * Reads the data provided in the constructor and returns the appropriate
     * time vector array.
     * @return A time vector array, or null if there is some problem or the
     * path used in the constructor was null.
     */
    public double [][] getTimeVector() {
        
        if (file_path == null) return null;
        
        double [][] t = null;
        
        try {
            FileReader fr = new FileReader(file_path);
            BufferedReader br = new BufferedReader(fr);
            
            // Default separator is space
            String sep = " ";
                        
            // First, put all lines into an ArrayList<String>
            ArrayList<String []> lines = new ArrayList<String []>();
            String line = null;
            while ((line = br.readLine()) != null) {
                // If it contains comments it is a PMOD file (for now),
                // so let's change the separator file and continue to next
                // line.
                if (line.indexOf('#') != -1) {
                    sep = "\t";
                    continue;
                }
                
                line = line.trim(); // remove white characters from both ends
                String [] s = line.split(sep);
                if (s.length == 2)
                    lines.add(s);
            }
            
            // Now put all inside t[][]
            int time_length = lines.size();
            t = new double[2][time_length];
            for (int i = 0; i < time_length; i++) {
                String [] s2 = lines.get(i);                
                t[0][i] = Double.parseDouble(s2[0]);
                t[1][i] = Double.parseDouble(s2[1]);
            }
            
        } catch (FileNotFoundException e) {
            // This file_path comes from a selection made by a FileChooser,
            // so it should exist. In the case we don't have permissions to
            // open the file, just log it and return null.
            IJ.log("Vector file not found.");
            return null;
        } catch (IOException e) {
            IJ.log("Couldn't read vector file.");
            return null;
        } 

        return t;
        
        
        
    }

}
