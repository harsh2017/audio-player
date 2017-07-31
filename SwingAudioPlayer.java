package net.codejava.audio;

import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;        
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.UnsupportedAudioFileException;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.filechooser.FileFilter;
//import sun.audio.AudioPlayer;
//import sun.audio.AudioPlayer;

/**
 * A Swing-based audio player program.
 * NOTE: Can play only WAVE (*.wav) file.
 * @author www.codejava.net
 *
 */
public class SwingAudioPlayer extends JFrame implements ActionListener {
	private final AudioPlayer player = new AudioPlayer();
	private Thread playbackThread;
	private PlayingTimer timer;

	private boolean isPlaying = false;
	private boolean isPause = false;
	
	private String audioFilePath;
	private String lastOpenPath;
	
	private final JLabel labelFileName = new JLabel("Playing File:");
	private final JLabel labelTimeCounter = new JLabel("00:00:00");
	private final JLabel labelDuration = new JLabel("00:00:00");
	
	private final JButton buttonOpen = new JButton("Open");
	private final JButton buttonPlay = new JButton("Play");
	private final JButton buttonPause = new JButton("Pause");
	
	private final JSlider sliderTime = new JSlider();
	
	// Icons used for buttons
	private final ImageIcon iconOpen = new ImageIcon(getClass().getResource(
			"/net/codejava/audio/images/Open.png"));
	private final ImageIcon iconPlay = new ImageIcon(getClass().getResource(
			"/net/codejava/audio/images/Play.gif"));
	private final ImageIcon iconStop = new ImageIcon(getClass().getResource(
			"/net/codejava/audio/images/Stop.gif"));
	private final ImageIcon iconPause = new ImageIcon(getClass().getResource(
			"/net/codejava/audio/images/Pause.png"));
	
	
	public SwingAudioPlayer() {
		super("Java Audio Player");
		setLayout(new GridBagLayout());
		GridBagConstraints constraints = new GridBagConstraints();
		constraints.insets = new Insets(5, 5, 5, 5);
		constraints.anchor = GridBagConstraints.WEST;
		
		buttonOpen.setFont(new Font("Sans", Font.BOLD, 14));
		buttonOpen.setIcon(iconOpen);
		
		buttonPlay.setFont(new Font("Sans", Font.BOLD, 14));
		buttonPlay.setIcon(iconPlay);
		buttonPlay.setEnabled(false);
		
		buttonPause.setFont(new Font("Sans", Font.BOLD, 14));
		buttonPause.setIcon(iconPause);
		buttonPause.setEnabled(false);
		
		labelTimeCounter.setFont(new Font("Sans", Font.BOLD, 12));
		labelDuration.setFont(new Font("Sans", Font.BOLD, 12));
		
		sliderTime.setPreferredSize(new Dimension(400, 20));
		sliderTime.setEnabled(false);
		sliderTime.setValue(0);

		constraints.gridx = 0;
		constraints.gridy = 0;
		constraints.gridwidth = 3;
		add(labelFileName, constraints);
		
		constraints.anchor = GridBagConstraints.CENTER;
		constraints.gridy = 1;
		constraints.gridwidth = 1;
		add(labelTimeCounter, constraints);
		
		constraints.gridx = 1;
		add(sliderTime, constraints);
		
		constraints.gridx = 2;
		add(labelDuration, constraints);
		
		JPanel panelButtons = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 5));
		panelButtons.add(buttonOpen);
		panelButtons.add(buttonPlay);
		panelButtons.add(buttonPause);
		
		constraints.gridwidth = 3;
		constraints.gridx = 0;
		constraints.gridy = 2;
		add(panelButtons, constraints);
		
		buttonOpen.addActionListener(this);
		buttonPlay.addActionListener(this);
		buttonPause.addActionListener(this);
		
		pack();
		setResizable(false);
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setLocationRelativeTo(null);
	}

	/**
	 * Handle click events on the buttons.
     * @param event
	 */
	@Override
	public void actionPerformed(ActionEvent event) {
		Object source = event.getSource();
		if (source instanceof JButton) {
			JButton button = (JButton) source;
			if (button == buttonOpen) {
				openFile();
			} else if (button == buttonPlay) {
				if (!isPlaying) {
					playBack();
				} else {
					stopPlaying();
				}
			} else if (button == buttonPause) {
				if (!isPause) {
					pausePlaying();
				} else {
					resumePlaying();
				}
			}
		}
	}

	private void openFile() {
                @SuppressWarnings("UnusedAssignment")
		JFileChooser fileChooser = null;
		
		if (lastOpenPath != null && !lastOpenPath.equals("")) {
			fileChooser = new JFileChooser(lastOpenPath);
		} else {
			fileChooser = new JFileChooser();
		}
		
		FileFilter wavFilter = new FileFilter() {
			@Override
			public String getDescription() {
				return "Sound file (*.wav)";
			}

			
                        @Override
			public boolean accept(File file) {
				if (file.isDirectory()) {
					return true;
				} else {
					return file.getName().toLowerCase().endsWith(".wav");
				}
			}
		};

		
		fileChooser.setFileFilter(wavFilter);
		fileChooser.setDialogTitle("Open Audio File");
		fileChooser.setAcceptAllFileFilterUsed(false);

		int userChoice = fileChooser.showOpenDialog(this);
		if (userChoice == JFileChooser.APPROVE_OPTION) {
			audioFilePath = fileChooser.getSelectedFile().getAbsolutePath();
			lastOpenPath = fileChooser.getSelectedFile().getParent();
			if (isPlaying || isPause) {
				stopPlaying();
				while (player.getAudioClip().isRunning()) {
					try {
						Thread.sleep(100);
					} catch (InterruptedException ex) {
					}
				}
			}
			playBack();
		}
	}

	/**
	 * Start playing back the sound.
	 */
	private void playBack() {
		timer = new PlayingTimer(labelTimeCounter, sliderTime);
		timer.start();
		isPlaying = true;
		playbackThread = new Thread(new Runnable() {

			@Override
			public void run() {
                            try {
                                buttonPlay.setText("Stop");
                                buttonPlay.setIcon(iconStop);
                                buttonPlay.setEnabled(true);
                                buttonPause.setText("Pause");
                                buttonPause.setEnabled(true);
                                try {
                                    player.load(audioFilePath);
                                } catch (UnsupportedAudioFileException | IOException | LineUnavailableException ex) {
                                    Logger.getLogger(SwingAudioPlayer.class.getName()).log(Level.SEVERE, null, ex);
                                }
                                timer.setAudioClip(player.getAudioClip());
                                labelFileName.setText("Playing File: " + audioFilePath);
                                sliderTime.setMaximum((int) player.getClipSecondLength());
                                labelDuration.setText(player.getClipLengthString());
                                player.play();
                                resetControls();
                                
                            } catch (IOException ex) {
                                Logger.getLogger(SwingAudioPlayer.class.getName()).log(Level.SEVERE, null, ex);
                            }

			}
		});

		playbackThread.start();
	}

	private void stopPlaying() {
		isPause = false;
		buttonPause.setText("Pause");
		buttonPause.setEnabled(false);
		timer.reset();
		timer.interrupt();
		player.stop();
		playbackThread.interrupt();
	}
	
	private void pausePlaying() {
		buttonPause.setText("Resume");
		isPause = true;
		player.pause();
		timer.pauseTimer();
		playbackThread.interrupt();
	}
	
	private void resumePlaying() {
		buttonPause.setText("Pause");
		isPause = false;
		player.resume();
		timer.resumeTimer();
		playbackThread.interrupt();		
	}
	
	private void resetControls() {
		timer.reset();
		timer.interrupt();

		buttonPlay.setText("Play");
		buttonPlay.setIcon(iconPlay);
		
		buttonPause.setEnabled(false);
		
		isPlaying = false;		
	}
	
	/**
	 * Launch the program
     * @param args
	 */
	public static void main(String[] args) {
		
		try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		} catch (ClassNotFoundException | InstantiationException | IllegalAccessException | UnsupportedLookAndFeelException ex) {
		}
		
		SwingUtilities.invokeLater(new Runnable() {
			
			@Override
			public void run() {
				new SwingAudioPlayer().setVisible(true);
			}
		});
	}


}