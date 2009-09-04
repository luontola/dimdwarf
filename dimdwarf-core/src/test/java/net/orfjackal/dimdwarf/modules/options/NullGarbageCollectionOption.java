// Copyright © 2008-2009, Esko Luontola. All Rights Reserved.
// This software is released under the MIT License.
// The license may be viewed at http://dimdwarf.sourceforge.net/LICENSE

package net.orfjackal.dimdwarf.modules.options;

import com.google.inject.*;
import net.orfjackal.dimdwarf.api.internal.ObjectIdMigration;
import net.orfjackal.dimdwarf.gc.*;
import net.orfjackal.dimdwarf.tasks.util.IncrementalTask;

import javax.annotation.Nullable;
import java.util.*;

/**
 * @author Esko Luontola
 * @since 11.12.2008
 */
public class NullGarbageCollectionOption extends AbstractModule {

    protected void configure() {
        bind(new TypeLiteral<GarbageCollector<ObjectIdMigration>>() {}).toInstance(new NullGarbageCollector());
        bind(new TypeLiteral<MutatorListener<ObjectIdMigration>>() {}).toInstance(new NullMutatorListener());
    }

    public static class NullGarbageCollector implements GarbageCollector<ObjectIdMigration> {

        public List<? extends IncrementalTask> getCollectorStagesToExecute() {
            return Collections.emptyList();
        }

        public MutatorListener<ObjectIdMigration> getMutatorListener() {
            return new NullMutatorListener();
        }

        public Enum<?> getColor(ObjectIdMigration node) {
            return null;
        }
    }

    public static class NullMutatorListener implements MutatorListener<ObjectIdMigration> {

        public void onNodeCreated(ObjectIdMigration node) {
        }

        public void onReferenceCreated(@Nullable ObjectIdMigration source, ObjectIdMigration target) {
        }

        public void onReferenceRemoved(@Nullable ObjectIdMigration source, ObjectIdMigration target) {
        }
    }
}
