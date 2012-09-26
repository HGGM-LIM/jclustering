package jclustering;

import java.util.Iterator;

/**
 * Provides an {@link Iterator} for an {@link ImagePlusHyp} object.
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
        slice_max = dim[3] + 1; // 1-based
        
        // Init first pointer
        x = 0;
        y = 0;
        slice = 1;
        
    }

    @Override
    public boolean hasNext() {
        return x < x_max && y < y_max && slice < slice_max;
    }

    @Override
    public Voxel next() {
        
        Voxel v = new Voxel(x, y, slice, ip.getTAC(x, y, slice));
        _updatePointers();
        return v;
        
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
