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

import AU.ALPACA.PreprocesorMain;
import Datastores.FileDataAdapter;

public class KeywordGUI extends JDialog {
	/**
	 * 
	 */
	private static final long serialVersionUID = 2185314151253210335L;
	protected JButton doitButton = null;
	//protected JTextField wordScorefileLocation = null;
	protected JLabel noticeLabel = null;

	public KeywordGUI(JFrame aFrame) {
		super(aFrame, true);
//		wordScorefileLocation = new JTextField();
//		wordScorefileLocation.setPreferredSize(new Dimension(200, 20));
//		CustomFileDirChooser wordScoreFileChooser = new CustomFileDirChooser(wordScorefileLocation, null, false,
//				aFrame);
		JPanel contentPanel = new JPanel(new SpringLayout());
		noticeLabel = new JLabel(" ");
		noticeLabel.setForeground(Color.RED);
		contentPanel.add(new JLabel(
				"Wordscore file will be output to: " + ALPACAManager.getInstance().dataDirectory + "wordScore/lv2/"));
		// contentPanel.add(addfileLocation);
		//contentPanel.add(wordScoreFileChooser);

		// Lay out the panel.
//		SpringUtilities.makeCompactGrid(contentPanel, 1, 2, // rows, cols
//				6, 6, // initX, initY
//				6, 6); // xPad, yPad

		// do it button
		JPanel DoitPanel = new JPanel(new BorderLayout());
		doitButton = new JButton("Click here to begin");
		doitButton.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				// TODO Auto-generated method stub
				ALPACAManager.getInstance().startKeywordAnalysingThread();
				clearAndHide();
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
