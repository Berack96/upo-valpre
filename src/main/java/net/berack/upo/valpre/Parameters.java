package net.berack.upo.valpre;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * Class that helps with parsing the parameters passed as input in the console.
 */
public class Parameters {
    private final Map<String, Boolean> arguments;
    private final String prefix;
    private Map<String, String> parameters;

    /**
     * Constructs a new Parameters object with the specified prefix and arguments.
     * The arguments can be with value, in that case in the map the boolean should
     * be true, otherwise it is only an argument that is a flag
     *
     * @param prefix    the prefix to be used
     * @param arguments a map of arguments where the key is a string and if the
     *                  boolean is true then the argument expect a value
     * @throws IllegalArgumentException if the arguments map is null or empty
     */
    public Parameters(String prefix, Map<String, Boolean> arguments) {
        if (arguments == null || arguments.size() == 0)
            throw new IllegalArgumentException();
        this.arguments = arguments;
        this.prefix = prefix;
    }

    /**
     * Get the size of the parameters.
     * 
     * @return the size of the parameters
     */
    public int size() {
        return this.parameters.size();
    }

    /**
     * Get the value of the argument passed as input.
     * 
     * @param key the key of the argument
     * @return the value of the argument
     */
    public String get(String key) {
        if (this.parameters == null)
            return null;
        return this.parameters.get(key);
    }

    /**
     * Get the value from the arguments or the default value if it is not present.
     * 
     * @param key   The key to get the value from.
     * @param parse The function to parse the value.
     * @param value The default value if the key is not present.
     * @return The value from the arguments or the default value if it is not
     *         present.
     */
    public <T> T getOrDefault(String key, Function<String, T> parse, T value) {
        var arg = this.get(key);
        return arg != null ? parse.apply(arg) : value;
    }

    /**
     * Return a string with the standard <arggument> <description> spaced enough
     * 
     * @param eventualDescription the description for the argument, if not present
     *                            the argument will be shown anyway/
     * @return a string of arguments
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
     * Parse the arguments passed and build a map of Argument --> Value that can
     * be used to retrieve the information. In the case that the arguments are not
     * in the correct format then an exception is thrown.
     * To get the arguments use the {@link #get(String)} method.
     * 
     * @param args the arguments in input
     * @throws IllegalArgumentException if the arguments are not formatted correctly
     *                                  or if there is an unknown argument or there
     *                                  are not arguments in the input
     */
    public void parse(String[] args) {
        if (args == null || args.length == 0)
            throw new IllegalArgumentException("No arguments passed");

        var result = new HashMap<String, String>();
        for (var i = 0; i < args.length; i += 1) {
            var current = args[i];
            var next = i + 1 < args.length ? args[i + 1] : null;

            var updateI = this.parseSingle(current, next, result);
            if (updateI)
                i += 1;
        }

        this.parameters = result;
    }

    /**
     * Parse one single argument and put it into the map.
     * 
     * @param current the current argument
     * @param next    the next argument if present
     * @param result  the map where to insert the value
     * @throws IllegalArgumentException if the arguments are not formatted correctly
     *                                  or if there is an unknown argument
     * @return true if the next argument is used
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
            throw new IllegalArgumentException("Unknown argument [" + current +  "]");
        return false;
    }

    /**
     * Parse the arguments passed and returns a map of Argument --> Value that can
     * be used to retrieve the information. In the case that the arguments are not
     * in the correct format then an exception is thrown and the helper is printed.
     * If the arguments passed are 0 then the helper is printed.
     * 
     * @param args         the arguments in input
     * @param prefix       the prefix to be used
     * @param arguments    a map of arguments where the key is a string and if the
     *                     boolean is true then the argument expect a value
     * @param descriptions a map of descriptions for the arguments
     * @throws IllegalArgumentException if the arguments are not formatted correctly
     *                                  or if there is an unknown argument
     * @return a map of the values
     */
    public static Parameters getArgsOrHelper(String[] args, String prefix, Map<String, Boolean> arguments,
            Map<String, String> descriptions) {

        var param = new Parameters(prefix, arguments);
        try {
            param.parse(args);
            return param;
        } catch (IllegalArgumentException e) {
            System.out.println(e.getMessage());
            System.out.println(param.helper(descriptions));
            throw new IllegalArgumentException("Invalid arguments");
        }
    }
}
