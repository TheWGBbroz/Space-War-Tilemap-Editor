package com.thewgb.spacewar.tmeditor;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.image.BufferedImage;
import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

import com.thewgb.spacewar.entity.Entity;
import com.thewgb.spacewar.entity.EntityType;
import com.thewgb.spacewar.sprite.Image;
import com.thewgb.spacewar.tile.Tile;

public class Main extends JPanel implements Runnable, MouseListener, KeyListener {
	private static final long serialVersionUID = 1L;
	
	public static final String version = "0.1 Beta";
	public static final String title = "Space War Tilemap Editor " + version;
	
	public static final int width = 320;
	public static final int tileList_height = 50;
	public static final int height = (width / 16 * 10 - 8) + tileList_height;
	public static final int tileList_y = height - tileList_height;
	public static final int game_height = height - tileList_height;
	public static final int PLAYER_ID = 2;
	public static final double PLAYER_HP = 20.0;
	public static final double ENTITY_HP = 20.0;
	
	public static final int scale = 4;
	public static final int update_rate = 30;
	
	public static Point mousePos = new Point(0, 0);
	
	private static JFrame window;
	
	public static void main(String[] args) {
		System.out.println("Width: " + width + ", Height: " + height);
		
		window = new JFrame(title);
		
		window.setContentPane(new Main());
		window.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		window.setResizable(false);
		window.pack();
		window.setVisible(true);
		window.setLocationRelativeTo(null);
	}
	
	private Thread thread;
	private boolean running;
	
	private BufferedImage image;
	private Graphics2D g;
	
	private short selectedTileId = 0;
	private Tile[][] tiles;
	private int rows, cols;
	
	private boolean keysPressed[] = new boolean[65565];
	private boolean mousePressed[] = new boolean[16];
	
	private BufferedImage[] entityTypes;
	private int entityDragging = -1;
	private List<SimpleEntity> entities;
	
	private Button saveButton;
	
	public Main() {
		setPreferredSize(new Dimension(width * scale, height * scale));
		setFocusable(true);
		requestFocus();
	}
	
	public void addNotify() {
		super.addNotify();
		
		if(thread == null) {
			thread = new Thread(this);
			thread.start();
		}
	}
	
	public void init() {
		running = true;
		
		image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
		g = (Graphics2D) image.getGraphics();
		g.setFont(new Font("Consolas", Font.PLAIN, 10));
		
		addMouseListener(this);
		addKeyListener(this);
		
		rows = width / Tile.TILE_SIZE;
		cols = game_height / Tile.TILE_SIZE;
		tiles = new Tile[rows][cols];
		
		entityTypes = new BufferedImage[EntityType.values().length];
		for(int i = 0; i < EntityType.values().length; i++) {
			try {
				if(EntityType.values()[i].getEntityClass() == null) {
					continue;
				}
				Image img = ((Entity) EntityType.values()[i].getEntityClass().newInstance()).getSprite();
				if(img == null)
					continue;
				entityTypes[i] = img.getSprite().getImage();
			} catch (InstantiationException | IllegalAccessException e) {
			}
		}
		
		entities = new ArrayList<>();
		
		saveButton = new Button(this, "Save", width - 30, height - 14, 26, 10);
		final JPanel instance = this;
		saveButton.addListener(new Button.ButtonListener() {
			public void buttonPressed() {
				if(getPlayer() == null) {
					JOptionPane.showMessageDialog(instance, "You need to place a player in order to save!", "Error", JOptionPane.ERROR_MESSAGE);
					return;
				}
				
				String name = JOptionPane.showInputDialog(instance, "Please enter the name of the file being saved:");
				if(!name.contains("."))
					name += ".lvl";
				
				File file = new File(name);
				if(file.exists()) {
					int answer = JOptionPane.showConfirmDialog(instance, "The file already exist. Overwrite it?");
					if(answer != JOptionPane.YES_OPTION)
						return;
					
					file.delete();
				}
				
				try{
					save(file);
				}catch(IOException e) {
					JOptionPane.showMessageDialog(instance, "An error accured while saving the file! (" + e + ")", "Error", JOptionPane.ERROR_MESSAGE);
					file.delete();
					return;
				}
				
				JOptionPane.showMessageDialog(instance, "Succesfully saved file " + name + "!");
			}
			
			public void buttonReleased() {
			}
		});
	}
	
	public void run() {
		init();
		
		long start, elapsed, wait;
		int waitMS = 1000 / update_rate;
		
		while(running) {
			start = System.currentTimeMillis();
			
			update();
			render();
			
			// Render To Screen
			Graphics g2 = getGraphics();
			g2.drawImage(image, 0, 0, width * scale, height * scale, null);
			
			elapsed = System.currentTimeMillis() - start;
			wait = waitMS - elapsed;
			if(wait > 0)
				try{
					Thread.sleep(wait);
				}catch(Exception e) {}
		}
	}
	
