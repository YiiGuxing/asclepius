package cn.yiiguxing.asclepius.form;

import javax.swing.*;
import java.awt.*;

public class SliceViewerForm extends JPanel {
    protected JPanel root;
    protected JPanel contentPanel;
    protected JLabel titleLabel;
    protected JScrollBar scrollBar;

    public SliceViewerForm() {
        super(new BorderLayout());
        add(root, BorderLayout.CENTER);
    }
}
