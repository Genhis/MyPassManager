package sk.genhis.mypassmanager;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.UserDefinedFileAttributeView;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JColorChooser;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.border.EtchedBorder;
import javax.swing.event.AncestorEvent;
import javax.swing.event.AncestorListener;
import javax.swing.filechooser.FileNameExtensionFilter;

public class MyPassManager extends JFrame {
	private static final long serialVersionUID = 0L;
	
	private static MyPassManager frame;
	private static final String title = "MyPassManager v1.0.0 - ";
	
	private final Map<Integer, PasswordData> passwords;
	private final JPanel content;
	private final JMenuItem miSave;
	private final JMenuItem miCopying;
	private final FileNameExtensionFilter filter;
	
	private File file;
	private String key;
	private byte attempts;
	private boolean copying;
	protected boolean changed = false;
	
	public MyPassManager() {
		this.passwords = new HashMap<Integer, PasswordData>();
		this.filter = new FileNameExtensionFilter("MyPassManager Files (*.pass)", "pass");
		this.content = new JPanel();
		this.content.setLayout(null);
		
		this.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
		this.addWindowListener(new WindowListener() {
			@Override
			public void windowClosing(WindowEvent e) {
				if(MyPassManager.this.canDiscardChanges())
					System.exit(0);
			}

			@Override
			public void windowActivated(WindowEvent e) {}

			@Override
			public void windowClosed(WindowEvent e) {}

			@Override
			public void windowDeactivated(WindowEvent e) {}

			@Override
			public void windowDeiconified(WindowEvent e) {}

			@Override
			public void windowIconified(WindowEvent e) {}

			@Override
			public void windowOpened(WindowEvent e) {}
		});
		
		JMenuBar mb = new JMenuBar();
		
		JMenu mFile = new JMenu("File");
		mb.add(mFile);
		
		JMenuItem miNew = new JMenuItem("New");
		miNew.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_N, KeyEvent.CTRL_MASK));
		miNew.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				MyPassManager.this.newFile();
			}
		});
		mFile.add(miNew);
		
		JMenuItem miOpen = new JMenuItem("Open");
		miOpen.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_O, KeyEvent.CTRL_MASK));
		miOpen.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				MyPassManager.this.open();
			}
		});
		mFile.add(miOpen);
		
		this.miSave = new JMenuItem("Save");
		this.miSave.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, KeyEvent.CTRL_MASK));
		this.miSave.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				MyPassManager.this.save(MyPassManager.this.key, MyPassManager.this.file);
			}
		});
		mFile.add(this.miSave);
		
		JMenuItem miSaveAs = new JMenuItem("Save As...");
		miSaveAs.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, KeyEvent.CTRL_MASK | KeyEvent.ALT_MASK));
		miSaveAs.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				MyPassManager.this.save();
			}
		});
		mFile.add(miSaveAs);
		
		JMenuItem miExit = new JMenuItem("Exit");
		miExit.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				if(MyPassManager.this.canDiscardChanges())
					System.exit(0);
			}
		});
		mFile.add(miExit);

		JMenu mEdit = new JMenu("Edit");
		mb.add(mEdit);
		
		JMenuItem miPass = new JMenuItem("Change password");
		miPass.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				String key = MyPassManager.showPasswordDialog(MyPassManager.this.key);
				if(key != null)
					MyPassManager.this.setKey(key);
			}
		});
		mEdit.add(miPass);
		
		JMenuItem miAttempts = new JMenuItem("Delete after...");
		miAttempts.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				String attempts = JOptionPane.showInputDialog(MyPassManager.this, "Delete after x attempts to open a file with the wrong password", MyPassManager.this.attempts);
				if(attempts != null && Byte.parseByte(attempts) >= -1)
					MyPassManager.this.setAttempts(Byte.parseByte(attempts));
			}
		});
		mEdit.add(miAttempts);
		
		this.miCopying = new JCheckBoxMenuItem("Allow duplication");
		this.miCopying.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				MyPassManager.this.setCopying(MyPassManager.this.miCopying.isSelected());
			}
		});
		mEdit.add(this.miCopying);
		
		JMenuItem miAdd = new JMenuItem("Add new password");
		miAdd.setMaximumSize(new Dimension(miAdd.getPreferredSize().width, miAdd.getMaximumSize().height));
		miAdd.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				JTextField name = new JTextField();
				JPasswordField pass = new JPasswordField();
				AtomicReference<Color> color = new AtomicReference<Color>(Color.WHITE);
				if(MyPassManager.showUserPassDialog(name, pass, color, "Add new password"))
					MyPassManager.this.add(name.getText(), new String(pass.getPassword()), color.get());
			}
		});
		mb.add(miAdd);
		
		this.getContentPane().add(mb, BorderLayout.NORTH);
		this.getContentPane().add(this.content, BorderLayout.CENTER);
		
		this.newFile();
	}
	
	public void newFile() {
		if(this.canDiscardChanges()) {
			this.setTitle(MyPassManager.title + "New file");
			this.setSize(600, 350);
			this.miSave.setEnabled(false);
			this.file = null;
			this.key = null;
			this.attempts = 3;
			this.copying = false;
			this.changed = false;
			this.clear();
		}
	}
	
	public void open() {
		JFileChooser fd = new JFileChooser();
		fd.setFileFilter(this.filter);
		if(this.canDiscardChanges() && fd.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
			try {
				UserDefinedFileAttributeView att = Files.getFileAttributeView(fd.getSelectedFile().toPath(), UserDefinedFileAttributeView.class);
				ByteBuffer b = ByteBuffer.allocate(1);
				att.read("mpm.copying", b);
				boolean copying = b.get(0) == 1;
				
				b = ByteBuffer.allocate(att.size("mpm.mtime"));
				att.read("mpm.mtime", b);
				b.flip();
				if(copying || Charset.defaultCharset().decode(b).toString().equals(Files.readAttributes(fd.getSelectedFile().toPath(), BasicFileAttributes.class).creationTime().toString())) {
					String key = MyPassManager.showPasswordDialog();
					if(key != null) {
						byte[] data = this.getData(key, MyPassManager.getFileContent(fd.getSelectedFile()), fd.getSelectedFile(), att);
						if(data != null) {
							b = ByteBuffer.allocate(1);
							att.read("mpm.attempts", b);
							att.write("mpm.counter", b);
							this.attempts = b.get(0);
							
							this.copying = copying;
							this.miCopying.setSelected(this.copying);
							
							this.key = key;
							this.file = fd.getSelectedFile();
							this.clear();
							
							ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(data));
							try {
								this.setSize((Dimension)ois.readObject());
								while(true) {
									PasswordData pd = (PasswordData)ois.readObject();
									pd.initPanel();
									this.add(pd);
								}
							}
							catch(EOFException ex) {}
							ois.close();
							
							this.changed = false;
							this.miSave.setEnabled(true);
							this.setTitle(MyPassManager.title + this.getFileName());
						}
					}
				}
				else {
					fd.getSelectedFile().delete();
					JOptionPane.showMessageDialog(this, "The file no longer exists!", "Error!", JOptionPane.ERROR_MESSAGE);
				}
			}
			catch(NoSuchFileException ex) {
				JOptionPane.showMessageDialog(this, "The file has a wrong format!", "Error!", JOptionPane.ERROR_MESSAGE);
			}
			catch(IOException ex) {
				ex.printStackTrace();
			}
			catch(ClassNotFoundException ex) {
				ex.printStackTrace();
			}
		}
	}
	
	public void save() {
		JFileChooser fd = new JFileChooser();
		fd.setFileFilter(this.filter);
		if(fd.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
			String key = MyPassManager.showPasswordDialog(this.key);
			if(key != null) {
				this.miSave.setEnabled(true);
				this.save(this.key = key, this.file = fd.getSelectedFile().getAbsolutePath().endsWith(".pass") ? fd.getSelectedFile() : new File(fd.getSelectedFile().getAbsolutePath() + ".pass"));
			}
		}
	}
	
	public void save(String key, File file) {
		try {
			this.setTitle(MyPassManager.title + this.getFileName());
			this.changed = false;
			
			FileOutputStream fos = new FileOutputStream(file);
			fos.write(this.getData(key, this.getPasswords()));
			fos.close();
			
			byte[] b = {this.attempts};
			UserDefinedFileAttributeView att = Files.getFileAttributeView(file.toPath(), UserDefinedFileAttributeView.class);
			att.write("mpm.attempts", ByteBuffer.wrap(b));
			att.write("mpm.counter", ByteBuffer.wrap(b));
			b[0] = (byte)(this.copying ? 1 : 0);
			att.write("mpm.copying", ByteBuffer.wrap(b));
			att.write("mpm.mtime", Charset.defaultCharset().encode(Files.readAttributes(file.toPath(), BasicFileAttributes.class).creationTime().toString()));
		}
		catch(IOException ex) {
			ex.printStackTrace();
		}
	}
	
	public void add(String name, String pass, Color color) {
		if(name == null || pass == null)
			throw new NullPointerException();
		this.add(new PasswordData(name, pass, color));
	}
	
	public void add(PasswordData data) {
		this.passwords.put(data.getID(), data);
		this.content.add(data.getPanel());
		this.repaintC();
		this.change();
	}
	
	public void remove(PasswordData data) {
		this.passwords.remove(data.getID());
		this.content.remove(data.getPanel());
		this.repaintC();
		this.change();
	}
	
	private void clear() {
		this.passwords.clear();
		this.content.removeAll();
		this.repaintC();
	}
	
	public void repaintC() {
		this.content.revalidate();
		this.content.repaint();
	}
	
	private boolean canDiscardChanges() {
		int option = JOptionPane.NO_OPTION;
		if(this.changed) {
			option = JOptionPane.showConfirmDialog(this, "You have unsaved changes! Do you want to save the file?", "Save changes", JOptionPane.YES_NO_CANCEL_OPTION);
			if(option == JOptionPane.OK_OPTION) {
				if(this.file != null)
					this.save(this.key, this.file);
				else
					this.save();
			}
		}
		return option == JOptionPane.NO_OPTION;
	}
	
	protected void change() {
		if(!this.changed && this.passwords.size() > 0) {
			this.changed = true;
			this.setTitle(MyPassManager.title + "*" + this.getFileName());
		}
	}
	
	public void setKey(String key) {
		this.key = key;
		this.change();
	}
	
	public void setAttempts(byte attempts) {
		this.attempts = attempts;
		this.change();
	}
	
	public void setCopying(boolean copying) {
		this.copying = copying;
		this.change();
	}
	
	private byte[] getPasswords() {
		byte[] b = new byte[0];
		try {
			ByteArrayOutputStream bos = new ByteArrayOutputStream();
			ObjectOutputStream oos = new ObjectOutputStream(bos);
			
			oos.writeObject(this.getSize());
			for(PasswordData data : this.passwords.values())
				oos.writeObject(data);
			
			b = bos.toByteArray();
			oos.close();
		}
		catch(IOException ex) {
			ex.printStackTrace();
		}
		return b;
	}
	
	public String getFileName() {
		return this.file == null ? "New file" : this.file.getName();
	}
	
	public byte[] getData(String key, byte[] data) {
		return this.getData(key, data, null, null);
	}
	
	public byte[] getData(String key, byte[] data, File file, UserDefinedFileAttributeView att) {
		try {
			int mode = file == null ? Cipher.ENCRYPT_MODE : Cipher.DECRYPT_MODE;
			byte salt[] = {0, 1, 2, 3, 4, 5, 6, 7};
			byte iv[] = {0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15};
			Cipher c = Cipher.getInstance("AES/CBC/PKCS5Padding");
			c.init(mode, new SecretKeySpec(SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1").generateSecret(new PBEKeySpec(key.toCharArray(), salt, 16384, 128)).getEncoded(), "AES"), new IvParameterSpec(iv));
			return c.doFinal(data);
		}
		catch (NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException | IllegalBlockSizeException | InvalidKeySpecException | InvalidAlgorithmParameterException ex) {
			ex.printStackTrace();
		}
		catch (BadPaddingException ex) {
			JOptionPane.showMessageDialog(this, "Wrong password!");
			
			try {
				ByteBuffer bb = ByteBuffer.allocate(1);
				att.read("mpm.attempts", bb);
				
				if(bb.get(0) != -1) {
					bb = ByteBuffer.allocate(1);
					att.read("mpm.counter", bb);
					byte[] b = {(byte)(bb.get(0) - 1)};
					
					if(b[0] <= 0)
						file.delete();
					else
						att.write("mpm.counter", ByteBuffer.wrap(b));
				}
			}
			catch (IOException ex2) {
				ex2.printStackTrace();
			}
		}
		return null;
	}

	public static void main(String[] args) {
		(MyPassManager.frame = new MyPassManager()).setVisible(true);
	}
	
	public static MyPassManager getFrame() {
		return MyPassManager.frame;
	}
	
	public static byte[] getFileContent(File file) throws IOException {
		byte[] b = new byte[(int)file.length()];
		FileInputStream fis = new FileInputStream(file);
		fis.read(b);
		fis.close();
		return b;
	}
	
	public static boolean showUserPassDialog(final JTextField name, JPasswordField pass, final AtomicReference<Color> color, String title) {
		name.addAncestorListener(new AncestorListener() {
			@Override
			public void ancestorAdded(AncestorEvent e) {
				name.requestFocusInWindow();
			}

			@Override
			public void ancestorMoved(AncestorEvent e) {}

			@Override
			public void ancestorRemoved(AncestorEvent e) {}
		});
		
		final JPanel c = new JPanel();
		c.setBorder(new EtchedBorder(EtchedBorder.LOWERED));
		c.setBackground(color.get());
		c.addMouseListener(new MouseListener() {
			@Override
			public void mouseClicked(MouseEvent e) {
				Color cl = JColorChooser.showDialog(MyPassManager.frame, "Color", color.get());
				c.setBackground(cl);
				color.set(cl);
			}

			@Override
			public void mouseEntered(MouseEvent e) {}

			@Override
			public void mouseExited(MouseEvent e) {}

			@Override
			public void mousePressed(MouseEvent e) {}

			@Override
			public void mouseReleased(MouseEvent e) {}
		});
		
		JPanel p = new JPanel();
		p.setLayout(new GridLayout(3, 2));
		p.add(new JLabel("Name:"));
		p.add(name);
		p.add(new JLabel("Password:"));
		p.add(pass);
		p.add(new JLabel("Color:"));
		p.add(c);
		return JOptionPane.showConfirmDialog(MyPassManager.frame, p, title, JOptionPane.OK_CANCEL_OPTION) == JOptionPane.OK_OPTION && name.getText().length() > 0 && pass.getPassword().length > 0;
	}
	
	public static String showPasswordDialog() {
		return MyPassManager.showPasswordDialog("");
	}
	
	public static String showPasswordDialog(String value) {
		final JPasswordField pass = new JPasswordField(value);
		pass.addAncestorListener(new AncestorListener() {
			@Override
			public void ancestorAdded(AncestorEvent e) {
				pass.requestFocusInWindow();
			}

			@Override
			public void ancestorMoved(AncestorEvent e) {}

			@Override
			public void ancestorRemoved(AncestorEvent e) {}
		});
		
		if(JOptionPane.showConfirmDialog(MyPassManager.frame, pass, "Password:", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE) == JOptionPane.OK_OPTION)
			return new String(pass.getPassword());
		return null;
	}
}
