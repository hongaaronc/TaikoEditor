package alchyr.taikoedit.desktop.config;

import com.badlogic.gdx.Graphics;
import com.badlogic.gdx.files.FileHandle;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.io.File;
import java.net.MalformedURLException;
import java.nio.file.Paths;

import static alchyr.taikoedit.desktop.DesktopLauncher.*;

public class ConfigMenu implements ActionListener, WindowListener {
    private JFrame frame;
    private ProgramConfig programConfig;
    private FileHandle graphicsFile;
    private Graphics.DisplayMode fullscreenMode;

    private JCheckBox fullscreenToggle;

    private JTextField widthField;
    private JTextField heightField;
    private JTextField songsField;

    private JButton accept;
    private JButton folder;

    private JFileChooser fileChooser;

    public int state; //0 = active, 1 = fail, 2 = success

    private Object lock;

    static
    {
        try
        {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        }
        catch (Exception e)
        {
            logger.error("Failed to set system look and feel for Swing.", e);
        }
    }

    public ConfigMenu(ProgramConfig programConfig, FileHandle graphicsFile, Graphics.DisplayMode fullscreenMode, Object lock)
    {
        this.programConfig = programConfig;
        this.graphicsFile = graphicsFile;
        this.fullscreenMode = fullscreenMode;

        this.lock = lock;

        state = 0;

        frame = new JFrame("Config");
        frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);

        Container contentPane = frame.getContentPane();

        GroupLayout layout = new GroupLayout(contentPane);
        contentPane.setLayout(layout);

        layout.setAutoCreateContainerGaps(true);
        layout.setAutoCreateGaps(true);

