package com.devexperts.web.dxtrade5.client.position.totals.model;

import com.devexperts.client.eventbus.SubscriptionContainer;
import com.devexperts.client.reusable.core.JsMap;
import com.devexperts.client.reusable.core.ListUtils;
import com.devexperts.client.reusable.core.MapUtils;
import com.devexperts.client.reusable.core.list.JsList;
import com.devexperts.client.reusable.core.list.JsMutableList;
import com.devexperts.client.reusable.table.model.AbstractTableModel;
import com.devexperts.client.reusable.table.model.TableModel;
import com.devexperts.client.reusable.table.model.aggregate.entity.AggregatedTableRowEntity;
import com.github.timofeevda.gwt.rxjs.interop.observable.Observable;
import com.github.timofeevda.gwt.rxjs.interop.observable.Subscriber;
import com.github.timofeevda.gwt.rxjs.interop.subscription.Subscription;
import com.google.gwt.core.client.Scheduler;

import java.util.function.Predicate;

public abstract class AbstractTotalsTableModel<T extends AbstractTotalsEntity<E>,
        E extends AggregatedTableRowEntity<E>> implements TableModel<T>
{

    private final JsMap<JsMutableList<Subscriber<T>>> entitySubscribers = MapUtils.map();
    private final SubscriptionContainer subscriptionContainer;

    protected T filteredTotalsEntity;
    protected T allTotalsEntity;

    private Subscription subscription;

    public AbstractTotalsTableModel(SubscriptionContainer subscriptionContainer,
                                    T allTotalsEntity, T filteredTotalsEntity) {
        this.subscriptionContainer = subscriptionContainer;
        this.allTotalsEntity = allTotalsEntity;
        this.filteredTotalsEntity = filteredTotalsEntity;
    }

    public void activate(AbstractTableModel<E> tableModel) {
        this.filteredTotalsEntity.initTableEntitiesSource(tableModel::getEntities);
        this.allTotalsEntity.initTableEntitiesSource(tableModel::getEntities);
        activateFilteredEntities(tableModel);
        activateAllEntities(tableModel);
    }

    private void activateFilteredEntities(AbstractTableModel<E> tableModel) {
        tableModel.observeFilteredEntities()
                .subscribe(entities -> {
                    JsMutableList<E> aggregatedEntities = ListUtils.list();
                    entities.forEach(e -> {
                        if (e.isAggregate() && e.isCollapsed()) {
                            //add also collapsed positions
                            aggregatedEntities.addAll(e.getFilteredEntities());
                        } else {
                            aggregatedEntities.add(e);
                        }
                    });
                    filteredTotalsEntity.setAggregatedEntities(aggregatedEntities.filter(e -> !e.isAggregate()));
                    Scheduler.get().scheduleFinally(() -> notifyEntityChanged(filteredTotalsEntity));
                });
    }

    private void activateAllEntities(AbstractTableModel<E> tableModel) {
        subscriptionContainer.add(
                tableModel.observeAllEntities()
                        .subscribe(list -> {
                            allTotalsEntity.setAggregatedEntities(list);
                            Scheduler.get().scheduleFinally(() -> notifyEntityChanged(allTotalsEntity));
                            subscribeMetrics(getEntityForMetricsSubscription(list), tableModel);
                        }));
    }

    protected E getEntityForMetricsSubscription(JsList<E> entities) {
        if (entities == null || entities.isEmpty()) {
            return null;
        }
        return entities.get(0);
    }

    /**
     * After all metrics applied notifies subscribers the entities has been changed
     * It doesn't matter for which entity metrics changes will be subscribed model
     * until server sent updates for all positions
     * <p>
     * The reason here is just put notifying about totals changes at the final scheduler step
     * after metrics had been received
     */
    private void subscribeMetrics(E entity, AbstractTableModel<E> tableModel) {
        if (entity == null) {
            return;
        }

        closeCurrentMetricsSubscription();

        subscription = tableModel.observeEntity(entity.getId())
                .subscribe(updatedEntity -> Scheduler.get().scheduleFinally(() -> {
                            notifyEntityChanged(filteredTotalsEntity);
                            notifyEntityChanged(allTotalsEntity);
                        }
                ));
    }

    private void closeCurrentMetricsSubscription() {
        if (subscription != null) {
            subscription.unsubscribe();
        }
    }

    @Override
    public Observable<T> observeEntity(String id) {
        final T entity = allTotalsEntity.getId().equals(id) ? allTotalsEntity : filteredTotalsEntity;
        return Observable.create(subscriber -> {
            JsMutableList<Subscriber<T>> subscribers = entitySubscribers.getOr(id, ListUtils.list());
            entitySubscribers.put(id, subscribers);
            subscribers.add(subscriber);
            subscriber.add(() -> entitySubscribers.getOr(id, ListUtils.list()).remove(subscriber));
            subscriber.next(entity);
            return subscriber;
        });
    }

    @Override
    public Observable<JsList<T>> observeAllEntities() {
        return Observable.create(subscriber -> {
            subscriber.next(ListUtils.listOf(filteredTotalsEntity, allTotalsEntity));
            return subscriber;
        });
    }

    @Override
    public Observable<JsList<T>> observeFilteredEntities() {
        return observeAllEntities();
    }

    public void deactivate() {
        closeCurrentMetricsSubscription();
        entitySubscribers.clear();
        subscriptionContainer.removeAllSubscription();
    }

    public T getAllTotalsEntity() {
        return allTotalsEntity;
    }

    public T getFilteredTotalsEntity() {
        return filteredTotalsEntity;
    }

    protected void notifyEntityChanged(T entity) {
        JsList<Subscriber<T>> subscribers = entitySubscribers.getOr(entity.getId(), ListUtils.list());
        subscribers.forEach(s -> s.next(entity));
    }

    public void setBaseFilter(Predicate<E> baseFilter) {
        filteredTotalsEntity.setBaseFilter(baseFilter);
        allTotalsEntity.setBaseFilter(baseFilter);
    }

    public void clearBaseFilter() {
        setBaseFilter(null);
    }
}