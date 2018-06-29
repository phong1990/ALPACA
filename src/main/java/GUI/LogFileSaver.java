package GUI;

import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.FileWriter;
import java.io.IOException;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.filechooser.FileFilter;

import Utils.Util;

public class LogFileSaver extends JPanel {

	/**
	 * 
	 */
	private static final long serialVersionUID = -8065078553900727212L;

	JButton go;

	JFileChooser chooser;
	JFrame aframe = null;
	FileFilter filter = null;

	public LogFileSaver(JFrame frame, FileFilter filterr) {
		filter = filterr;
		// TODO Auto-generated constructor stub
		aframe = frame;
		go = new JButton();
		go.setPreferredSize(new Dimension(20, 20));
		go.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent arg0) {
				// TODO Auto-generated method stub
				chooser = new JFileChooser();
				if (filter != null)
					chooser.setFileFilter(filter);
				chooser.setCurrentDirectory(new java.io.File("."));
				chooser.setDialogTitle("Choose your file/folder");
				chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
				//
				// disable the "All files" option.
				//
				chooser.setAcceptAllFileFilterUsed(false);
				//
				if (chooser.showOpenDialog(aframe) == JFileChooser.APPROVE_OPTION) {
					FileWriter fw = null;
					try {
						fw = new FileWriter(chooser.getSelectedFile());
						fw.write(ALPACAManager.getInstance().getLogContent());
						System.out.println(">> Exported log file to: " + chooser.getSelectedFile());
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					} finally {
						if (fw != null)
							try {
								fw.close();
							} catch (IOException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
					}
				} else {
					// System.out.println("No Selection ");
				}
			}
		});
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
