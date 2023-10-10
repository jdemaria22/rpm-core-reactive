package com.core.reactive.corereactive.overlay;

import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;

public class CirclePanel extends JPanel {
    private int x; // Coordenada X del círculo
    private int y; // Coordenada Y del círculo
    private int radius; // Radio del círculo

    public CirclePanel(int x, int y, int radius) {
        this.x = x;
        this.y = y;
        this.radius = radius;
        setPreferredSize(new Dimension(radius * 2, radius * 2));
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        g.setColor(Color.RED);
        g.fillOval(x - radius, y - radius, radius * 2, radius * 2);
    }

    public void updatePosition(int newX, int newY) {
        x = newX;
        y = newY;
        repaint();
    }
}