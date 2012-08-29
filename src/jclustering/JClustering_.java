package jclustering;

import java.awt.BorderLayout;
import java.awt.Button;
import java.awt.Choice;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.Label;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.ArrayList;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.ToolTipManager;

import jclustering.metrics.ClusteringMetric;
import jclustering.techniques.ClusteringTechnique;

import ij.IJ;
import ij.ImagePlus;
import ij.plugin.filter.PlugInFilter;
import ij.process.ImageProcessor;

import static jclustering.GUIUtils.*;
import static jclustering.Utils.*;
import static jclustering.Constants.NO_METRIC;
import static jclustering.Constants.NO_TECHNIQUE;

/**
 * <p>
 * JClustering ImageJ Plugin.
 * </p>
 * 
 * <p>
 * This plugin is intended to be used as a development framework for developing
 * clustering algorithms to be used in nuclear medicine dynamic studies.
 * Examples of said clustering algorithms are k-means, leader-follower,
 * Principal Component Analysis, Independent Component Analysis, Factorial
 * Analysis...
 * </p>
 * 
 * <p>
 * So far, the following algorithms are implemented: TODO Fill implemented
 * algorithms.
 * </p>
 * 
 * <p>
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation (http://www.gnu.org/licenses/gpl.txt).
 * </p>
 * 
 * <p>
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 * </p>
 * 
 * <p>
 * You should have received a copy of the GNU General Public License along with
 * this program; if not, write to the Free Software Foundation, Inc., 59 Temple
 * Place - Suite 330, Boston, MA 02111-1307, USA.
 * </p>
 * 
 * @author <a href="mailto:jmmateos@mce.hggm.es">Jos� Mar�a Mateos</a>.
 * 
 */
