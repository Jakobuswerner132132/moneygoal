package de.moneygoal;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

public class MoneyGoalClient implements ClientModInitializer {
    public static final String MOD_ID = "moneygoal";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    private static String cachedRegex = "";
    private static Pattern cachedPattern = null;

    @Override
    public void onInitializeClient() {
        Config.load();

        HudElementRegistry.addLast(
                Identifier.of(MOD_ID, "goal_bar"),
                (context, tickCounter) -> GoalHud.render(context));

        ClientCommandRegistrationCallback.EVENT.register(
                (dispatcher, registryAccess) -> registerCommands(dispatcher));

        ClientReceiveMessageEvents.GAME.register((message, overlay) -> {
            if (!overlay) {
                onGameMessage(message.getString());
            }
        });

        LOGGER.info("Money Goal geladen. Befehl: /moneygoal");
    }

    // ---------------------------------------------------------------- Chat-Erkennung (optional)

    private static Pattern compiled(String regex) {
        if (regex.equals(cachedRegex) && cachedPattern != null) {
            return cachedPattern;
        }
        try {
            cachedPattern = Pattern.compile(regex);
        } catch (PatternSyntaxException e) {
            cachedPattern = null;
        }
        cachedRegex = regex;
        return cachedPattern;
    }

    private static void onGameMessage(String text) {
        Config cfg = Config.get();
        if (cfg.balanceRegex == null || cfg.balanceRegex.isEmpty()) {
            return;
        }
        Pattern pattern = compiled(cfg.balanceRegex);
        if (pattern == null) {
            return;
        }
        Matcher matcher = pattern.matcher(text);
        if (matcher.find() && matcher.groupCount() >= 1) {
            Long value = Amounts.parse(matcher.group(1));
            if (value != null && value.longValue() != cfg.current) {
                cfg.current = value.longValue();
                Config.save();
            }
        }
    }

    // ---------------------------------------------------------------- Befehle

    private static void registerCommands(CommandDispatcher<FabricClientCommandSource> dispatcher) {
        dispatcher.register(ClientCommandManager.literal("moneygoal")
                .executes(ctx -> status(ctx.getSource()))

                .then(ClientCommandManager.literal("set")
                        .then(ClientCommandManager.argument("betrag", StringArgumentType.word())
                                .executes(ctx -> setGoal(ctx.getSource(),
                                        StringArgumentType.getString(ctx, "betrag")))))

                .then(ClientCommandManager.literal("current")
                        .then(ClientCommandManager.argument("betrag", StringArgumentType.word())
                                .executes(ctx -> setCurrent(ctx.getSource(),
                                        StringArgumentType.getString(ctx, "betrag")))))

                .then(ClientCommandManager.literal("add")
                        .then(ClientCommandManager.argument("betrag", StringArgumentType.word())
                                .executes(ctx -> change(ctx.getSource(),
                                        StringArgumentType.getString(ctx, "betrag"), true))))

                .then(ClientCommandManager.literal("remove")
                        .then(ClientCommandManager.argument("betrag", StringArgumentType.word())
                                .executes(ctx -> change(ctx.getSource(),
                                        StringArgumentType.getString(ctx, "betrag"), false))))

                .then(ClientCommandManager.literal("toggle")
                        .executes(ctx -> toggle(ctx.getSource())))

                .then(ClientCommandManager.literal("pos")
                        .then(ClientCommandManager.argument("x", IntegerArgumentType.integer(0))
                                .then(ClientCommandManager.argument("y", IntegerArgumentType.integer(0))
                                        .executes(ctx -> setPos(ctx.getSource(),
                                                IntegerArgumentType.getInteger(ctx, "x"),
                                                IntegerArgumentType.getInteger(ctx, "y"))))))

                .then(ClientCommandManager.literal("auto")
                        .executes(ctx -> autoShow(ctx.getSource()))
                        .then(ClientCommandManager.literal("off")
                                .executes(ctx -> autoSet(ctx.getSource(), "")))
                        .then(ClientCommandManager.argument("regex", StringArgumentType.greedyString())
                                .executes(ctx -> autoSet(ctx.getSource(),
                                        StringArgumentType.getString(ctx, "regex")))))

                .then(ClientCommandManager.literal("reset")
                        .executes(ctx -> reset(ctx.getSource()))));
    }

    private static Text msg(String text) {
        return Text.literal("[MoneyGoal] ").formatted(Formatting.GOLD)
                .append(Text.literal(text).formatted(Formatting.WHITE));
    }

