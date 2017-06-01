package ru.rage.spom;

class MachineException extends Exception
{
    MachineException(String format, Object... args)
    {
        super(String.format(format, args));
    }
}