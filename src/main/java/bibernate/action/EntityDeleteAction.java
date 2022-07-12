package bibernate.action;

import bibernate.session.impl.EntityPersister;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class EntityDeleteAction implements EntityAction {
    private final Object entity;
    private final EntityPersister persister;

    @Override
    public void execute() {
        persister.delete(entity);
    }

    @Override
    public int priority() {
        return 3;
    }
}
