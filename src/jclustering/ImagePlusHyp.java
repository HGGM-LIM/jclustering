package jclustering;

import java.util.Iterator;

import org.apache.commons.math3.stat.StatUtils;
import org.apache.commons.math3.stat.descriptive.SummaryStatistics;

import ij.ImagePlus;
import ij.ImageStack;

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

        // Compute the global mean (for noise comparison purposes)
        SummaryStatistics stats = new SummaryStatistics();
        for (int z = 0; z < dim[3]; z++)
            for (int x = 0; x < dim[0]; x++)
                for (int y = 0; y < dim[1]; y++)
                    stats.addValue(is.getVoxel(x, y, z));
        mean_amplitude = stats.getMean();

    }

    /**
     * Gets the time-activity curve (dixel, after "dynamic pixel") for the given
     * coordinates.
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
            result[frame - 1] = is.getVoxel(x, y, stack_number - 1);
        }

        return result;

    }

    /**
     * Sets integer pixel values at the specified location. Avoids having to
     * manually handle the ImageStack and ImageProcessor.
     * 
     * @param x x coordinate
     * @param y y coordinate
     * @param slice Slice number (1-based)
     * @param frame Frame (1-based). Use -1 for 3D stacks.
     * @param value Integer value to be set
     */
    public void set(int x, int y, int slice, int frame, int value) {

        set(x, y, slice, frame, (double) value);

    }

    /**
     * Sets double / float pixel values at the specified location. Avoids having
     * to manually handle the ImageStack and ImageProcessor.
     * 
     * @param x x coordinate
     * @param y y coordinate
     * @param slice Slice number (1-based)
     * @param frame Frame (1-based). Use -1 for 3D stacks.
     * @param value Integer value to be set
     */
    public void set(int x, int y, int slice, int frame, double value) {

        int slice_number = this.getStackIndex(dim[2], slice, frame);
        is.setVoxel(x, y, slice_number - 1, value);

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
