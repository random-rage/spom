package ru.rage.spom;

import ru.rage.spoml.ArgType;
import ru.rage.spoml.Argument;
import ru.rage.spoml.Command;

import javax.swing.text.BadLocationException;
import java.util.ArrayList;
import java.util.Formatter;
import java.util.List;

public class Machine
{
    private List<Command> program;      // Machine program memory
    private int[]         memory;       // Machine data memory
    private int           rx, pc;       // Main register and program counter
    private MachineState  state;        // Machine state

    private List<IMachineListener> listeners;

    public Machine()
    {
        memory = Program.DEFAULT_MEMORY;
        rx = 0;
        pc = 0;
        listeners = new ArrayList<>();
        state = MachineState.STOPPED;
    }

    void addListener(IMachineListener listener)
    {
        listeners.add(listener);
    }

    //region Memory
    int[] memoryDump()
    {
        return memory;
    }

    private void setMemory(int address, int value)
    {
        if (address >= memory.length || address < 0)
            throw new IndexOutOfBoundsException(String.format("Address %d out of bounds", address));

        memory[address] = value;

        for (IMachineListener listener : listeners)
            listener.OnMemoryChanged(address, value);
    }
    //endregion

    //region Registers
    private void setRx(int value)
    {
        rx = value;
        for (IMachineListener listener : listeners)
            listener.OnRxChanged(value);
    }

    int getPc()
    {
        return pc;
    }

    private void setPc(int value) throws BadLocationException
    {
        pc = value;
        for (IMachineListener listener : listeners)
            listener.OnPcChanged(value);
    }

    private void incPc() throws BadLocationException
    {
        setPc(pc + 1);
    }
    //endregion

    MachineState getState()
    {
        return state;
    }

    //region Machine control

    String loadProgram(String file, String libPath) throws Exception
    {
        Loader loader = new Loader(file, libPath);
        program = loader.getCode();
        int[] data = loader.getData();

        int memLen = (data.length > memory.length) ? memory.length : data.length;
        System.arraycopy(data, 0, memory, 0, memLen);

        Formatter formatter = new Formatter();
        for (Command cmd : program)
            formatter.format("%s\n", cmd);

        return formatter.toString();
    }

    void continueExecution() throws BadLocationException, MachineException
    {
        state = MachineState.RUNNING;
        for (IMachineListener listener : listeners)
            listener.OnStateChanged(state);

        while (step()) ;
    }

    boolean step() throws BadLocationException, MachineException
    {
        if (pc >= program.size() || !exec(program.get(pc)))
        {
            stop();
            return false;
        }
        incPc();

        state = MachineState.PAUSED;
        for (IMachineListener listener : listeners)
            listener.OnStateChanged(state);

        return true;
    }

    void stop() throws BadLocationException
    {
        setPc(0);
        setRx(0);
        state = MachineState.STOPPED;

        for (IMachineListener listener : listeners)
            listener.OnStateChanged(state);
    }
    //endregion

    private boolean exec(Command cmd) throws BadLocationException, MachineException
    {
        Argument arg = cmd.getArg();
        switch (cmd.getType())
        {
            case NOP:
                break;

            case ADD:
                if (arg.getType() == ArgType.INDIRECT)
                    setRx(rx + memory[arg.getValue()]);
                else if (arg.getType() == ArgType.IMMEDIATE)
                    setRx(rx + arg.getValue());
                break;

            case SUB:
                if (arg.getType() == ArgType.INDIRECT)
                    setRx(rx - memory[arg.getValue()]);
                else if (arg.getType() == ArgType.IMMEDIATE)
                    setRx(rx - arg.getValue());
                break;

            case INC:
                setRx(rx + 1);
                break;

            case DEC:
                setRx(rx - 1);
                break;

            case LDR:
                if (arg.getType() == ArgType.INDIRECT)
                    setRx(memory[arg.getValue()]);
                else if (arg.getType() == ArgType.IMMEDIATE)
                    setRx(arg.getValue());
                break;

            case STR:
                if (arg.getType() == ArgType.INDIRECT)
                    setMemory(arg.getValue(), rx);
                break;

            case IN:
                if (listeners.size() < 1)
                    throw new MachineException("No input listeners");
                setRx(listeners.get(0).OnInput());
                break;

            case OUT:
                for (IMachineListener listener : listeners)
                    listener.OnOutput(rx);
                break;

            case JMP:
                if (arg.getType() == ArgType.INDIRECT)
                    setPc(memory[arg.getValue()] - 1);
                else if (arg.getType() == ArgType.IMMEDIATE)
                    setPc(arg.getValue() - 1);
                break;

            case IFZ:
                if (rx != 0)
                    incPc();
                break;

            case IFN:
                if (rx >= 0)
                    incPc();
                break;

            case HLT:
                return false;

            default:
                throw new MachineException("Unknow command: %s", cmd);
        }
        return true;
    }
}
