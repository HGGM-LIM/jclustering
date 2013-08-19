package jclustering;

import java.util.Iterator;

import static jclustering.MathUtils.isMasked;

/**
 * Provides an {@link Iterator} for an {@link ImagePlusHyp} object. This is
 * the recommended way to iterate through all the voxels in a given image, as
 * this does not require the user to know its dimensions and will not return
 * voxels that have been masked previously, therefore diminishing the size of
 * the clustering problem to solve. 
 * 
 * @author <a href="mailto:jmmateos@mce.hggm.es">José María Mateos</a>.
 *
 */
public class ImagePlusHypIterator implements Iterator<Voxel> {
    
    // Data
    private ImagePlusHyp ip;
    
    // Current pointers
    private int x, y, slice;
    
    // Limits
    private int x_max, y_max, slice_max;
    
    // The voxel to return
    private Voxel v = null;
    
    /**
     * Public constructor.
     * @param ip The {@link ImagePlusHyp} object to be iterated.
     */
    public ImagePlusHypIterator(ImagePlusHyp ip) {
        
        this.ip = ip;
        
        // Set limits
        int [] dim = ip.getDimensions();
        x_max = dim[0];
        y_max = dim[1];
        slice_max = dim[3];
        
        // Init first pointer
        x = 0;
        y = 0;
        slice = 1;
        
        // Init first voxel reference
        _setNextVoxel();
        
    }

    @Override
    public boolean hasNext() {
        return (_withinLimits());
    }
    
    private boolean _withinLimits() {
        return (x < x_max) && (y < y_max) && (slice <= slice_max);
    }

    @Override
    public Voxel next() {
                
        Voxel res = new Voxel(v.x, v.y, v.slice, v.tac);
        _setNextVoxel();
        return res;        
        
    }
    
    private void _setNextVoxel() {
        
        boolean found = false;
        
        while(_withinLimits() && !found) {
            v = new Voxel(x, y, slice, ip.getTAC(x, y, slice));
            _updatePointers();
            if (!isMasked(v, ip.CALZERO)) 
                found = true;                
        }
        
    }

    @Override
    public void remove() throws UnsupportedOperationException {
        
        throw new UnsupportedOperationException();

    }
    
    /*
     * Update the pointers to address the next Voxel
     */
    private void _updatePointers() {
        
        // First, update x
        x++;
        
        // Am I out of bounds? Go to next row and start column
        if (x == x_max) {
            x = 0;
            y++;
            
            // Out of bounds again? Start row and jump to next slice
            if (y == y_max) {
                y = 0;
                slice++;
            }
        }        
    }
}
