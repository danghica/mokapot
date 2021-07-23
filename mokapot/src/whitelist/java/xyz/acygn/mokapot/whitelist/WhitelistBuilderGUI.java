package xyz.acygn.mokapot.whitelist;

import static java.awt.EventQueue.invokeLater;
import java.awt.event.ItemEvent;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

/**
 * A graphical user interface for creating whitelists. A whitelist is a set of
 * <code>.p12</code> files that all recognise each other; each such file is used
 * by a different Java virtual machine that participates in a distributed
 * communication, with each JVM having its own file.
 * <p>
 * Whitelists are created via cryptographic means: the key required for adding a
 * new entry to an existing whitelist is distinct from the keys stored in the
 * whitelist entries themselves. This key is known as a <i>whitelist
 * controller</i>. Normally, this would only exist temporarily while the
 * whitelist is being generated, and thus the whitelist would be static once
 * created. However, a persistent whitelist controller can be used to allow the
 * entries in a whitelist to be updated after the fact.
 *
 * @author Alex Smith
 */
@SuppressWarnings("serial")
public class WhitelistBuilderGUI extends javax.swing.JDialog {

    /**
     * Public constructor. Delegates to <code>JDialog</code>'s constructor, but
     * adds the user interface elements specific to a whitelist builder GUI.
     *
     * @param parent The frame to which the dialog belongs.
     * @param modal Whether this dialog should block input to its parent while
     * it exists.
     */
    public WhitelistBuilderGUI(java.awt.Frame parent, boolean modal) {
        super(parent, modal);
        initComponents();
    }

    /**
     * Adds the appropriate user interface elements to a partially constructed
     * whitelist builder GUI. This method is designed to be edited via NetBeans'
     * design editor, and as such is highly structured code that should not be
     * manually edited.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {
        java.awt.GridBagConstraints gridBagConstraints;

        whitelistTypeGroup = new javax.swing.ButtonGroup();
        entryPanelTabs = new javax.swing.JTabbedPane();
        isTemporaryWhitelist = new javax.swing.JRadioButton();
        isNewWhitelist = new javax.swing.JRadioButton();
        isExistingWhitelist = new javax.swing.JRadioButton();
        whitelistNameLabel = new javax.swing.JLabel();
        whitelistName = new javax.swing.JTextField();
        buttonPanel = new javax.swing.JPanel();
        generateButton = new javax.swing.JButton();
        cancelButton = new javax.swing.JButton();
        addEntryButton = new javax.swing.JButton();
        deleteEntryButton = new javax.swing.JButton();
        spaceAboveTabs = new javax.swing.Box.Filler(new java.awt.Dimension(0, 15), new java.awt.Dimension(0, 15), new java.awt.Dimension(0, 0));

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setTitle("Whitelist Builder");
        setModal(true);
        setResizable(false);
        getContentPane().setLayout(new java.awt.GridBagLayout());

        // Tab 0 is permanently used for the controller, but is disabled if
        // we're using a temporary controller
        entryPanelTabs.addTab("Controller", new WhitelistBuilderEntryGUI());
        entryPanelTabs.setEnabledAt(0, false);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 5;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        getContentPane().add(entryPanelTabs, gridBagConstraints);

        whitelistTypeGroup.add(isTemporaryWhitelist);
        isTemporaryWhitelist.setMnemonic('t');
        isTemporaryWhitelist.setSelected(true);
        isTemporaryWhitelist.setText("Use a temporary whitelist controller");
        isTemporaryWhitelist.addItemListener(new java.awt.event.ItemListener() {
            public void itemStateChanged(java.awt.event.ItemEvent evt) {
                isTemporaryWhitelistItemStateChanged(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
        getContentPane().add(isTemporaryWhitelist, gridBagConstraints);

        whitelistTypeGroup.add(isNewWhitelist);
        isNewWhitelist.setMnemonic('r');
        isNewWhitelist.setText("Make a new persistent whitelist controller");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
        getContentPane().add(isNewWhitelist, gridBagConstraints);

        whitelistTypeGroup.add(isExistingWhitelist);
        isExistingWhitelist.setMnemonic('x');
        isExistingWhitelist.setText("Use an existing whitelist controller");
        isExistingWhitelist.addItemListener(new java.awt.event.ItemListener() {
            public void itemStateChanged(java.awt.event.ItemEvent evt) {
                isExistingWhitelistItemStateChanged(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
        getContentPane().add(isExistingWhitelist, gridBagConstraints);

        whitelistNameLabel.setDisplayedMnemonic('m');
        whitelistNameLabel.setLabelFor(whitelistName);
        whitelistNameLabel.setText("Name: ");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        getContentPane().add(whitelistNameLabel, gridBagConstraints);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        getContentPane().add(whitelistName, gridBagConstraints);

        java.awt.GridBagLayout buttonPanelLayout = new java.awt.GridBagLayout();
        buttonPanelLayout.columnWeights = new double[] {0.0, 0.0, 1.0, 0.0, 0.0};
        buttonPanel.setLayout(buttonPanelLayout);

        generateButton.setMnemonic('g');
        generateButton.setText("Generate");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 3;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        buttonPanel.add(generateButton, gridBagConstraints);

        cancelButton.setMnemonic('C');
        cancelButton.setText("Cancel");
        cancelButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                cancelButtonActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 4;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        buttonPanel.add(cancelButton, gridBagConstraints);

        addEntryButton.setMnemonic('a');
        addEntryButton.setText("Add entry");
        addEntryButton.setToolTipText("Add a new entry to be generated");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        buttonPanel.add(addEntryButton, gridBagConstraints);

        deleteEntryButton.setMnemonic('d');
        deleteEntryButton.setText("Delete entry");
        deleteEntryButton.setToolTipText("Delete (i.e. do not generate) the currently selected whitelist entry");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        buttonPanel.add(deleteEntryButton, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 6;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        getContentPane().add(buttonPanel, gridBagConstraints);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.gridwidth = 2;
        getContentPane().add(spaceAboveTabs, gridBagConstraints);

        pack();
    }// </editor-fold>//GEN-END:initComponents

    /**
     * Enables or disables the "Controller" tab and "Controller name" field. The
     * intended use of this is to handle changes to the radio button that
     * specifies that a temporary whitelist should be used.
     *
     * @param evt An <code>ItemEvent</code> describing
     * <code>isTemporaryWhitelist</code> being selected or deselected.
     */
    private void isTemporaryWhitelistItemStateChanged(java.awt.event.ItemEvent evt) {//GEN-FIRST:event_isTemporaryWhitelistItemStateChanged
        if (evt.getItemSelectable() != isTemporaryWhitelist) {
            return;
        }
        switch (evt.getStateChange()) {
            case ItemEvent.SELECTED:
                whitelistName.setEnabled(true);
                whitelistNameLabel.setEnabled(true);
                entryPanelTabs.setEnabledAt(0, false);
                if (entryPanelTabs.getSelectedIndex() == 0
                        && entryPanelTabs.getTabCount() > 1) {
                    entryPanelTabs.setSelectedIndex(1);
                }
                break;
            case ItemEvent.DESELECTED:
                whitelistName.setEnabled(false);
                whitelistNameLabel.setEnabled(false);
                entryPanelTabs.setEnabledAt(0, true);
                break;
        }
    }//GEN-LAST:event_isTemporaryWhitelistItemStateChanged

