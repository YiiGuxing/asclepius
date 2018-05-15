package cn.yiiguxing.asclepius.form;

import javax.swing.*;
import java.awt.*;

public class WindowingDialogForm extends JDialog {
    protected JPanel contentPane;
    protected JButton buttonOK;
    protected JButton buttonCancel;
    protected JSpinner windowLevelSpinner;
    protected JSpinner windowWidthSpinner;

    protected WindowingDialogForm(Frame owner) {
        super(owner, "Edit Windowing", true);
    }
}
