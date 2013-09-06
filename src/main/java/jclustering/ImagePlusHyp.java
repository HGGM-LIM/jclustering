package jclustering;

import java.util.Iterator;
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
public class ImagePlusHyp implements Iterable<Voxel>{

    /*
     * dim[0] -> width (x) dim[1] -> height (y) dim[2] -> nChannels (1-based)
     * dim[3] -> nSlices (1-based) dim[4] -> nFrames (1-based)
     */
    private int[] dim;
    private ImagePlus imp;
    private ImageStack is;
    private Calibration cal;
    
    /**
     * The calibrated 0.0 value (which may not be equal to 0.0)
     */
    public final double CALZERO;

    /**
     * Creates a new ImagePlusHyp using a general ImagePlus.
     * 
     * @param ip The ImagePlus used to construct this class.
     */
    public ImagePlusHyp(ImagePlus ip) {

        imp = ip;
        this.is = ip.getStack();
        this.dim = ip.getDimensions();   
        this.cal = ip.getCalibration();
        CALZERO = cal.getCValue(0.0);
        
    }

    /**
     * Gets the time-activity curve (dixel, after "dynamic pixel") for the 
     * given coordinates. Please note that this method returns the calibrated
     * curve according to calibration data present in the image header, not
     * the raw values. Please refer to ImageJ's {@link Calibration} object
     * documentation for more information regarding this. 
     * 
     * It is <strong>strongly discouraged</strong> to use this method directly.
     * Use the {@link Iterator} provided by this object instead. Example:
     *  
     * <pre> 
     * for (Voxel v: ip) { // ip is a reference to a ImagePlusHyp object.
     *  ... // your actual code goes in here
     * } 
     * </pre>
     * 
     * This has certain advantages. For instance, you do not need to know
     * the dimensions of your image for it to work, and the {@link Iterator},
     * provided by {@link ImagePlusHypIterator}, will not return voxels that
     * have been masked previously, therefore diminishing the size of the
     * clustering problem to be solved. 
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
            int stack_number = imp.getStackIndex(dim[2], slice, frame);            
            // Use calibration to return true value
            result[frame - 1] = cal.getCValue(
                                    is.getVoxel(x, y, stack_number - 1));
        }

        return result;

    }
    
    /**
     * @return The dimensions of the {@link ImagePlus} used to create 
     * this object.
     */
    public int [] getDimensions() {
        return dim;
    }
    
    /**
     * @return The local {@link Calibration} object extracted from the original
     * {@link ImagePlus}.
     */
    public Calibration getCalibration() {
        return cal;
    }
    
    /**
     * @return The internal reference to the {@link ImagePlus} object.
     */
    public ImagePlus getImagePlus() {
        return imp;
    }

    @Override
    public Iterator<Voxel> iterator() {
        return new ImagePlusHypIterator(this);
    }

}
