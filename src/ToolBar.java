import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.event.*;
import java.io.File;

//import titech.image.dsp.*;

public class ToolBar extends JPanel {

	public static final int ICON_WIDTH=28; // 24 + 4 de margen
	public static final int ICON_HEIGHT=28;
	
	JCheckBox thumbsB;
	JCheckBox resizeB;
	JTextField widthB, heightB;
	JCheckBox ccorrectB;
	JCheckBox previewB;
	
    public ToolBar() {
			    	
		Dimension iconDim = new Dimension(ICON_WIDTH,ICON_HEIGHT);

		setLayout(new FlowLayout(FlowLayout.LEFT)); // left to right
		
		previewB = new JCheckBox("preview");
		previewB.setActionCommand("preview");
		//JButton loadB = new JButton(new //ImageIcon(this.getClass().getResource("/resources/loadp.png")));
		//loadB.setPreferredSize(iconDim);
		//loadB.setActionCommand("load");
		thumbsB = new JCheckBox("Thumbs");
		thumbsB.setActionCommand("thumbs");
		//thumbsB.addActionListener(this);
		
		resizeB = new JCheckBox("Resize & Overwrite");
		resizeB.setActionCommand("resize");
		//resizeB.addActionListener(this);
		
		widthB = new JTextField("800",4);
		heightB = new JTextField("800",4);

		ccorrectB = new JCheckBox("Color Correct");
		ccorrectB.setActionCommand("ccorrect");
		
		//loadB.addActionListener(this);
		
		
		add(previewB);
		add(thumbsB);
		add(resizeB);
		add(widthB);
		add(heightB);
		add(ccorrectB);
    }
	
	public boolean resize() {
		return resizeB.isSelected();
	}
	
	public boolean ccorrect() {
		return ccorrectB.isSelected();
	}
	
	public boolean thumbs() {
		return thumbsB.isSelected();
	}
	
	public int getWidth() {
		return Integer.parseInt(widthB.getText());
	}
	
	public int getHeight() {
		return Integer.parseInt(heightB.getText());
	}
	
	public boolean preview() {
		return previewB.isSelected();
	}

}


