package de.moneygoal;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;

import java.util.Locale;

/** Zeichnet die Geldziel-Leiste. Farben sind ARGB (Alpha muss gesetzt sein, sonst unsichtbar). */
public final class GoalHud {
    private static final int BG = 0xB0000000;
    private static final int BAR_BG = 0xFF3A3A3A;
    private static final int BAR_FILL = 0xFF4CAF50;
    private static final int BAR_DONE = 0xFFFFC107;
    private static final int TEXT_TITLE = 0xFFFFD54F;
    private static final int TEXT_MAIN = 0xFFFFFFFF;
    private static final int TEXT_DIM = 0xFFB0B0B0;

    private GoalHud() {
    }

    public static void render(DrawContext ctx) {
        Config cfg = Config.get();
        if (!cfg.visible || cfg.goal <= 0) {
            return;
        }

        MinecraftClient mc = MinecraftClient.getInstance();
        TextRenderer tr = mc.textRenderer;

        boolean reached = cfg.current >= cfg.goal;
        double rawPct = cfg.current * 100.0 / cfg.goal;
        double barPct = Math.max(0.0, Math.min(100.0, rawPct));

        String title = reached ? "Geldziel erreicht!" : "Geldziel";
        String amounts = Amounts.format(cfg.current) + " / " + Amounts.format(cfg.goal);
        String footer = String.format(Locale.GERMANY, "%.1f %%", rawPct);
        if (!reached) {
            footer = footer + "  |  noch " + Amounts.format(cfg.goal - cfg.current);
        }

        int textWidth = Math.max(tr.getWidth(title), Math.max(tr.getWidth(amounts), tr.getWidth(footer)));
        int w = Math.max(130, textWidth + 12);
        int h = 48;

        int sw = mc.getWindow().getScaledWidth();
        int sh = mc.getWindow().getScaledHeight();
        int x = Math.max(0, Math.min(cfg.x, sw - w));
        int y = Math.max(0, Math.min(cfg.y, sh - h));

        int accent = reached ? BAR_DONE : BAR_FILL;

        ctx.fill(x, y, x + w, y + h, BG);
        ctx.fill(x, y, x + w, y + 1, accent);

        ctx.drawTextWithShadow(tr, title, x + 6, y + 5, TEXT_TITLE);
        ctx.drawTextWithShadow(tr, amounts, x + 6, y + 16, TEXT_MAIN);

        int bx = x + 6;
        int by = y + 27;
        int bw = w - 12;
        int bh = 6;
        ctx.fill(bx, by, bx + bw, by + bh, BAR_BG);
        int fillW = (int) Math.round(bw * barPct / 100.0);
        if (fillW > 0) {
            ctx.fill(bx, by, bx + fillW, by + bh, accent);
        }

        ctx.drawTextWithShadow(tr, footer, x + 6, y + 37, TEXT_DIM);
    }
}
