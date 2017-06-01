package ru.rage.spom;

import ru.rage.spoml.*;

import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

class Loader
{
    private ArrayList<Command> _cmds;
    private int[]              _data;

    Loader(String fileName, String libPath) throws Exception
    {
        _cmds = new ArrayList<>();
        _data = null;

        // Считываем размеры секций
        byte[] file = Files.readAllBytes(Paths.get(fileName));
        ByteBuffer bb = ByteBuffer.wrap(file, 0, Integer.BYTES * 3);    // Не берём внешние символы
        bb.order(Command.BYTE_ORDER);

        int incSize = bb.getInt();
        int dataSize = bb.getInt();
        int codeSize = bb.getInt();
        int offset = Integer.BYTES * 4;

        // Считываем включения
        ArrayList<Include> includes = new ArrayList<>();
        String[] incs = new String(file, offset, incSize).split("[\\r\\n]+");
        for (String include : incs)
        {
            if (include.length() < 5)
                continue;

            includes.add(new Include(include));
        }
        offset += incSize;

        // Считываем данные
        _data = new int[dataSize / Integer.BYTES];
        bb = ByteBuffer.wrap(file, offset, dataSize);
        bb.order(Command.BYTE_ORDER);

        for (int i = 0; i < _data.length; i++)
            _data[i] = bb.getInt();

        // Считываем команды
        offset += dataSize;
        LinkedList<Byte> code = new LinkedList<>();
        for (int i = offset; i < offset + codeSize; i++)
            code.add(file[i]);

        // Динамическая компоновка
        for (Include inc: includes)
            code.addAll(inc.getAddr(), inc.extractCode(libPath));

        byte[] bytes = new byte[code.size()];
        {
            int i = 0;
            for (Byte b : code)
                bytes[i++] = b;
        }

        // Распознаём команды
        bb = ByteBuffer.wrap(bytes);
        bb.order(Command.BYTE_ORDER);

        while (bb.hasRemaining())
        {
            int opcode = bb.get();
            ArgType argType = Command.getArgType(opcode);
            Argument arg = (argType == ArgType.NONE) ?
                           new Argument(null) :
                           new Argument(bb.getInt(), argType);

            _cmds.add(new Command(opcode, arg));
        }
    }

    List<Command> getCode()
    {
        return _cmds;
    }

    int[] getData()
    {
        return (_data == null) ? new int[0] : _data;
    }
}
