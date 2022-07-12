package bibernate.action;

import bibernate.session.impl.EntityPersister;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class EntityUpdateAction implements EntityAction {
    private final Object entity;
    private final EntityPersister persister;

    @Override
    public void execute() {
        persister.update(entity);
    }

    @Override
    public int priority() {
        return 2;
    }
}
