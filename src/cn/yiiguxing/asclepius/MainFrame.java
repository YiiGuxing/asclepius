package cn.yiiguxing.asclepius;

import javax.swing.*;
import java.awt.*;

public class MainFrame extends JFrame {
    private JPanel root;
    public JPanel axial;
    public JPanel coronal;
    public JPanel sagittal;
    public JPanel volume;
    public JScrollBar axialScrollBar;
    public JScrollBar coronalScrollBar;
    public JScrollBar sagittalScrollBar;

    public MainFrame() {
        super("Asclepius");

        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());
        add(root, BorderLayout.CENTER);
    }
}