    /**
     * Enables or disables the "name" and "validity" fields of the controller
     * tab. The intended use of this is to handle changes to the radio button
     * that specifies that an existing whitelist should be used.
     *
     * @param evt An <code>ItemEvent</code> describing
     * <code>isExistingWhitelist</code> being selected or deselected.
     */
    private void isExistingWhitelistItemStateChanged(java.awt.event.ItemEvent evt) {//GEN-FIRST:event_isExistingWhitelistItemStateChanged
        if (evt.getItemSelectable() != isExistingWhitelist) {
            return;
        }
        switch (evt.getStateChange()) {
            case ItemEvent.SELECTED:
                ((WhitelistBuilderEntry) entryPanelTabs.getComponentAt(0)).setExisting(true);
                break;
            case ItemEvent.DESELECTED:
                ((WhitelistBuilderEntry) entryPanelTabs.getComponentAt(0)).setExisting(false);
                break;
        }
    }//GEN-LAST:event_isExistingWhitelistItemStateChanged

    /**
     * Disposes of the dialog box displaying the GUI. This would typically be
     * done upon a click of the "Cancel" button.
     *
     * @param evt Ignored.
     */
    private void cancelButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_cancelButtonActionPerformed
        dispose();
    }//GEN-LAST:event_cancelButtonActionPerformed

    /**
     * @param args the command line arguments
     */
    public static void main(String args[]) {

        /* Attempt to set the system look and feel. */
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (ClassNotFoundException | InstantiationException
                | IllegalAccessException | UnsupportedLookAndFeelException ex) {
            /* This isn't fatal; just continue with any look and feel. */
        }

        /* Create and display the dialog */
        invokeLater(() -> {
            WhitelistBuilderGUI dialog
                    = new WhitelistBuilderGUI(null, true);
            dialog.setDefaultCloseOperation(DISPOSE_ON_CLOSE);
            dialog.setVisible(true);
        });
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton addEntryButton;
    private javax.swing.JPanel buttonPanel;
    private javax.swing.JButton cancelButton;
    private javax.swing.JButton deleteEntryButton;
    private javax.swing.JTabbedPane entryPanelTabs;
    private javax.swing.JButton generateButton;
    private javax.swing.JRadioButton isExistingWhitelist;
    private javax.swing.JRadioButton isNewWhitelist;
    private javax.swing.JRadioButton isTemporaryWhitelist;
    private javax.swing.Box.Filler spaceAboveTabs;
    private javax.swing.JTextField whitelistName;
    private javax.swing.JLabel whitelistNameLabel;
    private javax.swing.ButtonGroup whitelistTypeGroup;
    // End of variables declaration//GEN-END:variables
}
