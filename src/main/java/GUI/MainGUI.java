package GUI;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.text.DefaultCaret;

import AU.ALPACA.PreprocesorMain;
import Datastores.FileDataAdapter;

public class MainGUI extends JPanel implements ActionListener {
	/**
	 * 
	 */
	private static final boolean DEBUG = false;
	private static final long serialVersionUID = -6213851010100668003L;
	protected static final String DataFolderString = "DataFolder";
	protected static final String ConfigFileString = "Config";
	protected static final String prepString = "Preprocessing";
	protected static final String kwString = "keyword analysing";
	protected static final String topicString = "topic analysing";
	protected static final String patternString = "pattern analysing";
	protected static final String opinionString = "opinion analysing";
	protected static final String csvString = "csv";
	protected JButton prepButton = null;
	protected JButton keywordButton = null;
	protected JButton topicButton = null;
	protected JButton patternButton = null;
	protected JButton opinionButton = null;
	protected JButton importcsvButton = null;
	protected JLabel actionLabel;
	protected JTextField dataFolderTextField = null;
	protected JTextField configTextField = null;
	protected PreprocessingGUI preprocessingDialog;
	// protected KeywordGUI keywordDialog;
	protected PatternGUI patternDiaglog;
	protected CSVImporterGUI csvImporterDialog;
	protected TopicGUI topicDialog;
	protected OpinionGUI opinionDialog;
	JTextArea textLog = null;
	protected boolean isDatafolderReady = false;
	protected boolean isConfigReady = false;
	public JProgressBar progressBar = null;
	protected JButton cancelTaskButton = null;
	protected CustomFileDirChooser dataFileChoser = null;
	protected CustomFileDirChooser ConfigFileChoser = null;
	protected ClusteringGUI clusteringDialog = null;

	public void enableAllFunction(boolean enable) {
		importcsvButton.setEnabled(enable);
		prepButton.setEnabled(enable);
		dataFileChoser.setEnabled(enable);
		ConfigFileChoser.setEnabled(enable);
		enableAnalyzing(enable);
		cancelTaskButton.setEnabled(!enable);
	}

	public void enableBasicFunctions(boolean enable) {

		dataFileChoser.setEnabled(enable);
		importcsvButton.setEnabled(enable);
		ConfigFileChoser.setEnabled(enable);
		cancelTaskButton.setEnabled(!enable);
	}

	public void setProgressbarValue(int val) {

		progressBar.setValue(val);
	}

	public void enableAnalyzing(boolean enable) {
		keywordButton.setEnabled(enable);
		topicButton.setEnabled(enable);
		patternButton.setEnabled(enable);
		opinionButton.setEnabled(enable);
	}

