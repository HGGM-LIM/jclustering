package jclustering.techniques;

import static jclustering.GUIUtils.getMetricList;
import static jclustering.Utils.getClusteringMetric;
import ij.ImagePlus;

import java.awt.Choice;
import java.awt.Component;
import java.awt.Panel;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.ArrayList;

import javax.swing.JLabel;
import javax.swing.JPanel;

import jclustering.Cluster;
import jclustering.GUIUtils;
import jclustering.ImagePlusHyp;
import jclustering.Utils;
import jclustering.metrics.ClusteringMetric;

/**
 * This superclass should be extended by all user-implemented clustering
 * techniques. If no configuration panel is needed, only the {@link #process()}
 * method must be implemented. Otherwise, the method {@link #makeConfig()} must
 * also be filled.
 * 
 * @author <a href="mailto:jmmateos@mce.hggm.es">José María Mateos</a>.
 */
public abstract class ClusteringTechnique implements ItemListener {

    /**
     * The metric for this technique.
     */
    protected ClusteringMetric metric;

    /**
     * The configuration panel.
     */
    protected JPanel jp;

    /**
     * Should this metric skip noisy voxels?
     */
    protected boolean skip_noisy;

    /**
     * Will store the given clusters
     */
    protected ArrayList<Cluster> clusters;

    /**
     * Reference to the full image in case it is needed.
     */
    protected ImagePlusHyp ip;

    /**
     * @return The name of this metric.
     */
    public String getName() {

        return jclustering.Utils.getName(this);

    }

    /**
     * Builds a configuration {@link Panel} that will provide all the necessary
     * interfaces for the technique configuration. If implemented, the technique
     * class must also implement the necessary listeners.
     * <p>
     * 
     * Developers may use the classes provided in the {@link GUIUtils} static
     * methods.
     * 
     * @return {@code null} by default, or the appropriate {@link Panel} if
     *         implemented.
     */
    public JPanel getConfig() {
        if (jp == null) {
            jp = makeConfig();
        }
        return jp;
    }

    /**
     * This function is called only once and returns the configuration panel
     * that will be called by {@code #getConfig()} on each successive call.
     * Needs to be overridden by the extending classes.
     * 
     * @return The configuration panel returned by {@code #getConfig()}.
     */
    protected JPanel makeConfig() {
        return null;
    }

    /**
     * Performs the actual processing for this clustering technique. This class
     * returns a dynamic image with n + 1 frames, with n being the total number
     * of clusters formed, following the convention as follows:
     * <p>
     * <ul>
     * <li>From the first frame to the n-th frame, individual clusters are
     * represented</li>
     * <li>The last frame (n + 1) shows all the clusters in the image
     * simultaneously. The first cluster gets a value of 1, the second one a
     * value of 2... non-analyzed voxels get a value of 0.</li>
     * </ul>
     * 
     * In order to return this structure, the developer may just fill a regular
     * {@link ImagePlus} structure with each voxel containing the value of the
     * cluster that voxel belongs to (1-based) and then use the
     * {@link Utils#expand(ImagePlus, int)} method. The {@code int} value that
     * this method needs will always be (except weird cases) the one obtained
     * via {@code clusters.size()}.
     * <p>
     * <strong>Please note that the minimum cluster number must be 1, not 0.
     * </strong>
     * <p>
     * This class must also build an {@link ArrayList}, with each element being
     * a {@link Cluster} object.
     * 
     * @return A dynamic image representing each cluster following the previous
     *         convention.
     */
    public abstract ImagePlus process();

    /**
     * This helper method is the method called from the main class. It
     * initializes the local {@link Cluster} object every time a clustering
     * operation is called and returns the result of calling the
     * {@link #process()} method.
     * 
     * @return The resulting {@link ImagePlus} with the clustering data.
     */
    public ImagePlus compute() {

        init();

        return process();

    }

    /**
     * @return The {@code ClusteringMetric} used by this technique, if any
     */
    public ClusteringMetric getMetric() {

        return metric;

    }

    /**
     * Sets the current ClusteringMetric
     * 
     * @param m A new ClusteringMetric
     */
    public void setMetric(ClusteringMetric m) {

        metric = m;

    }

