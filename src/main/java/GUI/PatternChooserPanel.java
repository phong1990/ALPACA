package GUI;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTextField;
import javax.swing.SpringLayout;

import Utils.Util;

public class PatternChooserPanel extends JPanel {

	/**
	 * 
	 */
	private static final long serialVersionUID = -8861738768050421351L;
	//public static final String REQUEST_PATH = Util.class.getResource("/seedPatterns/request.csv").getPath();
	public static final String REQUEST_PATH = "/seedPatterns/request.csv";
	//public static final String BUG_PATH = Util.class.getResource("/seedPatterns/bugreport.csv").getPath();
	public static final String BUG_PATH = "/seedPatterns/bugreport.csv";
	private String patternPath = null;
	protected JTextField patternFileLocation = null;
	private boolean isCustom = true;
	private static final String[] thresholds = { "Exact Match", "Strictly Match", "Moderately Match", "Flexible Match",
			"Loosely Match" };
	private JComboBox thresholdList = null;
	private double thresholdDouble = 0.7;

	public String getPatternPath() {
		if (isCustom)
			return patternFileLocation.getText();
		else
			return patternPath;
	}

	public double getThreshold() {
		return thresholdDouble;
	}

	public PatternChooserPanel(String label, JFrame aFrame) {
		// TODO Auto-generated constructor stub
		// Create the radio buttons.
		JRadioButton requestButton = new JRadioButton("Requests");
		requestButton.setMnemonic(KeyEvent.VK_B);
		requestButton.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				// TODO Auto-generated method stub
				patternPath = REQUEST_PATH;
				isCustom = false;
			}
		});
		;

		JRadioButton bugButton = new JRadioButton("Complaints (Bug reports, dislikes, negative remarks, etc)");
		bugButton.setMnemonic(KeyEvent.VK_C);
		bugButton.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				// TODO Auto-generated method stub
				patternPath = BUG_PATH;
				isCustom = false;
			}
		});

		JRadioButton customButton = new JRadioButton("Custom Patterns");
		customButton.setMnemonic(KeyEvent.VK_D);
		customButton.setSelected(true);
		customButton.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				// TODO Auto-generated method stub
				isCustom = true;
			}
		});

		// Group the radio buttons.
		ButtonGroup group = new ButtonGroup();
		group.add(requestButton);
		group.add(bugButton);
		group.add(customButton);

		patternFileLocation = new JTextField();
		patternFileLocation.setPreferredSize(new Dimension(200, 20));
		CustomFileDirChooser patternFileChooser = new CustomFileDirChooser(patternFileLocation, null, false, aFrame,
				true,null);
		JPanel patternFileChooserPanel = new JPanel(new BorderLayout());
		patternFileChooserPanel.add(patternFileChooser, BorderLayout.LINE_START);

		thresholdList = new JComboBox(thresholds);
		thresholdList.setSelectedIndex(2);
		thresholdList.setPreferredSize(new Dimension(100, 20));
		thresholdList.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				// TODO Auto-generated method stub
				JComboBox cb = (JComboBox) e.getSource();
				String choosenOne = (String) cb.getSelectedItem();

				switch (choosenOne) {
				case "Exact Match":
					thresholdDouble = 0.99;
					break;
				case "Strictly Match":
					thresholdDouble = 0.8;
					break;
				case "Moderately Match":
					thresholdDouble = 0.7;
					break;
				case "Flexible Match":
					thresholdDouble = 0.6;
					break;
				case "Loosely Match":
					thresholdDouble = 0.45;
					break;
				}
			}
		});
		JPanel thresholdPanel = new JPanel(new BorderLayout());
		thresholdPanel.add(thresholdList,BorderLayout.LINE_START);
		this.setLayout(new SpringLayout());
		this.add(requestButton);
		this.add(new JLabel(""));
		this.add(bugButton);
		this.add(new JLabel(""));
		this.add(customButton);
		this.add(patternFileChooserPanel);
		this.add(new JLabel("Level of pattern matching:"));
		this.add(thresholdPanel);
		this.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createTitledBorder(label),
				BorderFactory.createEmptyBorder(5, 5, 5, 5)));
		// Lay out the panel.
		SpringUtilities.makeCompactGrid(this, 4, 2, // rows, cols
				6, 6, // initX, initY
				6, 6); // xPad, yPad
	}
}
