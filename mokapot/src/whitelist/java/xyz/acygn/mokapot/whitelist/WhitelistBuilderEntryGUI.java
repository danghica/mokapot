package xyz.acygn.mokapot.whitelist;

import java.nio.file.Path;
import java.nio.file.Paths;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JPanel;
import javax.swing.SpinnerModel;
import javax.swing.SpinnerNumberModel;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.filechooser.FileNameExtensionFilter;

/**
 * A whitelist builder entry that can be customized via a graphical user
 * interface.
 *
 * @author Alex Smith
 */
@SuppressWarnings("serial")
public class WhitelistBuilderEntryGUI extends JPanel
        implements WhitelistBuilderEntry {

    /**
     * Creates new form WhitelistBuilderEntryGUI
     */
    public WhitelistBuilderEntryGUI() {
        initComponents();
        entryName.getDocument().addDocumentListener(
                new FieldDocumentListener(entryName));
        location.getDocument().addDocumentListener(
                new FieldDocumentListener(location));
        entryPassword.getDocument().addDocumentListener(
                new FieldDocumentListener(entryPassword));
    }

    /**
     * Places the graphical controls onto the panel that this object represents.
     * Note: this code is intended to be edited via Netbeans' form editor, and
     * should not be edited manually.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {
        java.awt.GridBagConstraints gridBagConstraints;

        locationLabel = new javax.swing.JLabel();
        location = new javax.swing.JTextField();
        entryNameLabel = new javax.swing.JLabel();
        entryName = new javax.swing.JTextField();
        entryPasswordLabel = new javax.swing.JLabel();
        entryPassword = new javax.swing.JPasswordField();
        validityLabel = new javax.swing.JLabel();
        validity = new javax.swing.JSpinner();
        locationBrowse = new javax.swing.JButton();

        setLayout(new java.awt.GridBagLayout());

        locationLabel.setDisplayedMnemonic('L');
        locationLabel.setHorizontalAlignment(javax.swing.SwingConstants.TRAILING);
        locationLabel.setText("Location: ");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_END;
        add(locationLabel, gridBagConstraints);

        location.setColumns(20);
        location.setToolTipText("The filesystem location at which the .p12 file should be generated.");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        add(location, gridBagConstraints);

        entryNameLabel.setDisplayedMnemonic('N');
        entryNameLabel.setHorizontalAlignment(javax.swing.SwingConstants.TRAILING);
        entryNameLabel.setText("Name: ");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_END;
        add(entryNameLabel, gridBagConstraints);

        entryName.setToolTipText("The human-readable name used to identify the entry.");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        add(entryName, gridBagConstraints);

        entryPasswordLabel.setDisplayedMnemonic('P');
        entryPasswordLabel.setHorizontalAlignment(javax.swing.SwingConstants.TRAILING);
        entryPasswordLabel.setText("Password: ");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_END;
        add(entryPasswordLabel, gridBagConstraints);

        entryPassword.setToolTipText("The password used to protect the generated .p12 file.");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        add(entryPassword, gridBagConstraints);

        validityLabel.setDisplayedMnemonic('V');
        validityLabel.setHorizontalAlignment(javax.swing.SwingConstants.TRAILING);
        validityLabel.setText("Validity (days): ");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_END;
        add(validityLabel, gridBagConstraints);

        validity.setModel(new javax.swing.SpinnerNumberModel(1, 1, null, 1));
        validity.setToolTipText("The number of days for which the .p12 file will be valid. (After this duration, it will become unusable.)");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        add(validity, gridBagConstraints);

        locationBrowse.setMnemonic('B');
        locationBrowse.setText("Browse...");
        locationBrowse.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                locationBrowseActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        add(locationBrowse, gridBagConstraints);
    }// </editor-fold>//GEN-END:initComponents

    /**
     * The default directory to in which to save <code>.p12</code> files via the
     * Browse interface. This is updated whenever a file is selected.
     */
    private static Path defaultBrowsePath = Paths.get("");

    /**
     * Opens a window allowing the filesystem location to be set interactively
     * via a directory tree. This is called when a user clicks on the
     * "Browse..." button.
     *
     * @param evt The <code>ActionEvent</code> describing the click.
     */
    private void locationBrowseActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_locationBrowseActionPerformed
        JFileChooser fc = new JFileChooser();
        fc.setFileFilter(new FileNameExtensionFilter("PKCS #12 files", "p12"));
        Path existingPath = getFilesystemLocation();
        if (existingPath != null && existingPath.getParent() != null) {
            defaultBrowsePath = existingPath.getParent();
        }
        fc.setCurrentDirectory(defaultBrowsePath.toAbsolutePath().toFile());
        if ((isExisting() ? fc.showOpenDialog(this) : fc.showSaveDialog(this))
                == JFileChooser.APPROVE_OPTION) {
            Path newPath = fc.getSelectedFile().toPath();
            setFilesystemLocation(newPath);
            if (newPath.getParent() != null) {
                defaultBrowsePath = newPath.getParent();
            }
        }

        listVerificationFailures(-1);
    }//GEN-LAST:event_locationBrowseActionPerformed

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JTextField entryName;
    private javax.swing.JLabel entryNameLabel;
    private javax.swing.JPasswordField entryPassword;
    private javax.swing.JLabel entryPasswordLabel;
    private javax.swing.JTextField location;
    private javax.swing.JButton locationBrowse;
    private javax.swing.JLabel locationLabel;
    private javax.swing.JSpinner validity;
    private javax.swing.JLabel validityLabel;
    // End of variables declaration//GEN-END:variables

    @Override
    public Path getFilesystemLocation() {
        String locationText = location.getText();
        if (locationText.isEmpty()) {
            return null;
        }
        return Paths.get(locationText);
    }

    @Override
    public void setFilesystemLocation(Path newLocation) {
        if (newLocation == null) {
            location.setText("");
        } else {
            location.setText(newLocation.toString());
        }
    }

    @Override
    public String getEntryName() {
        return entryName.getText();
    }

    @Override
    public void setEntryName(String newEntryName) {
        entryName.setText(newEntryName);
    }

    @Override
    public int getValidity() {
        SpinnerModel model = validity.getModel();
        return ((SpinnerNumberModel) model).getNumber().intValue();
    }

    @Override
    public void setValidity(int newValidity) {
        validity.getModel().setValue(newValidity);
    }

    @Override
    public char[] getPassword() {
        return entryPassword.getPassword();
    }

    @Override
    public void setPassword(char[] newPassword) {
        // TODO: This is insecure but I don't think there's a secure way to do
        // it. (Besides, the codepaths that exist at the time of writing aren't
        // capable of /calling/ setPassword in a secure manner, anyway.)
        entryPassword.setText(String.valueOf(newPassword));
    }

    @Override
    public void clearPassword() {
        entryPassword.setText("");
    }

    @Override
    public boolean isExisting() {
        return !entryName.isEnabled();
    }

    @Override
    public void setExisting(boolean existing) {
        validity.setEnabled(!existing);
        validityLabel.setEnabled(!existing);
        entryName.setEnabled(!existing);
        entryNameLabel.setEnabled(!existing);
    }

    /**
     * A document listener that re-verifies this entry whenever changes to it
     * are made. This is used as the listener for each text field.
     */
    private class FieldDocumentListener implements DocumentListener {

        /**
         * Creates a field document listener for the document belonging to a
         * given component.
         *
         * @param documentOwner The component that owns the document.
         */
        FieldDocumentListener(JComponent documentOwner) {
            this.documentOwner = documentOwner;
        }

        /**
         * The component that owns the Document. Used to prevent the listeners
         * running recursively.
         */
        private final JComponent documentOwner;

        /**
         * Marks non-blank fields of this entry that have invalid values.
         *
         * @param e Ignored.
         */
        @Override
        public void insertUpdate(DocumentEvent e) {
            if (documentOwner.isEnabled()) {
                listVerificationFailures(-1);
            }
        }

        /**
         * Marks non-blank fields of this entry that have invalid values.
         *
         * @param e Ignored.
         */
        @Override
        public void removeUpdate(DocumentEvent e) {
            if (documentOwner.isEnabled()) {
                listVerificationFailures(-1);
            }
        }

        /**
         * Marks non-blank fields of this entry that have invalid values.
         *
         * @param e Ignored.
         */
        @Override
        public void changedUpdate(DocumentEvent e) {
            if (documentOwner.isEnabled()) {
                listVerificationFailures(-1);
            }
        }
    }
}
