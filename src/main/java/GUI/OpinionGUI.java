
package GUI;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SpringLayout;

public class OpinionGUI extends JDialog {
	/**
	 * 
	 */
	private static final long serialVersionUID = -6985925894270983920L;
	protected JButton doitButton = null;
	protected JTextField outFileLocation = null;
	protected JLabel noticeLabel = null;
	protected JTextArea keywordEditor = null;
	protected JButton importWordsButton = null;
	protected PatternChooserPanel patternPanel = null;

	public OpinionGUI(JFrame aFrame) {
		super(aFrame, true);

		this.setTitle("Discovering opinions using topic and intention");
		patternPanel = new PatternChooserPanel("Patterns For Intention", aFrame);
		noticeLabel = new JLabel(" ");
		noticeLabel.setForeground(Color.RED);
		keywordEditor = new JTextArea("");
		outFileLocation = new JTextField();
		outFileLocation.setPreferredSize(new Dimension(200, 20));
		CustomFileDirChooser outFileChooser = new CustomFileDirChooser(outFileLocation, null, true, aFrame, true, null);

		KeywordEditorPanel keywordPanel = new KeywordEditorPanel(keywordEditor, noticeLabel, aFrame);
		JPanel contentPanel = new JPanel(new SpringLayout());
		contentPanel.add(patternPanel);
		contentPanel.add(keywordPanel);
		contentPanel.add(new JLabel("Output directory:"));
		contentPanel.add(outFileChooser);
		// Lay out the panel.
		SpringUtilities.makeCompactGrid(contentPanel, 4, 1, // rows, cols
				6, 6, // initX, initY
				6, 6); // xPad, yPad
		// do it button
		JPanel DoitPanel = new JPanel(new BorderLayout());
		doitButton = new JButton("Click here to begin looking for opinions");
		doitButton.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				// TODO Auto-generated method stub
				boolean start = true;
				String wordsInputed = keywordEditor.getText();
				Set<String> wordSet = null;
				if (wordsInputed.isEmpty()) {
					noticeLabel.setText("Notice: please add some keywords");
					start = false;
				} else {
					wordSet = new HashSet<>(Arrays.asList(wordsInputed.split(",")));
				}

				String sourceFile = patternPanel.getPatternPath();
				File tmpFile = null;
				if (sourceFile.equals(patternPanel.REQUEST_PATH) || sourceFile.equals(patternPanel.BUG_PATH)) {
				} else {
					tmpFile = new File(sourceFile);
					if (!tmpFile.exists() || !tmpFile.isFile()) {
						noticeLabel.setText("Notice: please select pattern file");
						sourceFile = null;
						start = false;
					}
				}
				double dValue = patternPanel.getThreshold();
				String outFile = outFileLocation.getText();
				tmpFile = new File(outFile);
				if (!tmpFile.isDirectory()) {
					noticeLabel.setText("Notice: please select output folder");
					outFile = null;
					start = false;
				}
				if (start == true) {

					ALPACAManager manager = ALPACAManager.getInstance();
					manager.startOpinionExtractionThread(wordSet, dValue, outFile, sourceFile);
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
