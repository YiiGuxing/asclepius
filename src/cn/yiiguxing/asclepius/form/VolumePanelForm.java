package cn.yiiguxing.asclepius.form;

import cn.yiiguxing.asclepius.Icons;
import cn.yiiguxing.asclepius.widget.ActionLabel;

import javax.swing.*;
import java.awt.*;

public class VolumePanelForm extends JPanel {
    protected JPanel root;
    protected JPanel contentPanel;
    protected ActionLabel maximizeButton;

    public VolumePanelForm() {
        super(new BorderLayout());
        add(root, BorderLayout.CENTER);
    }

    private void createUIComponents() {
        maximizeButton = new ActionLabel(Icons.INSTANCE.getMAXIMIZE());
    }
}
