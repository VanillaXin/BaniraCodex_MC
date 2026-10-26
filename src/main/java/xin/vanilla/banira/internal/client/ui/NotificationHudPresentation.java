package xin.vanilla.banira.internal.client.ui;

import net.minecraft.network.chat.Style;
import net.minecraft.util.FormattedCharSequence;
import xin.vanilla.banira.client.data.ScreenCoordinate;
import xin.vanilla.banira.client.gui.component.Notification;
import xin.vanilla.banira.client.gui.quickaction.QuickActionRegistry;
import xin.vanilla.banira.client.gui.quickaction.QuickIcon;
import xin.vanilla.banira.client.notification.NotificationHudState;
import xin.vanilla.banira.client.notification.NotificationTypeRegistry;
import xin.vanilla.banira.common.util.ColorUtils;

import java.util.*;
import java.util.function.Function;

/** Presentation only; the notification manager retains all input and lifetime ownership. */
public final class NotificationHudPresentation implements AutoCloseable {
    public record Run(String text, Style style) { }
    private final UiDomAccess dom;
    private final Function<ScreenCoordinate, ScreenCoordinate> projection;
    private final Map<Notification, Card> cards = new IdentityHashMap<>();
    private final List<Card> active = new ArrayList<>();
    private UiDomAccess.Node unread, mail, count;
    private String unreadStyle, countLabel;

    public NotificationHudPresentation(UiDomAccess dom, Function<ScreenCoordinate, ScreenCoordinate> projection) {
        this.dom = dom; this.projection = projection;
    }

    public static List<Run> runs(FormattedCharSequence line) {
        List<Run> result = new ArrayList<>();
        StringBuilder text = new StringBuilder();
        Style[] current = {null};
        line.accept((index, style, codePoint) -> {
            if (!Objects.equals(current[0], style) && !text.isEmpty()) {
                result.add(new Run(text.toString(), current[0])); text.setLength(0);
            }
            current[0] = style; text.appendCodePoint(codePoint); return true;
        });
        if (!text.isEmpty()) result.add(new Run(text.toString(), current[0]));
        return List.copyOf(result);
    }

    public void update(HudDrawList frame) {
        Set<Notification> keep = Collections.newSetFromMap(new IdentityHashMap<>());
        active.clear();
        boolean showUnread = false;
        for (HudDrawList.Part part : frame.parts()) {
            if (part.kind == HudDrawList.Kind.NOTIFICATION && part.notification.usesAuiLayout()) {
                Notification n = part.notification;
                keep.add(n);
                Card card = cards.computeIfAbsent(n, key -> new Card(key));
                card.update(part); active.add(card);
            } else if (part.kind == HudDrawList.Kind.UNREAD) {
                showUnread = true; updateUnread(part);
            }
        }
        var iterator = cards.entrySet().iterator();
        while (iterator.hasNext()) { var entry = iterator.next(); if (!keep.contains(entry.getKey())) { entry.getValue().close(); iterator.remove(); } }
        if (!showUnread) removeUnread();
    }

    public void afterLayout() {
        for (Card card : active) {
            List<Notification.TextHit> hits = new ArrayList<>();
            for (int i = 0; i < card.fragments.size(); i++)
                hits.add(new Notification.TextHit(card.fragments.get(i).bounds(), card.styles.get(i)));
            card.notification.auiHitLayout(card.root.bounds(), card.close.bounds(), card.body.bounds(), hits);
        }
    }

    private UiDomAccess.Node child(UiDomAccess.Node parent, String tag, String css) {
        UiDomAccess.Node node = dom.create(tag); node.attribute("class", css);
        String style = switch (css) {
            case "notice-mask" -> "position:absolute;pointer-events:none;";
            case "notice-icon" -> "position:absolute;left:6px;top:5px;width:9px;height:9px;image-rendering:pixelated;";
            case "notice-name" -> "position:absolute;left:19px;top:5px;right:28px;height:10px;font-family:default;font-size:7px;line-height:10px;white-space:nowrap;overflow:hidden;";
            case "notice-severity" -> "position:absolute;right:20px;top:5px;width:8px;height:8px;";
            case "notice-close" -> "position:absolute;right:4px;top:4px;width:11px;height:11px;image-rendering:pixelated;";
            case "notice-body" -> "position:absolute;left:6px;right:6px;top:" + xin.vanilla.banira.client.notification.NotificationBubbleLayout.BODY_TOP
                    + "px;bottom:" + xin.vanilla.banira.client.notification.NotificationBubbleLayout.BOTTOM + "px;overflow:hidden;";
            case "notice-line" -> "display:flex;flex-direction:row;align-items:flex-start;width:100%;height:10px;min-height:10px;overflow:hidden;";
            default -> "";
        };
        if (!style.isEmpty()) node.attribute("style", style);
        parent.append(node); return node;
    }

