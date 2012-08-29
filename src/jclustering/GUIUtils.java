package jclustering;

import java.awt.Button;
import java.awt.Choice;
import java.awt.Label;
import java.awt.LayoutManager;
import java.awt.event.ActionListener;
import java.awt.event.ComponentListener;
import java.awt.event.FocusListener;
import java.awt.event.ItemListener;
import java.util.ArrayList;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import jclustering.metrics.ClusteringMetric;
import jclustering.techniques.ClusteringTechnique;

import static jclustering.Constants.NO_METRIC;
import static jclustering.Constants.NO_TECHNIQUE;
import static jclustering.Utils.getAllMetrics;
import static jclustering.Utils.getClusteringMetric;

/**
 * Auxiliar class with static utility methods for GUI creation
 * 
 * @author <a href="mailto:jmmateos@mce.hggm.es">José María Mateos</a>.
 * 
 */
public class GUIUtils {

    /**
     * Auxiliar method for creating a named JPanel object.
     * 
     * @param name JPanel name.
     * @param lm Panel LayoutManager
     * @param cl A ComponentListner for handling events
     * @return The named JPanel.
     */
    public static JPanel createJPanel(String name, LayoutManager lm,
            ComponentListener cl) {

        JPanel jp;

        if (lm != null) {
            jp = new JPanel(lm);
        } else {
            jp = new JPanel();
        }

        jp.setName(name);
        jp.addComponentListener(cl);

        return jp;

    }

    /**
     * Auxiliar method for creating a named JPanel object.
     * 
     * @param name JPanel name.
     * @param cl A ComponentListner for handling events
     * @return The named JPanel.
     */
    public static JPanel createJPanel(String name, ComponentListener cl) {

        return createJPanel(name, null, cl);

    }

    /**
     * Creates a {@link Button} with a given label used as name and an 
     * ActionListener.
     * 
     * @param name The name and label for the button.
     * @param a An ActionListener for action performing.
     * @return The created {@link Button}.
     */
    public static Button createButton(String name, ActionListener a) {

        return createButton(name, name, a);

    }
    
    
    /**
     * Creates a {@link Button} with a given text, a given name and a given
     * ActionListener.
     * 
     * @param text The label for the button.
     * @param name The name for the button.
     * @param a An ActionListener for action performing.
     * @return The created {@link Button}.
     */
    public static Button createButton(String text, String name, 
            ActionListener a) {
        
        Button b = new Button(text);
        b.setName(name);
        b.addActionListener(a);

        return b;
        
    }
    /**
     * Creates a {@link JButton} with a given text, a given name and a given
     * ActionListener.
     * 
     * @param text The label for the button.
     * @param name The name for the button.
     * @param a An ActionListener for action performing.
     * @return The created {@link JButton}.    
     */
    public static JButton createJButton(String text, String name, 
            ActionListener a) {
        
        JButton jb = new JButton(text);
        jb.setName(name);
        jb.addActionListener(a);
        
        return jb;
        
    }
    
    /**
     * Creates a {@link Button} with a given label used as name and an 
     * ActionListener.
     * 
     * @param text The name and label for the button.
     * @param a An ActionListener for action performing.
     * @return The created {@link Button}.
     */
    public static JButton createJButton(String text, ActionListener a) {
                
        return createJButton(text, text, a);
        
    }

    /**
     * Creates a Choice dropdown list.
     * 
     * @param name The name for the dropdown list.
     * @param values The values to be shown.
     * @param i An ItemListener to wait for an action.
     * @return The Choice list.
     */
    public static Choice createChoices(String name, String[] values,
            ItemListener i) {

        Choice c = new Choice();
        for (String v : values) {
            c.add(v);
        }
        c.setName(name);
        c.addItemListener(i);

        return c;
    }

    /**
     * Creates a Choice dropdown list.
     * 
     * @param name The name for the dropdown list.
     * @param values The values to be shown.
     * @param i An ItemListener to wait for an action.
     * @return The Choice list.
     */
    public static Choice createChoices(String name, ArrayList<String> values,
            ItemListener i) {

        String[] val = new String[values.size()];
        val = values.toArray(val);
        return createChoices(name, val, i);

    }

    /**
     * Creates a JLabel with a given text and a given help message
     * 
     * @param text Text for the label
     * @param help Help text that will appear when user hovers mouse over label
     * @return Label
     */
    public static JLabel createJLabel(String text, String help) {

        JLabel l = new JLabel(text);
        l.setToolTipText(help);

        return l;

    }
    
    /**
     * Creates a new JTextField with the given name, integer value and adds
     * a FocusListener.
     * @param name JTextField name.
     * @param value JTextField value.
     * @param fl FocusListener for this object's events
     * @return The newly formed JTextField.
     */
    public static JTextField createJTextField(String name, 
            int value, FocusListener fl) {
        
        JTextField jt = _createJTextField(name, fl);
        jt.setText(Integer.toString(value));
        
        return jt;
        
    }
    
    /**
     * Creates a new JTextField with the given name, double value and adds
     * a FocusListener.
     * @param name JTextField name.
     * @param value JTextField value.
     * @param fl FocusListener for this object's events
     * @return The newly formed JTextField.
     */    
    public static JTextField createJTextField(String name, double value,
            FocusListener fl) {
        
        JTextField jt = _createJTextField(name, fl);
        jt.setText(Double.toString(value));
        
        return jt;
        
    }
    
    /*
     * Helper method for JTextField creation.
     */
    private static JTextField _createJTextField(String name, 
            FocusListener fl) {
        
        JTextField jt = new JTextField();
        jt.setName(name);
        jt.addFocusListener(fl);
        
        return jt;
        
    }
    

    /**
     * Removes all the elements from the given panel and restores the default
     * message.
     * 
     * @param p The panel to be reset.
     */
    public static void resetTechPanel(JPanel p) {

        _resetPanel(p, NO_TECHNIQUE);

    }

    /**
     * Removes all the elements from the given panel and restores the default
     * message.
     * 
     * @param p The panel to be reset.
     */
    public static void resetMetricPanel(JPanel p) {

        _resetPanel(p, NO_METRIC);

    }

    /**
     * Private helper method for the reset*Panel methods.
     * 
     * @param p The panel to be reset.
     * @param s The message to be set in the label.
     */
    private static void _resetPanel(JPanel p, String s) {

        p.removeAll();
        p.add(new Label(s));
        p.updateUI();

    }

    /**
     * Sets a panel inside a panel and updates the size of the containing
     * window.
     * 
     * @param parent The parent panel.
     * @param child The panel to set inside the parent one.
     * @param f The frame containing everything.
     */
    public static void setPanel(JPanel parent, JPanel child, JFrame f) {

        parent.removeAll();
        parent.add(child);
        parent.updateUI();
        f.pack();

    }

    /**
     * Returns a {@link Choice} of {@link ClusteringMetric} objects to be used
     * inside the {@link ClusteringTechnique} {@code t}.
     * 
     * @param t The {@link ClusteringTechnique} to host the given
     *            {@link ClusteringMetric}.
     * @param ip A reference to the working image.
     * @return A {@link Choice} object.
     */
    public static Choice getMetricList(ClusteringTechnique t, ImagePlusHyp ip) {

        Choice c = createChoices("metrics", getAllMetrics(), t);
        t.setMetric(getClusteringMetric(c.getItem(0), ip));

        return c;

    }

}
