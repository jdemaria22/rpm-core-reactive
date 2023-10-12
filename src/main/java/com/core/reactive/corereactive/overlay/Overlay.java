package com.core.reactive.corereactive.overlay;

import com.core.reactive.corereactive.component.gametime.GameTimeComponent;
import com.core.reactive.corereactive.component.renderer.RendererComponent;
import com.core.reactive.corereactive.component.unitmanager.impl.ChampionComponent;
import com.core.reactive.corereactive.component.unitmanager.model.Champion;
import com.sun.jna.Native;
import com.sun.jna.platform.win32.User32;
import com.sun.jna.platform.win32.WinDef;
import com.sun.jna.platform.win32.WinUser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.SwingUtilities;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

@Component
@RequiredArgsConstructor
@Slf4j
public class Overlay {

    public static final String OVERLAY = "Overlay";
    public static JFrame FRAME = new JFrame(OVERLAY);
    public static final int WIDTH = 1920;
    public static final int HEIGHT = 1080;
    public static final String ARIAL = "Arial";
    public static final String SPELL_Q = "Q";
    public static final String SPELL_W = "W";
    public static final String SPELL_E = "E";
    public static final String SPELL_F = "F";
    public static final String SPELL_D = "D";
    public static final String SPELL_R = "R";
    private boolean isCreated = false;
    private final Map<String, JProgressBar> progressBarMap = new HashMap<>();
    private Map<String, CirclePanel> circleMap = new HashMap<>();
    private final Map<String, Map<String, JPanel>> squareSpellBookMap = new HashMap<>();
    private final ChampionComponent championComponent;
    private final GameTimeComponent gameTime;
    private final RendererComponent rendererComponent;

    public Mono<Boolean> update() {
        SwingUtilities.invokeLater(() -> {
            if (isCreated) {
                this.updateInfo();
            } else {
                FRAME.setUndecorated(true);
                FRAME.setBackground(new Color(0, 0, 0, 0));
                FRAME.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
                FRAME.pack();
                FRAME.setVisible(true);
                FRAME.setSize(WIDTH, HEIGHT);
                FRAME.setAlwaysOnTop(true);
                this.createProgressBar();
                FRAME.setVisible(true);
                setWindowTransparentAndClickThrough();
                this.isCreated = true;
            }
        });

        return Mono.fromCallable(() -> Boolean.TRUE);
    }

    private static void setWindowTransparentAndClickThrough() {
        User32 user32 = User32.INSTANCE;
        WinDef.HWND hwnd = new WinDef.HWND(Native.getWindowPointer(FRAME));
        int styles = user32.GetWindowLong(hwnd, WinUser.GWL_EXSTYLE);
        styles = styles | WinUser.WS_EX_LAYERED | WinUser.WS_EX_TRANSPARENT;
        user32.SetWindowLong(hwnd, WinUser.GWL_EXSTYLE, styles);
        user32.SetWindowLong(hwnd, WinUser.GWL_EXSTYLE, styles | WinUser.WS_EX_TRANSPARENT);
    }