public class JClustering_ implements PlugInFilter, ActionListener,
        ItemListener, ComponentListener {

    private ImagePlusHyp iph;
    private ClusteringTechnique technique;
    private JPanel main_panel, tech_panel, met_panel;
    private JFrame f;
    private boolean skip_noisy = true;
    private String file_saving_format = null;
    private String file_saving_path = null;
    private String file_reading_path = null;

    @Override
    public void run(ImageProcessor ip) {

        // The "run" method builds the interface and shows the clustering
        // techniques and the metrics that are implemented. It does not
        // performs any calculation by itself, that is left to the listeners
        // for the interface elements.

        // Main window
        f = new JFrame("JClustering");
        f.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        // Main panel, with tabs
        JTabbedPane container = new JTabbedPane();
        // Main panel (contains initial menu).
        main_panel = createJPanel("main_panel", this);
        // Panel for the chosen clustering technique (if any)
        tech_panel = createJPanel("tech_panel", this);
        // Panel for the chosen metric (if any)
        met_panel = createJPanel("met_panel", this);
        // Panel for the bottom buttons
        JPanel button_panel = createJPanel("button_panel", this);

        // Default message for technique panel
        tech_panel.add(new Label(NO_TECHNIQUE));

        // Default message for metric panel
        met_panel.add(new Label(NO_METRIC));

        // Config main panel.
        // Add metrics
        Choice c = createChoices("techniques", getAllTechniques(), this);
        main_panel.setLayout(new GridLayout(5, 2, 5, 5));
        
        main_panel.add(createJLabel("Select a technique: ",
                "<html>HTML helper text</html>"));
        main_panel.add(c);
        
        // Add file saving logic
        main_panel.add(new JLabel("File saving format:"));
        String [] file_saving_choices = new String[] {"CSV", "PMOD",
                "tab-separated"};
        Choice c_file = createChoices("file_saving", file_saving_choices, this);
        file_saving_format = c_file.getItem(0);
        main_panel.add(c_file);
        
        main_panel.add(new JLabel("File saving path:"));        
        JButton jb_file = createJButton("Click here to choose a path", 
                "jb_file", this);        
        main_panel.add(jb_file);
        
        // Time vector reading logic
        String tvector_read_help = "<html>If you wish to choose a time " +
        		"vector to be used then saving the TACs to file, do so " +
        		"now.<br>" +        		
        		"This file should contain two columns (one for the frame " +
        		"starting time, other for the frame ending time), " +
        		"separated by a space, " +
        		"tab or other blank character.</html>";
        main_panel.add(createJLabel("Select a time vector file*:", 
                tvector_read_help));
        JButton jb_tvread = createJButton("Click here to choose a file",
                "jb_tvread", this);
        main_panel.add(jb_tvread);       

        // Add discard noisy voxels checkbox
        String dnv_help = "<html>When this box is checked, all the noisy"
                + " voxels in the image will be skipped. That improves both"
                + " the speed of the algorithm and the accuracy of the final"
                + " result.<p>Please note that this funcionatlity"
                + " will only work if the selected technique has implemented"
                + " it.</html>";
        main_panel.add(createJLabel("Skip noisy voxels:*", dnv_help));
        JCheckBox jcb_discard = new JCheckBox();
        jcb_discard.setSelected(skip_noisy);
        jcb_discard.setName("jcb_discard");
        jcb_discard.addItemListener(this);
        main_panel.add(jcb_discard);

        // Set the current technique to the first one shown
        technique = getClusteringTechnique(c.getItem(0), iph, skip_noisy);

        // Initialize panels contents
        _initializePanels();

        // Add panels to panel and panel to frame
        container.add(main_panel, "Main");
        container.add(tech_panel, "Technique");
        container.add(met_panel, "Metric");
        f.add(container, BorderLayout.PAGE_START);

        // Create buttons panel and make window visible
        Button b_ok = createButton("OK", this);
        Button b_cancel = createButton("Cancel", this);
        button_panel.add(b_ok);
        button_panel.add(b_cancel);
        f.add(button_panel, BorderLayout.PAGE_END);
        f.setVisible(true);

        // Center it on screen
        f.pack();
        Dimension dim = Toolkit.getDefaultToolkit().getScreenSize();
        f.setLocation((dim.width - f.getWidth()) / 2,
                (dim.height - f.getHeight()) / 2);

        // Config tooltips
        ToolTipManager.sharedInstance().setInitialDelay(200);
        ToolTipManager.sharedInstance().setDismissDelay(60000);

    }

    @Override
    public int setup(String arg, ImagePlus imp) {

        // Version number check.
        boolean not_required_version = IJ.versionLessThan("1.47a");
        if (not_required_version) {
            return DONE;
        }

        // Check if we have a HyperStack
        if (imp == null || !imp.isHyperStack()) {
            // No input image or input image not hyperstack
            IJ.error("jClustering works on hyperstacks.");
            return DONE;
        } else {
            // Go ahead
            this.iph = new ImagePlusHyp(imp);
            return DOES_ALL;
        }
    }

    /**
     * Close all windows and exits.
     */
    private void closeAll() {

        // Destroy main frame and its contents
        f.dispose();

    }

    /**
     * Implements the main method for the clustering process: calls the
     * {@link ClusteringTechnique#process()} method for the configured technique
     * and shows the result.
     */
    private void doProcess() {
    
        long start = System.currentTimeMillis();
    
        // Actual clustering operations and image showing.
        ImagePlus ip = technique.compute();
        ArrayList<Cluster> clusters = technique.getClusters();
        ip.show();
        // Show last frame of the image, which is the one containing all
        // clusters at once
        
        int last_frame = clusters.size() + 1;
        ip.setT(last_frame);
    
        // Set contrast (from 0 to last cluster number)        
        String contrast = String.format("setMinAndMax(0, %d)", last_frame - 1);
        IJ.runMacro(contrast);
    
        // Display elapsed time.
        long end = System.currentTimeMillis();
        IJ.log(String.format("Time elapsed: %.3f seconds.", 
                (double) (end - start) / 1000.0));
        
        // Save data to file if the path has been selected
        if (file_saving_path != null) {
            
            // Get time vector
            // TODO It would be great if this information could be extracted
            // from the image.
            TimeVectorReader tvr = new TimeVectorReader(file_reading_path);
            double [][] t = tvr.getTimeVector();
            
            FileSaver fs = new FileSaver(file_saving_format, clusters, t);
            
            fs.save(file_saving_path);
            
        }
    
    }

    @Override
    public void actionPerformed(ActionEvent e) {

        Component c = (Component) e.getSource();
        String source = c.getName();

        if (source.equals("OK")) {
            doProcess();
        } else if (source.equals("Cancel")) {
            closeAll();
        } else if (source.equals("jb_file")) { 
            // file saving
            JFileChooser jfc = new JFileChooser(file_saving_path);
            jfc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            int val = jfc.showOpenDialog(f);
            
            if (val == JFileChooser.APPROVE_OPTION) {
                file_saving_path = jfc.getSelectedFile().getAbsolutePath();
                ((JButton)c).setText(file_saving_path);
                ((JButton)c).setToolTipText(file_saving_path);
            }            
        } else if (source.equals("jb_tvread")) { 
            // file reading for time vector
            JFileChooser jfc = new JFileChooser(file_reading_path);
            int val = jfc.showOpenDialog(f);
            
            if (val == JFileChooser.APPROVE_OPTION) {
                file_reading_path = jfc.getSelectedFile().getAbsolutePath();
                ((JButton)c).setText(file_reading_path);
                ((JButton)c).setToolTipText(file_reading_path);                
            }
            
        }

    }

    @Override
    public void itemStateChanged(ItemEvent arg0) {

        Component c = (Component) arg0.getSource();
        String source = c.getName();

        if (source.equals("techniques")) {
            // Technique selector. Set the technique selected
            String t = ((Choice) c).getSelectedItem();
            technique = getClusteringTechnique(t, iph, skip_noisy);

            // Re-init panels
            _initializePanels();

        } else if (source.equals("jcb_discard")) {
            // Discard noisy voxels checkbox.
            skip_noisy = ((JCheckBox) c).isSelected();
            technique.skipNoisy(skip_noisy);
        } else if (source.equals("file_saving")) {
            // File saving Choice object
            file_saving_format = ((Choice)c).getSelectedItem();            
        }

    }

    @Override
    public void componentShown(ComponentEvent arg0) {

        // This method comes into action then the user changes between
        // tabs. The main use of this method is to set the appropriate
        // panels in place.

        Component c = (Component) arg0.getSource();
        String source = c.getName();

        ClusteringMetric metric = technique.getMetric();

        if (source.equals("met_panel") && metric != null) {
            // User has changed to the metric panel
            JPanel p;
            if ((p = metric.getConfig()) != null) {
                setPanel(met_panel, p, f);
            } else {
                resetMetricPanel(met_panel);
            }

        }

    }

    /**
     * This functions initializes tech_panel and met_panel with the
     * corresponding panels from the technique and the metric, if available.
     */
    private void _initializePanels() {

        JPanel p = technique.getConfig();
        if (p != null) {
            setPanel(tech_panel, p, f);
        } else {
            resetTechPanel(tech_panel);
        }

        ClusteringMetric m = technique.getMetric();

        if (m != null) {
            JPanel p2 = m.getConfig();
            if (p2 != null) {
                setPanel(met_panel, p2, f);
            } else {
                resetMetricPanel(met_panel);
            }
        } else {
            resetMetricPanel(met_panel);
        }
    }


    // Unimplemented
    public void componentHidden(ComponentEvent arg0) {}
    public void componentMoved(ComponentEvent arg0) {}
    public void componentResized(ComponentEvent arg0) {}
    

}