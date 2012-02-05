import java.awt.*;
import java.awt.image.*;
import java.awt.event.*;
import javax.swing.event.*;
import java.io.*;
import java.util.Vector;
import javax.media.jai.*;
import javax.swing.*;
import titech.file.*;
import titech.image.*;
import titech.image.dsp.*;

public class IconBrowser extends ScrollableJPanel implements ActionListener {

	public static final int THUMB_LONG=80;
	public static final int THUMB_SHORT=60;
	
	public static final String[] rotations = new String[] 
		{ "---", "right", "left", "flip" };

	public void populate(File[] fileList) {
		if (fileList == null) return;
		// always empty before adding
		removeAll();
		for (int i=0;i<fileList.length;i++) {
			try {
				PlanarImage pimg = JAI.create("fileload", fileList[i].getAbsolutePath());
				pimg = adjustImage(pimg, THUMB_LONG, THUMB_SHORT);				
				JButton b = new JButton(new ImageIcon(pimg.getAsBufferedImage()));
				JComboBox rcombo = new JComboBox(rotations);
				b.setActionCommand(""+i);
				b.addActionListener(this);
				
				
				// LAYOUT
			    // --------------------------------------------
			    GridBagLayout gridbag = new GridBagLayout();
			    GridBagConstraints cgb = new GridBagConstraints();
			    cgb.insets = new Insets(2, 2, 2, 2);
			    cgb.weightx = 1.0;
			    cgb.gridheight = 1;
			    cgb.gridwidth = 1;
			    cgb.gridwidth = GridBagConstraints.REMAINDER;
			    gridbag.setConstraints(b, cgb);
			    gridbag.setConstraints(rcombo, cgb);
		
				JPanel icon = new JPanel(gridbag);
				icon.add(b);
				icon.add(rcombo);
				
				add(icon);
			} catch (Exception e) {
				System.err.println("populateIconBrowser: "+e);
			}
		}
		getParent().validate();
	}	

	public PlanarImage adjustImage(PlanarImage pimg, int l, int s) {
		int resultWidth = l, resultHeight=l;
		
		if (pimg.getWidth()>pimg.getHeight()) {
			resultWidth = l; resultHeight = s;
		} else if (pimg.getWidth()<pimg.getHeight()) {
			resultWidth = s; resultHeight = l;
		}

		RenderedOp rop = COps.scale(pimg,resultWidth,resultHeight);

		// adjust type if necessary
		if (rop.getSampleModel().getDataType() != DataBuffer.TYPE_BYTE) {


		//switch (visualizationMode) {

			//case ABSOLUTE:
				rop = COps.absolute(rop);
				System.out.println("... displaying float ...\n");

				//break;

			//case RESCALE:
				//rop = COps.rescale(rop, 128f, 0.5f);

				//break;

			//case INVERSE:
				//prep = COps.reformat(COps.absolute(im), DataBuffer.TYPE_BYTE);

				//return COps.invert(prep);

			//default:
				//prep = COps.absolute(im);
			rop =COps.reformat(rop, DataBuffer.TYPE_BYTE); 
		}
				
		return rop;
	}

	/**
	 * Implementacion del ActionListener
	 *
	 * @param  e  Description of the Parameter
	 */
	public void actionPerformed(ActionEvent e) {
		JComponent src = (JComponent) (e.getSource());
		String s = e.getActionCommand();
		
		System.out.println(s);
	}
		
}

