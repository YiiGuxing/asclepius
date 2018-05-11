package cn.yiiguxing.asclepius.form;

import cn.yiiguxing.asclepius.SliceViewer;
import cn.yiiguxing.asclepius.VolumePanel;

import javax.swing.*;
import java.awt.*;

public class MainFrame extends JFrame {
    protected JPanel root;
    protected SliceViewer axialViewer;
    protected SliceViewer coronalViewer;
    protected SliceViewer sagittalViewer;
    protected VolumePanel volumeViewer;

    public MainFrame() {
        super("Asclepius");

        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());
        add(root, BorderLayout.CENTER);
    }

    private void createUIComponents() {
        axialViewer = new SliceViewer("Axial");
        axialViewer.setBorder(Color.RED);
        coronalViewer = new SliceViewer("Coronal");
        coronalViewer.setBorder(Color.GREEN);
        sagittalViewer = new SliceViewer("Sagittal");
        sagittalViewer.setBorder(Color.BLUE);
        volumeViewer = new VolumePanel();
    }
}
