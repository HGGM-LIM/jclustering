package jclustering.metrics;

import java.awt.Label;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.util.Arrays;

import javax.swing.JPanel;
import javax.swing.JTextField;

/**
 * This {@link ClusteringMetric} implements a <a
 * href="http://en.wikipedia.org/wiki/Norm_(mathematics)#p-norm">p-norm</a>
 * distance.
 * <p>
 * The only variable that this metric needs to set is {@code p}, which has a
 * default value of {@code 2.0}. In this case, the distance is an <a
 * href="http://en.wikipedia.org/wiki/Euclidean_distance">Euclidean
 * distance</a>.
 * 
 * @author <a href="mailto:jmmateos@mce.hggm.es">José María Mateos</a>.
 */
public class PNorm extends ClusteringMetric implements FocusListener {

    private double p = 2.0;
    private JTextField jt;

    @Override
    /**
     * Computes the p-norm between the {@code centroid} and {@code data} 
     * vectors.
     */
    public double distance(double[] centroid, double[] data) {

        if (Arrays.equals(centroid, data) || centroid.length != data.length)
            return 0.0;

        double result = 0.0;

        for (int i = 0; i < data.length; i++) {
            result += Math.pow(Math.abs(centroid[i] - data[i]), p);
        }

        return Math.pow(result, 1.0 / p);
    }

    @Override
    /**
     * Build a JTextField to introduce the new value for p.
     * <p>
     * p is set when the JTextField loses the focus thanks to
     * this class implementing the FocusListener interface.
     */
    public JPanel makeConfig() {

        JPanel jp = new JPanel();
        jp.add(new Label("Insert new p value:"));
        jt = new JTextField(Double.toString(p));
        jt.setColumns(5);
        jt.addFocusListener(this);
        jp.add(jt);
        return jp;

    }

    @Override
    /**
     * Assigns the p value when the JTextField loses the focus.
     */
    public void focusLost(FocusEvent e) {

        try {
            p = Double.parseDouble(((JTextField) e.getComponent()).getText());
        } catch (NumberFormatException ex) {
            // Wrong input. Back to default value.
            p = 2.0;
        }

        jt.setText(Double.toString(p));

    }

    @Override
    /**
     * Just selects all the text inside the JTextField, for usability purposes.
     */
    public void focusGained(FocusEvent e) {

        jt.selectAll();

    }

}
