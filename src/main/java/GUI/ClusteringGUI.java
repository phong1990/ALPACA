
package GUI;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.ComboBoxModel;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SpringLayout;

public class ClusteringGUI extends JDialog implements ActionListener {
	/**
	 * 
	 */
	private static final long serialVersionUID = 2523864131641150282L;
	/**
	 * 
	 */
	private static final String FILE_OPTION = "file";
	private static final String WORDS_OPTION = "word";
	protected JButton doitButton = null;
	protected JTextField outFileLocation = null;
	protected JLabel noticeLabel = null;
	JTextArea keywordEditor = null;
	private JComboBox schemaList = null;
	private JComboBox numberList = null;
	private String choosenOne = null;
	private int choosenNumber = 0;
	private int choosenOption = 0;
	private KeywordEditorPanel keywordPanel = null;

	public String getSelectedSchema() {
		return choosenOne;
	}

	public int getNumber() {
		return choosenNumber;
	}

	public void setComboBox() {

		String[] options;
		try {
			options = ALPACAManager.getInstance().readRankingSchemas();
			schemaList.setModel(new DefaultComboBoxModel<>(options));
			choosenOne = options[0];
			choosenNumber = 50;
			schemaList.setSelectedIndex(0);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public ClusteringGUI(JFrame aFrame) {
		super(aFrame, true);
		this.setTitle("Clustering keywords into topics using MARK's engine");
		// creating the output location chooser
		outFileLocation = new JTextField();
		outFileLocation.setPreferredSize(new Dimension(200, 20));
		CustomFileDirChooser outFileChooser = new CustomFileDirChooser(outFileLocation, null, true, aFrame, true, null);

		// creating the words editor
		noticeLabel = new JLabel(" ");
		noticeLabel.setForeground(Color.RED);
		keywordEditor = new JTextArea("");
		keywordPanel = new KeywordEditorPanel(keywordEditor, noticeLabel, aFrame);
		keywordPanel.setEnabled(false);
		// creating the top word selector
		schemaList = new JComboBox();
		schemaList.setPreferredSize(new Dimension(100, 20));
		schemaList.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				// TODO Auto-generated method stub
				JComboBox cb = (JComboBox) e.getSource();
				choosenOne = (String) cb.getSelectedItem();
			}
		});
		String[] numbers = new String[] { "50", "100", "150", "200", "250", "300", "350", "400", "450", "500" };
		numberList = new JComboBox<>(numbers);
		numberList.setPreferredSize(new Dimension(100, 20));
		numberList.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				// TODO Auto-generated method stub
				JComboBox cb = (JComboBox) e.getSource();
				choosenNumber = Integer.parseInt((String) cb.getSelectedItem());
			}
		});
		JPanel topWordsPanel = new JPanel(new SpringLayout());
		topWordsPanel.add(new JLabel("Score type:"));
		topWordsPanel.add(schemaList);
		topWordsPanel.add(new JLabel("Top keywords:"));
		topWordsPanel.add(numberList);
		topWordsPanel.setBorder(
				BorderFactory.createCompoundBorder(BorderFactory.createTitledBorder("Top keywords by a scoring"),
						BorderFactory.createEmptyBorder(5, 5, 5, 5)));

		// Lay out the panel.
		SpringUtilities.makeCompactGrid(topWordsPanel, 2, 2, // rows, cols
				6, 6, // initX, initY
				6, 6); // xPad, yPad

		// creating the clustering input chooser
		JRadioButton fileButton = new JRadioButton("Cluster top N keywords by a scoring");
		fileButton.setMnemonic(KeyEvent.VK_B);
		fileButton.setActionCommand(FILE_OPTION);
		fileButton.addActionListener(this);
		fileButton.setSelected(true);
		JRadioButton wordButton = new JRadioButton("Cluster a selected set of keywords");
		wordButton.setMnemonic(KeyEvent.VK_C);
		wordButton.addActionListener(this);
		wordButton.setActionCommand(WORDS_OPTION);
		ButtonGroup group = new ButtonGroup();
		group.add(fileButton);
		group.add(wordButton);

		// creating the main panel
		JPanel mainPanel = new JPanel(new SpringLayout());
		mainPanel.add(fileButton);
		mainPanel.add(topWordsPanel);
		mainPanel.add(wordButton);
		mainPanel.add(keywordPanel);
		// Lay out the panel.
		SpringUtilities.makeCompactGrid(mainPanel, 4, 1, // rows, cols
				6, 6, // initX, initY
				6, 6); // xPad, yPad
		JPanel contentPanel = new JPanel(new SpringLayout());
		contentPanel.add(mainPanel);
		contentPanel.add(new JLabel("Output folder (ALPACA will write each topic in a separated file):"));
		contentPanel.add(outFileChooser);
		// Lay out the panel.
		SpringUtilities.makeCompactGrid(contentPanel, 3, 1, // rows, cols
				6, 6, // initX, initY
				6, 6); // xPad, yPad
		// do it button
		JPanel DoitPanel = new JPanel(new BorderLayout());
		doitButton = new JButton("Click here to begin clustering keywords");
		doitButton.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				// TODO Auto-generated method stub
				boolean start = true;
				if (choosenOption == 1) {
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
						ALPACAManager.getInstance().startClusteringWordsThread(wordSet, outDir);
						clearAndHide();
					}
				}
				if (choosenOption == 0) {
					String outDir = outFileLocation.getText();
					File tmpFile = new File(outDir);
					if (!tmpFile.isDirectory()) {
						noticeLabel.setText("Notice: please select output directory for this topic");
						outDir = null;
						start = false;
					}
					if (start == true) {
						// ALPACAManager manager = ALPACAManager.getInstance();
						ALPACAManager.getInstance().startClusteringWordsThread(choosenOne, choosenNumber, outDir);
						clearAndHide();
					}
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

	public void actionPerformed(ActionEvent e) {
		switch (e.getActionCommand()) {
		case FILE_OPTION:
			keywordPanel.setEnabled(false);
			schemaList.setEnabled(true);
			numberList.setEnabled(true);
			choosenOption = 0;
			break;
		case WORDS_OPTION:
			keywordPanel.setEnabled(true);
			schemaList.setEnabled(false);
			numberList.setEnabled(false);
			choosenOption = 1;
			break;
		}
	}

	/** This method clears the dialog and hides it. */
	public void clearAndHide() {
		setVisible(false);
	}
}
