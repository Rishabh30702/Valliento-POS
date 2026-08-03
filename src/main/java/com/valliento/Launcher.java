package com.valliento;

import javax.swing.JOptionPane;
import java.io.PrintWriter;
import java.io.StringWriter;

public class Launcher {

    public static void main(String[] args) {
        try {
            MainApp.main(args);
        } catch (Throwable e) {
            e.printStackTrace();

            StringWriter sw = new StringWriter();
            e.printStackTrace(new PrintWriter(sw));

            JOptionPane.showMessageDialog(
                    null,
                    sw.toString(),
                    "Startup Error",
                    JOptionPane.ERROR_MESSAGE
            );
        }
    }
}