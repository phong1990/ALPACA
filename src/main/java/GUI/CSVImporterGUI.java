package GUI;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SpringLayout;
import javax.swing.SwingConstants;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.filechooser.FileNameExtensionFilter;

public class CSVImporterGUI extends JDialog {
	/**
	 * 
	 */
	private static final long serialVersionUID = -1571306826858094417L;
	/**
	 * 
	 */
	protected JButton doitButton = null;
	protected JLabel noticeLabel = null;
	protected JTextField csvLocation = null;
	protected JTextField outfileLocation = null;

	public CSVImporterGUI(JFrame aFrame) {
		super(aFrame, true);

		this.setTitle("Populate a data folder with reviews from a csv data file");
		outfileLocation = new JTextField();
		outfileLocation.setPreferredSize(new Dimension(200, 20));
		CustomFileDirChooser outFileChooser = new CustomFileDirChooser(outfileLocation, null, true, aFrame, false,null);
		JPanel contentPanel = new JPanel(new SpringLayout());
		noticeLabel = new JLabel(" ");
		noticeLabel.setForeground(Color.RED);

		csvLocation = new JTextField();
		csvLocation.setPreferredSize(new Dimension(200, 20));
		CustomFileDirChooser csvFileChooser = new CustomFileDirChooser(csvLocation, null, false, aFrame, false,
				new FileNameExtensionFilter("csv file", "csv"));
		csvLocation.setEnabled(false);

		contentPanel.add(new JLabel("Choose the csv file", SwingConstants.CENTER));
		contentPanel.add(csvFileChooser);
		contentPanel.add(new JLabel("Expands patterns to this location: ", SwingConstants.CENTER));
		contentPanel.add(outFileChooser);

		SpringUtilities.makeCompactGrid(contentPanel, 2, 2, // rows, cols
				6, 6, // initX, initY
				6, 6); // xPad, yPad
		// do it button
		JPanel DoitPanel = new JPanel(new BorderLayout());
		doitButton = new JButton("Click here to begin");
		doitButton.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				// TODO Auto-generated method stub
				boolean start = true;

				String csvFile = csvLocation.getText();
				File tmpFile = new File(csvFile);
				if (tmpFile.isDirectory()) {
					noticeLabel.setText("Notice: please select output file");
					csvFile = null;
					start = false;
				} else {
					File file = tmpFile.getParentFile();
					if (!file.exists()) {
						noticeLabel.setText("Notice: please select output file");
						csvFile = null;
						start = false;
					}

				}
				String outFile = outfileLocation.getText();
				tmpFile = new File(outFile);
				if (!tmpFile.isDirectory()) {
					noticeLabel.setText("Notice: please select output data folder");
					outFile = null;
					start = false;
				}
				if (start == true) {
					// ALPACAManager.getInstance().preprocessing();
					ALPACAManager.getInstance().startDATAFolderMakerThread(csvFile, outFile);
					clearAndHide();
				}
			}
		});
		DoitPanel.add(doitButton, BorderLayout.CENTER);
		DoitPanel.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createTitledBorder("Setup"),
				BorderFactory.createEmptyBorder(5, 5, 5, 5)));
		JPanel borderPanel = new JPanel(new BorderLayout());
		borderPanel.add(contentPanel, BorderLayout.PAGE_START);
		borderPanel.add(noticeLabel, BorderLayout.CENTER);
		borderPanel.add(DoitPanel, BorderLayout.PAGE_END);
		setContentPane(borderPanel);
		// Handle window closing correctly.
		setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
		addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent we) {
				/*
				 * Instead of directly closing the window, we're going to change the
				 * JOptionPane's value property.
				 */
				clearAndHide();
			}
		});
	}

	/** This method clears the dialog and hides it. */
	public void clearAndHide() {
		setVisible(false);
	}
}
