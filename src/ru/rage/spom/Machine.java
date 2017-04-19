package ru.rage.spom;

import javax.swing.text.BadLocationException;
import java.io.*;
import java.util.ArrayList;
import java.util.List;

interface MachineListener
{
    void OnMemoryChanged(int address, int value);
    void OnRxChanged(int value);
    void OnPcChanged(int value) throws BadLocationException;
    int  OnInput();
    void OnOutput(int output);
    void OnStateChanged(MachineState newState);
}

enum MachineState
{
    STOPPED,
    RUNNING,
    PAUSED
}

public class Machine
{
    private int[] program;      // Machine program memory
    private int[] offsets;      // Offsets in program memory
    private int[] memory;       // Machine data memory
    private int rx, pc;         // Main register and program counter
    private MachineState state; // Machine state

    private List<MachineListener> listeners;

    public Machine()
    {
        memory = Program.DEFAULT_MEMORY;
        rx = 0;
        pc = 0;
        listeners = new ArrayList<>();
        state = MachineState.STOPPED;
    }

    void addListener(MachineListener listener)
    {
        listeners.add(listener);
    }

    //region Memory
    int[] memoryDump()
    {
        return memory;
    }

    public int getMemory(int address)
    {
        return memory[address];
    }

    private void setMemory(int address, int value)
    {
        if (address >= memory.length || address < 0)
            throw new IndexOutOfBoundsException(String.format("Address %d out of bounds", address));

        memory[address] = value;

        for (MachineListener listener : listeners)
            listener.OnMemoryChanged(address, value);
    }
    //endregion

    //region Registers
    private void setRx(int value)
    {
        rx = value;
        for (MachineListener listener : listeners)
            listener.OnRxChanged(value);
    }

    int getPc()
    {
        return pc;
    }

    private void setPc(int value) throws IllegalArgumentException, BadLocationException
    {
        pc = value;
        for (MachineListener listener : listeners)
            listener.OnPcChanged(value);
    }

    private int incPc() throws IllegalArgumentException, BadLocationException
    {
        setPc(pc + 1);
        return pc;
    }
    //endregion

    MachineState getState()
    {
        return state;
    }

    private static int getCommandLength(int opcode)
    {
        opcode &= 0x0f;
        if ((opcode > 0 && opcode < 3) || (opcode > 4 && opcode < 7) || opcode == 9)
            return 2;
        else
            return 1;
    }

    //region Machine control
    /**
     * Loads executable file into program memory
     * @param file SPOM executable program file
     * @throws IOException if file not found or file read error
     */
    String loadProgram(File file) throws IOException
    {
        String str = new String();
        FileInputStream fin = new FileInputStream(file);
        int len = (int) file.length();
        boolean skip = false;

        program = new int[len + 1];
        offsets = new int[len];

        for (int i = 0, j = 0; i < len; i++)
        {
            program[i] = fin.read();
            if (skip)
            {
                skip = false;
                str = String.format("%s %02x\n", str, program[i]);
            }
            else
            {
                skip = getCommandLength(program[i]) == 2;
                offsets[j++] = i;

                str = String.format("%s%02x", str, program[i]);
                if (!skip)
                    str += "\n";
            }
        }
        return str;
    }

    /**
     * Continues program memory execution
     * @throws NotActiveException if no input listeners found
     */
    void continueExecution() throws NotActiveException, IllegalArgumentException, BadLocationException
    {
        int offset = offsets[pc];

        state = MachineState.RUNNING;
        for (MachineListener listener : listeners)
            listener.OnStateChanged(state);

        while (offset < offsets.length)
        {
            if (!exec(program[offset], program[offset + 1]))
            {
                stop();
                return;
            }
            incPc();
            if (pc + 1 < offsets.length)
                offset = offsets[pc];
            else
                offset = program.length;
        }
        stop();
    }

    /**
     * Performs step in program execution
     * @throws NotActiveException
     */
    void step() throws NotActiveException, IllegalArgumentException, BadLocationException
    {
        if (pc + 1 > offsets.length || !exec(program[offsets[pc]], program[offsets[pc] + 1]))
        {
            stop();
            return;
        }
        incPc();

        state = MachineState.PAUSED;

        for (MachineListener listener : listeners)
            listener.OnStateChanged(state);
    }

    void stop() throws IllegalArgumentException, BadLocationException
    {
        setPc(0);
        setRx(0);
        state = MachineState.STOPPED;

        for (MachineListener listener : listeners)
            listener.OnStateChanged(state);
    }
    //endregion

    /**
     * Performs program instruction execution
     * @param opcode Operation code
     * @param arg Instruction argument
     * @return False if machine stops
     * @throws NotActiveException if no input listeners found
     * @throws IllegalArgumentException if opcode is unknow
     */
    private boolean exec(int opcode, int arg) throws NotActiveException, IllegalArgumentException, BadLocationException
    {
        switch (opcode)
        {
            case 0:
                break;
            case 1:
                setRx(rx + memory[arg]);
                break;
            case 2:
                setRx(rx - memory[arg]);
                break;
            case 3:
                setRx(rx + 1);
                break;
            case 4:
                setRx(rx + 1);
                break;
            case 5:
                setRx(memory[arg]);
                break;
            case 6:
                setMemory(arg, rx);
                break;
            case 7:
                if (listeners.size() < 1)
                    throw new NotActiveException("No input listeners");
                setRx(listeners.get(0).OnInput());
                break;
            case 8:
                for (MachineListener listener : listeners)
                    listener.OnOutput(rx);
                break;
            case 9:
                setPc(memory[arg] - 1);
                break;
            case 10:
                if (rx != 0)
                    incPc();
                break;
            case 11:
                if (rx >= 0)
                    incPc();
                break;
            case 12:
                return false;
            case 17:
                setRx(rx + arg);
                break;
            case 18:
                setRx(rx + arg);
                break;
            case 21:
                setRx(arg);
                break;
            case 25:
                setPc(arg - 1);
                break;
            default:
                throw new IllegalArgumentException(String.format("Illegal opcode: %d", opcode));
        }
        return true;
    }
}