    private static int status(FabricClientCommandSource src) {
        Config cfg = Config.get();
        if (cfg.goal <= 0) {
            src.sendFeedback(msg("Noch kein Ziel gesetzt. Beispiel: /moneygoal set 5m"));
            src.sendFeedback(msg("Weitere Befehle: current, add, remove, toggle, pos, auto, reset"));
            return 1;
        }
        double pct = cfg.current * 100.0 / cfg.goal;
        src.sendFeedback(msg(Amounts.format(cfg.current) + " / " + Amounts.format(cfg.goal)
                + String.format(Locale.GERMANY, " (%.1f %%)", pct)));
        return 1;
    }

    private static int setGoal(FabricClientCommandSource src, String raw) {
        Long value = Amounts.parse(raw);
        if (value == null || value.longValue() <= 0) {
            src.sendError(Text.literal("Ung\u00fcltiger Betrag: " + raw + " (Beispiele: 5000000, 5m, 2,5m, 750k)"));
            return 0;
        }
        Config cfg = Config.get();
        cfg.goal = value.longValue();
        cfg.visible = true;
        Config.save();
        src.sendFeedback(msg("Neues Ziel: " + Amounts.format(cfg.goal)));
        return 1;
    }

    private static int setCurrent(FabricClientCommandSource src, String raw) {
        Long value = Amounts.parse(raw);
        if (value == null || value.longValue() < 0) {
            src.sendError(Text.literal("Ung\u00fcltiger Betrag: " + raw));
            return 0;
        }
        Config cfg = Config.get();
        cfg.current = value.longValue();
        Config.save();
        src.sendFeedback(msg("Aktueller Stand: " + Amounts.format(cfg.current)));
        return 1;
    }

    private static int change(FabricClientCommandSource src, String raw, boolean add) {
        Long value = Amounts.parse(raw);
        if (value == null || value.longValue() < 0) {
            src.sendError(Text.literal("Ung\u00fcltiger Betrag: " + raw));
            return 0;
        }
        Config cfg = Config.get();
        long next = add ? cfg.current + value.longValue() : cfg.current - value.longValue();
        cfg.current = Math.max(0L, next);
        Config.save();
        src.sendFeedback(msg("Aktueller Stand: " + Amounts.format(cfg.current)));
        return 1;
    }

    private static int toggle(FabricClientCommandSource src) {
        Config cfg = Config.get();
        cfg.visible = !cfg.visible;
        Config.save();
        src.sendFeedback(msg(cfg.visible ? "Anzeige eingeblendet" : "Anzeige ausgeblendet"));
        return 1;
    }

    private static int setPos(FabricClientCommandSource src, int x, int y) {
        Config cfg = Config.get();
        cfg.x = x;
        cfg.y = y;
        Config.save();
        src.sendFeedback(msg("Position: " + x + ", " + y));
        return 1;
    }

    private static int reset(FabricClientCommandSource src) {
        Config cfg = Config.get();
        cfg.goal = 0;
        cfg.current = 0;
        Config.save();
        src.sendFeedback(msg("Ziel und Stand zur\u00fcckgesetzt"));
        return 1;
    }

    private static int autoShow(FabricClientCommandSource src) {
        Config cfg = Config.get();
        if (cfg.balanceRegex == null || cfg.balanceRegex.isEmpty()) {
            src.sendFeedback(msg("Auto-Erkennung ist aus."));
            src.sendFeedback(msg("Beispiel: /moneygoal auto Kontostand: ([\\d.,]+[kKmMbB]?)"));
            src.sendFeedback(msg("Die Klammer-Gruppe muss den Betrag aus der Chatnachricht des Servers treffen."));
        } else {
            src.sendFeedback(msg("Auto-Erkennung aktiv: " + cfg.balanceRegex));
        }
        return 1;
    }

    private static int autoSet(FabricClientCommandSource src, String regex) {
        if (!regex.isEmpty()) {
            try {
                Pattern pattern = Pattern.compile(regex);
                if (pattern.matcher("").groupCount() < 1) {
                    src.sendError(Text.literal("Der Regex braucht eine Gruppe in Klammern, z.B. ([\\d.,]+)"));
                    return 0;
                }
            } catch (PatternSyntaxException e) {
                src.sendError(Text.literal("Ung\u00fcltiger Regex: " + e.getDescription()));
                return 0;
            }
        }
        Config cfg = Config.get();
        cfg.balanceRegex = regex;
        Config.save();
        src.sendFeedback(msg(regex.isEmpty() ? "Auto-Erkennung ausgeschaltet" : "Auto-Erkennung gespeichert"));
        return 1;
    }
}
