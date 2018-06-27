package GUI;

import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ContainerEvent;
import java.awt.event.ContainerListener;
import java.io.File;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.filechooser.FileFilter;

import Utils.Util;

public class CustomFileDirChooser extends JPanel {

	/**
	 * 
	 */
	private static final long serialVersionUID = -8065078553900727212L;

	JButton go;

	JFileChooser chooser;
	JTextField textField;
	JLabel label;
	boolean isDir = false;
	JFrame aframe = null;
	boolean dataDir = false;
	FileFilter filter = null;
	public CustomFileDirChooser(JTextField resultTextField, JLabel resultLabel, boolean directory, JFrame frame,
			boolean dataDire, FileFilter filterr) {
		filter = filterr;
		// TODO Auto-generated constructor stub
		this.dataDir = dataDire;
		aframe = frame;
		isDir = directory;
		textField = resultTextField;
		label = resultLabel;
		go = new JButton();
		go.setPreferredSize(new Dimension(20, 20));
		go.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent arg0) {
				// TODO Auto-generated method stub
				chooser = new JFileChooser();
				if (filter != null)
					chooser.setFileFilter(filter);
				if (dataDir) {
					chooser.setCurrentDirectory(new File(ALPACAManager.getInstance().dataDirectory));
				} else
					chooser.setCurrentDirectory(new java.io.File("."));
				chooser.setDialogTitle("Choose your file/folder");
				if (isDir)
					chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
				else
					chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
				//
				// disable the "All files" option.
				//
				chooser.setAcceptAllFileFilterUsed(false);
				//
				if (chooser.showOpenDialog(aframe) == JFileChooser.APPROVE_OPTION) {
					if (textField != null)
						textField.setText(chooser.getSelectedFile().getPath());
					if (label != null) {
						// shorten the path on label
						System.out.println();
						label.setText(chooser.getSelectedFile().getPath());
					}
				} else {
					// System.out.println("No Selection ");
				}
			}
		});
		if (textField != null)
			add(textField);
		add(go);
		ImageIcon fileIcon = Util.createImageIcon("/img/if_folder-close_173016.png", "folder icon");
		if (fileIcon != null) {
			go.setIcon(fileIcon);
		} else {
			go.setText("NoIMG");
		}
	}

	@Override
	public void setEnabled(boolean enabled) {
		// TODO Auto-generated method stub
		super.setEnabled(enabled);
		go.setEnabled(enabled);
	}

	// public Dimension getPreferredSize() {
	// return new Dimension(200, 200);
	// }
	//
	// public static void main(String s[]) {
	// JFrame frame = new JFrame("");
	// DemoJFileChooser panel = new DemoJFileChooser();
	// frame.addWindowListener(new WindowAdapter() {
	// public void windowClosing(WindowEvent e) {
	// System.exit(0);
	// }
	// });
	// frame.getContentPane().add(panel, "Center");
	// frame.setSize(panel.getPreferredSize());
	// frame.setVisible(true);
	// }

}
