package ru.yandex.qatools.camelot.common.builders;

import org.apache.camel.CamelContext;
import ru.yandex.qatools.camelot.api.annotations.Aggregate;
import ru.yandex.qatools.camelot.beans.AggregatorConfig;
import ru.yandex.qatools.camelot.beans.AggregatorConfigImpl;
import ru.yandex.qatools.camelot.common.CamelotAggregationStrategy;
import ru.yandex.qatools.camelot.common.CamelotFSMBuilder;
import ru.yandex.qatools.camelot.common.PluginMethodAggregationKeyStrategy;
import ru.yandex.qatools.camelot.common.RouteConfigReader;
import ru.yandex.qatools.camelot.config.PluginContext;
import ru.yandex.qatools.camelot.error.MetadataException;

import java.lang.reflect.Constructor;

import static java.lang.String.format;
import static ru.yandex.qatools.camelot.util.ReflectUtil.getAnnotationWithinHierarchy;


/**
 * @author Ilya Sadykov (mailto: smecsia@yandex-team.ru)
 */
public class CamelotAggregationStrategyBuilder implements AggregationStrategyBuilder {

    protected final CamelContext camelContext;
    protected final ClassLoader classLoader;
    protected final PluginContext pluginContext;
    protected AggregatorConfigImpl config;
    protected Class<?> fsmClass;
    protected Object fsmBuilder;

    public CamelotAggregationStrategyBuilder(CamelContext camelContext, String fsmClass, PluginContext context)
            throws Exception { //NOSONAR
        this.camelContext = camelContext;
        this.classLoader = context.getClassLoader();
        this.fsmClass = classLoader.loadClass(fsmClass);
        this.pluginContext = context;
        initFsmBuilder();
        initPluginConfig();
    }

    /**
     * Build the aggregation method and collect the configuration parameters
     */
    @Override
    public CamelotAggregationStrategy build() throws Exception { //NOSONAR
        return new CamelotAggregationStrategy(camelContext, classLoader, getFsmBuilder(), pluginContext);
    }

    @Override
    public Object getFsmBuilder() {
        return fsmBuilder;
    }

    @Override
    public void setFsmBuilder(Object fsmBuilder) {
        this.fsmBuilder = fsmBuilder;
    }

    @Override
    public Class<?> getFsmClass() {
        return fsmClass;
    }

    @Override
    public AggregatorConfig getConfig() {
        return config;
    }


    private void initFsmBuilder() {
        try {
            final Class<?> fsmBuilderClass = classLoader.loadClass(CamelotFSMBuilder.class.getName());
            final Class clClazz = classLoader.loadClass(Class.class.getName());
            Constructor fsmBuilderC = fsmBuilderClass.getConstructor(clClazz);
            fsmBuilder = fsmBuilderC.newInstance(fsmClass);
        } catch (Exception e) {
            throw new MetadataException("Failed to initialize the FSM builder", e);
        }
    }

    private void initPluginConfig() throws ReflectiveOperationException {
        config = new AggregatorConfigImpl(new RouteConfigReader(pluginContext).read());

        Object aggregator = getAnnotationWithinHierarchy(fsmClass, Aggregate.class);
        if (aggregator != null) {
            config.setStrategyInstance(new PluginMethodAggregationKeyStrategy(pluginContext));
        } else {
            throw new MetadataException(format("Failed to read @%s annotation on class %s",
                    Aggregate.class.getSimpleName(), fsmClass));
        }
    }
}
