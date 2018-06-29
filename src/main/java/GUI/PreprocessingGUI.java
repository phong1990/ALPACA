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
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SpringLayout;


public class PreprocessingGUI extends JDialog {
	/**
	 * 
	 */
	private static final long serialVersionUID = 4534770043547171279L;
	/**
	 * 
	 */
	protected JButton doitButton = null;
	protected JCheckBox word2vecCheckBox = null;
	protected JCheckBox potentialPattCheckBox = null;
	protected JTextField addfileLocation = null;
	protected JLabel noticeLabel = null;

	public PreprocessingGUI(JFrame aFrame) {
        super(aFrame, true);
		this.setTitle("Pre-process data");
		word2vecCheckBox = new JCheckBox("Training word2vec model (you can add additional text file ->)");
		word2vecCheckBox.setSelected(true);
		potentialPattCheckBox = new JCheckBox("Extracting potential patterns (can be VERY slow)");
		potentialPattCheckBox.setSelected(true);
		addfileLocation = new JTextField();
		addfileLocation.setPreferredSize(new Dimension(200, 20));
		CustomFileDirChooser additionalTextFileChooser = new CustomFileDirChooser(addfileLocation, null, false,aFrame, true,null);
		JPanel contentPanel = new JPanel(new SpringLayout());
		noticeLabel = new JLabel(" ");
		noticeLabel.setForeground(Color.RED);
		contentPanel.add(word2vecCheckBox);
		// contentPanel.add(addfileLocation);
		contentPanel.add(additionalTextFileChooser);
		contentPanel.add(potentialPattCheckBox);
		// contentPanel.add(pattfileLocation);
		contentPanel.add(new JLabel());

		// Lay out the panel.
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
				String additionalFile = addfileLocation.getText();
				if (word2vecCheckBox.isSelected()) {
					File tmpFile = new File(additionalFile);
					if (!tmpFile.exists() || !tmpFile.isFile()) {
						//noticeLabel.setText("Notice: please select additional file location");
						additionalFile = null;
						//start = false;
					}
				}else {
					additionalFile = null;
				}
				if (start == true) {

					ALPACAManager manager = ALPACAManager.getInstance();
					manager.doWord2Vec = word2vecCheckBox.isSelected();
					manager.doPatternExtraction = potentialPattCheckBox.isSelected();
					ALPACAManager.getInstance().startPreprocessingThread(additionalFile);
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