    /**
     * @return The clusters formed in this clustering technique. This method
     *         should be called after {@link #process()}, which must populate
     *         them.
     */
    public ArrayList<Cluster> getClusters() {
        return clusters;
    }

    /**
     * Setup method, as the constructor will always be called empty. Provides a
     * reference to the working image.
     * 
     * @param ip The working image.
     */
    public void setup(ImagePlusHyp ip) {

        this.ip = ip;
        init();

    }

    /**
     * Initializes the clusters ArrayList.
     */
    public void init() {
        this.clusters = new ArrayList<Cluster>();
    }

    @Override
    /**
     * This particular implementation takes care of a selector that lists
     * all the available metrics. It can be called in a class extending
     * this one as {@code super.itemStateChanged(arg0)}.
     */
    public void itemStateChanged(ItemEvent arg0) {

        Component c = (Component) arg0.getSource();
        String source = c.getName();

        if (source.equals("metrics")) {
            // Technique selector. Set the technique selected
            String t = ((Choice) c).getSelectedItem();
            metric = getClusteringMetric(t, ip);
        }

    }

    /**
     * Adds a {@link Choice} element containing all the available metrics to the
     * given panel.
     * 
     * @param jp The panel to be modified.
     */
    public void addMetricsToJPanel(JPanel jp) {

        Choice c = getMetricList(this, this.ip);
        jp.add(new JLabel("Select a technique: "));
        jp.add(c);

    }

    /**
     * Changes this techniques's behavior with respect to noisy voxels.
     * 
     * @param skip_noisy Boolean parameter stating whether noisy voxels should
     *            be discarded.
     */
    public void skipNoisy(boolean skip_noisy) {

        this.skip_noisy = skip_noisy;

    }

    /**
     * Helper method. Just calls {@link ImagePlusHyp#isNoise(double[])} method.
     * 
     * @param data The TAC to be tested.
     * @return true if the given TAC is noise with respect to this image.
     */
    public boolean isNoise(double[] data) {

        return ip.isNoise(data);

    }

    /**
     * Provides a safe way to get the {@link Cluster} at the {@code index}
     * position.
     * 
     * @param index The index for the cluster to be returned (1-based).
     * @return The cluster at the given position, or a new cluster if no cluster
     *         exists yet at that index.
     */
    public Cluster getClusterAt(int index) {

        Cluster res = null;

        // If I'm trying to index beyond the clusters already created,
        // create as many as necessary
        if (index >= clusters.size()) {
            for (int i = clusters.size(); i <= index; i++) {
                clusters.add(new Cluster());
            }
        }

        res = clusters.get(index - 1);

        return res;

    }

    /**
     * Provides a fast way to add a {@code double []} tac to a given cluster.
     * 
     * @param tac The TAC data.
     * @param cluster The index for the cluster into which to insert the data
     *            (1-based).
     */
    public void addTACtoCluster(double[] tac, int cluster) {

        Cluster c = getClusterAt(cluster);
        c.add(tac);

    }


    /**
     * Finds the cluster with a centroid as close as possible to the given TAC.
     * 
     * @param tac The TAC to be tested.
     * @return The index of the cluster with the closest centroid, or -1 if none
     *         is found.
     */
    public int getCloserClusterIndex(double[] tac) {

        int res = -1;
        double distance = Double.MAX_VALUE;

        // Iterate through the array and find the index for the
        // cluster which centroid is closer to the tac.
        int length = clusters.size();
        for (int i = 0; i < length; i++) {

            // If the cluster is empty, get the next one
            Cluster c = clusters.get(i);
            if (c.isEmpty())
                continue;

            // Compute the distance and update res accordingly
            double aux_distance = metric.distance(c.getCentroid(), tac);
            if (aux_distance < distance) {
                distance = aux_distance;
                res = i;
            }
        }

        return res;

    }

    /**
     * Creates a new cluster with the {@code double [] tac} as the centroid,
     * adds it to the cluster ArrayList and returns it.
     * 
     * @param tac The initial centroid for the cluster.
     * @return The newly created cluster.
     */
    public Cluster addCluster(double[] tac) {

        Cluster c = new Cluster(tac);
        clusters.add(c);

        return c;

    }

}
