package com.hazelcast.training.compute.lab13;

import com.hazelcast.client.HazelcastClient;
import com.hazelcast.client.config.ClientConfig;
import com.hazelcast.config.Config;
import com.hazelcast.config.QueryCacheConfig;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IMap;
import com.hazelcast.core.IEnterpriseMap;
import com.hazelcast.map.QueryCache;
import com.hazelcast.query.Predicate;

import java.util.Map;
import java.util.concurrent.TimeUnit;


/**
 * This example demonstrates the usage of a continuous-query-cache (CQC) from Hazelcast client.
 * Also in this example you can see how a CQC is configured from client side.
 */
public class ClientServer {

    public static void main(String[] args) {
        String licenseKey="ENTERPRISE_HD#10Nodes#ENjb6waySV1mkAHr07fUTKluO5JIF7600100091100511101109701111191";
        String mapName = "mapName";
        String cacheName = "cqc";
        Config config = new Config();
        config.setLicenseKey(licenseKey);
        HazelcastInstance server1 = Hazelcast.newHazelcastInstance(config);
        HazelcastInstance server2 = Hazelcast.newHazelcastInstance(config);


        QueryCacheConfig queryCacheConfig = new QueryCacheConfig(cacheName);
        queryCacheConfig.getPredicateConfig().setImplementation(new OddKeysPredicate());


        ClientConfig clientConfig = new ClientConfig();
        clientConfig.setLicenseKey(licenseKey);
        clientConfig.addQueryCacheConfig(mapName, queryCacheConfig);
        HazelcastInstance client = HazelcastClient.newHazelcastClient(clientConfig);


        IEnterpriseMap<Integer, Integer> clientMap = (IEnterpriseMap) client.getMap(mapName);
        QueryCache<Integer, Integer> cache = clientMap.getQueryCache(cacheName);


        IMap<Integer, Integer> serverMap = server1.getMap(mapName);
        for (int i = 0; i < 1002; i++) {
            try {
                serverMap.put(i, i);
            } catch(Exception e) {
                e.printStackTrace();
            }
        }

        while (true) {
            sleepSeconds(1);
            int size = cache.size();
            System.out.println("Continuous query cache size = " + size);
            if (size == 501) {
                break;
            }
        }


        try {
            for (int i = 0; i < 1002; i += 2) {
                Integer cached = cache.get(i);
                if (cached != null && i != cached) {
                    throw new AssertionError("Unexpected error, values should be equal expected = "
                            + i + ", cached = " + cached);
                }
            }

            System.out.println("All expected values are in cache and they equal to the values in underlying map");
        } finally {
            HazelcastClient.shutdownAll();
            Hazelcast.shutdownAll();
        }
    }


    private static class OddKeysPredicate implements Predicate<Integer, Integer> {

        @Override
        public boolean apply(Map.Entry<Integer, Integer> entry) {
            return entry.getKey() % 2 != 0;
        }
    }

    private static void sleepSeconds(int seconds) {
        try {
            TimeUnit.SECONDS.sleep(seconds);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
