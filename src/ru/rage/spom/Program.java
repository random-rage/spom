package ru.rage.spom;

public class Program
{
    static final int[] DEFAULT_MEMORY =
            {
                    0x41, 0x42, 0x43, 0x44, 0x45, 0x46, 0x47, 0x48,     // A B C D E F G H
                    0x49, 0x4A, 0x4B, 0x4C, 0x4D, 0x4E, 0x4F, 0x50,     // I J K L M N O P
                    0x51, 0x52, 0x53, 0x54, 0x55, 0x56, 0x57, 0x58,     // Q R S T U V W X
                    0x59, 0x5A, 0x2E, 0x2C, 0x21, 0x3F, 0x5F, 0x20      // Y Z . , ! ? _
            };

    public static void main(String[] args)
    {
        new MainForm();
    }
}
