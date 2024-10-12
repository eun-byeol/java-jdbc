package com.interface21.transaction.support;

import java.sql.Connection;
import java.util.HashMap;
import java.util.Map;
import javax.sql.DataSource;

public abstract class TransactionSynchronizationManager {

    private static final ThreadLocal<Map<DataSource, Connection>> resources = new ThreadLocal<>();

    private TransactionSynchronizationManager() {
    }

    public static Connection getResource(DataSource key) {
        Map<DataSource, Connection> connections = resources.get();
        if (connections != null && connections.containsKey(key)) {
            return connections.get(key);
        }
        return null;
    }

    public static void bindResource(DataSource key, Connection value) {
        Map<DataSource, Connection> connections = resources.get();
        if (connections == null) {
            connections = new HashMap<>();
            resources.set(connections);
        }
        connections.put(key, value);
    }

    public static Connection unbindResource(DataSource key) {
        Map<DataSource, Connection> connections = resources.get();
        if (connections != null && connections.containsKey(key)) {
            return connections.remove(key);
        }
        return null;
    }
}
