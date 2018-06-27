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

public class TopicGUI extends JDialog {
	/**
	 * 
	 */
	private static final long serialVersionUID = 2523864131641150282L;
	/**
	 * 
	 */
	protected JButton doitButton = null;
	protected JTextField outFileLocation = null;
	protected JLabel noticeLabel = null;
	JTextArea keywordEditor = null;

	public TopicGUI(JFrame aFrame) {
		super(aFrame, true);
		this.setTitle("Expanding topic by discovering similar phrases/keywords");
		noticeLabel = new JLabel(" ");
		noticeLabel.setForeground(Color.RED);
		keywordEditor = new JTextArea("");

		outFileLocation = new JTextField();
		outFileLocation.setPreferredSize(new Dimension(200, 20));
		CustomFileDirChooser outFileChooser = new CustomFileDirChooser(outFileLocation, null, true, aFrame, true,null);

		KeywordEditorPanel keywordPanel = new KeywordEditorPanel(keywordEditor, noticeLabel, aFrame);
		JPanel contentPanel = new JPanel(new SpringLayout());
		contentPanel.add(keywordPanel);
		contentPanel.add(new JLabel("Output folder (ALPACA will write topic keywords and topic descriptions in two separate files):"));
		contentPanel.add(outFileChooser);
		// Lay out the panel.
		SpringUtilities.makeCompactGrid(contentPanel, 3, 1, // rows, cols
				6, 6, // initX, initY
				6, 6); // xPad, yPad
		// do it button
		JPanel DoitPanel = new JPanel(new BorderLayout());
		doitButton = new JButton("Click here to begin expanding keywords");
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

				String outDir = outFileLocation.getText();
				File tmpFile = new File(outDir);
				if (!tmpFile.isDirectory()) {
					noticeLabel.setText("Notice: please select output directory for this topic");
					outDir = null;
					start = false;
				}
				if (start == true) {

					// ALPACAManager manager = ALPACAManager.getInstance();
					ALPACAManager.getInstance().startKeywordExpandingThread(wordSet, outDir);
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
