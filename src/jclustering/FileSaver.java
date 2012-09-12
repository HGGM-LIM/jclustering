package jclustering;

import ij.IJ;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;

/**
 * Class for file saving. It accepts several formats as output. Logs result
 * of writing operation using the {@code IJ.log} functionality.
 * 
 * @author <a href="mailto:jmmateos@mce.hggm.es">José María Mateos</a>.
 *
 */
public class FileSaver {

    private String format;
    private ArrayList<Cluster> clusters;
    private double [][] t;
    private int time_length;
    
    /**
     * Constructor
     * @param format File saving format.
     * @param clusters The TAC data to be saved.
     * @param t Time vector.
     */
    public FileSaver(String format, ArrayList<Cluster> clusters, 
            double [][] t) {

        this.format = format;
        this.clusters = clusters;
        this.t = t;       
        
        // Get first non-empty cluster        
        for (Cluster c : clusters) {
            double [] temp = c.getCentroid();
            if (temp != null) {
                time_length = temp.length;
                break;
            }
        }
        
    }

    /**
     * Saves the data in the specified path in the specified format.
     * 
     * @param path The path for the file to be saved. May include the file
     *  name.     
     */
    public void save(String path) {

        // Check if the time vectors agree. It not don't use variable t.
        if (t != null && t[0].length != time_length) {
            IJ.log("The provided time vector length does not agree " +
            		"with the number of frames in the image. Writing " +
            		"data without time information.");
            t = null;
        }
        
        // Will store error, if any
        String m = null;
        
        if (format == "CSV") {
            m = _saveCharSeparated(path, ',');            
        } else if (format == "tab-separated")  {
            m = _saveCharSeparated(path, '\t');
        } else if (format == "PMOD") {
            m = _savePMOD(path);            
        } else {            
            // No suitable format found (shouldn't happen, as all formats 
            // will come from the GUI, but just in case...)
            IJ.log("Data not saved. Provided format was unknown"); 
        }
        
        // If there has been any kind of error, report it via log window.
        if (m == null) {
            IJ.log("Data saved in " + path + " in " + format + " format");
        } else {
            IJ.log("Data not saved. Error message: " + m);
        }       

    }
    
    /*
     * Save data in character separated format. Return exception message, 
     * if any.
     */
    private String _saveCharSeparated(String path, char sep, 
            String header) {
        
        double [][] data = _getData(clusters, t);
        File f = _getFile(path);        
        
        try {
            BufferedWriter bw = new BufferedWriter(new FileWriter(f));
            
            if (header != null) {
                bw.write(header);
                bw.newLine();
            }            
            
            for (int row = 0; row < data[0].length; row++) {
                for (int col = 0; col < data.length; col++) {
                    bw.write(data[col][row] + "");
                    // Don't write a separator at the end of the line
                    if (col < data.length - 1) bw.write(sep);                    
                }
                bw.newLine();
            }
            bw.close();
            
        } catch (IOException e) {
            return e.getLocalizedMessage();
        }               
        
        return null;
        
    }
    
    /*
     * Shortcut for _saveCharSeparated(...) when header is null
     */
    private String _saveCharSeparated(String path, char sep) {
        
        return _saveCharSeparated(path, sep, null);
        
    }
    
    
    /*
     * Save data in PMOD format. Return exception message, if any.
     */
    private String _savePMOD(String path) {
        
        // PMOD needs a time vector, mandatory. If none has been provided,
        // one can be invented and a note should be made about this.
        
        if (t == null) {
            IJ.log("PMOD file format needs a time vector, and no time" +
            		"\nvector was supplied. One consisting of each frame" +
            		"\nlasting one second will be used instead");
            t = new double[2][time_length];
            double frame = 0.0;
            for (int i = 0; i < time_length; i++) {
                t[0][i] = frame;
                frame += 1.0;
                t[1][i] = frame;                
            }
        }

        // Build header line
        String header = "start[seconds] \tend[kBq/cc]\t";
        int nclusters = clusters.size();
        for (int i = 0; i < nclusters; i++) {
            header += "cluster" + _n2s(i + 1);
            if (i < nclusters - 1)
                header += "\t";
        }
        
        // A PMOD file is just another tab-separated file with the given
        // header, so let's call that method.
        return _saveCharSeparated(path, '\t', header);        
        
    }
    
    /*
     * Converts cluster and time data into a 2-dimensional array to be
     * processed directly.
     */
    private double [][] _getData(ArrayList<Cluster> cluster, double [][] t) {
        
        // The first dimension is the amount of columns in the file (time
        // dimension + cluster dimension).
        // The second one is the amount of rows.
        int csize = cluster.size();
        double [][] res = null;
        int offset = 0;        
        
        // Initialize the result array. Beware that t might be null
        if (t != null) {
            res = new double[t.length + csize][time_length];
            
            // Fill the time part
            for (int i = 0; i < t.length; i++) {
                for (int j = 0; j < time_length; j++) {
                    res[i][j] = t[i][j];
                }
            }
            
            offset = t.length;
            
        } else {
            res = new double[csize][time_length];
        }  
        
        // Fill the data
        for (int i = offset; i < res.length; i++) {
            double [] tac = cluster.get(i - offset).getCentroid();
            if (tac == null) {
                // It may be possible that certain clusters from the 
                // "cluster" object are empty, if they have been filled
                // using the addTACtoCluster() methods. In this case,
                // an empty TAC (all 0) is created.
                tac = new double[time_length];
                Arrays.fill(tac, 0);
            }
            for (int j = 0; j < time_length; j++) {
                res[i][j] = tac[j];
            }
        }
        
        return res;
        
    }
    
    /*
     * Returns an open file for data writing
     */
    private File _getFile(String path) {
        
        // Compute date
        Calendar now = Calendar.getInstance();
        String year = Long.toString(now.get(Calendar.YEAR));
        String month = _n2s(now.get(Calendar.MONTH) + 1);        
        String day = _n2s(now.get(Calendar.DAY_OF_MONTH));        
        String hour = _n2s(now.get(Calendar.HOUR_OF_DAY));
        String min = _n2s(now.get(Calendar.MINUTE));
        String sec = _n2s(now.get(Calendar.SECOND));        
        
        // Build file name
        String name = "jclustering-" + year + "-" + month + "-" + day
                + "-" + hour + "-" + min + "-" + sec + ".txt";
        
        // Build and return file
        File f = new File(path + "/" + name);
        return f;
        
    }
    
    /*
     * Number to string, with a trailing 0 if necessary
     */
    private String _n2s(long l) {
        
        if (l < 10)
            return "0" + Long.toString(l);
        else
            return Long.toString(l);
        
    }
    
}
