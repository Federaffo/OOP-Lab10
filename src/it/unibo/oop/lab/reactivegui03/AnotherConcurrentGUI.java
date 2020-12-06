package it.unibo.oop.lab.reactivegui03;

import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.lang.reflect.InvocationTargetException;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

public final class AnotherConcurrentGUI extends JFrame {

    private static final long serialVersionUID = 1L;
    private static final double WIDTH_PERC = 0.2;
    private static final double HEIGHT_PERC = 0.1;
    private final JLabel display = new JLabel();
    private final JButton stop = new JButton("stop");
    private final JButton up = new JButton("up");
    private final JButton down = new JButton("down");

    /**
     * Builds a new CGUI.
     */
    public AnotherConcurrentGUI() {
        super();
        final Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        this.setSize((int) (screenSize.getWidth() * WIDTH_PERC), (int) (screenSize.getHeight() * HEIGHT_PERC));
        this.setDefaultCloseOperation(EXIT_ON_CLOSE);
        final JPanel panel = new JPanel();
        panel.add(display);
        panel.add(up);
        panel.add(stop);
        panel.add(down);
        this.getContentPane().add(panel);
        this.setVisible(true);
        /*
         * Create the counter agent and start it. This is actually not so good: thread
         * management should be left to java.util.concurrent.ExecutorService
         */
        final StopCountingAgent stopCountingAgent = new StopCountingAgent();
        new Thread(stopCountingAgent).start();

        final Agent agent = new Agent();
        new Thread(agent).start();


        stop.addActionListener(new ActionListener() {
            /**
             * event handler associated to action event on button stop.
             * 
             * @param e
             *              the action event that will be handled by this listener
             */
            @Override
            public void actionPerformed(final ActionEvent e) {
                // Agent should be final
                agent.stopCounting();
                stop.setEnabled(false);
                up.setEnabled(false);
                down.setEnabled(false);

            }
        });
        
        up.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                agent.goUp();
            }
        });

        down.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                agent.goDown();
            }
        });
    }


    private class StopCountingAgent implements Runnable {
        private static final int TIME_TO_WAIT = 10000;

        @Override
        public void run() {
            while (true) {

                try {
                    Thread.sleep(TIME_TO_WAIT);
                    SwingUtilities.invokeAndWait(() -> stop.doClick());
                    stop.doClick();

                } catch (InterruptedException | InvocationTargetException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
        }
    }

    /*
     * The counter agent is implemented as a nested class. This makes it invisible
     * outside and encapsulated.
     */
    private class Agent implements Runnable {
        /*
         * Stop is volatile to ensure visibility. Look at:
         * 
         * http://archive.is/9PU5N - Sections 17.3 and 17.4
         * 
         * For more details on how to use volatile:
         * 
         * http://archive.is/4lsKW
         * 
         */
        private volatile boolean stop;
        private volatile int counter;
        private volatile boolean isReversed = false;

        @Override
        public void run() {
            while (!this.stop) {
                try {
                    /*
                     * All the operations on the GUI must be performed by the Event-Dispatch Thread
                     * (EDT)!
                     */
                    SwingUtilities
                            .invokeAndWait(() -> AnotherConcurrentGUI.this.display.setText(Integer.toString(Agent.this.counter)));
                    this.counter = (this.isReversed ? this.counter - 1 : this.counter + 1);
                    Thread.sleep(100);
                } catch (InvocationTargetException | InterruptedException ex) {
                    /*
                     * This is just a stack trace print, in a real program there should be some
                     * logging and decent error reporting
                     */
                    ex.printStackTrace();
                }
            }
        }

        public void goDown() {
            this.isReversed = true;
        }

        public void goUp() {
            this.isReversed = false;
        }

        /**
         * External command to stop counting.
         */
        public void stopCounting() {
            this.stop = true;
        }
    }

}
