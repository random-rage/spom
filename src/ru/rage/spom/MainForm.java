package ru.rage.spom;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.text.BadLocationException;
import java.awt.event.ActionListener;
import java.util.Formatter;

public class MainForm extends JFrame implements IMachineListener
{
    private JFileChooser fileChooser;
    private JPanel       mainPanel;
    private JTabbedPane  tabPane;
    private JPanel       dbgTab;
    private JPanel       memTab;
    private JPanel       mainTab;
    private JLabel       stateLabel;
    private JTextField   programTextField;
    private JButton      programBrowseButton;
    private JLabel       programLabel;
    private JButton      loadButton;
    private JButton      stopButton;
    private JButton      continueButton;
    private JPanel       inTab;
    private JPanel       outTab;
    private JTextArea    inputBytes;
    private JLabel       inputBytesLabel;
    private JLabel       inputAsciiLabel;
    private JTextArea    outputBytes;
    private JLabel       outputBytesLabel;
    private JTextArea    outputAscii;
    private JLabel       outputAsciiLabel;
    private JRadioButton decRadioButton;
    private JRadioButton hexRadioButton;
    private JTextArea    memTextArea;
    private JTextArea    progMemTextArea;
    private JButton      stepButton;
    private JLabel       rxDecLabel;
    private JLabel       rxHexLabel;
    private JLabel       pcDecLabel;
    private JLabel       pcHexLabel;
    private JButton      clearBtn;
    private JLabel       libPathLabel;
    private JTextField   libPathTextField;
    private JButton      libPathBrowseButton;
    private JScrollPane  memScrollPane;
    private JScrollPane  progScrollPane;
    private JTextArea    inputAscii;
    private JScrollPane  inputBytesScrollPane;
    private JScrollPane  inputAsciiScrollPane;
    private JScrollPane  outputBytesScrollPane;
    private JScrollPane  outputAsciiScrollPane;

    private Machine machine;

    MainForm()
    {
        fileChooser = new JFileChooser();
        fileChooser.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
        setContentPane(mainPanel);
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        setTitle("SPOM");
        pack();
        setVisible(true);
        machine = new Machine();
        machine.addListener(this);

        tabPane.addChangeListener(new ChangeListener()
        {
            private int lastTabId = 0;

            @Override
            public void stateChanged(ChangeEvent e)
            {
                if (lastTabId != 1 && tabPane.getSelectedIndex() == 1)    // Memory tab
                    updateMemoryView();

                lastTabId = tabPane.getSelectedIndex();
            }
        });

        ChangeListener memViewChangeListener = new ChangeListener()
        {
            private boolean lastHex = false;

            @Override
            public void stateChanged(ChangeEvent e)
            {
                if (lastHex != hexRadioButton.isSelected())
                    updateMemoryView();

                lastHex = hexRadioButton.isSelected();
            }
        };

        ActionListener browseAction = e ->
        {
            Object source = e.getSource();
            if (fileChooser.showOpenDialog(mainPanel) == JFileChooser.APPROVE_OPTION)
            {
                if (source == programBrowseButton)
                    programTextField.setText(fileChooser.getSelectedFile().getAbsolutePath());
                else if (source == libPathBrowseButton)
                    libPathTextField.setText(fileChooser.getSelectedFile().getAbsolutePath());
            }
        };

        decRadioButton.addChangeListener(memViewChangeListener);
        hexRadioButton.addChangeListener(memViewChangeListener);
        programBrowseButton.addActionListener(browseAction);
        libPathBrowseButton.addActionListener(browseAction);
        loadButton.addActionListener(e ->
        {
             try
             {
                 String program = machine.loadProgram(programTextField.getText(), libPathTextField.getText());
                 machine.stop();
                 progMemTextArea.setText(program);
                 stepButton.setEnabled(true);
                 continueButton.setEnabled(true);
                 stopButton.setEnabled(true);
                 OnPcChanged(machine.getPc());
             }
             catch (Exception ex)
             {
                 JOptionPane.showMessageDialog(mainPanel, ex, "Error", JOptionPane.ERROR_MESSAGE);
             }
        });
        continueButton.addActionListener(e ->
        {
             MachineState state = machine.getState();
             if (state == MachineState.PAUSED || state == MachineState.STOPPED)
                 try
                 {
                     machine.continueExecution();
                 }
                 catch (Exception ex)
                 {
                     JOptionPane.showMessageDialog(mainPanel, ex, "Error", JOptionPane.ERROR_MESSAGE);
                 }
        });
        stopButton.addActionListener(e ->
        {
            try
            {
                machine.stop();
                continueButton.setEnabled(true);
            }
            catch (Exception ex)
            {
                JOptionPane.showMessageDialog(mainPanel, ex, "Error", JOptionPane.ERROR_MESSAGE);
            }
        });
        stepButton.addActionListener(e ->
        {
            MachineState state = machine.getState();
            if (state == MachineState.PAUSED || state == MachineState.STOPPED)
                try
                {
                    machine.step();
                }
                catch (Exception ex)
                {
                    JOptionPane.showMessageDialog(mainPanel, ex, "Error", JOptionPane.ERROR_MESSAGE);
                }
        });
        clearBtn.addActionListener(e ->
        {
            outputBytes.setText("");
            outputAscii.setText("");
        });
    }

    private void updateMemoryView()
    {
        int[] mem = machine.memoryDump();
        Formatter formatter = new Formatter();

        for (int i = 0; i < mem.length; i++)
            formatter.format("%08x\t" + ((hexRadioButton.isSelected()) ? "%08x" : "%d") + "\t'%c'\n",
                             i,
                             mem[i],
                             (char)mem[i]);

        memTextArea.setText(formatter.toString());
    }

    @Override
    public void OnMemoryChanged(int address, int value)
    {
        updateMemoryView();
    }

    @Override
    public void OnRxChanged(int value)
    {
        rxDecLabel.setText("RX: " + Integer.toString(value));
        rxHexLabel.setText("RX: 0x" + Integer.toHexString(value).toUpperCase());
    }

    @Override
    public void OnPcChanged(int value) throws BadLocationException
    {
        pcDecLabel.setText("PC: " + Integer.toString(value));
        pcHexLabel.setText("PC: 0x" + Integer.toHexString(value).toUpperCase());
        if (value >= 0 && progMemTextArea.getLineCount() > value)
        {
            int start = progMemTextArea.getLineStartOffset(value);
            int end = progMemTextArea.getLineEndOffset(value);
            progMemTextArea.grabFocus();
            progMemTextArea.select(start, end);
        }
    }

    @Override
    public int OnInput()
    {
        if (inputBytes.getText().length() > 0)
        {
            String[] s = inputBytes.getText().split(" ");
            String inByte = s[0];
            if (s.length > 1)
                inputBytes.replaceRange("", 0, inByte.length() + 1);
            else
                inputBytes.setText("");
            return Integer.parseInt(inByte);
        }
        else
            return -1;
    }

    @Override
    public void OnOutput(int output)
    {
        outputBytes.append(Integer.toString(output) + " ");
        outputAscii.append(Character.toString((char)output));
    }

    @Override
    public void OnStateChanged(MachineState newState)
    {
        String s = "Machine state: ";
        switch (newState)
        {
            case STOPPED:
                stopButton.setEnabled(false);
                s += "stopped";
                break;
            case PAUSED:
                stopButton.setEnabled(true);
                s += "paused";
                break;
            case RUNNING:
                stopButton.setEnabled(true);
                s += "running";
                break;
            default:
                s += "unknow";
                break;
        }
        stateLabel.setText(s);
    }
}
