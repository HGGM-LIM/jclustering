package jclustering.metrics;

import java.awt.Panel;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;

import javax.swing.JPanel;

import jclustering.GUIUtils;
import jclustering.ImagePlusHyp;
import jclustering.Voxel;
import jclustering.techniques.ClusteringTechnique;

/**
 * This abstract class provides a template with the basic functions that a
 * metric should implement, specially the distance(double [], double[]) method.
 * 
 * @author <a href="mailto:jmmateos@mce.hggm.es">José María Mateos</a>.
 * 
 */
public abstract class ClusteringMetric implements ActionListener, ItemListener {

    /**
     * Panel to be returned if this metric needs configuration.
     */
    protected JPanel jp;

    /**
     * Reference to the full image, if needed.
     */
    protected ImagePlusHyp ip;
    
    /**
     * Should we skip noisy voxels? Some metrics might need to know this.
     */
    protected boolean skip_noisy;

    /**
     * Computes the distance between to TACs according to this particular
     * metric. Extending classes must implement this method.
     *
     * @param data The TAC to compare.
     * @param centroid The cluster centroid.
     *
     * @return The distance between both arrays.
     */
    public abstract double distance(double[] data, double[] centroid);
    
    /**
     * Provides a shortcut for computing the distance between a {@link Voxel}
     * and any TAC.
     * @param v The Voxel which distance is to be computed.
     * @param centroid The cluster centroid.
     * @return The distance between both TACs.
     */
    public double distance(Voxel v, double[] centroid) {
        
        return distance(v.tac, centroid);
        
    }

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
     * that will be called by {@link #getConfig()} on each successive call.
     * Needs to be overridden by the extending classes.
     * 
     * @return The configuration panel returned by {@link #getConfig()}.
     */
    public JPanel makeConfig() {
        return null;
    }

    /**
     * Setup method, as the constructor will always be called empty. Provides a
     * reference to the working image.
     * 
     * @param ip The working image.
     */
    public void setup(ImagePlusHyp ip) {

        this.ip = ip;        

    }
    
    /**
     * Ease of access for the {@link ImagePlusHyp#isNoise(double[])} method.
     * @param tac The TAC to be tested.
     * @return true if it noise.
     */
    public boolean isNoise(double [] tac) {
        return ip.isNoise(tac);
    }

    /**
     * Ease of access for the {@link ImagePlusHyp#isNoise(double[])} method.
     * @param v The voxel to be tested.
     * @return true if it noise.
     */
    public boolean isNoise(Voxel v) {
        return ip.isNoise(v.tac);
    }

    /**
     * If the metric needs some previous computations, it should override
     * this method. This is called just once prior to 
     * {@link ClusteringTechnique#process()}. By default it does nothing.
     */
    public void init() {}
    
    /**
     * Sets the skip_noisy variable for this metric.
     * @param skip_noisy Tells the init process whether to skip noisy voxels.
     */
    public void skip_noisy(boolean skip_noisy) {
        this.skip_noisy = skip_noisy;
    }

    // These two methods are overridden so that the extending classes don't
    // have to deal with them if they don't have to.
    public void actionPerformed(ActionEvent e) {
    }

    public void itemStateChanged(ItemEvent e) {
    }

}
