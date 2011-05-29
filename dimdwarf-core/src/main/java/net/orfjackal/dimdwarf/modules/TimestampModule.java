// Copyright © 2008-2011 Esko Luontola <www.orfjackal.net>
// This software is released under the Apache License 2.0.
// The license text is at http://dimdwarf.sourceforge.net/LICENSE

package net.orfjackal.dimdwarf.modules;

import com.google.inject.AbstractModule;
import net.orfjackal.dimdwarf.domain.*;

public class TimestampModule extends AbstractModule {

    protected void configure() {
        bind(Clock.class).toInstance(new Clock(new SimpleTimestamp(0L)));
    }
}
