package de.hpvag.pirover;

import java.awt.BorderLayout;
import java.awt.FlowLayout;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;

import com.sun.org.apache.xerces.internal.impl.xpath.regex.ParseException;

import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import java.awt.GridLayout;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import javax.swing.JPasswordField;

public class ConnectionDialog extends JDialog {

	private final JPanel contentPanel = new JPanel();
	private JTextField txtAddress;
	private JTextField txtPort;
	private JPasswordField pwdPassword;

	/**
	 * Launch the application.
	 */
	/*public static void main(String[] args) {
		try {
			ConnectionDialog dialog = new ConnectionDialog();
			dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
			dialog.setVisible(true);
			System.out.println("Dialog wird angezeigt.");
		} catch (Exception e) {
			e.printStackTrace();
		}
	}*/

	/**
	 * Create the dialog.
	 */
	public ConnectionDialog() {
		setTitle("Connect to the PiRover");
		setModal(true);
		setBounds(100, 100, 376, 160);
		getContentPane().setLayout(new BorderLayout());
		contentPanel.setBorder(new EmptyBorder(5, 5, 5, 5));
		getContentPane().add(contentPanel, BorderLayout.CENTER);
		contentPanel.setLayout(new GridLayout(0, 2, 0, 0));
		{
			JLabel lblHostNameOr = new JLabel("Host name or ip address:");
			lblHostNameOr.setHorizontalAlignment(SwingConstants.LEFT);
			contentPanel.add(lblHostNameOr);
		}
		{
			txtAddress = new JTextField();
			contentPanel.add(txtAddress);
			txtAddress.setColumns(10);
		}
		{
			JLabel lblPortNumber = new JLabel("Port number:");
			contentPanel.add(lblPortNumber);
		}
		{
			txtPort = new JTextField();
			txtPort.setText("1987");
			contentPanel.add(txtPort);
			txtPort.setColumns(5);
		}
		{
			JLabel lblPassword = new JLabel("Password:");
			contentPanel.add(lblPassword);
		}
		{
			pwdPassword = new JPasswordField();
			pwdPassword.setText("uMieY6ophu[a");
			contentPanel.add(pwdPassword);
		}
		{
			JPanel buttonPane = new JPanel();
			buttonPane.setLayout(new FlowLayout(FlowLayout.RIGHT));
			getContentPane().add(buttonPane, BorderLayout.SOUTH);
			{
				JButton okButton = new JButton("OK");
				okButton.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent e) {
						// Eingaben ueberpruefen...
						// ...zuerst den Hostnamen/IP-Adresse.
						if (txtAddress.getText().isEmpty()) {
							JOptionPane.showMessageDialog(null, "Host name or ip address must not be empty!", "Error", JOptionPane.ERROR_MESSAGE);
							return;
						}
						
						// ...dann den Port.
						try {
							int port = Integer.parseInt(txtPort.getText().trim());
							
							if (port < 1 || port > 65535) {
								JOptionPane.showMessageDialog(null, "Port number must be a value between 1 und 65535!", "Error", JOptionPane.ERROR_MESSAGE);
								return;
							}
							
						} catch (NumberFormatException nfe) {
							JOptionPane.showMessageDialog(null, "Port number must be numeric!", "Error", JOptionPane.ERROR_MESSAGE);
							return;
						}
						
						// Sind die Eingaben in Ordnung, wird der Dialog geschlossen.
						dispose();
					}
				});
				okButton.setActionCommand("OK");
				buttonPane.add(okButton);
				getRootPane().setDefaultButton(okButton);
			}
			{
				JButton cancelButton = new JButton("Cancel");
				cancelButton.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent e) {
						System.exit(0);
					}
				});
				cancelButton.setActionCommand("Cancel");
				buttonPane.add(cancelButton);
			}
		}
	}
	
	/**
	 * Zeigt den Dialog an und gibt Hostnamen und IP-Adresse als Array von Strings zurueck.
	 */
	public String[] showDialog() {
		this.setVisible(true);
		String[] str = {txtAddress.getText().trim(), txtPort.getText().trim(), new String(pwdPassword.getPassword())};
		return str;
	}

}
