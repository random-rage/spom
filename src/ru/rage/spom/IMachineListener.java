package ru.rage.spom;

import javax.swing.text.BadLocationException;

interface IMachineListener
{
    void OnMemoryChanged(int address, int value);
    void OnRxChanged(int value);
    void OnPcChanged(int value) throws BadLocationException;
    int OnInput();
    void OnOutput(int output);
    void OnStateChanged(MachineState newState);
}