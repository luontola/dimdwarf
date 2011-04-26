// Copyright © 2008-2011 Esko Luontola <www.orfjackal.net>
// This software is released under the Apache License 2.0.
// The license text is at http://dimdwarf.sourceforge.net/LICENSE

package end2endtests;

import end2endtests.apps.echo.EchoApp;
import end2endtests.runner.*;
import org.junit.*;

public class SessionMessagesTest {

    private final ServerRunner server = new ServerRunner();
    private final ClientRunner client = new ClientRunner(server).withUsername("user1");
    private final ClientRunner client2 = new ClientRunner(server).withUsername("user2");

    @After
    public void shutdownServer() {
        try {
            server.assertIsRunning();
        } finally {
            client2.disconnect();
            client.disconnect();
            server.shutdown();
        }
    }

    @Ignore
    @Test
    public void send_and_receive_messages() throws Exception {
        server.startApplication(EchoApp.class);
        client.login();

        client.sendMessage("hello");
        client.receivesMessage("hello");
    }

    // TODO: two clients sending session messages to each other (requires persistence?)
}
