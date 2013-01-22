// Copyright © 2008-2013 Esko Luontola <www.orfjackal.net>
// This software is released under the Apache License 2.0.
// The license text is at http://dimdwarf.sourceforge.net/LICENSE

package net.orfjackal.dimdwarf.server;

import com.google.inject.*;
import net.orfjackal.dimdwarf.actors.ActorStarter;
import net.orfjackal.dimdwarf.modules.*;
import net.orfjackal.dimdwarf.util.MavenUtil;
import org.kohsuke.args4j.*;
import org.slf4j.*;

import java.io.*;
import java.util.*;

public class Main {
    private static final Logger logger = LoggerFactory.getLogger(Main.class);

    // TODO: speed up startup by loading classes in parallel
    // Loading the classes is what takes most of the time in startup - on JDK 7 it can be speeded up
    // by loading the classes in parallel. Preliminary tests promise 50% speedup (and 15% slowdown on JDK 6).
    // Doing the following operations in different threads might be able to parallelize the class loading:
    // - create a Guice injector for an empty module (loads Guice's classes)
    // - open a MINA socket acceptor in a random port and close it (loads MINA's classes)
    // - instantiate and run Dimdwarf's modules outside Guice (loads some of Dimdwarf's classes)
    // - create the actual injector with Dimdwarf's modules and return it via a Future (what we really wanted)

    public static void main(String[] args) throws InterruptedException, IOException {
        Thread.setDefaultUncaughtExceptionHandler(new KillProcessOnUncaughtException());

        ServerOptions options = parseArguments(args);
        logger.info("Dimdwarf {} starting up", getVersion());

        try {
            Module appModule = loadApplication(options.applicationDir.getCanonicalFile());
            List<Module> modules = configureServerModules(options.port, appModule);
            logger.info("Modules configured");

            Injector injector = Guice.createInjector(Stage.PRODUCTION, modules);
            logger.info("Modules loaded");

            injector.getInstance(ActorStarter.class).start();
            logger.info("Server started");

        } catch (ConfigurationException e) {
            logger.error(e.getMessage(), e);
            System.exit(1);
        }
    }

    private static ServerOptions parseArguments(String[] args) {
        ServerOptions options = new ServerOptions();
        CmdLineParser parser = new CmdLineParser(options);
        try {
            parser.parseArgument(args);
        } catch (CmdLineException e) {
            // TODO: do not hard code the launcher file name?
            System.err.println(e.getMessage());
            System.err.println("java -jar launcher.jar" + parser.printExample(ExampleMode.REQUIRED));
            parser.printUsage(System.err);
            System.exit(1);
        }
        return options;
    }

    private static String getVersion() {
        String version = MavenUtil.getPom("net.orfjackal.dimdwarf", "dimdwarf-core").getProperty("version");
        return version != null ? version : "<unknown version>";
    }

    private static Module loadApplication(File applicationDir) throws ConfigurationException {
        logger.info("Opening application directory {}", applicationDir);

        ApplicationLoader loader = new ApplicationLoader(applicationDir, Main.class.getClassLoader());
        logger.info("Found application {}", loader.getApplicationName());

        logger.info("Loading application module {}", loader.getApplicationModule());
        return loader.newModuleInstance();
    }

    public static List<Module> configureServerModules(int port, Module appModule) {
        List<Module> modules = new ArrayList<>();
        modules.add(new ActorInstallerModule(
                new ControllerModule(),
                new AuthenticatorModule(),
                new NetworkModule(port)
        ));
        modules.add(new TimestampModule());
        modules.add(appModule);
        return modules;
    }
}
