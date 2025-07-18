package main;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

import wrimsv2.commondata.wresldata.Param;
import wrimsv2.components.BuildProps;
import wrimsv2.components.ControlData;
import wrimsv2.wreslparser.elements.LogUtils;
import wrimsv2.wreslparser.elements.StudyParser;
import wrimsv2.wreslparser.elements.StudyUtils;
import wrimsv2.wreslplus.elements.Workflow;

public class WreslCheckGUI {
	
	public static void main(String[] args) {
				
		JFrame frame = new JFrame(Param.wreslChekerName + new BuildProps().getVN());
		frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		JPanel wreslCheckPanel = new JPanel();
		wreslCheckPanel.setLayout(new BoxLayout(wreslCheckPanel, BoxLayout.Y_AXIS));
		final InputFilePanel inputFilePanel = new InputFilePanel(
				"Main Wresl File: ");
		wreslCheckPanel.add(inputFilePanel);
		
		//Font displayFont = new Font("Consolas", Font.BOLD, 14);
		
		final JButton runButton = new JButton("Run Wresl Checker");
		final JCheckBox chk_wreslPlus =new JCheckBox("WreslPlus");	
		//final JCheckBox chk_csv =new JCheckBox("Generate report");	
		final JTextArea text_parsingTime = new JTextArea(" ");
//		final JProgressBar progressBar = new JProgressBar(0);
//		
//		progressBar.setIndeterminate(true);
//		progressBar.setValue(0);
//		progressBar.setStringPainted(true);
//		
		//runButton.setFont(displayFont);
		chk_wreslPlus.setSelected(true);
		//chk_csv.setSelected(true);
		text_parsingTime.setEditable(false);
		
		wreslCheckPanel.add(chk_wreslPlus);
		//wreslCheckPanel.add(chk_csv);
		wreslCheckPanel.add(runButton);
//		wreslCheckPanel.add(progressBar);
		wreslCheckPanel.add(text_parsingTime);


		runButton.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent e) {
								
				
				runButton.setEnabled(false);
				runButton.setText("Parsing..........");
				text_parsingTime.setText(" Parsing in progress ....");
				SwingUtilities.invokeLater(new Runnable() {
					public void run() {				
						
						ControlData.outputWreslCSV=true;
						
						try {
							long start = System.currentTimeMillis();
							
							if (chk_wreslPlus.isSelected()){
								
								StudyUtils.useWreslPlus = true;
									
							} 

							StudyUtils.checkStudy(inputFilePanel.getMainFilePath());
							
//							if (chk_csv.isSelected()) {
//								StudyUtils.checkStudy(inputFilePanel.getFile(), "=WreslCheck_csv=", false);
//							}
//							else {
//								StudyUtils.checkStudy(inputFilePanel.getFile(), null, false);
//							}

							float elapsedSec = (System.currentTimeMillis() - start) / 1000F;
							String elapsedSecStr = Float.toString(elapsedSec);
							text_parsingTime.setText("Total errors: " + StudyUtils.total_errors
									+ "; Parsing time in seconds: " + elapsedSecStr);

						} catch (Exception ex) {
							
							ex.printStackTrace();
							
							text_parsingTime.setText(" ");
							JOptionPane.showMessageDialog(runButton, ex
									.getMessage(), "Error!",
									JOptionPane.ERROR_MESSAGE);
						} finally {
							runButton.setText("Run Wresl Checker");
							runButton.setEnabled(true);
							//chk_csv.setSelected(false);
							
						}
					}
				});
			}
		});
		frame.getContentPane().add(wreslCheckPanel);
		frame.setLocationRelativeTo(null);
		frame.pack();
		frame.setVisible(true);
	}

	/**
	 * Creates a panel consisting of label, a textfield populated by a browse
	 * (file browser) button
	 *
	 */
	@SuppressWarnings("serial")
	private static class InputFilePanel extends JPanel {
		private JTextField textfield;
		private JButton browseButton;

		public InputFilePanel(String label) {
			this.setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
			this.add(new JLabel(label));
			this.add(textfield = new JTextField(40));
			this.add(browseButton = new JButton("Browse..."));
			browseButton.addActionListener(new ActionListener() {

				public void actionPerformed(ActionEvent e) {
					JFileChooser fileChooser = new JFileChooser(System.getProperty("user.dir"));
					fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
					int showOpenDialog = fileChooser
							.showOpenDialog(InputFilePanel.this);
					if (showOpenDialog == JFileChooser.APPROVE_OPTION) {
						textfield.setText(fileChooser.getSelectedFile()
								.getAbsolutePath());
					}
				}
			});
		}

		public String getMainFilePath() {
			return textfield.getText();
		}
	}

}
