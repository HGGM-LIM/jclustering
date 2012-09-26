package jclustering;

import java.util.Iterator;

import org.apache.commons.math3.stat.StatUtils;
import org.apache.commons.math3.stat.descriptive.SummaryStatistics;

import ij.ImagePlus;
import ij.ImageStack;
import ij.measure.Calibration;

/**
 * This class extends {@link ImagePlus} in order to add a handy {@link #getTAC}
 * method that allows to easily grab time-activity curves.
 * 
 * @author <a href="mailto:jmmateos@mce.hggm.es">José María Mateos</a>.
 * 
 */
public class ImagePlusHyp extends ImagePlus implements Iterable<Voxel>{

    /*
     * dim[0] -> width (x) dim[1] -> height (y) dim[2] -> nChannels (1-based)
     * dim[3] -> nSlices (1-based) dim[4] -> nFrames (1-based)
     */
    private int[] dim;
    private ImageStack is;
    private double mean_amplitude; // mean amplitude for the whole volume

    // Calibration curve
    private double offset, slope;

    /**
     * Creates a new ImagePlusHyp using a general ImagePlus.
     * 
     * @param ip The ImagePlus used to construct this class.
     */
    public ImagePlusHyp(ImagePlus ip) {

        super(ip.getTitle(), ip.getStack());
        this.is = ip.getStack();
        this.setProcessor(ip.getProcessor());
        this.setImage(ip);
        this.dim = ip.getDimensions();
        
        // Get calibration data from the image or set default values if
        // no calibration has been used
        Calibration cal = ip.getCalibration();
        if (cal.calibrated()) {
            double[] coefs = cal.getCoefficients();
            offset = coefs[0];
            slope = coefs[1];
        } else {
            offset = 0.0;
            slope = 1.0;
        }

        // Compute the global calibrated mean (for noise comparison purposes)
        SummaryStatistics stats = new SummaryStatistics();
        for (int z = 0; z < dim[3]; z++)
            for (int x = 0; x < dim[0]; x++)
                for (int y = 0; y < dim[1]; y++)
                    stats.addValue(offset + slope * is.getVoxel(x, y, z));
        mean_amplitude = stats.getMean();
        
        
    }

    /**
     * Gets the time-activity curve (dixel, after "dynamic pixel") for the 
     * given coordinates. Please note that this method returns the calibrated
     * curve according to calibration data present in the image header, not
     * the raw values. Please refer to ImageJ's {@link Calibration} object
     * documentation for more information regarding this. 
     * 
     * @param x The x coordinate of the desired dixel.
     * @param y The y coordinate of the desired dixel.
     * @param slice Slice (1-based) of the desired dixel.
     * @return A double array containing the values for the given voxel on each
     *         frame, or null if the coordinates are not valid.
     */
    public double[] getTAC(int x, int y, int slice) {

        // Dimension check
        if (x >= dim[0] || x < 0 || y >= dim[1] || y < 0 || slice > dim[3]
                || slice < 1) {
            return null;
        }

        // Alloc space for the result
        double[] result = new double[dim[4]];

        // Set the desired slice and iterate through the frames
        for (int frame = 1; frame <= dim[4]; frame++) {
            int stack_number = this.getStackIndex(dim[2], slice, frame);            
            // Use calibration to return true value
            result[frame - 1] = offset + 
                                slope * is.getVoxel(x, y, stack_number - 1);
        }

        return result;

    }

    /**
     * @param data The TAC to be tested
     * @return true if the given TAC is noise with respect to this image.
     */
    public boolean isNoise(double[] data) {

        double sd = StrictMath.sqrt(StatUtils.variance(data));
        double m = StatUtils.mean(data);

        if ((sd > 100 * m) || (m < mean_amplitude / 3)
             || (StrictMath.abs(StatUtils.min(data)) >= StatUtils.max(data))) {
            return true;
        }
        return false;

    }

    @Override
    public Iterator<Voxel> iterator() {
        return new ImagePlusHypIterator(this);
    }

}
