package jclustering;

import static jclustering.Constants.PACKAGE_NAME;
import static jclustering.Constants.PLUGIN_PATH;

import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;

import java.io.File;
import java.util.ArrayList;

import jclustering.metrics.ClusteringMetric;
import jclustering.techniques.ClusteringTechnique;

/**
 * Auxiliary class with misc static methods
 * 
 * @author <a href="mailto:jmmateos@mce.hggm.es">José María Mateos</a>
 */
public class Utils {

    /**
     * Finds all the classes that extend the ClusteringTechnique superclass and
     * returns a list of their names.
     * 
     * @return An ArrayList<String> containing all the class names.
     */
    public static ArrayList<String> getAllTechniques() {

        return _findClasses("ClusteringTechnique", "techniques");

    }

    /**
     * Finds all the classes that extend the ClusteringMetric superclass and
     * returns a list of their names.
     * 
     * @return An ArrayList<String> containing all the class names.
     */
    public static ArrayList<String> getAllMetrics() {

        return _findClasses("ClusteringMetric", "metrics");

    }

    /**
     * Finds all the classes that extend a given superclass and returns a list
     * of their names.
     * 
     * @param sc The superclass that all the found classes must extend.
     * @param sp The subpackage to search for classes
     * @return An ArrayList<String> containing all the class names.
     */
    private static ArrayList<String> _findClasses(String sc, String sp) {

        // FIXME Add logic for extracting classes from a .jar file.
        ArrayList<String> res = new ArrayList<String>();
        File classes = new File(PLUGIN_PATH + "/" + sp);

        // Get all the classes in the given path
        for (String c : classes.list()) {
            try {
                String cn = PACKAGE_NAME + "." + sp + "."
                        + c.substring(0, c.indexOf("."));

                // Create an instance of that class and get the superclass name
                Class<?> cl = Class.forName(cn);
                String sup = cl.getSuperclass().toString();

                // If this class extends sc, add it to the result
                if (sup.endsWith(sc)) {
                    res.add(cn.substring(cn.lastIndexOf(".") + 1, cn.length()));
                }

            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            }
        }

        return res;

    }

    /**
     * Builds a new instance for a {@link ClusteringTechnique} object and
     * returns it.
     * 
     * @param name The name of the ClusteringTechnique to build.
     * @param ip A reference to the working image.
     * @param skip_noisy True if noisy voxels are to be skipped.
     * @return A new instance of the said ClusteringTechnique.
     */
    public static ClusteringTechnique getClusteringTechnique(String name,
            ImagePlusHyp ip, boolean skip_noisy) {

        Class<?> c = _getClassByName(name, "techniques");
        ClusteringTechnique ct = null;
        try {
            ct = (ClusteringTechnique) c.newInstance();
            ct.setup(ip);
            ct.skipNoisy(skip_noisy);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return ct;

    }

    /**
     * Builds a new instance for a {@link ClusteringMetric} object and returns
     * it.
     * 
     * @param name The name of the ClusteringMetric to build.
     * @param ip A reference to the working image.
     * @return A new instance of the said ClusteringMetric.
     */
    public static ClusteringMetric getClusteringMetric(String name,
            ImagePlusHyp ip) {

        Class<?> c = _getClassByName(name, "metrics");
        ClusteringMetric cm = null;
        try {
            cm = (ClusteringMetric) c.newInstance();
            cm.setup(ip);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return cm;

    }

    /**
     * Returns a class with the name used as argument
     * 
     * @param name The name of the class
     * @param sp The name of the subpackage to search for the class
     */
    private static Class<?> _getClassByName(String name, String sp) {

        // Build the full class name accordingly
        if (!name.startsWith(PACKAGE_NAME)) {
            if (sp != null && !sp.equals("")) {
                name = PACKAGE_NAME + "." + sp + "." + name;
            } else {
                name = PACKAGE_NAME + "." + name;
            }
        }

        Class<?> c = null;

        try {
            c = Class.forName(name);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }

        return c;

    }

    /**
     * Returns the name of a given class, without the package name
     * 
     * @param c The class
     * @return The name of the class
     */
    public static <T> String getName(T c) {

        String name = c.toString();
        name = name.substring(name.lastIndexOf('.') + 1, name.indexOf('@'));
        return name;

    }

    /**
     * Creates an empty ImagePlus with the given dimensions. The resulting image
     * will have a bit depth of 8 bits, which should be enough to store all the
     * clusters (maximum clusters = 255).
     * 
     * @param x Width, in pixels.
     * @param y Height, in pixels.
     * @param z Number of slices.
     * @param nFrames Number of frames.
     * @return An empty ImagePlus object.
     */
    public static ImagePlus createImagePlus(int x, int y, int z, int nFrames) {

        ImagePlus aux = IJ.createImage("", "8-bit", x, y, z);
        ImagePlus result = aux.createHyperStack("Clusters", 1, z, nFrames, 8);

        return result;

    }

    /**
     * Transforms a static {@link ImagePlus} object into a dynamic one, with the
     * correct structure for a cluster result.
     * 
     * @param ip The static {@link ImagePlus} obtained from the
     *            {@link ClusteringTechnique#process()} method.
     * @param cluster_number The number of clusters.
     * @return A clustering result, dynamic {@link ImagePlus}.
     */
    public static ImagePlus expand(ImagePlus ip, int cluster_number) {

        // Create a new HyperStack with the given cluster number
        // This Image will have the same dimensions and title as the original
        // image (width, height, slices) and cluster_number + 1 frames in
        // total. The code below duplicates part of the createHyperStack()
        // method present in ImageJ from version 1.47a, but doing it this way
        // prevents the user from needing a version that is so recent (and
        // a daily build, for that matter).

        int[] dim = ip.getDimensions();
        ImagePlus res = IJ.createImage(ip.getTitle(), dim[0], dim[1], dim[3]
                * (cluster_number + 1), ip.getBitDepth());
        res.setDimensions(1, dim[3], cluster_number + 1);
        res.setOpenAsHyperStack(true);

        // Fill data the following way:
        // - The frames from 1 to cluster_number contain only the relevant
        // cluster.
        // - The frame cluster_number + 1 contains all the clusters, just like
        // the original ImagePlus ip.

        ImageStack source = ip.getStack();
        ImageStack target = res.getStack();

        for (int slice = 1; slice <= dim[3]; slice++) {
            for (int x = 0; x < dim[0]; x++) {
                for (int y = 0; y < dim[1]; y++) {
                    // Get voxel value for original image
                    int val = (int) source.getVoxel(x, y, slice - 1);

                    // If the value is beyond the clusters to be represented.
                    if (val > cluster_number || val == 0)
                        continue;

                    // Get slice number for target stack
                    int s = res.getStackIndex(1, slice, val);

                    // Set the voxel for that stack. The value is set to
                    // cluster_number so as to not have problems with the
                    // contrast levels afterwards
                    target.setVoxel(x, y, s - 1, cluster_number);

                    // Also, set the last frame
                    s = res.getStackIndex(1, slice, cluster_number + 1);
                    target.setVoxel(x, y, s - 1, val);
                }
            }
        }

        return res;

    }

    /**
     * Provides a handy method for setting voxel values using slices as
     * z-indexes (which are 0-based).
     * 
     * @param is The {@link ImageStack} to be modified
     * @param x x-coordinate
     * @param y y-coordinate
     * @param slice Slice number (1-based).
     * @param value The value to be set.
     */
    public static void setVoxel(ImageStack is, int x, int y, int slice,
            double value) {

        is.setVoxel(x, y, slice - 1, value);

    }

}
