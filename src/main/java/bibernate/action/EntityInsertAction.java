package bibernate.action;

import bibernate.session.impl.EntityPersister;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class EntityInsertAction implements EntityAction {
    private final Object entity;
    private final EntityPersister persister;

    @Override
    public void execute() {
        persister.insert(entity);
    }

    @Override
    public int priority() {
        return 1;
    }
}
