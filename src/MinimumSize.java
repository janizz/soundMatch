import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;

import javax.swing.JComponent;
import javax.swing.JFrame;

public class MinimumSize extends JFrame {

	protected CustomComponent cc;

	private static final long serialVersionUID = 1L;

	public MinimumSize() {
		setTitle("Custom Component Test");
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
	}

	public void display() {
		cc = new CustomComponent();
		add(cc);
		pack();
		setMinimumSize(getSize());// enforces the minimum size of both frame
									// and component
		setVisible(true);
	}

}

class CustomComponent extends JComponent {

	private static final long serialVersionUID = 1L;

	// protected Graphics g2d;

	@Override
	public Dimension getMinimumSize() {
		return new Dimension(100, 100);
	}

	@Override
	public Dimension getPreferredSize() {
		return new Dimension(400, 400);
	}

	@Override
	public void paintComponent(Graphics g) {
		// int margin = 10;
		// Dimension dim = getSize();
		super.paintComponent(g);
		// g2d.setColor(Color.red);
		// g2d.fillRect(margin, margin, dim.width - margin * 2, dim.height
		// - margin * 2);
		// g2d.fillRect(300, 300, 50, 50);
		// paintLine(g);
	}

	public void reDraw() {
		Graphics myGraphics = getComponentGraphics(getGraphics());
		// myGraphics.drawImage(background, 0, 0, getWidth(), getHeight(),
		// this);
		// myGraphics.drawImage(bird, cordX, cordY, this);
		// myGraphics.getClip();
		myGraphics.setClip(getBounds());
	}

	protected void paintLine(int cas, Complex[][] buffer) {
		Graphics g2d = (Graphics2D) getComponentGraphics(getGraphics());
		// System.out.println("Results:" + Main.results.length);
		for (int i = 0; i < buffer.length; i++) {
			int freq = 1;
			int size = 400;
			for (int Dline = 1; Dline < size; Dline++) {
				// To get the magnitude of the sound at a given frequency slice
				// get the abs() from the complex number.
				// In this case I use Math.log to get a more manageable number
				// (used for color)
				double magnitude = Math.log(buffer[i][freq].abs() + 1);

				// The more blue in the color the more intensity for a given
				// frequency point:
				g2d.setColor(new Color(0, (int) magnitude * 10,
						(int) magnitude * 20));
				// g2d.setColor(Color.red);
				int blockSizeY = Main.blockSizeY, blockSizeX = Main.blockSizeX;
				// Fill:
				/*if (Main.stejCas == false) {
					cas = 0;
				}*/
				g2d.fillRect(i * blockSizeX + cas, (size - Dline) * blockSizeY,
						blockSizeX, blockSizeY);

				boolean logModeEnabled = false;
				// I used an improvised logarithmic scale and normal scale:
				if (logModeEnabled
						&& (Math.log10(Dline) * Math.log10(Dline)) > 1) {
					freq += (int) (Math.log10(Dline) * Math.log10(Dline));
				} else {
					freq++;
				}
			}
		}
	}
}