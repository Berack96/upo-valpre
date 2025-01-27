package net.berack.upo.valpre;

import java.util.HashMap;
import java.util.Map;

/**
 * TODO
 */
public class Parameters {
    private final Map<String, Boolean> arguments;
    private final String prefix;

    /**
     * TODO
     * 
     * @param arguments
     */
    public Parameters(String prefix, Map<String, Boolean> arguments) {
        if (arguments == null || arguments.size() == 0)
            throw new IllegalArgumentException();
        this.arguments = arguments;
        this.prefix = prefix;
    }

    /**
     * TODO
     * 
     * @param eventualDescription
     * @return
     */
    public String helper(Map<String, String> eventualDescription) {
        var size = 0;
        var parameters = new HashMap<String, String>();

        for (var param : this.arguments.entrySet()) {
            var string = this.prefix + param.getKey();
            if (param.getValue())
                string += " <value>";

            parameters.put(param.getKey(), string);
            size = Math.max(size, string.length());
        }
        size += 2; // spacing

        var builder = new StringBuilder();
        for (var param : parameters.entrySet()) {
            var key = param.getKey();
            var args = param.getValue();

            builder.append("  ");
            builder.append(args);

            var desc = eventualDescription.get(key);
            if (desc != null) {
                builder.append(" ".repeat(size - args.length()));
                builder.append(desc);
            }
            builder.append("\n");
        }

        return builder.toString();
    }

    /**
     * TODO
     * 
     * @param args
     * @return
     */
    public Map<String, String> parse(String[] args) {
        var result = new HashMap<String, String>();
        if (args == null || args.length == 0)
            return result;

        for (var i = 0; i < args.length; i += 1) {
            var current = args[i];
            var next = i + 1 < args.length ? args[i + 1] : null;

            var updateI = this.parseSingle(current, next, result);
            if (updateI)
                i += 1;
        }

        return result;
    }

    /**
     * TODO
     * 
     * @param current
     * @param next
     * @param result
     * @return
     */
    private boolean parseSingle(String current, String next, Map<String, String> result) {
        if (!current.startsWith(this.prefix))
            throw new IllegalArgumentException("Missing prefix [" + current + "]");
        current = current.substring(this.prefix.length());

        var value = this.arguments.get(current);
        if (value != null) {
            result.put(current, value ? next : "");
            return value;
        }

        var finalSize = result.size() + current.length();
        for (var letter : current.split(""))
            if (this.arguments.get(letter) != null)
                result.put(current, "");

        if (finalSize != result.size())
            throw new IllegalArgumentException("Argument unknown");
        return false;
    }
}
