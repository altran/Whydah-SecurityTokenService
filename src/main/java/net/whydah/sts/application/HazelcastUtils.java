package net.whydah.sts.application;

import com.hazelcast.config.Config;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;

public class HazelcastUtils {

    public static HazelcastInstance initWithConfigWithFallbackToDefault(Config hazelcastConfig) {
        try {
            return Hazelcast.newHazelcastInstance(hazelcastConfig);
        } catch (Exception e) {
            return Hazelcast.newHazelcastInstance();
        }
    }
}
