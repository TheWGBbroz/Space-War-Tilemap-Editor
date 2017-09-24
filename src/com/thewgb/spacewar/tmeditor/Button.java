package com.thewgb.spacewar.tmeditor;

import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.ArrayList;
import java.util.List;

public class Button implements MouseListener {
	public static final int MOUSE_NONE = 0;
	public static final int MOUSE_HOVER = 1;
	public static final int MOUSE_CLICK = 2;
	
	private static final Color BORDER = Color.BLACK;
	private static final Color BACKGROUND_NORMAL = Color.WHITE;
	private static final Color BACKGROUND_HOVER = Color.GRAY;
	private static final Color BACKRGOUND_CLICK = Color.DARK_GRAY;
	private static final Color TEXT_COLOR = Color.BLACK;
	
	private String text;
	private int x, y, width, height;
	private Rectangle rect;
	private int mouseState;
	private List<ButtonListener> listeners;
	
	public Button(Component c, String text, int x, int y, int width, int height) {
		c.addMouseListener(this);
		
		this.text = text;
		this.x = x;
		this.y = y;
		this.width = width;
		this.height = height;
		this.rect = new Rectangle(x, y, width, height);
		
		listeners = new ArrayList<>();
	}
	
	public void render(Graphics2D g) {
		if(mouseState == MOUSE_HOVER)
			g.setColor(BACKGROUND_HOVER);
		else if(mouseState == MOUSE_CLICK)
			g.setColor(BACKRGOUND_CLICK);
		else
			g.setColor(BACKGROUND_NORMAL);
		g.fillRect(x, y, width, height);
		
		g.setColor(BORDER);
		g.drawRect(x, y, width, height);
		
		g.setColor(TEXT_COLOR);
		g.drawString(text, x + 1, y + height / 2 + 3);
	}
	
	public void update() {
		if(rect.contains(Main.mousePos) && mouseState == MOUSE_NONE)
			mouseState = MOUSE_HOVER;
		if(!rect.contains(Main.mousePos))
			mouseState = MOUSE_NONE;
	}
	
	public void mousePressed(MouseEvent e) {
		if(e.getButton() == 1) {
			if(rect.contains(Main.mousePos)) {
				mouseState = MOUSE_CLICK;
				for(ButtonListener bl : listeners)
					bl.buttonPressed();
			}
		}
	}
	
	public void mouseReleased(MouseEvent e) {
		if(e.getButton() == 1) {
			if(mouseState == MOUSE_CLICK) {
				mouseState = MOUSE_NONE;
				for(ButtonListener bl : listeners)
					bl.buttonReleased();
			}
		}
	}
	
	public void addListener(ButtonListener bl) {
		listeners.add(bl);
	}
	
	public interface ButtonListener {
		public void buttonPressed();
		
		public void buttonReleased();
	}
	
	public void mouseClicked(MouseEvent e) {
	}
	
	public void mouseEntered(MouseEvent e) {
	}
	
	public void mouseExited(MouseEvent e) {
	}
}