	public void update() {
		// Register mouse position
		Point p = getMousePosition();
		if(p != null) {
			p.x /= scale;
			p.y /= scale;
			mousePos = p;
		}
		
		// Shift + Left Click
		if(mousePressed[1] && keysPressed[KeyEvent.VK_SHIFT])
			setTileToMouse();
		
		if(mousePressed[3] && keysPressed[KeyEvent.VK_SHIFT])
			setAirToMouse();
		
		saveButton.update();
	}
	
	public void render() {
		// Clear Screen
		g.setColor(Color.WHITE);
		g.fillRect(0, 0, width, height);
		
		// Render lines
		g.setColor(Color.RED);
		for(int i = 0; i < height / Tile.TILE_SIZE; i++) {
			g.drawLine(0, i * Tile.TILE_SIZE, width, i * Tile.TILE_SIZE);
		}
		for(int i = 0; i < width / Tile.TILE_SIZE; i++) {
			g.drawLine(i * Tile.TILE_SIZE, 0, i * Tile.TILE_SIZE, height);
		}
		
		// Tile list
		g.setColor(Color.BLACK);
		g.fillRect(0, tileList_y, width, height);
		
		for(int i = 0; i < Tile.getTileList().length; i++) {
			Tile t = Tile.getTileList()[i];
			if(t == null)
				continue;
			
			int x = 10 + i * (Tile.TILE_SIZE + 2);
			int y = tileList_y + 10;
			
			Rectangle rect = new Rectangle(x, y, Tile.TILE_SIZE, Tile.TILE_SIZE);
			if(selectedTileId == i) {
				g.setColor(Color.BLUE);
				g.drawRect(x - 1, y - 1, Tile.TILE_SIZE + 1, Tile.TILE_SIZE + 1);
			}else if(rect.contains(mousePos)) {
				g.setColor(Color.RED);
				g.drawRect(x - 1, y - 1, Tile.TILE_SIZE + 1, Tile.TILE_SIZE + 1);
			}
			
			g.setColor(Color.WHITE);
			g.fillRect(x, y, Tile.TILE_SIZE, Tile.TILE_SIZE);
			g.drawString(String.valueOf(t.getId()), x, y - 1);
			
			if(t.getImage() != null)
				g.drawImage(Tile.getTileList()[i].getImage(), x, y, null);
		}
		
		// Entity list
		int yadd = 0;
		for(int i = 0; i < entityTypes.length; i++) {
			if(entityTypes[i] == null)
				continue;
			
			yadd += entityTypes[i].getWidth() + 2;
			int x = 10 + yadd;
			int y = tileList_y + 28;
			
			g.drawImage(entityTypes[i], x, y, null);
		}
		
		// Render tiles
		for(int row = 0; row < rows; row++) {
			for(int col = 0; col < cols; col++) {
				Tile t = tiles[row][col];
				if(t == null)
					t = Tile.AIR;
				if(t.getImage() != null)
					g.drawImage(t.getImage(), row * Tile.TILE_SIZE, col * Tile.TILE_SIZE, null);
			}
		}
		
		// Entity Dragging
		if(entityDragging != -1) {
			BufferedImage img = entityTypes[entityDragging];
			g.drawImage(img, mousePos.x, mousePos.y, null);
		}
		
		// Render Entities
		for(int i = 0; i < entities.size(); i++) {
			entities.get(i).render(g);
		}
		
		saveButton.render(g);
	}
	
	public void setTileToMouse() {
		int row = mousePos.x / Tile.TILE_SIZE;
		int col = mousePos.y / Tile.TILE_SIZE;
		if(row >= 0 && row < rows && col >= 0 && col < cols) {
			Tile t = Tile.getTile(selectedTileId);
			tiles[row][col] = t == Tile.AIR ? null : t;
		}
	}
	
	public void setAirToMouse() {
		int row = mousePos.x / Tile.TILE_SIZE;
		int col = mousePos.y / Tile.TILE_SIZE;
		if(row >= 0 && row < rows && col >= 0 && col < cols)
			tiles[row][col] = null;
	}
	
	public SimpleEntity getPlayer() {
		for(SimpleEntity en : entities) {
			if(en.entityId == PLAYER_ID)
				return en;
		}
		
		return null;
	}
	
	public EntityType getEntityByCustomId(int customId) {
		return EntityType.values()[customId];
	}
	
