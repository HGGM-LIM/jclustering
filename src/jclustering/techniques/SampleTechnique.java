package jclustering.techniques;

import java.awt.event.ItemEvent;
import javax.swing.JPanel;

/**
 * @author jmmateos
 * 
 */
public class SampleTechnique extends ClusteringTechnique {

    @Override
    public void process() {

        // Return a stack with the value of the frame of maximum intensity
        // for each voxel
        int[] dim = ip.getDimensions();

        for (int slice = 1; slice <= dim[3]; slice++) {
            for (int x = 0; x < dim[0]; x++) {
                for (int y = 0; y < dim[1]; y++) {

                    // Get TAC
                    double[] tac = ip.getTAC(x, y, slice);

                    // If is noise, skip
                    if (skip_noisy && isNoise(tac))
                        continue;

                    // Else, set the corresponding result
                    int n = _getMaxIndex(tac) + 1; // +1, min_cluster = 1.

                    // Set temporal result
                    addTACtoCluster(tac, x, y, slice, n);
                }
            }
        }
    }

    @Override
    protected JPanel makeConfig() {

        JPanel jp = new JPanel();
        addMetricsToJPanel(jp);

        return jp;

    }

    @Override
    public void itemStateChanged(ItemEvent arg0) {

        super.itemStateChanged(arg0);

    }

    private int _getMaxIndex(double[] d) {

        int res = 0;
        double aux = d[0];

        for (int i = 0; i < d.length; i++) {
            if (d[i] > aux) {
                res = i;
                aux = d[i];
            }
        }

        return res;
    }
}
