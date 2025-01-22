package net.berack.upo.valpre.sim.stats;

/**
 * TODO
 */
public class ConsoleTable {

    private StringBuilder builder = new StringBuilder();
    private final int maxLen;
    private final String border;

    /**
     * TODO
     * 
     * @param header
     */
    public ConsoleTable(String... header) {
        var max = 0;
        for (var name : header)
            max = Math.max(max, name.length());
        this.maxLen = max + 2;
        this.border = ("+" + "═".repeat(maxLen)).repeat(header.length) + "+\n";
        this.builder.append(border);
        this.addRow(header);
    }

    /**
     * TODO
     * 
     * @param values
     */
    public void addRow(String... values) {
        for (var val : values) {
            var diff = maxLen - val.length();
            var first = (int) Math.ceil(diff / 2.0);
            builder.append('║');
            builder.append(" ".repeat(first));
            builder.append(val);
            builder.append(" ".repeat(diff - first));
        }

        builder.append("║\n");
        builder.append(border);
    }

    @Override
    public String toString() {
        return builder.toString();
    }
}
