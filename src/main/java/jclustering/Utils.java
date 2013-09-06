package jclustering;

import static jclustering.Constants.PACKAGE_NAME;

import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import org.apache.commons.math3.linear.RealMatrix;

import jclustering.metrics.ClusteringMetric;
import jclustering.techniques.ClusteringTechnique;

import static jclustering.MathUtils.isMasked;

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

        final ArrayList<String> res = new ArrayList<String>();
        final URL url = Utils.class.getResource("/" + Utils.class.getName().replace('.', '/') + ".class");
        if (url == null) {
            return res;
        }
        String location = url.toString();
        location = location.substring(0, location.lastIndexOf('/') + 1); // strip Util.class
        final String prefix = PACKAGE_NAME + "/" + sp + "/";

        // find files in the same class path element as the current class
        final String[] files;
        if (location.startsWith("jar:file:")) try {
            final List<String> fileNames = new ArrayList<String>();
            final JarFile jar = new JarFile(location.substring("jar:file:".length(), location.indexOf('!')));
            for (Enumeration<JarEntry> e = jar.entries(); e.hasMoreElements(); ) {
                final JarEntry entry = e.nextElement();
                final String fileName = entry.getName();
                if (fileName.startsWith(prefix) && !fileName.endsWith("/")) {
                    fileNames.add(fileName.substring(prefix.length()));
                }
            }
            files = fileNames.toArray(new String[fileNames.size()]);
        } catch (IOException e) {
            return res;
        } else if (location.startsWith("file:")) {
            files = new File(location.substring("file:".length()), sp).list();
        } else {
            return res;
        }

        // Get all the classes in the given path
        for (String c : files) {
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
     * @return A new instance of the said ClusteringTechnique.
     */
    public static ClusteringTechnique getClusteringTechnique(String name,
            ImagePlusHyp ip) {

        Class<?> c = _getClassByName(name, "techniques");
        ClusteringTechnique ct = null;
        try {
            ct = (ClusteringTechnique) c.newInstance();
            ct.setup(ip);            
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
        ImagePlus res = IJ.createImage(ip.getTitle(), "16-bit", dim[0], dim[1], 
                dim[3] * (cluster_number + 1));
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
    
    /**
     * Transforms a {@link RealMatrix} object into a ImageJ image.
     * @param rm The RealMatrix to be converted.
     * @param dim The desired dimensions for the final image
     * @param ip A reference to the image object that generated this operation    
     * @param name The name for the new image.
     * @return The newly generated {@code ImagePlus} object.
     */
    public static ImagePlus RealMatrix2IJ(RealMatrix rm, int [] dim, 
            ImagePlusHyp ip, String name) {
     // Get number of components
        int components = rm.getRowDimension();
        
        // Create dynamic image
        ImagePlus image = IJ.createImage(name, "32-bit", 
                                             dim[0], dim[1],
                                             dim[3] * components);
        image.setDimensions(1, dim[3], components);
        image.setOpenAsHyperStack(true);
        
        // Get stack for easy access
        ImageStack is = image.getStack();
        
        int column_index = 0;
        
        // Assign voxels to values. It is important to iterate the image
        // in the correct order (first x, then y, then slices), because
        // that is the way the images are normally processed when using
        // the ImagePlusHypIterator object.
        for(int z = 0; z < dim[3]; z++) {
            for(int y = 0; y < dim[1]; y++) {
                for(int x = 0; x < dim[0]; x++) {
                    
                    // Test it the TAC is noise. If it is noise, 
                    // jump to the next one
                    double [] tac = ip.getTAC(x, y, z + 1);
                    
                    // Check for masked voxels                    
                    if (!isMasked(tac, ip.CALZERO)) {
                        // Not masked: get the next column
                        double [] comp = rm.getColumn(column_index++);
                        
                        // Iterate through the component and set the values.
                        // Each row of the component is in one frame.
                        for (int t = 0; t < components; t++) {
                            // Get internal slice number
                            int sn = image.getStackIndex(1, z + 1, t + 1);
                            is.setVoxel(x, y, sn, comp[t]);                            
                        }       
                    }                   
                }
            }
        }        
        
        return image;
        
    }

}
