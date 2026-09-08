package br.com.totvs.infrastructure.pdf;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class PdfBuilder {

    public static final float PAGE_WIDTH = 595f;
    public static final float PAGE_HEIGHT = 842f;
    public static final float MARGIN_LEFT = 50f;
    public static final float MARGIN_RIGHT = 50f;
    public static final float MARGIN_TOP = 64f;
    public static final float MARGIN_BOTTOM = 48f;
    public static final float CONTENT_WIDTH = PAGE_WIDTH - MARGIN_LEFT - MARGIN_RIGHT;

    private final List<StringBuilder> pages = new ArrayList<>();
    private final String footerText;
    private float cursorY;

    public PdfBuilder(String footerText) {
        this.footerText = footerText == null ? "" : footerText;
        newPage();
    }

    public float cursorY() {
        return cursorY;
    }

    public void newPage() {
        StringBuilder page = new StringBuilder();
        pages.add(page);
        cursorY = PAGE_HEIGHT - MARGIN_TOP;
        drawFooter(page);
    }

    public void ensureSpace(float height) {
        if (cursorY - height < MARGIN_BOTTOM) {
            newPage();
        }
    }

    public void spacing(float amount) {
        ensureSpace(amount);
        cursorY -= amount;
    }

    public void text(String content, float size, boolean bold, float r, float g, float b) {
        ensureSpace(size + 4);
        StringBuilder p = current();
        p.append("q\n").append(color(r, g, b)).append(" rg\nBT\n")
                .append(bold ? "/F2 " : "/F1 ").append(num(size)).append(" Tf\n")
                .append(num(MARGIN_LEFT)).append(" ").append(num(cursorY)).append(" Td\n")
                .append("(").append(escape(content)).append(") Tj\nET\nQ\n");
        cursorY -= size * 1.3f;
    }

    public void paragraph(String content, float size, boolean bold, float r, float g, float b) {
        List<String> lines = wrap(content, CONTENT_WIDTH, size, bold);
        for (String line : lines) {
            text(line, size, bold, r, g, b);
        }
    }

    public void divider(float r, float g, float b) {
        ensureSpace(12f);
        cursorY -= 4f;
        StringBuilder p = current();
        p.append("q\n").append(strokeColor(r, g, b)).append(" RG\n1 w\n")
                .append(num(MARGIN_LEFT)).append(" ").append(num(cursorY)).append(" m\n")
                .append(num(PAGE_WIDTH - MARGIN_RIGHT)).append(" ").append(num(cursorY)).append(" l\nS\nQ\n");
        cursorY -= 10f;
    }

    public void rect(float x, float y, float w, float h, float r, float g, float b) {
        StringBuilder p = current();
        p.append("q\n").append(color(r, g, b)).append(" rg\n")
                .append(num(x)).append(" ").append(num(y)).append(" ").append(num(w)).append(" ").append(num(h))
                .append(" re f\nQ\n");
    }

    public void tableRow(String[] cols, float[] widths, float rowHeight, float fontSize,
                         boolean bold, float[][] bg, float[][] fg) {
        ensureSpace(rowHeight);
        float y = cursorY;
        float x = MARGIN_LEFT;
        for (int i = 0; i < cols.length; i++) {
            rect(x, y - rowHeight, widths[i], rowHeight, bg[i][0], bg[i][1], bg[i][2]);
            String fitted = truncateToWidth(cols[i], widths[i] - 12, fontSize, bold);
            StringBuilder p = current();
            p.append("q\n").append(color(fg[i][0], fg[i][1], fg[i][2])).append(" rg\nBT\n")
                    .append(bold ? "/F2 " : "/F1 ").append(num(fontSize)).append(" Tf\n")
                    .append(num(x + 6)).append(" ").append(num(y - rowHeight + (rowHeight - fontSize) / 2f + 2))
                    .append(" Td\n(").append(escape(fitted)).append(") Tj\nET\nQ\n");
            x += widths[i];
        }
        cursorY = y - rowHeight;
    }

    public void callout(String marker, String message, float[] bar, float[] bgColor, float[] textColor) {
        float fontSize = 10.5f;
        float lineHeight = fontSize * 1.35f;
        List<String> lines = wrap(message, CONTENT_WIDTH - 30f, fontSize, false);
        float blockHeight = Math.max(26f, lines.size() * lineHeight + 14f);

        ensureSpace(blockHeight + 8f);
        float top = cursorY;

        rect(MARGIN_LEFT, top - blockHeight, CONTENT_WIDTH, blockHeight, bgColor[0], bgColor[1], bgColor[2]);
        rect(MARGIN_LEFT, top - blockHeight, 4f, blockHeight, bar[0], bar[1], bar[2]);

        StringBuilder p = current();
        p.append("q\n").append(color(bar[0], bar[1], bar[2])).append(" rg\nBT\n/F2 11 Tf\n")
                .append(num(MARGIN_LEFT + 14)).append(" ").append(num(top - lineHeight))
                .append(" Td\n(").append(escape(marker)).append(") Tj\nET\nQ\n");

        float ty = top - lineHeight;
        for (String line : lines) {
            p.append("q\n").append(color(textColor[0], textColor[1], textColor[2])).append(" rg\nBT\n/F1 ")
                    .append(num(fontSize)).append(" Tf\n")
                    .append(num(MARGIN_LEFT + 30)).append(" ").append(num(ty)).append(" Td\n(")
                    .append(escape(line)).append(") Tj\nET\nQ\n");
            ty -= lineHeight;
        }
        cursorY = top - blockHeight - 8f;
    }

    private void drawFooter(StringBuilder page) {
        page.append("q\n").append(color(0.55f, 0.55f, 0.55f)).append(" rg\nBT\n/F1 8 Tf\n")
                .append(num(MARGIN_LEFT)).append(" 28 Td\n(")
                .append(escape(footerText)).append(") Tj\nET\nQ\n");
    }

    private StringBuilder current() {
        return pages.get(pages.size() - 1);
    }

    private static String color(float r, float g, float b) {
        return num(r) + " " + num(g) + " " + num(b);
    }

    private static String strokeColor(float r, float g, float b) {
        return num(r) + " " + num(g) + " " + num(b);
    }

    private static String num(float v) {
        return String.format(Locale.US, "%.2f", v);
    }

    private static String escape(String s) {
        if (s == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '(' || c == ')' || c == '\\') {
                sb.append('\\').append(c);
            } else if (c == '\n' || c == '\r' || c == '\t') {
                sb.append(' ');
            } else if (c > 255) {
                sb.append('?');
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    private static float charWidth(char c, boolean bold) {
        float w;
        if (c == ' ') {
            w = 0.28f;
        } else if (Character.isDigit(c)) {
            w = 0.56f;
        } else if (Character.isUpperCase(c)) {
            w = 0.72f;
        } else if ("mwMW".indexOf(c) >= 0) {
            w = 0.9f;
        } else if ("iIl.,;:'!|".indexOf(c) >= 0) {
            w = 0.30f;
        } else {
            w = 0.52f;
        }
        return bold ? w * 1.06f : w;
    }

    private static float textWidth(String s, float size, boolean bold) {
        float total = 0f;
        for (int i = 0; i < s.length(); i++) {
            total += charWidth(s.charAt(i), bold) * size;
        }
        return total;
    }

    private static String truncateToWidth(String s, float maxWidth, float size, boolean bold) {
        if (s == null) {
            return "";
        }
        if (textWidth(s, size, bold) <= maxWidth) {
            return s;
        }
        String ellipsis = "...";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            String candidate = sb.toString() + s.charAt(i) + ellipsis;
            if (textWidth(candidate, size, bold) > maxWidth) {
                break;
            }
            sb.append(s.charAt(i));
        }
        return sb + ellipsis;
    }

    public static List<String> wrap(String text, float maxWidth, float size, boolean bold) {
        List<String> lines = new ArrayList<>();
        if (text == null || text.isBlank()) {
            lines.add("");
            return lines;
        }
        String normalized = text.replace("\r\n", "\n").replace("\r", "\n");
        for (String paragraph : normalized.split("\n", -1)) {
            if (paragraph.isBlank()) {
                lines.add("");
                continue;
            }
            String[] words = paragraph.trim().split("\\s+");
            StringBuilder current = new StringBuilder();
            for (String word : words) {
                String candidate = current.length() == 0 ? word : current + " " + word;
                if (textWidth(candidate, size, bold) > maxWidth && current.length() > 0) {
                    lines.add(current.toString());
                    current = new StringBuilder(word);
                } else {
                    current = new StringBuilder(candidate);
                }
            }
            lines.add(current.toString());
        }
        return lines;
    }

    public byte[] build() throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        List<Long> offsets = new ArrayList<>();
        offsets.add(0L);

        writeRaw(out, "%PDF-1.4\n%\u00E2\u00E3\u00CF\u00D3\n");

        int catalogObj = 1;
        int pagesObj = 2;
        int fontRegularObj = 3;
        int fontBoldObj = 4;
        int firstPageObj = 5;

        int pageCount = pages.size();
        int[] pageObjs = new int[pageCount];
        int[] contentObjs = new int[pageCount];
        for (int i = 0; i < pageCount; i++) {
            pageObjs[i] = firstPageObj + i * 2;
            contentObjs[i] = firstPageObj + i * 2 + 1;
        }

        registerOffset(offsets, out);
        writeRaw(out, catalogObj + " 0 obj\n<< /Type /Catalog /Pages " + pagesObj + " 0 R >>\nendobj\n");

        registerOffset(offsets, out);
        StringBuilder kids = new StringBuilder();
        for (int p : pageObjs) {
            kids.append(p).append(" 0 R ");
        }
        writeRaw(out, pagesObj + " 0 obj\n<< /Type /Pages /Kids [" + kids.toString().trim()
                + "] /Count " + pageCount + " >>\nendobj\n");

        registerOffset(offsets, out);
        writeRaw(out, fontRegularObj
                + " 0 obj\n<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica /Encoding /WinAnsiEncoding >>\nendobj\n");

        registerOffset(offsets, out);
        writeRaw(out, fontBoldObj
                + " 0 obj\n<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica-Bold /Encoding /WinAnsiEncoding >>\nendobj\n");

        for (int i = 0; i < pageCount; i++) {
            registerOffset(offsets, out);
            writeRaw(out, pageObjs[i] + " 0 obj\n<< /Type /Page /Parent " + pagesObj
                    + " 0 R /MediaBox [0 0 " + (int) PAGE_WIDTH + " " + (int) PAGE_HEIGHT + "]"
                    + " /Resources << /Font << /F1 " + fontRegularObj + " 0 R /F2 " + fontBoldObj + " 0 R >> >>"
                    + " /Contents " + contentObjs[i] + " 0 R >>\nendobj\n");

            registerOffset(offsets, out);
            byte[] streamBytes = pages.get(i).toString().getBytes(StandardCharsets.ISO_8859_1);
            writeRaw(out, contentObjs[i] + " 0 obj\n<< /Length " + streamBytes.length + " >>\nstream\n");
            out.write(streamBytes);
            writeRaw(out, "\nendstream\nendobj\n");
        }

        long xrefOffset = out.size();
        int totalObjs = 4 + pageCount * 2;
        writeRaw(out, "xref\n0 " + (totalObjs + 1) + "\n");
        writeRaw(out, "0000000000 65535 f \n");
        for (int i = 1; i <= totalObjs; i++) {
            writeRaw(out, String.format(Locale.US, "%010d 00000 n \n", offsets.get(i)));
        }

        writeRaw(out, "trailer\n<< /Size " + (totalObjs + 1) + " /Root " + catalogObj + " 0 R >>\n");
        writeRaw(out, "startxref\n" + xrefOffset + "\n%%EOF");

        return out.toByteArray();
    }

    private void registerOffset(List<Long> offsets, ByteArrayOutputStream out) {
        offsets.add((long) out.size());
    }

    private void writeRaw(ByteArrayOutputStream out, String s) throws IOException {
        out.write(s.getBytes(StandardCharsets.ISO_8859_1));
    }
}