	public MainGUI(JFrame aframe) {
		setLayout(new BorderLayout());

		preprocessingDialog = new PreprocessingGUI(aframe);
		preprocessingDialog.pack();
		clusteringDialog = new ClusteringGUI(aframe);
		//clusteringDialog.pack();
		// keywordDialog = new KeywordGUI(aframe);
		// keywordDialog.pack();
		patternDiaglog = new PatternGUI(aframe);
		patternDiaglog.pack();
		topicDialog = new TopicGUI(aframe);
		topicDialog.pack();
		opinionDialog = new OpinionGUI(aframe);
		opinionDialog.pack();
		csvImporterDialog = new CSVImporterGUI(aframe);
		csvImporterDialog.pack();
		// Data Folder: [ ][change]
		dataFolderTextField = new JTextField(10);
		dataFolderTextField.setActionCommand(DataFolderString);
		dataFolderTextField.addActionListener(this);
		dataFolderTextField.setEnabled(false);
		dataFolderTextField.getDocument().addDocumentListener(new DocumentListener() {

			@Override
			public void removeUpdate(DocumentEvent e) {
				// TODO Auto-generated method stub

			}

			@Override
			public void insertUpdate(DocumentEvent e) {
				// TODO Auto-generated method stub

				String dataFolder = dataFolderTextField.getText();
				if (!ALPACAManager.getInstance().isThisLikely2beADatafolder(dataFolder + "/")) {
					actionLabel.setText("Error: This is not a Data folder!");
					prepButton.setEnabled(false);
					isDatafolderReady = false;
					enableAnalyzing(false);
				} else {
					isDatafolderReady = true;
					ALPACAManager.getInstance().dataDirectory = dataFolder + "/";
					if (isDatafolderReady && isConfigReady) {
						prepButton.setEnabled(true);
						if (ALPACAManager.getInstance().isDatafolderPreprocessed(dataFolder + "/")) {
							enableAnalyzing(true);
						} else {
							enableAnalyzing(false);
						}
					}
				}
			}

			@Override
			public void changedUpdate(DocumentEvent e) {
				// TODO Auto-generated method stub
			}
		});
		JLabel DataFieldLabel = new JLabel("Data Folder: ");
		DataFieldLabel.setLabelFor(dataFolderTextField);
		dataFileChoser = new CustomFileDirChooser(dataFolderTextField, null, true, aframe, false, null);

		// Text Normalizer config file: [ ][change]
		configTextField = new JTextField(10);
		configTextField.setEnabled(false);
		configTextField.setActionCommand(ConfigFileString);
		configTextField.getDocument().addDocumentListener(new DocumentListener() {

			@Override
			public void removeUpdate(DocumentEvent e) {
				// TODO Auto-generated method stub

			}

			@Override
			public void insertUpdate(DocumentEvent e) {
				// TODO Auto-generated method stub

				String configFile = configTextField.getText();
				if (!ALPACAManager.getInstance().startReadingConfigINIThread(configFile)) {
					isConfigReady = false;
					isDatafolderReady = false;
					enableAnalyzing(false);
				} else {
					isConfigReady = true;
					if (isDatafolderReady && isConfigReady) {
						prepButton.setEnabled(true);
						if (ALPACAManager.getInstance().isDatafolderPreprocessed(dataFolderTextField.getText() + "/")) {
							enableAnalyzing(true);
						} else {
							enableAnalyzing(false);
						}
					}
				}
			}

			@Override
			public void changedUpdate(DocumentEvent e) {
				// TODO Auto-generated method stub
			}
		});
		JLabel ConfigFieldLabel = new JLabel("Text Normalizer Config File: ");
		ConfigFieldLabel.setLabelFor(configTextField);
		ConfigFileChoser = new CustomFileDirChooser(configTextField, null, false, aframe, false,
				new FileNameExtensionFilter("ini file", "ini"));

		// panel for setting.
		// Create a label to put messages during an action event.
		actionLabel = new JLabel("Notice: none", SwingConstants.LEFT);
		actionLabel.setBorder(BorderFactory.createEmptyBorder(10, 0, 0, 0));
		actionLabel.setForeground(Color.RED);
		JPanel settingsPane = new JPanel(new BorderLayout());
		JPanel subsettingsPane = new JPanel();
		subsettingsPane.setLayout(new SpringLayout());
		// settingsPane.add(actionLabel, c);
		settingsPane.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createTitledBorder("Setup"),
				BorderFactory.createEmptyBorder(5, 5, 5, 5)));

		importcsvButton = new JButton("Import CSV");
		importcsvButton.setActionCommand(csvString);
		importcsvButton.addActionListener(this);
		importcsvButton.setPreferredSize(new Dimension(100, 20));
		JPanel panelcsv = new JPanel();
		panelcsv.add(importcsvButton);

		subsettingsPane.add(DataFieldLabel);
		subsettingsPane.add(dataFileChoser);
		subsettingsPane.add(panelcsv);
		subsettingsPane.add(ConfigFieldLabel);
		subsettingsPane.add(ConfigFileChoser);
		subsettingsPane.add(new JLabel(""));

		SpringUtilities.makeCompactGrid(subsettingsPane, 2, 3, // rows, cols
				6, 6, // initX, initY
				6, 6); // xPad, yPad

		settingsPane.add(subsettingsPane, BorderLayout.PAGE_START);
		settingsPane.add(actionLabel, BorderLayout.PAGE_END);

		// create non-editable text field for displaying log.
		// Create a text log area.
		textLog = new JTextArea("Start Time: " + System.currentTimeMillis() + "\n");

		DefaultCaret caret = (DefaultCaret) textLog.getCaret(); // ←
		caret.setUpdatePolicy(DefaultCaret.ALWAYS_UPDATE);
		if (!DEBUG) {
			// Now create a new TextAreaOutputStream to write to our JTextArea control and
			// wrap a
			// PrintStream around it to support the println/printf methods.
			PrintStream out = new PrintStream(new TextAreaOutputStream(textLog));
			// redirect standard output stream to the TextAreaOutputStream
			System.setOut(out);
			// redirect standard error stream to the TextAreaOutputStream
			System.setErr(out);
		}
		textLog.setFont(new Font("Verdana", Font.PLAIN, 14));
		textLog.setLineWrap(true);
		textLog.setWrapStyleWord(true);
		textLog.setEditable(false);
		JScrollPane areaScrollPane = new JScrollPane(textLog);
		areaScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
		areaScrollPane.setPreferredSize(new Dimension(500, 300));

		// log panel
		JPanel logPanel = new JPanel(new BorderLayout());
		logPanel.setBorder(BorderFactory
				.createCompoundBorder(BorderFactory.createCompoundBorder(BorderFactory.createTitledBorder("Log"),
						BorderFactory.createEmptyBorder(5, 5, 5, 5)), areaScrollPane.getBorder()));
		JPanel upperLogPanel = new JPanel(new BorderLayout());
		JLabel logFileLocation = new JLabel("logALPACA.log");
		LogFileSaver logSaverFileChooser = new LogFileSaver(aframe, new FileNameExtensionFilter("txt file", "txt"));
		upperLogPanel.add(logFileLocation, BorderLayout.LINE_START);
		upperLogPanel.add(logSaverFileChooser, BorderLayout.LINE_END);
		JPanel bottomLogPanel = new JPanel(new BorderLayout());
		progressBar = new JProgressBar(0, 100);
		progressBar.setValue(0);
		progressBar.setStringPainted(true);
		cancelTaskButton = new JButton("Cancel");
		cancelTaskButton.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent arg0) {
				// TODO Auto-generated method stub
				System.out.println(">>>>>>>>>>>>>>>ACTIVATING THREADS TERMINATION SEQUENCE<<<<<<<<<<<<<<\n "
						+ "(this may take sometime for the current singular task to be finished)");
				ALPACAManager.Kill_Switch = true;
				cancelTaskButton.setEnabled(false);
			}
		});
		cancelTaskButton.setEnabled(false);
		bottomLogPanel.add(progressBar, BorderLayout.LINE_START);
		bottomLogPanel.add(cancelTaskButton, BorderLayout.LINE_END);
		logPanel.add(upperLogPanel, BorderLayout.PAGE_START);
		logPanel.add(areaScrollPane, BorderLayout.CENTER);
		logPanel.add(bottomLogPanel, BorderLayout.PAGE_END);
		// create function panel
		JPanel fucntionPanel = new JPanel(new GridLayout(5, 1));
		fucntionPanel.setBorder(BorderFactory
				.createCompoundBorder(BorderFactory.createCompoundBorder(BorderFactory.createTitledBorder("Functions"),
						BorderFactory.createEmptyBorder(5, 5, 5, 5)), areaScrollPane.getBorder()));
		prepButton = new JButton("Pre-process data");
		prepButton.setActionCommand(prepString);
		prepButton.addActionListener(this);
		prepButton.setEnabled(false);
		prepButton.setPreferredSize(new Dimension(180, 30));
		JPanel panelPrep = new JPanel();
		panelPrep.add(prepButton);
		keywordButton = new JButton("Cluster Keywords");
		keywordButton.setActionCommand(kwString);
		keywordButton.addActionListener(this);
		keywordButton.setEnabled(false);
		keywordButton.setPreferredSize(new Dimension(180, 30));
		JPanel panelkw = new JPanel();
		panelkw.add(keywordButton);
		topicButton = new JButton("Expand Topics");
		topicButton.setActionCommand(topicString);
		topicButton.addActionListener(this);
		topicButton.setEnabled(false);
		topicButton.setPreferredSize(new Dimension(180, 30));
		JPanel paneltp = new JPanel();
		paneltp.add(topicButton);
		patternButton = new JButton("Expand Intent Patterns");
		patternButton.setActionCommand(patternString);
		patternButton.addActionListener(this);
		patternButton.setEnabled(false);
		patternButton.setPreferredSize(new Dimension(180, 30));
		JPanel panelpt = new JPanel();
		panelpt.add(patternButton);
		opinionButton = new JButton("Extract Opinions");
		opinionButton.setActionCommand(opinionString);
		opinionButton.addActionListener(this);
		opinionButton.setEnabled(false);
		opinionButton.setPreferredSize(new Dimension(180, 30));
		JPanel panelop = new JPanel();
		panelop.add(opinionButton);
		fucntionPanel.add(panelPrep, -1);
		fucntionPanel.add(panelkw, -1);
		fucntionPanel.add(paneltp, -1);
		fucntionPanel.add(panelpt, -1);
		fucntionPanel.add(panelop, -1);
		// create the bottom panel, it holds all functions and log
		JPanel bottomPane = new JPanel(new BorderLayout());
		bottomPane.add(fucntionPanel, BorderLayout.LINE_START);
		bottomPane.add(logPanel, BorderLayout.LINE_END);

		add(settingsPane, BorderLayout.PAGE_START);
		add(bottomPane, BorderLayout.PAGE_END);

	}

	private void addLabelTextRows(JLabel[] labels, CustomFileDirChooser[] textFields, GridBagLayout gridbag,
			Container container) {
		GridBagConstraints c = new GridBagConstraints();
		c.anchor = GridBagConstraints.EAST;
		int numLabels = labels.length;

		for (int i = 0; i < numLabels; i++) {
			c.gridwidth = GridBagConstraints.RELATIVE; // next-to-last
			c.fill = GridBagConstraints.NONE; // reset to default
			c.weightx = 0.0; // reset to default
			container.add(labels[i], c);

			c.gridwidth = GridBagConstraints.REMAINDER; // end row
			c.fill = GridBagConstraints.HORIZONTAL;
			c.weightx = 1.0;
			container.add(textFields[i], c);
		}
	}

	public void actionPerformed(ActionEvent e) {
		// String prefix = "You typed \"";
		// if (textFieldString.equals(e.getActionCommand())) {
		// JTextField source = (JTextField) e.getSource();
		// actionLabel.setText(prefix + source.getText() + "\"");
		// } else if (passwordFieldString.equals(e.getActionCommand())) {
		// JPasswordField source = (JPasswordField) e.getSource();
		// actionLabel.setText(prefix + new String(source.getPassword()) + "\"");
		// } else if (buttonString.equals(e.getActionCommand())) {
		// Toolkit.getDefaultToolkit().beep();
		// }
		String datadirectory = null;
		File tmpFile = null;
		switch (e.getActionCommand()) {
		case prepString:
			datadirectory = dataFolderTextField.getText();
			tmpFile = new File(datadirectory);
			if (!tmpFile.exists() || !tmpFile.isDirectory()) {
				actionLabel.setText("Notice: please select data folder location");
			} else {

				preprocessingDialog.setLocationRelativeTo(this);
				preprocessingDialog.setVisible(true);
			}

			break;
		case kwString:
			datadirectory = dataFolderTextField.getText();
			tmpFile = new File(datadirectory);
			if (!tmpFile.exists() || !tmpFile.isDirectory()) {
				actionLabel.setText("Notice: please select data folder file location");
			} else {
				clusteringDialog.setLocationRelativeTo(this);
				clusteringDialog.setComboBox();
				clusteringDialog.pack();
				clusteringDialog.setVisible(true);
			}

			break;
		case topicString:
			datadirectory = dataFolderTextField.getText();
			tmpFile = new File(datadirectory);
			if (!tmpFile.exists() || !tmpFile.isDirectory()) {
				actionLabel.setText("Notice: please select data folder file location");
			} else {
				String pattFile = FileDataAdapter.getLevelLocationDir("POSpatterns/", datadirectory + "/",
						PreprocesorMain.LV2_ROOTWORD_STEMMING) + "rawPattern.csv";
				System.out.println(pattFile);
				tmpFile = new File(pattFile);
				if (!tmpFile.exists()) {
					System.out.println(">> ERROR: You must run POS pattern preprocessing on this dataset first!");
				} else {
					topicDialog.setLocationRelativeTo(this);
					topicDialog.setVisible(true);
				}
			}

			break;
		case patternString:
			datadirectory = dataFolderTextField.getText();
			tmpFile = new File(datadirectory);
			if (!tmpFile.exists() || !tmpFile.isDirectory()) {
				actionLabel.setText("Notice: please select data folder file location");
			} else {

				patternDiaglog.setLocationRelativeTo(this);
				patternDiaglog.setVisible(true);
			}

			break;
		case opinionString:
			datadirectory = dataFolderTextField.getText();
			tmpFile = new File(datadirectory);
			if (!tmpFile.exists() || !tmpFile.isDirectory()) {
				actionLabel.setText("Notice: please select data folder file location");
			} else {

				opinionDialog.setLocationRelativeTo(this);
				opinionDialog.setVisible(true);
			}

			break;
		case csvString:
			csvImporterDialog.setLocationRelativeTo(this);
			csvImporterDialog.setVisible(true);

			break;
		}
	}

	/**
	 * Create the GUI and show it. For thread safety, this method should be invoked
	 * from the event dispatch thread.
	 */
	public static MainGUI createAndShowGUI() {
		// Create and set up the window.
		JFrame frame = new JFrame("ALPACA");
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		MainGUI mainGUI = new MainGUI(frame);
		// Add content to the window.
		frame.add(mainGUI);
		frame.addWindowListener(new WindowListener() {

			@Override
			public void windowOpened(WindowEvent e) {
				// TODO Auto-generated method stub
			}

			@Override
			public void windowIconified(WindowEvent e) {
				// TODO Auto-generated method stub

			}

			@Override
			public void windowDeiconified(WindowEvent e) {
				// TODO Auto-generated method stub

			}

			@Override
			public void windowDeactivated(WindowEvent e) {
				// TODO Auto-generated method stub

			}

			@Override
			public void windowClosing(WindowEvent e) {
				// TODO Auto-generated method stub

			}

			@Override
			public void windowClosed(WindowEvent e) {
				// TODO Auto-generated method stub
				mainGUI.dispose();
			}

			@Override
			public void windowActivated(WindowEvent e) {
				// TODO Auto-generated method stub

			}
		});
		// Display the window.
		frame.pack();
		frame.setVisible(true);
		return mainGUI;
	}

	protected void dispose() {
		// TODO Auto-generated method stub

		preprocessingDialog.dispose();
		// keywordDialog.dispose();
		patternDiaglog.dispose();
		topicDialog.dispose();
		opinionDialog.dispose();
		;
	}

}