	public void mousePressed(MouseEvent e) {
		int button = e.getButton();
		mousePressed[button] = true;
		
		if(button == 1) {
			for(int i = 0; i < Tile.getTileList().length; i++) {
				if(Tile.getTileList()[i] == null)
					continue;
				
				int x = 10 + i * (Tile.TILE_SIZE + 2);
				int y = tileList_y + 10;
				
				Rectangle rect = new Rectangle(x, y, Tile.TILE_SIZE, Tile.TILE_SIZE);
				if(rect.contains(mousePos)) {
					selectedTileId = (short) i;
					return;
				}
			}
			
			int yadd = 0;
			for(int i = 0; i < entityTypes.length; i++) {
				if(entityTypes[i] == null)
					continue;
				
				yadd += entityTypes[i].getWidth() + 2;
				int x = 10 + yadd;
				int y = tileList_y + 28;
				
				Rectangle rect = new Rectangle(x, y, entityTypes[i].getWidth(), entityTypes[i].getHeight());
				if(rect.contains(mousePos)) {
					entityDragging = i;
					return;
				}
			}
			
			setTileToMouse();
		}else if(button == 3) {
			for(int i = 0; i < entities.size(); i++) {
				SimpleEntity se = entities.get(i);
				if(se.rect.contains(mousePos)) {
					entities.remove(i);
					i--;
					
					return;
				}
			}
			
			setAirToMouse();
		}else if(button == 2) {
			for(int i = 0; i < entities.size(); i++) {
				SimpleEntity se = entities.get(i);
				if(se.rect.contains(mousePos)) {
					entityDragging = se.entityId;
					
					if(!keysPressed[KeyEvent.VK_SHIFT]) {
						entities.remove(i);
						i--;
					}
					
					return;
				}
			}
		}
	}

	public void mouseReleased(MouseEvent e) {
		int button = e.getButton();
		mousePressed[button] = false;
		
		if(entityDragging != -1) {
			if(entityDragging == PLAYER_ID) {
				SimpleEntity p = getPlayer();
				if(p != null) {
					entities.remove(p);
				}
			}
			
			entities.add(new SimpleEntity(mousePos.x, mousePos.y, entityDragging));
			
			
			
			entityDragging = -1;
		}
	}
	
	public void keyPressed(KeyEvent e) {
		int key = e.getKeyCode();
		keysPressed[key] = true;
	}

	public void keyReleased(KeyEvent e) {
		int key = e.getKeyCode();
		keysPressed[key] = false;
	}
	
	private void save(File file) throws IOException {
		DataOutputStream out = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(file)));
		
		// Tilemap
		out.writeInt(rows);
		out.writeInt(cols);
		
		for(int col = 0; col < cols; col++) {
			for(int row = 0; row < rows; row++) {
				Tile t = tiles[row][col];
				if(t == null)
					t = Tile.AIR;
				out.writeShort(t.getId());
			}
		}
		
		// Player
		SimpleEntity player = getPlayer();
		entities.remove(player);
		if(player == null)
			player = new SimpleEntity(0, 0, PLAYER_ID);
		
		out.writeDouble(player.rect.x);
		out.writeDouble(player.rect.y);
		out.writeDouble(PLAYER_HP);
		
		// Entities
		out.writeInt(entities.size());
		
		for(int i = 0; i < entities.size(); i++) {
			SimpleEntity se = entities.get(i);
			
			out.writeUTF(getEntityByCustomId(se.entityId).toString());
			out.writeInt(se.rect.x);
			out.writeInt(se.rect.y);
			out.writeInt((int) (ENTITY_HP * 1000));
		}
		
		// Lightmap
		out.writeBoolean(false);
		
		
		out.close();
		
		entities.add(player);
	}
	
	public void mouseClicked(MouseEvent e) {
	}
	
	public void mouseEntered(MouseEvent e) {
	}
	
	public void mouseExited(MouseEvent e) {
	}
	
	public void keyTyped(KeyEvent e) {
	}
	
	private class SimpleEntity {
		public Rectangle rect;
		public int entityId;
		
		public SimpleEntity(int x, int y, int entityId) {
			this.entityId = entityId;
			
			int width = entityId == -1 ? 16 : entityTypes[entityId].getWidth();
			int height = entityId == -1 ? 16 : entityTypes[entityId].getHeight();
			
			this.rect = new Rectangle(x, y, width, height);
		}
		
		public void render(Graphics2D g) {
			if(entityId != -1)
				g.drawImage(entityTypes[entityId], rect.x, rect.y, null);
			
			g.setColor(Color.BLACK);
			g.drawRect(rect.x, rect.y, rect.width, rect.height);
		}
	}
}