        fileChooser = new JFileChooser();
        fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);

        accept = new JButton("OK");
        JButton cancel = new JButton("Cancel");
        try
        {
            folder = new JButton(new ImageIcon(getLocalFile("folder.gif").file().toURI().toURL()));
        }
        catch (MalformedURLException e)
        {
            logger.error("Failed to load folder icon.");
            e.printStackTrace();
            folder = new JButton("Browse");
        }

        fullscreenToggle = new JCheckBox("Fullscreen", programConfig.fullscreen);
        fullscreenToggle.setSelected(programConfig.fullscreen);

        JLabel widthLabel = new JLabel("Width");
        JLabel heightLabel = new JLabel("Height");

        widthField = new JTextField(Integer.toString(programConfig.width), 60);
        heightField = new JTextField(Integer.toString(programConfig.height), 60);

        IntFilter filter = new IntFilter();

        ((PlainDocument) widthField.getDocument()).setDocumentFilter(filter);
        ((PlainDocument) heightField.getDocument()).setDocumentFilter(filter);

        widthField.setEnabled(!programConfig.fullscreen);
        heightField.setEnabled(!programConfig.fullscreen);

        JLabel osuLocation = new JLabel("osu! Folder");
        songsField = new JTextField(getSongFolder(), 60);

        fullscreenToggle.setActionCommand("FULLSCREEN");
        accept.setActionCommand("ACCEPT");
        cancel.setActionCommand("CANCEL");
        folder.setActionCommand("FOLDER");

        layout.setHorizontalGroup(
                layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup()
                                .addComponent(fullscreenToggle)
                                .addComponent(widthLabel)
                                .addComponent(heightLabel)
                                .addComponent(osuLocation)
                                .addComponent(accept))
                        .addGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                                .addComponent(widthField)
                                .addComponent(heightField)
                                .addComponent(songsField))
                        .addGroup(layout.createParallelGroup()
                            .addComponent(folder)
                            .addComponent(cancel))
        );
        layout.setVerticalGroup(
                layout.createSequentialGroup()
                        .addComponent(fullscreenToggle)
                        .addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                .addComponent(widthLabel)
                                .addComponent(widthField))
                        .addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                .addComponent(heightLabel)
                                .addComponent(heightField))
                        .addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                .addComponent(osuLocation)
                                .addComponent(songsField)
                                .addComponent(folder))
                        .addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                .addComponent(accept)
                                .addComponent(cancel))
        );

        //accept.setBounds(25, 250, 90, 30);
        //cancel.setBounds(125, 250, 90, 30);

        /*frame.add(fullscreenToggle);
        frame.add(widthLabel);
        frame.add(widthField);
        frame.add(heightLabel);
        frame.add(heightField);
        frame.add(accept);
        frame.add(cancel);*/

        fullscreenToggle.addActionListener(this);
        accept.addActionListener(this);
        cancel.addActionListener(this);
        folder.addActionListener(this);

        songsField.getDocument().addDocumentListener(new DocumentListener() {
            public void changedUpdate(DocumentEvent e) {
                testSongFolder(songsField.getText());
            }
            public void removeUpdate(DocumentEvent e) {
                testSongFolder(songsField.getText());
            }
            public void insertUpdate(DocumentEvent e) {
                testSongFolder(songsField.getText());
            }
        });

        frame.addWindowListener(this);

        frame.pack();
        frame.setResizable(false);
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        switch (e.getActionCommand())
        {
            case "FULLSCREEN":
                widthField.setEnabled(!fullscreenToggle.isSelected());
                heightField.setEnabled(!fullscreenToggle.isSelected());
                break;
            case "ACCEPT":
                //validate
                programConfig.fullscreen = fullscreenToggle.isSelected();
                programConfig.osuFolder = songsField.getText();

                if (!programConfig.fullscreen)
                {
                    //width and height must be valid
                    try {
                        programConfig.width = Integer.parseInt(widthField.getText());
                        programConfig.height = Integer.parseInt(heightField.getText());

                        if (programConfig.width > 0 && programConfig.height > 0)
                        {
                            //Give a warning for configurations that are smaller than recommended minimum or larger than the resolution of the display
                            if (programConfig.width > fullscreenMode.width || programConfig.height > fullscreenMode.height)
                            {
                                int result = JOptionPane.showConfirmDialog(frame, "Your chosen resolution may not fit on your screen. Are you sure you wish to continue?", "Warning", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);

                                if (result != JOptionPane.OK_OPTION)
                                {
                                    break;
                                }
                            }

                            finish();
                        }
                        else
                        {
                            logger.error("Somehow an invalid dimension was entered.");
                            JOptionPane.showMessageDialog(frame, "Invalid dimensions entered.", "Invalid Input", JOptionPane.WARNING_MESSAGE);
                        }
                    }
                    catch (Exception ex) {
                        //Error will be caught here if game crashes.
                        logger.error("Error; ", ex);
                        System.exit(ex.hashCode());
                    }
                }
                else
                {
                    finish();
                }
                break;
            case "CANCEL":
                frame.dispatchEvent(new WindowEvent(frame, WindowEvent.WINDOW_CLOSING));
                break;
            case "FOLDER":
                if (fileChooser.showOpenDialog(frame) == JFileChooser.APPROVE_OPTION) {
                    File file = fileChooser.getSelectedFile();

                    songsField.setText(file.getPath());
                    testSongFolder(file);
                }
        }
    }

    private void finish()
    {
        graphicsFile.writeString(programConfig.toString(), false);
        state = 2;

        frame.dispatchEvent(new WindowEvent(frame, WindowEvent.WINDOW_CLOSING));
    }

    @Override
    public void windowOpened(WindowEvent e) {

    }

    @Override
    public void windowClosing(WindowEvent e) {
        synchronized (lock) {
            if (state == 0)
                state = 1;
            lock.notify();
        }
    }

    @Override
    public void windowClosed(WindowEvent e) {

    }

    @Override
    public void windowIconified(WindowEvent e) {

    }

    @Override
    public void windowDeiconified(WindowEvent e) {

    }

    @Override
    public void windowActivated(WindowEvent e) {

    }

    @Override
    public void windowDeactivated(WindowEvent e) {

    }

    private static class IntFilter extends DocumentFilter {
        @Override
        public void insertString(FilterBypass fb, int offset, String string,
                                 AttributeSet attr) throws BadLocationException {

            Document doc = fb.getDocument();
            StringBuilder sb = new StringBuilder();
            sb.append(doc.getText(0, doc.getLength()));
            sb.insert(offset, string);

            if (test(sb.toString())) {
                super.insertString(fb, offset, string, attr);
            }
        }

        private boolean test(String text) {
            try {
                return Integer.parseInt(text) > 0;
            } catch (NumberFormatException e) {
                return false;
            }
        }

        @Override
        public void replace(FilterBypass fb, int offset, int length, String text,
                            AttributeSet attrs) throws BadLocationException {

            Document doc = fb.getDocument();
            StringBuilder sb = new StringBuilder();
            sb.append(doc.getText(0, doc.getLength()));
            sb.replace(offset, offset + length, text);

            if (test(sb.toString())) {
                super.replace(fb, offset, length, text, attrs);
            }
        }

        @Override
        public void remove(FilterBypass fb, int offset, int length)
                throws BadLocationException {
            Document doc = fb.getDocument();
            StringBuilder sb = new StringBuilder();
            sb.append(doc.getText(0, doc.getLength()));
            sb.delete(offset, offset + length);

            if (test(sb.toString())) {
                super.remove(fb, offset, length);
            }
        }
    }


    private String getSongFolder()
    {
        String location;

        String OS = (System.getProperty("os.name")).toUpperCase();

        if (OS.contains("WIN"))
        {
            location = System.getenv("LOCALAPPDATA");
            if (location != null && testSongFolder(location = location + File.separatorChar + "osu!"))
            {
                return location;
            }
            location = System.getenv("APPDATA");
            if (location != null && testSongFolder(location = location + File.separatorChar + "osu!"))
            {
                return location;
            }
            location = System.getenv("ProgramFiles(x86)");
            if (location != null && testSongFolder(location = location + File.separatorChar + "osu!"))
            {
                return location;
            }
            location = System.getenv("ProgramFiles");
            if (location != null && testSongFolder(location = location + File.separatorChar + "osu!"))
            {
                return location;
            }
        }
        else
        {
            //Eh I'm too lazy to deal with other operating systems. Locate it yourself.
        }
        return "";
    }

    private boolean testSongFolder(String location)
    {
        File f = Paths.get(location).toFile();

        return testSongFolder(f);
    }
    private boolean testSongFolder(File f)
    {
        if (f != null && f.isDirectory() && songsExists(f))
        {
            fileChooser.setCurrentDirectory(f);
            accept.setEnabled(true);
            return true;
        }
        accept.setEnabled(false);
        return false;
    }
    private boolean songsExists(File directory)
    {
        File[] files = directory.listFiles(File::isDirectory);
        if (files != null)
        {
            for (File f : files)
            {
                if (f.getName().equals("Songs"))
                {
                    return true;
                }
            }
        }
        return false;
    }
}



        /*layout.putConstraint(SpringLayout.WEST, fullscreenToggle, 50, SpringLayout.WEST, frame);
        layout.putConstraint(SpringLayout.EAST, fullscreenToggle, 50, SpringLayout.EAST, frame);
        layout.putConstraint(SpringLayout.NORTH, fullscreenToggle, 10, SpringLayout.NORTH, frame);

        layout.putConstraint(SpringLayout.WEST, widthLabel, 15, SpringLayout.WEST, frame);
        layout.putConstraint(SpringLayout.NORTH, widthLabel, 30, SpringLayout.SOUTH, fullscreenToggle);

        layout.putConstraint(SpringLayout.WEST, widthField, 5, SpringLayout.EAST, widthLabel);
        layout.putConstraint(SpringLayout.NORTH, widthField, 13, SpringLayout.SOUTH, fullscreenToggle);

        layout.putConstraint(SpringLayout.WEST, heightLabel, 20, SpringLayout.WEST, frame);
        layout.putConstraint(SpringLayout.NORTH, heightLabel, 20, SpringLayout.SOUTH, widthLabel);

        layout.putConstraint(SpringLayout.WEST, heightField, 5, SpringLayout.EAST, heightLabel);
        layout.putConstraint(SpringLayout.NORTH, heightField, 18, SpringLayout.SOUTH, widthLabel);

        layout.putConstraint(SpringLayout.NORTH, accept, 40, SpringLayout.SOUTH, heightLabel);
        layout.putConstraint(SpringLayout.WEST, accept, 25, SpringLayout.WEST, frame);
        layout.putConstraint(SpringLayout.SOUTH, accept, 25, SpringLayout.SOUTH, frame);

        layout.putConstraint(SpringLayout.NORTH, cancel, 40, SpringLayout.SOUTH, heightLabel);
        layout.putConstraint(SpringLayout.WEST, cancel, 25, SpringLayout.EAST, accept);
        layout.putConstraint(SpringLayout.EAST, cancel, 25, SpringLayout.EAST, frame);
        layout.putConstraint(SpringLayout.SOUTH, cancel, 25, SpringLayout.SOUTH, frame);*/
        //Spring layout didn't work out. :(