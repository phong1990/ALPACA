package GUI;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.io.File;
import java.io.FileNotFoundException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Scanner;
import java.util.Set;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SpringLayout;
import javax.swing.border.Border;

import Utils.Util;

public class KeywordEditorPanel extends JPanel {
	/**
	 * 
	 */
	private static final long serialVersionUID = -6464846830289739380L;
	private JTextArea editor = null;
	protected JButton importWordsButton = null;
	protected JTextField wordfileLocation = null;
	private JLabel noticeLabel = null;
	JFrame aframe;
	public void setEnabled(boolean enabled) {
		editor.setEnabled(enabled);
		importWordsButton.setEnabled(enabled);
		wordfileLocation.setEnabled(enabled);
	}
	public KeywordEditorPanel(JTextArea keywordEditor, JLabel noticeLabe, JFrame frame) {
		aframe = frame;
		this.noticeLabel = noticeLabe;
		editor = keywordEditor;

		wordfileLocation = new JTextField();
		wordfileLocation.setPreferredSize(new Dimension(200, 20));
		CustomFileDirChooser keywordFileChooser = new CustomFileDirChooser(wordfileLocation, null, false,aframe, true,null);
		Border border = BorderFactory.createLineBorder(Color.BLACK);
		keywordEditor
				.setBorder(BorderFactory.createCompoundBorder(border, BorderFactory.createEmptyBorder(10, 10, 10, 10)));
		keywordEditor.setPreferredSize(new Dimension(300, 100));
		keywordEditor.setFont(new Font("Verdana", Font.PLAIN, 16));
		keywordEditor.setLineWrap(true);
		keywordEditor.setWrapStyleWord(true);
		keywordEditor.addKeyListener(new KeyListener() {

			@Override
			public void keyTyped(KeyEvent e) {
				// TODO Auto-generated method stub
				char charEntered = e.getKeyChar();
				if ((charEntered >= 'A' && charEntered <= 'Z') || (charEntered >= 'a' && charEntered <= 'z')
						|| (charEntered >= '0' && charEntered <= '9') || charEntered == ',' || charEntered == ' ') {

				} else {
					e.consume();
					// Document doc = keywordEditor.getDocument();
					// try {
					// doc.remove(doc.getLength() - 1, 1);
					// } catch (BadLocationException e1) {
					// // TODO Auto-generated catch block
					// e1.printStackTrace();
					// }
				}
			}

			@Override
			public void keyReleased(KeyEvent e) {
				// TODO Auto-generated method stub

			}

			@Override
			public void keyPressed(KeyEvent e) {
				// TODO Auto-generated method stub

			}
		});

		importWordsButton = new JButton("Import");
		importWordsButton.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent arg0) {
				// TODO Auto-generated method stub
				String wordFile = wordfileLocation.getText();
				if (wordFile.isEmpty()) {
					noticeLabel.setText("Error: choose the keyword file first!");
					return;
				}
				Scanner scn = null;
				try {
					scn = new Scanner(new File(wordFile));
					Set<String> keywords = new HashSet<>();
					while (scn.hasNextLine()) {
						String content = scn.nextLine();
						if (!Util.isKeywordInput(content)) {
							noticeLabel.setText("Error: this file is not a keyword file!");
							return;
						}
						keywords.addAll(Arrays.asList(content.split(",")));
					}
					StringBuilder strBuilder = new StringBuilder();
					for (String w : keywords) {
						strBuilder.append(w).append(",");
					}
					strBuilder.deleteCharAt(strBuilder.length()-1);
					editor.setText(strBuilder.toString());
				} catch (FileNotFoundException e) {
					// TODO Auto-generated catch block
					noticeLabel.setText("Error: file not exist!");
				} finally {
					if (scn != null)
						scn.close();
				}
			}
		});

		JPanel editorPanel = new JPanel(new SpringLayout());
		editorPanel.add(new JLabel("Type in keywords or phrases (separated by comma): "));
		editorPanel.add(keywordEditor);
		// Lay out the panel.
		SpringUtilities.makeCompactGrid(editorPanel, 2, 1, // rows, cols
				6, 6, // initX, initY
				6, 6); // xPad, yPad
		JPanel midPanel = new JPanel();
		midPanel.add(new JLabel("OR"));
		JPanel kwFilePanel = new JPanel(new SpringLayout());
		kwFilePanel.add(new JLabel("Choose a keyword file:"));
		kwFilePanel.add(keywordFileChooser);
		kwFilePanel.add(importWordsButton);
		// Lay out the panel.
		SpringUtilities.makeCompactGrid(kwFilePanel, 3, 1, // rows, cols
				6, 6, // initX, initY
				6, 6); // xPad, yPad
		this.setLayout(new SpringLayout());
		this.add(editorPanel);
		this.add(midPanel);
		this.add(kwFilePanel);
		this.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createTitledBorder("Keywords"),
				BorderFactory.createEmptyBorder(5, 5, 5, 5)));
		// Lay out the panel.
		SpringUtilities.makeCompactGrid(this, 1, 3, // rows, cols
				6, 6, // initX, initY
				6, 6); // xPad, yPad
	}
}
