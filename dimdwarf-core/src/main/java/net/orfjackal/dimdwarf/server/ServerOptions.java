// Copyright © 2008-2011 Esko Luontola <www.orfjackal.net>
// This software is released under the Apache License 2.0.
// The license text is at http://dimdwarf.sourceforge.net/LICENSE

package net.orfjackal.dimdwarf.server;

import org.kohsuke.args4j.Option;

import java.io.File;

public class ServerOptions {

    @Option(name = "--app",
            usage = "path to the application directory",
            required = true)
    public File applicationDir;

    // TODO: read the port number from the app's configuration file?
    @Option(name = "--port",
            usage = "port to listen for client connections",
            required = true)
    public int port;
}
