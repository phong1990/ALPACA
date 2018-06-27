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

@SuppressWarnings("serial")
public class PatternGUI extends JDialog {
	/**
	 * 
	 */
	protected JButton doitButton = null;
	protected JLabel noticeLabel = null;
	protected JTextField outfileLocation = null;

	protected PatternChooserPanel patternPanel = null;

	public PatternGUI(JFrame aFrame) {
		super(aFrame, true);

		this.setTitle("Expanding intention patterns");
		patternPanel = new PatternChooserPanel("Choose seed intention patterns", aFrame);
		outfileLocation = new JTextField();
		outfileLocation.setPreferredSize(new Dimension(200, 20));
		CustomFileDirChooser outFileChooser = new CustomFileDirChooser(outfileLocation, null, false, aFrame, true,
				null);
		JPanel contentPanel = new JPanel(new BorderLayout());
		noticeLabel = new JLabel(" ");
		noticeLabel.setForeground(Color.RED);
		contentPanel.add(patternPanel, BorderLayout.PAGE_START);
		contentPanel.add(new JLabel("Expands patterns to this location: ", SwingConstants.CENTER), BorderLayout.CENTER);
		contentPanel.add(outFileChooser, BorderLayout.PAGE_END);

		// do it button
		JPanel DoitPanel = new JPanel(new BorderLayout());
		doitButton = new JButton("Click here to begin");
		doitButton.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				// TODO Auto-generated method stub
				boolean start = true;
				File tmpFile = null;
				String seedFile = patternPanel.getPatternPath();
				if (seedFile.equals(patternPanel.REQUEST_PATH) || seedFile.equals(patternPanel.BUG_PATH)) {
				} else {
					tmpFile = new File(seedFile);
					if (!tmpFile.exists() || !tmpFile.isFile()) {
						noticeLabel.setText("Notice: please select seed pattern file");
						seedFile = null;
						start = false;
					}
				}
				String outFile = outfileLocation.getText();
				tmpFile = new File(outFile);
				if (tmpFile.isDirectory()) {
					noticeLabel.setText("Notice: please select output file");
					outFile = null;
					start = false;
				} else {
					File file = tmpFile.getParentFile();
					if (!file.exists()) {
						noticeLabel.setText("Notice: please select output file");
						outFile = null;
						start = false;
					}
				}
				double thresholdValue = patternPanel.getThreshold();
				if (start == true) {
					// ALPACAManager.getInstance().preprocessing();
					ALPACAManager.getInstance().startPatternExpansionThread(seedFile, outFile, thresholdValue);
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
