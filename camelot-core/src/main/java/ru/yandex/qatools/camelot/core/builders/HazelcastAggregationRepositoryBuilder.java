package ru.yandex.qatools.camelot.core.builders;

import com.hazelcast.core.HazelcastInstance;
import org.apache.camel.CamelContext;
import org.apache.camel.spi.AggregationRepository;
import ru.yandex.qatools.camelot.api.Storage;
import ru.yandex.qatools.camelot.config.Plugin;
import ru.yandex.qatools.camelot.core.impl.HazelcastStorage;
import ru.yandex.qatools.camelot.core.impl.LocalKeysHazelcastAggregatorRepository;
import ru.yandex.qatools.camelot.hazelcast.HazelcastAggregationRepository;

import static ru.yandex.qatools.camelot.util.NameUtil.pluginStorageKey;

/**
 * @author Ilya Sadykov (mailto: smecsia@yandex-team.ru)
 */
public class HazelcastAggregationRepositoryBuilder extends MemoryAggregationRepositoryBuilder {

    private final HazelcastInstance hazelcastInstance;
    private final long waitForLockSec;
    private final long lockWaitHeartBeatSec;

    public HazelcastAggregationRepositoryBuilder(HazelcastInstance hazelcastInstance, CamelContext camelContext,
                                                 long waitForLockSec, long lockWaitHeartBeatSec) {
        super(camelContext, waitForLockSec);
        this.hazelcastInstance = hazelcastInstance;
        this.waitForLockSec = waitForLockSec;
        this.lockWaitHeartBeatSec = lockWaitHeartBeatSec;
    }

    /**
     * Initialize the HazelcastRepository instance
     */
    @Override
    public AggregationRepository initWritable(Plugin plugin) throws Exception {
        final HazelcastAggregationRepository aggregationRepository = new LocalKeysHazelcastAggregatorRepository();
        aggregationRepository.setRepository(plugin.getId());
        aggregationRepository.setHazelcastInstance(hazelcastInstance);
        aggregationRepository.setWaitForLockSec(waitForLockSec);
        aggregationRepository.setLockWaitHeartbeatSec(lockWaitHeartBeatSec);
        aggregationRepository.doStart();
        return aggregationRepository;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> Storage<T> initStorage(Plugin plugin) throws Exception {
        return new HazelcastStorage<>(hazelcastInstance, pluginStorageKey(plugin.getId()));
    }

}