    private void updateUnread(HudDrawList.Part part) {
        if (unread == null) {
            unread = child(dom.find("overlay"), "div", "unread-hud");
            mail = child(unread, "texture", "unread-mail"); mail.attribute("src", "apricityui:apricity/banira_codex/icons/hud-mail.png");
            count = child(unread, "div", "unread-count");
        }
        ScreenCoordinate b = projection.apply(new ScreenCoordinate(part.x, part.y, part.width, part.height));
        double units = b.width() / part.width;
        String next = position(b) + "box-sizing:border-box;display:flex;align-items:center;justify-content:center;pointer-events:none;font-family:default;color:#506d57;background:#fbfdfbf0;"
                + "gap:" + 3 * units + "px;border:" + units + "px solid #c8d9ce;border-radius:" + 3 * units + "px;";
        boolean resized = !next.equals(unreadStyle);
        if (resized) {
            unread.attribute("style", next); unreadStyle = next;
            mail.attribute("style", "flex-shrink:0;width:" + 12 * units + "px;height:" + 12 * units + "px;");
        }
        String label = NotificationHudState.countLabel(part.color);
        if (resized || !label.equals(countLabel)) count.attribute("style", "display:" + (label.isEmpty() ? "none" : "block")
                + ";white-space:nowrap;font-family:default;font-size:" + 8 * units + "px;line-height:" + 12 * units + "px;");
        if (!label.equals(countLabel)) { count.text(label); countLabel = label; }
    }

    private void removeUnread() {
        if (unread != null) { mail.remove(); count.remove(); unread.remove(); unread = mail = count = null; unreadStyle = countLabel = null; }
    }

    private final class Card implements AutoCloseable {
        final Notification notification;
        final UiDomAccess.Node mask, root, interior, rule, icon, name, severity, close, body;
        final List<UiDomAccess.Node> fragments = new ArrayList<>(), lines = new ArrayList<>();
        final List<Style> styles = new ArrayList<>();
        List<List<Run>> content = List.of();
        String namespace, moduleName, chrome, maskStyle, appearance;

        Card(Notification notification) {
            this.notification = notification;
            mask = child(dom.find("overlay"), "div", "notice-mask");
            root = child(mask, "div", "notice");
            interior = child(root, "div", "notice-interior");
            rule = child(interior, "div", "notice-rule");
            icon = child(interior, "texture", "notice-icon");
            name = child(interior, "div", "notice-name");
            severity = child(interior, "texture", "notice-severity");
            close = child(interior, "texture", "notice-close");
            close.attribute("src", "apricityui:apricity/banira_codex/icons/close.png");
            body = child(interior, "div", "notice-body");
        }