    private void createProgressBar() {
        JPanel progressBarPanel = new JPanel(new BorderLayout());
        progressBarPanel.setOpaque(false);

        JPanel contentPanel = new JPanel();
        contentPanel.setOpaque(false);
        contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));
        AtomicInteger i = new AtomicInteger();
        championComponent.getMapChampion().forEach((aLong, champion) -> {
            if (Objects.equals(champion.getTeam(), this.championComponent.getLocalPlayer().getTeam())) {
                return;
            }
            JPanel progressBarWithTitlePanel = new JPanel(new BorderLayout());
            progressBarWithTitlePanel.setOpaque(false);

            JLabel titleLabel = new JLabel(champion.getName());
            titleLabel.setForeground(Color.WHITE);
            titleLabel.setFont(new Font(ARIAL, Font.PLAIN, 12));

            JPanel squarePanelContainer = new JPanel();
            squarePanelContainer.setOpaque(false);
            squarePanelContainer.setLayout(new FlowLayout(FlowLayout.RIGHT, 15, 0));
            this.drawSpells(squarePanelContainer, champion);

            JProgressBar progressBar = new JProgressBar();
            this.progressBarMap.put(champion.getName(), progressBar);
            progressBar.setValue(50);
            progressBar.setPreferredSize(new Dimension(200, 15));
            progressBar.setForeground(Color.GREEN);

            progressBarWithTitlePanel.add(titleLabel, BorderLayout.NORTH);
            progressBarWithTitlePanel.add(progressBar, BorderLayout.CENTER);
            progressBarWithTitlePanel.add(squarePanelContainer, BorderLayout.EAST);

            contentPanel.add(progressBarWithTitlePanel);

            if (i.get() < championComponent.getMapChampion().size() - 1) {
                contentPanel.add(Box.createVerticalStrut(20));
            }
            i.getAndIncrement();
        });
        progressBarPanel.add(contentPanel, BorderLayout.WEST);
        FRAME.add(progressBarPanel, BorderLayout.SOUTH);

    }

    private void updateInfo(){
        championComponent.getMapChampion().forEach((aLong, champion) -> {
            if (Objects.equals(champion.getTeam(), this.championComponent.getLocalPlayer().getTeam())) {
                return;
            }
            Float maxHealth = champion.getMaxHealth();
            Float currentHealth = champion.getHealth();
            int percentage = (int) (currentHealth / maxHealth * 100);
            this.progressBarMap.get(champion.getName()).setValue(percentage);
            this.updateSpell(champion);
        });
    }

    private void updateSpell(Champion champion) {
        //log.info("champ name {}, {}, {}",champion.getName(), champion.getSpellBook().getQ().getLevel(), champion.getSpellBook().getQ().getReadyAtSeconds());
        if (champion.getSpellBook().getQ().getLevel() > 0 && this.gameTime.getGameTime() - champion.getSpellBook().getQ().getReadyAtSeconds() > 0) {
            this.squareSpellBookMap.get(champion.getName()).get(SPELL_Q).setBackground(Color.green);
            this.squareSpellBookMap.get(champion.getName()).get(SPELL_Q).repaint();
        } else {
            this.squareSpellBookMap.get(champion.getName()).get(SPELL_Q).setBackground(Color.gray);
            this.squareSpellBookMap.get(champion.getName()).get(SPELL_Q).repaint();
        }

        if (champion.getSpellBook().getW().getLevel() > 0 && this.gameTime.getGameTime() - champion.getSpellBook().getW().getReadyAtSeconds() > 0) {
            this.squareSpellBookMap.get(champion.getName()).get(SPELL_W).setBackground(Color.green);
            this.squareSpellBookMap.get(champion.getName()).get(SPELL_W).repaint();
        } else {
            this.squareSpellBookMap.get(champion.getName()).get(SPELL_W).setBackground(Color.gray);
            this.squareSpellBookMap.get(champion.getName()).get(SPELL_W).repaint();
        }

        if (champion.getSpellBook().getE().getLevel() > 0 && this.gameTime.getGameTime() - champion.getSpellBook().getE().getReadyAtSeconds() > 0) {
            this.squareSpellBookMap.get(champion.getName()).get(SPELL_E).setBackground(Color.green);
            this.squareSpellBookMap.get(champion.getName()).get(SPELL_E).repaint();
        } else {
            this.squareSpellBookMap.get(champion.getName()).get(SPELL_E).setBackground(Color.gray);
            this.squareSpellBookMap.get(champion.getName()).get(SPELL_E).repaint();
        }

        if (champion.getSpellBook().getR().getLevel() > 0 && this.gameTime.getGameTime() - champion.getSpellBook().getR().getReadyAtSeconds() > 0) {
            this.squareSpellBookMap.get(champion.getName()).get(SPELL_R).setBackground(Color.green);
            this.squareSpellBookMap.get(champion.getName()).get(SPELL_R).repaint();
        } else {
            this.squareSpellBookMap.get(champion.getName()).get(SPELL_R).setBackground(Color.gray);
            this.squareSpellBookMap.get(champion.getName()).get(SPELL_R).repaint();
        }

        if (champion.getSpellBook().getD().getLevel() > 0 && this.gameTime.getGameTime() - champion.getSpellBook().getD().getReadyAtSeconds() > 0) {
            this.squareSpellBookMap.get(champion.getName()).get(SPELL_D).setBackground(Color.orange);
            this.squareSpellBookMap.get(champion.getName()).get(SPELL_D).repaint();
        } else {
            this.squareSpellBookMap.get(champion.getName()).get(SPELL_D).setBackground(Color.gray);
            this.squareSpellBookMap.get(champion.getName()).get(SPELL_D).repaint();
        }

        if (champion.getSpellBook().getF().getLevel() > 0 && this.gameTime.getGameTime() - champion.getSpellBook().getF().getReadyAtSeconds() > 0) {
            this.squareSpellBookMap.get(champion.getName()).get(SPELL_F).setBackground(Color.orange);
            this.squareSpellBookMap.get(champion.getName()).get(SPELL_F).repaint();
        } else {
            this.squareSpellBookMap.get(champion.getName()).get(SPELL_F).setBackground(Color.gray);
            this.squareSpellBookMap.get(champion.getName()).get(SPELL_F).repaint();
        }
    }

    private void drawSpells(JPanel squarePanelContainer, Champion champion) {
        Map<String, JPanel> jPanelMap = new HashMap<>();

        JPanel q = this.createSquare(Color.green);
        JLabel qLabel = new JLabel(SPELL_Q);
        q.add(qLabel);
        squarePanelContainer.add(q);

        JPanel w = this.createSquare(Color.green);
        JLabel wLabel = new JLabel(SPELL_W);
        w.add(wLabel);
        squarePanelContainer.add(w);

        JPanel e = this.createSquare(Color.green);
        JLabel eLabel = new JLabel(SPELL_E);
        e.add(eLabel);
        squarePanelContainer.add(e);

        JPanel r = this.createSquare(Color.green);
        JLabel rLabel = new JLabel(SPELL_R);
        r.add(rLabel);
        squarePanelContainer.add(r);

        JPanel d = this.createSquare(Color.orange);
        JLabel dLabel = new JLabel(SPELL_D);
        d.add(dLabel);
        squarePanelContainer.add(d);

        JPanel f = this.createSquare(Color.orange);
        JLabel fLabel = new JLabel(SPELL_F);
        f.add(fLabel);
        squarePanelContainer.add(f);

        jPanelMap.put(SPELL_Q, q);
        jPanelMap.put(SPELL_W, w);
        jPanelMap.put(SPELL_E, e);
        jPanelMap.put(SPELL_R, r);
        jPanelMap.put(SPELL_D, d);
        jPanelMap.put(SPELL_F, f);
        this.squareSpellBookMap.put(champion.getName(), jPanelMap);
    }

    private JPanel createSquare(Color color) {
        JPanel squarePanel = new JPanel();
        squarePanel.setPreferredSize(new Dimension(15, 15));
        squarePanel.setBackground(color);
        return squarePanel;
    }

    private JPanel createCirclePanel(int x, int y, int radius) {
        return new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                g.setColor(Color.RED);
                g.fillOval(x - radius, y - radius, radius * 2, radius * 2);
            }
        };
    }
}
