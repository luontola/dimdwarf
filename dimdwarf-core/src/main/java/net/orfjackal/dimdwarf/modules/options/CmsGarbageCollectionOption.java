// Copyright © 2008-2009, Esko Luontola. All Rights Reserved.
// This software is released under the MIT License.
// The license may be viewed at http://dimdwarf.sourceforge.net/LICENSE

package net.orfjackal.dimdwarf.modules.options;

import com.google.inject.*;
import net.orfjackal.dimdwarf.api.internal.ObjectIdMigration;
import net.orfjackal.dimdwarf.gc.*;
import net.orfjackal.dimdwarf.gc.cms.ConcurrentMarkSweepCollector;

/**
 * @author Esko Luontola
 * @since 10.12.2008
 */
public class CmsGarbageCollectionOption extends AbstractModule {

    protected void configure() {
        bind(new TypeLiteral<GarbageCollector<ObjectIdMigration>>() {}).toProvider(GarbageCollectorProvider.class);
        bind(new TypeLiteral<MutatorListener<ObjectIdMigration>>() {}).toProvider(MutatorListenerProvider.class);
    }

    private static class GarbageCollectorProvider implements Provider<GarbageCollector<ObjectIdMigration>> {
        @Inject public Graph<ObjectIdMigration> graph;
        @Inject public NodeSetFactory factory;

        public GarbageCollector<ObjectIdMigration> get() {
            return new ConcurrentMarkSweepCollector<ObjectIdMigration>(graph, factory);
        }
    }

    private static class MutatorListenerProvider implements Provider<MutatorListener<ObjectIdMigration>> {
        @Inject public GarbageCollector<ObjectIdMigration> collector;

        public MutatorListener<ObjectIdMigration> get() {
            return collector.getMutatorListener();
        }
    }

    // TODO: reference counting collector
    // TODO: run the CMS collector periodically
}