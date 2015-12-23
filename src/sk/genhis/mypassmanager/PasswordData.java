package sk.genhis.mypassmanager;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.GridLayout;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.io.Serializable;
import java.util.concurrent.atomic.AtomicReference;

import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.border.EmptyBorder;
import javax.swing.event.AncestorEvent;
import javax.swing.event.AncestorListener;

public class PasswordData implements Serializable {
	private static final long serialVersionUID = 0L;
	private static int nextid = 0;
	
	private int id;
	private String name;
	private String pass;
	private Color color;
	private Point location;
	
	private transient Point ml;
	
	private transient JPanel panel;
	private transient JButton btn;
	private transient JButton btnD;
	private transient JButton btnE;
	
	public PasswordData(String name, String pass, Color color) {
		this.id = PasswordData.nextid++;
		this.name = name;
		this.pass = pass;
		this.color = color;
		this.location = new Point(0, 0);
		this.initPanel();
	}
	
	protected void initPanel() {
		if(PasswordData.nextid <= this.id)
			PasswordData.nextid = this.id + 1;
		
		JPanel p = new JPanel();
		p.setLayout(new GridLayout(2, 1));
		
		this.btnD = new JButton();
		this.btnE = new JButton();
		MouseListener ml = new MouseListener() {
			private Icon i;

			@Override
			public void mouseEntered(MouseEvent e) {
				PasswordData.this.btnD.setVisible(true);
				PasswordData.this.btnE.setVisible(true);
				JButton b = (JButton)e.getSource();
				this.i = b.getIcon();
				b.setIcon(b.getPressedIcon());
			}

			@Override
			public void mouseExited(MouseEvent e) {
				PasswordData.this.btnD.setVisible(false);
				PasswordData.this.btnE.setVisible(false);
				((JButton)e.getSource()).setIcon(this.i);
			}

			@Override
			public void mousePressed(MouseEvent e) {}

			@Override
			public void mouseReleased(MouseEvent e) {}

			@Override
			public void mouseClicked(MouseEvent e) {}
		};
		
		this.btnD.setContentAreaFilled(false);
		this.btnD.setIcon(new ImageIcon(this.getClass().getResource("/res/delete.png")));
		this.btnD.setPressedIcon(new ImageIcon(this.getClass().getResource("/res/delete_hover.png")));
		this.btnD.setBorder(new EmptyBorder(0, 0, 0, 0));
		this.btnD.setVisible(false);
		this.btnD.addMouseListener(ml);
		this.btnD.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				MyPassManager.getFrame().remove(PasswordData.this);
			}
		});
		
		this.btnE.setIcon(new ImageIcon(this.getClass().getResource("/res/edit.png")));
		this.btnE.setPressedIcon(new ImageIcon(this.getClass().getResource("/res/edit_hover.png")));
		this.btnE.setBorder(new EmptyBorder(0, 0, 0, 0));
		this.btnE.setVisible(false);
		this.btnE.addMouseListener(ml);
		this.btnE.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				JTextField name = new JTextField(PasswordData.this.name);
				JPasswordField pass = new JPasswordField(PasswordData.this.pass);
				AtomicReference<Color> color = new AtomicReference<Color>(PasswordData.this.color);
				if(MyPassManager.showUserPassDialog(name, pass, color, "Edit password")) {
					PasswordData.this.btn.setText(PasswordData.this.name = name.getText());
					PasswordData.this.pass = new String(pass.getPassword());
					PasswordData.this.setColor(PasswordData.this.color = color.get());
					PasswordData.this.resize();
					MyPassManager.getFrame().change();
				}
			}
		});
		
		this.btn = new JButton(this.name);
		this.setColor(this.color);
		this.btn.addMouseListener(new MouseListener() {
			@Override
			public void mouseEntered(MouseEvent e) {
				PasswordData.this.btnD.setVisible(true);
				PasswordData.this.btnE.setVisible(true);
			}

			@Override
			public void mouseExited(MouseEvent e) {
				PasswordData.this.btnD.setVisible(false);
				PasswordData.this.btnE.setVisible(false);
			}

			@Override
			public void mousePressed(MouseEvent e) {
				PasswordData.this.ml = e.getLocationOnScreen();
				PasswordData.this.btn.setCursor(Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR));
			}

			@Override
			public void mouseReleased(MouseEvent e) {
				PasswordData.this.btn.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
			}

			@Override
			public void mouseClicked(MouseEvent e) {
				Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(PasswordData.this.pass), null);
			}
		});
		this.btn.addMouseMotionListener(new MouseMotionListener() {
			@Override
			public void mouseDragged(MouseEvent e) {
				PasswordData.this.setLocation(new Point(e.getXOnScreen() - PasswordData.this.ml.x + PasswordData.this.location.x, e.getYOnScreen() - PasswordData.this.ml.y + PasswordData.this.location.y));
				PasswordData.this.ml = e.getLocationOnScreen();
			}

			@Override
			public void mouseMoved(MouseEvent e) {}
		});

		p.add(this.btnE);
		p.add(this.btnD);
		this.panel = new JPanel();
		this.panel.setLayout(new BorderLayout());
		this.panel.add(this.btn, BorderLayout.CENTER);
		this.panel.add(p, BorderLayout.EAST);
		this.panel.addAncestorListener(new AncestorListener() {
			@Override
			public void ancestorAdded(AncestorEvent e) {
				PasswordData.this.setLocation();
			}

			@Override
			public void ancestorMoved(AncestorEvent e) {}

			@Override
			public void ancestorRemoved(AncestorEvent e) {}
		});
		this.resize();
	}
	
	private void resize() {
		this.panel.setSize(this.btn.getPreferredSize().width + this.btnD.getPreferredSize().width, Math.max(this.btn.getPreferredSize().height, this.btnD.getPreferredSize().height + this.btnE.getPreferredSize().height));
	}
	
	public void setColor(Color c) {
		this.btn.setBackground(c);
		this.btn.setForeground(Color.RGBtoHSB(c.getRed(), c.getGreen(), c.getBlue(), null)[2] < 0.8 ? Color.WHITE : Color.BLACK);
	}
	
	public void setLocation() {
		this.panel.setLocation(this.location);
		this.panel.getParent().revalidate();
		this.panel.getParent().repaint();
	}
	
	public void setLocation(Point location) {
		this.location = location;
		MyPassManager.getFrame().change();
		this.setLocation();
	}
	
	public int getID() {
		return this.id;
	}
	
	public JPanel getPanel() {
		return this.panel;
	}
}
