package jclustering.techniques;

import static jclustering.GUIUtils.getMetricList;
import static jclustering.Utils.getClusteringMetric;
import ij.ImagePlus;

import java.awt.Component;
import java.awt.Panel;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.ArrayList;

import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;

import jclustering.Cluster;
import jclustering.GUIUtils;
import jclustering.ImagePlusHyp;
import jclustering.Voxel;
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
     * Will store the given clusters
     */
    protected ArrayList<Cluster> clusters;

    /**
     * Reference to the full image in case it is needed.
     */
    protected ImagePlusHyp ip;
    
    /**
     * The additional information string. It is null by default, new techniques
     * should fill it if needed.
     */
    protected String[] additionalInfo = null;

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
     * Performs the actual processing for this clustering technique. This 
     * method fills an {@link ArrayList} object containing objects of the
     * {@link Cluster} class. Each cluster contains the TACs belonging to it.
     * As the {@code Cluster} object remembers the coordinates of every 
     * voxel that has been added to it, there is enough information to build
     * a {@link ImagePlus} for representation then the processing is finished.
     */
    public abstract void process();

    /**
     * This helper method is the method called from the main class. It
     * initializes the local {@link Cluster} object every time a clustering
     * operation is called and calls the main {@code process()} method.
     */
    public void compute() {

        init();
        
        if (metric != null)             
            metric.init();        

        process();

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
            String t = (String)((JComboBox) c).getSelectedItem();
            metric = getClusteringMetric(t, ip);
        }

    }

    /**
     * Adds a {@link JComboBox} element containing all the available metrics to the
     * given panel.
     * 
     * @param jp The panel to be modified.
     */
    public void addMetricsToJPanel(JPanel jp) {

        JComboBox c = getMetricList(this, this.ip);
        jp.add(new JLabel("Select a metric: "));
        jp.add(c);

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
     * @param x X-coordinate for added TAC.
     * @param y Y-coordinate for added TAC.
     * @param slice Slice (1-based) for added TAC.
     * @param cluster The index for the cluster into which to insert the data
     *            (1-based).
     */
    public void addTACtoCluster(double[] tac, int x, int y, int slice, 
            int cluster) {

        Cluster c = getClusterAt(cluster);
        c.add(tac, x, y, slice);

    }
    
    /**
     * Provides a fast way to add a {@link Voxel} to a given cluster.
     * 
     * @param v The voxel to be added.
     * @param cluster The index for the cluster into which to insert data
     * (1-based).
     */
    public void addTACtoCluster(Voxel v, int cluster) {
        
        addTACtoCluster(v.tac, v.x, v.y, v.slice, cluster);
        
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
    
    /**
     * Returns additional information generated by this technique in a String
     * array.
     * @return <p>An even-length String array with the following structure:</p>
     * <ul>
     *          <li>
     *          i: the suggested name for the file used to store this info.
     *          </li>
     *          <li>
     *          i+1: the actual contents of the additional information.
     *          </li>
     * </ul>
     * 
     * where <code>i</code> starts at 0. All the file names occupy the 
     * even-numbered indexes of the array. File names cannot be 
     * <code>null</code>.
     */
    public String [] getAdditionalInfo() {
        
        return additionalInfo;
        
    }

}
