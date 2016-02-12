/*
 * Copyright © 2015 John H Uckele. All rights reserved.
 */

package io.vivarium.visualization;

import java.awt.Graphics;
import java.awt.Graphics2D;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.WindowConstants;

import io.vivarium.core.processor.NeuralNetwork;
import io.vivarium.core.processor.Processor;

public class ProcessorViewerDemo extends JPanel
{
    private static JFrame _window;

    private static final long serialVersionUID = -3105685457075818705L;

    // Animation variables
    private SwingGeometricGraphics _swingGraphics;
    private ProcessorRenderer _processorRenderer;

    public ProcessorViewerDemo(Processor processor)
    {
        _processorRenderer = new ProcessorRenderer(processor);
        _swingGraphics = new SwingGeometricGraphics(_processorRenderer, this);
    }

    @Override
    public void paintComponent(Graphics g)
    {
        super.paintComponent(g);

        // Get the graphics for the graphical delegate
        Graphics2D g2 = (Graphics2D) g;
        _swingGraphics.setResources(g2);

        // Hand off to the graphical delegate to perform the render
        _swingGraphics.executeRender();
    }

    public static void main(String[] args)
    {
        Processor processor = new NeuralNetwork(4, 4, true, 0);

        // Create and show the window
        ProcessorViewerDemo bv = new ProcessorViewerDemo(processor);
        _window = new JFrame();
        _window.add(bv);
        _window.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        _window.validate();

        // Set everything to be visible
        _window.setSize(500, 800);
        _window.setVisible(true);
    }
}