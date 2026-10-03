package com.satvik.grader.ui.theme;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.GraphicsEnvironment;
import java.awt.Paint;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.TexturePaint;
import java.awt.Toolkit;
import java.awt.image.BufferedImage;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Random;
import java.util.Set;

/** Central palette, fonts and rendering helpers for the "liquid glass" look. */
public final class Theme {

    private Theme() {
    }

    // ---- Backdrop -----------------------------------------------------------------------------
    public static final Color BG_TOP = new Color(0x080C1B);
    public static final Color BG_BOTTOM = new Color(0x121838);
    public static final Color BLOB_VIOLET = new Color(0x6E56FF);
    public static final Color BLOB_CYAN = new Color(0x00B7FF);
    public static final Color BLOB_PINK = new Color(0xFF4FA3);
    public static final Color BLOB_TEAL = new Color(0x00D1B2);

    // ---- Text ---------------------------------------------------------------------------------
    public static final Color TEXT = new Color(0xEEF1FB);
    public static final Color TEXT_SECONDARY = new Color(0xAEB6D2);
    public static final Color TEXT_DIM = new Color(0x6C7596);

    // ---- Accents ------------------------------------------------------------------------------
    public static final Color ACCENT = new Color(0x5B8CFF);
    public static final Color ACCENT_2 = new Color(0xA25BFF);
    public static final Color DANGER = new Color(0xFF5C7A);
    public static final Color DANGER_2 = new Color(0xFF3D9A);
    public static final Color SUCCESS = new Color(0x3DDC97);
    public static final Color ERROR = new Color(0xFF5C7A);
    public static final Color WARNING = new Color(0xFFC857);
    public static final Color INFO = new Color(0x6EC1FF);

    // ---- Glass --------------------------------------------------------------------------------
    public static final Color GLASS_TOP = new Color(255, 255, 255, 30);
    public static final Color GLASS_BOTTOM = new Color(255, 255, 255, 11);
    public static final Color GLASS_EDGE_TOP = new Color(255, 255, 255, 100);
    public static final Color GLASS_EDGE_BOTTOM = new Color(255, 255, 255, 20);
    public static final Color SURFACE = new Color(5, 7, 17, 172);
    public static final Color SURFACE_EDGE = new Color(255, 255, 255, 26);
    public static final Color SELECTION = new Color(91, 140, 255, 95);
    public static final Color CURRENT_LINE = new Color(255, 255, 255, 11);
    public static final Color POPUP_BG = new Color(0x151A30);

    // ---- Diff ---------------------------------------------------------------------------------
    public static final Color DIFF_REMOVED = new Color(255, 92, 122, 62);
    public static final Color DIFF_ADDED = new Color(61, 220, 151, 55);
    public static final Color DIFF_EMPTY = new Color(255, 255, 255, 7);

    // ---- Syntax -------------------------------------------------------------------------------
    public static final Color CODE_TEXT = new Color(0xD9DEEE);
    public static final Color SYN_KEYWORD = new Color(0xC792EA);
    public static final Color SYN_TYPE = new Color(0x7FDBFF);
    public static final Color SYN_STRING = new Color(0xC3E88D);
    public static final Color SYN_NUMBER = new Color(0xF78C6C);
    public static final Color SYN_COMMENT = new Color(0x6B7598);
    public static final Color SYN_PREPROCESSOR = new Color(0xFF7AC6);
    public static final Color SYN_FUNCTION = new Color(0x82AAFF);
    public static final Color SYN_CONSTANT = new Color(0xFFCB6B);

    public static Color alpha(Color c, int a) {
        return new Color(c.getRed(), c.getGreen(), c.getBlue(), Math.max(0, Math.min(255, a)));
    }

    public static Color mix(Color a, Color b, float t) {
        float u = 1f - t;
        return new Color(
                Math.round(a.getRed() * u + b.getRed() * t),
                Math.round(a.getGreen() * u + b.getGreen() * t),
                Math.round(a.getBlue() * u + b.getBlue() * t),
                Math.round(a.getAlpha() * u + b.getAlpha() * t));
    }

    // ---- Fonts --------------------------------------------------------------------------------
    private static final Set<String> FAMILIES = new HashSet<>(Arrays.asList(
            GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames()));

    private static final String UI_FAMILY = pick("Segoe UI", "Inter", "SF Pro Text", "Helvetica Neue", Font.SANS_SERIF);
    private static final String UI_SEMIBOLD = pick("Segoe UI Semibold", "Inter SemiBold", null);
    private static final String MONO_FAMILY = pick("JetBrains Mono", "Cascadia Code", "Cascadia Mono", "Consolas",
            "Menlo", Font.MONOSPACED);

    private static String pick(String... candidates) {
        for (String c : candidates) {
            if (c == null) {
                return null;
            }
            if (FAMILIES.contains(c) || c.equals(Font.SANS_SERIF) || c.equals(Font.MONOSPACED)) {
                return c;
            }
        }
        return null;
    }

    public static Font ui(float size) {
        return new Font(UI_FAMILY, Font.PLAIN, 12).deriveFont(size);
    }

    public static Font uiSemibold(float size) {
        return UI_SEMIBOLD != null
                ? new Font(UI_SEMIBOLD, Font.PLAIN, 12).deriveFont(size)
                : new Font(UI_FAMILY, Font.BOLD, 12).deriveFont(size);
    }
    
    public static final Font TITLE_FONT = uiSemibold(14f);
    public static final Font UI_FONT = ui(13f);
    public static final Color TEXT_PRIMARY = TEXT;

    public static Font mono(float size) {
        return new Font(MONO_FAMILY, Font.PLAIN, 12).deriveFont(size);
    }

    public static String monoFamily() {
        return MONO_FAMILY;
    }

    // ---- Rendering ----------------------------------------------------------------------------
    private static final Map<?, ?> DESKTOP_HINTS =
            (Map<?, ?>) Toolkit.getDefaultToolkit().getDesktopProperty("awt.font.desktophints");

    /** Anti-aliasing for shapes + desktop-matching text rendering. */
    public static void hints(Graphics2D g) {
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
        if (DESKTOP_HINTS != null) {
            g.addRenderingHints(DESKTOP_HINTS);
        } else {
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        }
    }

    private static TexturePaint noise;

    /** Very faint grain that gives glass surfaces a "frosted" feel. */
    public static Paint noise() {
        if (noise == null) {
            int size = 96;
            BufferedImage img = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
            Random r = new Random(7);
            for (int y = 0; y < size; y++) {
                for (int x = 0; x < size; x++) {
                    int v = 128 + r.nextInt(128);
                    int a = r.nextInt(12);
                    img.setRGB(x, y, (a << 24) | (v << 16) | (v << 8) | v);
                }
            }
            noise = new TexturePaint(img, new Rectangle(0, 0, size, size));
        }
        return noise;
    }
}
