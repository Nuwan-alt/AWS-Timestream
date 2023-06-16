package org.ohlcTS.TimeStream;

import software.amazon.awssdk.services.timestreamwrite.TimestreamWriteClient;
import software.amazon.awssdk.services.timestreamwrite.model.Record;

import java.util.LinkedList;
import java.util.List;

public class FlushManager implements Runnable{

    ServiceManager serviceManager;
    String table_name;
    TimestreamWriteClient writeClient;
    LinkedList<Record> records;
    public FlushManager(ServiceManager serviceManager,TimestreamWriteClient writeClient,LinkedList<Record> records, String table_name){
        this.table_name = table_name;
        this.serviceManager = serviceManager;
        this.writeClient = writeClient;
        this.records = records;
    }
    @Override
    public void run() {
        this.flushData(serviceManager,writeClient, records, table_name);
    }

    private void flushData(ServiceManager serviceManager, TimestreamWriteClient writeClient,LinkedList<Record> records,String table_name) {

        if(records.size() > 0){
            serviceManager.writeRecords(writeClient,records,table_name);
        }else {
            System.out.println("No data to flush-----!");
        }

    }

}
