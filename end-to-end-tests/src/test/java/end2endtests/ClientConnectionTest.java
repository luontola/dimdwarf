// Copyright © 2008-2010 Esko Luontola <www.orfjackal.net>
// This software is released under the Apache License 2.0.
// The license text is at http://dimdwarf.sourceforge.net/LICENSE

package end2endtests;

import end2endtests.apps.echo.EchoApp;
import end2endtests.runner.*;
import org.junit.*;

public class ClientConnectionTest {

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

    @Test
    public void login_with_wrong_password_fails() throws Exception {
        server.startApplication(EchoApp.class);

        client.setPassword("wrong-password");
        client.sendLogin();
        client.failsToLogin();
    }

    @Test
    public void login_and_logout_successfully() throws Exception {
        server.startApplication(EchoApp.class);

        client.sendLogin();
        client.getsLoggedIn();

        client.sendLogout();
        client.getsLoggedOut();
    }

    @Test
    public void multiple_clients_can_be_connected_to_the_server() throws Exception {
        server.startApplication(EchoApp.class);

        client.sendLogin();
        client2.sendLogin();

        client.getsLoggedIn();
        client2.getsLoggedIn();

        client.sendLogout();
        client2.sendLogout();

        client.getsLoggedOut();
        client2.getsLoggedOut();
    }
}
