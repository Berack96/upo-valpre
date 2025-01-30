package net.berack.upo.valpre;

import java.io.File;
import java.net.URISyntaxException;
import java.util.Arrays;

public class Main {
    public static void main(String[] args) throws Exception {
        if (args.length == 0)
            exit();

        var subArgs = Arrays.copyOfRange(args, 1, args.length);
        switch (args[0]) {
            case "simulation":
                var sim = new Simulation(subArgs);
                sim.run();
                break;
            case "plot":
                var plot = new Plot(subArgs);
                plot.show();
                break;
            default:
                exit();
        }
    }

    public static void exit() throws URISyntaxException {
        var uri = Main.class.getProtectionDomain().getCodeSource().getLocation().toURI();
        var name = new File(uri).getName();
        System.out.println("Usage: java -jar " + name + ".jar [simulation|plot] [args]");
        System.exit(1);
    }
}