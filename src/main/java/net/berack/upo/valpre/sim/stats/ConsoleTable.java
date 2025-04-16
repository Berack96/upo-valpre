package net.berack.upo.valpre.sim.stats;

/**
 * Class used to build a nice table from the header and row passed.
 * The result can be retrieved by {@link #toString()}
 */
public class ConsoleTable {

    private StringBuilder builder = new StringBuilder();
    private final int maxLen;
    private final int columns;
    private final String border;

    /**
     * Create a new table with the header passed as input.
     * The table will have as many columns as the length of the header array.
     * Each column will have the same size and will be the max length of all the
     * headers string.
     * 
     * @param header an array of strings
     * @throws NullPointerException if the array is null
     */
    public ConsoleTable(String... header) {
        this(-1, header);
    }

    /**
     * Create a new table with the header passed as input.
     * The table will have as many columns as the length of the header array.
     * Each column will have the same size and will be the max length of all the
     * headers string or the maxLen passed as input.
     * 
     * @param maxLen the max length of the columns
     * @param header an array of strings
     * @throws NullPointerException if the array is null
     */
    public ConsoleTable(int maxLen, String... header) {
        var max = Math.max(0, maxLen);
        for (var name : header)
            max = Math.max(max, name.length());

        this.columns = header.length;
        this.maxLen = max + 2;
        this.border = ("+" + "═".repeat(this.maxLen)).repeat(header.length) + "+\n";
        this.builder.append(border);
        this.addRow(header);
    }

    /**
     * Add to the table the values of the array. If the array null or the length is
     * different than the header.len then an exception i thrown.
     * 
     * @param values an array of values to print
     * @throw IllegalArgumentException if the values.len is != than the headers.len
     */
    public void addRow(String... values) {
        if (this.columns != values.length)
            throw new IllegalArgumentException("Length must be " + this.columns);

        for (var val : values) {
            var diff = maxLen - val.length();
            var first = Math.max((int) Math.ceil(diff / 2.0), 0);
            builder.append('║');
            builder.append(" ".repeat(first));
            builder.append(val);
            builder.append(" ".repeat(Math.max(diff - first, 0)));
        }

        builder.append("║\n");
        builder.append(border);
    }

    @Override
    public String toString() {
        return builder.toString();
    }
}
