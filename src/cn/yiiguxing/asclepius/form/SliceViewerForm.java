package cn.yiiguxing.asclepius.form;

import cn.yiiguxing.asclepius.Icons;
import cn.yiiguxing.asclepius.widget.ActionLabel;

import javax.swing.*;
import java.awt.*;

public class SliceViewerForm extends JPanel {
    protected JPanel root;
    protected JPanel contentPanel;
    protected JLabel titleLabel;
    protected JScrollBar scrollBar;
    protected ActionLabel maximizeButton;

    public SliceViewerForm() {
        super(new BorderLayout());
        add(root, BorderLayout.CENTER);
    }

    private void createUIComponents() {
        maximizeButton = new ActionLabel(Icons.INSTANCE.getMAXIMIZE());
    }
}