        void update(HudDrawList.Part part) {
            ScreenCoordinate b = projection.apply(new ScreenCoordinate(part.x, part.y, part.width, part.height));
            ScreenCoordinate clip = part.clip == null ? null : projection.apply(part.clip);
            String maskCss = "position:absolute;pointer-events:none;z-index:" + (int) part.depth + ";";
            if (clip == null) maskCss += "left:0;top:0;width:100%;height:100%;";
            else {
                maskCss += position(clip) + "overflow:hidden;";
                b = new ScreenCoordinate(b.x() - clip.x(), b.y() - clip.y(), b.width(), b.height());
            }
            if (!maskCss.equals(maskStyle)) { mask.attribute("style", maskCss); maskStyle = maskCss; }
            int alpha = notification.renderAlpha() < 0 ? 255 : Math.max(0, notification.renderAlpha());
            double units = part.width <= 0 ? 1 : b.width() / part.width;
            String style = notification.style() == null ? "normal" : notification.style().name().toLowerCase(Locale.ROOT);
            String bg = style.equals("warning") ? "#fffdf6f5" : style.equals("error") ? "#fff8faf7" : "#fbfdfbf5";
            String border = style.equals("warning") ? "#e1d4b4" : style.equals("error") ? "#e5c8d1" : "#c8d9ce";
            String origin = HudDomPresentation.origin(notification.renderScaleCenter() == null ? "TOP_LEFT" : notification.renderScaleCenter().name());
            String css = position(b) + "box-sizing:border-box;pointer-events:none;overflow:hidden;font-family:default;color:#384d40;background:" + bg
                    + ";border:" + units + "px solid " + border + ";border-radius:" + 4 * units + "px;opacity:" + alpha / 255.0 + ";transform:scale(" + Math.max(.01, notification.renderScale())
                    + ");transform-origin:" + origin + ";";
            if (!css.equals(chrome)) {
                root.attribute("style", css); chrome = css;
                interior.attribute("style", "position:absolute;left:0;top:0;width:" + part.width + "px;height:" + part.height
                        + "px;transform-origin:0 0;transform:scale(" + units + ");");
            }
            String type = Objects.toString(notification.notificationType(), "default");
            String ns = type.equals("default") ? "banira_codex" : type.split("[.:]", 2)[0];
            var display = NotificationTypeRegistry.modDisplayNameInternal(ns);
            String label = display == null ? ns : display.toString();
            if (!ns.equals(namespace)) {
                namespace = ns;
                String source = QuickActionRegistry.get().allEntriesInOrder().stream()
                        .filter(entry -> entry.id().startsWith(ns + ":") && entry.quickIcon() != null
                                && entry.quickIcon().kind() == QuickIcon.Kind.RESOURCE && entry.quickIcon().texture() != null)
                        .map(entry -> entry.quickIcon().texture().location().toString()).findFirst()
                        .orElse("banira_codex:gui/quick_icon.png");
                icon.attribute("src", source);
            }
            if (!label.equals(moduleName)) { name.text(label); moduleName = label; }
            String nextAppearance = ns + ":" + style;
            if (!nextAppearance.equals(appearance)) {
                root.attribute("class", "notice " + style);
                name.attribute("style", "position:absolute;left:19px;top:5px;right:28px;height:10px;font-family:default;font-size:7px;line-height:10px;white-space:nowrap;overflow:hidden;color:"
                        + HudDrawList.css(notification.notificationTheme().textSecondary()) + ";");
                String tone = switch (style) { case "warning" -> "#b4924d"; case "error" -> "#ba6a7a"; case "success" -> "#6f9e85"; default -> "#729789"; };
                rule.attribute("style", "position:absolute;left:0;top:0;width:2px;height:100%;background:" + tone + ";");
                severity.attribute("src", "apricityui:apricity/banira_codex/icons/hud-" + style + ".png");
                appearance = nextAppearance;
            }
            List<List<Run>> next = notification.richDrawLines().stream().map(NotificationHudPresentation::runs).toList();
            if (!next.equals(content)) {
                fragments.forEach(UiDomAccess.Node::remove); lines.forEach(UiDomAccess.Node::remove);
                fragments.clear(); lines.clear(); styles.clear();
                int total = next.stream().mapToInt(List::size).sum();
                if (total > 128) throw new IllegalStateException("Notification rich text budget");
                for (List<Run> line : next) {
                    UiDomAccess.Node row = child(body, "div", "notice-line"); lines.add(row);
                    for (Run run : line) {
                        UiDomAccess.Node span = child(row, "span", "notice-text");
                        Style s = run.style();
                        int color = s.getColor() == null ? 0xff384d40 : s.getColor().getValue() | 0xff000000;
                        color = ColorUtils.ensureReadableTextArgb(color, 0xfffbfdfb);
                        span.attribute("style", "flex-shrink:0;white-space:pre;font-family:default;font-size:8px;line-height:10px;color:" + HudDrawList.css(color) + ";"
                                + (s.isBold() ? "font-weight:bold;" : "") + (s.isItalic() ? "font-style:italic;" : "")
                                + (s.isUnderlined() || s.getClickEvent() != null ? "text-decoration:underline;" : ""));
                        span.text(run.text()); fragments.add(span); styles.add(s);
                    }
                }
                content = next;
            }
        }

        @Override public void close() {
            fragments.forEach(UiDomAccess.Node::remove); lines.forEach(UiDomAccess.Node::remove);
            for (UiDomAccess.Node node : List.of(body, close, severity, name, icon, rule, interior, root, mask)) node.remove();
        }
    }

    private static String position(ScreenCoordinate b) {
        return "position:absolute;left:" + b.x() + "px;top:" + b.y() + "px;width:" + b.width() + "px;height:" + b.height() + "px;";
    }

    @Override public void close() { cards.values().forEach(Card::close); cards.clear(); active.clear(); removeUnread(); }
}
