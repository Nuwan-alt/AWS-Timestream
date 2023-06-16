package org.ohlcTS.TimeStream;

import com.amazonaws.services.timestreamwrite.model.WriteRecordsResult;
import org.ohlcTS.KafkaManager;
import org.ohlcTS.TimeStream.Clients.ClientImpl;
import org.ohlcTS.models.OHLC_TS;
import org.ohlcTS.models.TestModel;
import org.ohlcTS.utils.Setting;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.core.SdkPojo;
import software.amazon.awssdk.services.timestreamquery.TimestreamQueryClient;
import software.amazon.awssdk.services.timestreamquery.model.*;
import software.amazon.awssdk.services.timestreamquery.paginators.QueryIterable;
import software.amazon.awssdk.services.timestreamwrite.TimestreamWriteClient;
import software.amazon.awssdk.services.timestreamwrite.model.*;
import software.amazon.awssdk.services.timestreamwrite.model.ConflictException;
import software.amazon.awssdk.services.timestreamwrite.model.MeasureValueType;
import software.amazon.awssdk.services.timestreamwrite.model.Record;
import software.amazon.awssdk.services.timestreamwrite.paginators.ListDatabasesIterable;

import java.io.FileNotFoundException;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


public class ServiceManager {

    TimestreamWriteClient tswc = new ClientImpl().buildWriteClient();
    private Map<String,String> Settings;
    public static final Logger uploadedDataLogger = LoggerFactory.getLogger("uploadedData");
    public static final Logger uploadedSummeryLogger = LoggerFactory.getLogger("uploadedSummery");
    public static final Logger batchUploadStatus = LoggerFactory.getLogger("batchUploadStatus");
    public static final Logger rejectedSummery = LoggerFactory.getLogger("rejectedSummery");
    public static final Logger rejectedData = LoggerFactory.getLogger("rejectedData");

    private static final SimpleDateFormat sdf3 = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    String DATABASE_NAME;
    String TABLE_NAME;
    int ONE_GB_IN_BYTES = 1073741824;


    public ServiceManager() throws FileNotFoundException {
        Setting setting = new Setting();
        this.Settings = setting.getSettings();
        this.DATABASE_NAME = Settings.get("DATABASE_NAME");
        this.TABLE_NAME = Settings.get("TABLE_NAME");
    }

    public void createTable(TimestreamWriteClient timestreamWriteClient) {
        System.out.println("Creating table");
        long HT_TTL_HOURS = 24;
        long CT_TTL_DAYS = 5;

        final RetentionProperties retentionProperties = RetentionProperties.builder()
                .memoryStoreRetentionPeriodInHours(HT_TTL_HOURS)
                .magneticStoreRetentionPeriodInDays(CT_TTL_DAYS).build();
        final CreateTableRequest createTableRequest = CreateTableRequest.builder()
                .databaseName(DATABASE_NAME).tableName(TABLE_NAME).retentionProperties(retentionProperties).build();

        try {
            timestreamWriteClient.createTable(createTableRequest);
            System.out.println("Table [" + TABLE_NAME + "] successfully created.");
        } catch (ConflictException e) {

            System.out.println("Table [" + TABLE_NAME + "] exists on database [" + DATABASE_NAME + "] . Skipping database creation");
        }

    }

    public void writeRecords(TimestreamWriteClient timestreamWriteClient, List<Record> records, String table_name) {

        int rejectCount = 0;
        System.out.println("Records writing started");
        batchUploadStatus.info(" **** {} - Records writing started **** ", table_name);
        
        WriteRecordsRequest writeRecordsRequest = WriteRecordsRequest.builder()
                .databaseName(DATABASE_NAME)
                .tableName(table_name)
                .records(records)
                .build();

        try {
            WriteRecordsResult writeRecordsResult;
            WriteRecordsResponse WriteRecordsResponse = timestreamWriteClient.writeRecords(writeRecordsRequest);

            batchUploadStatus.info("==== {} - Successfully Uploaded! Batch size - {} ====",table_name,records.size());

        } catch (RejectedRecordsException e) {

            for (RejectedRecord rejectedRecord : e.rejectedRecords()) {
                rejectCount++;
                System.out.println( records.get(rejectedRecord.recordIndex()));
                System.out.println(rejectedRecord.reason());
                System.out.println("^^^^^^^^^^^^^^^^");

                String key = records.get(rejectedRecord.recordIndex()).dimensions().get(0).value();
                String tradeTime = records.get(rejectedRecord.recordIndex()).measureValues().get(0).value();

                rejectedData.trace(" {} ",records.get(rejectedRecord.recordIndex()));
                rejectedSummery.debug("Trade time - {}   key - {}   reason - {} ",tradeTime,key,rejectedRecord.reason());

//                System.out.println(rejectedRecord.record().dimensions());

            }
            batchUploadStatus.info(" ==== Batch size  - {}, Rejected data - {}, Uploaded - {} ==== ",records.size(),rejectCount,records.size()-rejectCount);
            System.out.println("Other records were written successfully. ");
        } catch (Exception e) {
            batchUploadStatus.error("Batch upload failed - {}",e);
        }
    }

    private Record buildRecords(OHLC_TS ohlcTs){

        List<MeasureValue> measureValues = new ArrayList<>();

        measureValues.add(MeasureValue.builder().name("ID").value(ohlcTs.getId()).type(MeasureValueType.VARCHAR).build());
        measureValues.add(MeasureValue.builder().name("KEY").value(ohlcTs.getKey()).type(MeasureValueType.VARCHAR).build());
        measureValues.add(MeasureValue.builder().name("TICKER_SERIAL").value(String.valueOf(ohlcTs.getTickerSerial())).type(MeasureValueType.BIGINT).build());
        measureValues.add(MeasureValue.builder().name("TICKER_ID").value(ohlcTs.getTickerId()).type(MeasureValueType.VARCHAR).build());
        measureValues.add(MeasureValue.builder().name("SOURCE_ID").value(ohlcTs.getSourceId()).type(MeasureValueType.VARCHAR).build());

        measureValues.add(MeasureValue.builder().name("INSTRUMENT_TYPE_ID").value(String.valueOf(ohlcTs.getInstrumentTypeId())).type(MeasureValueType.BIGINT).build());
        measureValues.add(MeasureValue.builder().name("TRANSACTION_DATE").value(String.valueOf(ohlcTs.getTransactionDate())).type(MeasureValueType.VARCHAR).build());
        measureValues.add(MeasureValue.builder().name("TRADE_TIME").value(String.valueOf(ohlcTs.getTradeTime())).type(MeasureValueType.VARCHAR).build());
        measureValues.add(MeasureValue.builder().name("OPEN").value(String.valueOf(ohlcTs.getOpen())).type(MeasureValueType.DOUBLE).build());
        measureValues.add(MeasureValue.builder().name("HIGH").value(String.valueOf(ohlcTs.getHigh())).type(MeasureValueType.DOUBLE).build());

        measureValues.add(MeasureValue.builder().name("LOW").value(String.valueOf(ohlcTs.getLow())).type(MeasureValueType.DOUBLE).build());
        measureValues.add(MeasureValue.builder().name("CLOSE").value(String.valueOf(ohlcTs.getClose())).type(MeasureValueType.DOUBLE).build());
        measureValues.add(MeasureValue.builder().name("VOLUME").value(String.valueOf(ohlcTs.getVolume())).type(MeasureValueType.DOUBLE).build());
        measureValues.add(MeasureValue.builder().name("NUMBER_OF_TRADES").value(String.valueOf(ohlcTs.getNumberOfTrades())).type(MeasureValueType.BIGINT).build());
        measureValues.add(MeasureValue.builder().name("TURNOVER").value(String.valueOf(ohlcTs.getTurnover())).type(MeasureValueType.BIGINT).build());

        measureValues.add(MeasureValue.builder().name("AVG_PRICE").value(String.valueOf(ohlcTs.getAvgPrice())).type(MeasureValueType.DOUBLE).build());
        measureValues.add(MeasureValue.builder().name("CHANGE").value(String.valueOf(ohlcTs.getChange())).type(MeasureValueType.DOUBLE).build());
        measureValues.add(MeasureValue.builder().name("PCT_CHANGE").value(String.valueOf(ohlcTs.getPctChange())).type(MeasureValueType.DOUBLE).build());
        measureValues.add(MeasureValue.builder().name("PREV_CLOSED").value(String.valueOf(ohlcTs.getPrevClosed())).type(MeasureValueType.DOUBLE).build());
        measureValues.add(MeasureValue.builder().name("CF_IN_COUNT").value(String.valueOf(ohlcTs.getCfInCount())).type(MeasureValueType.BIGINT).build());

        measureValues.add(MeasureValue.builder().name("CF_IN_VOLUME").value(String.valueOf(ohlcTs.getCfInVolume())).type(MeasureValueType.BIGINT).build());
        measureValues.add(MeasureValue.builder().name("CF_IN_TURNOVER").value(String.valueOf(ohlcTs.getCfInTurnover())).type(MeasureValueType.DOUBLE).build());
        measureValues.add(MeasureValue.builder().name("CF_OUT_COUNT").value(String.valueOf(ohlcTs.getCfOutCount())).type(MeasureValueType.BIGINT).build());
        measureValues.add(MeasureValue.builder().name("CF_OUT_VOLUME").value(String.valueOf(ohlcTs.getCfOutVolume())).type(MeasureValueType.BIGINT).build());
        measureValues.add(MeasureValue.builder().name("CF_OUT_TURNOVER").value(String.valueOf(ohlcTs.getCfOutTurnover())).type(MeasureValueType.DOUBLE).build());

        measureValues.add(MeasureValue.builder().name("LAST_UPDATED_ON").value(String.valueOf(ohlcTs.getLastUpdatedOn())).type(MeasureValueType.VARCHAR).build());
        measureValues.add(MeasureValue.builder().name("SNAPSHOT_TYPE").value(String.valueOf(ohlcTs.getSnapshotType())).type(MeasureValueType.VARCHAR).build());
        measureValues.add(MeasureValue.builder().name("CONTRIBUTOR").value(String.valueOf(ohlcTs.getContributor())).type(MeasureValueType.VARCHAR).build());
        measureValues.add(MeasureValue.builder().name("ADJUSTED_DATE").value(String.valueOf(ohlcTs.getAdjustedDate())).type(MeasureValueType.DOUBLE).build());
        measureValues.add(MeasureValue.builder().name("MARKET_CAP").value(String.valueOf(ohlcTs.getMarketCap())).type(MeasureValueType.DOUBLE).build());

        measureValues.add(MeasureValue.builder().name("FREE_FLOAT_MARKET_CAP").value(String.valueOf(ohlcTs.getFreeFloatMarketCap())).type(MeasureValueType.DOUBLE).build());
        measureValues.add(MeasureValue.builder().name("IS_ANN").value(String.valueOf(ohlcTs.getIsAnn())).type(MeasureValueType.BIGINT).build());
        measureValues.add(MeasureValue.builder().name("SPLIT_ACT").value(String.valueOf(ohlcTs.getSplitAct())).type(MeasureValueType.VARCHAR).build());
        measureValues.add(MeasureValue.builder().name("NEWS_PROVIDER").value(String.valueOf(ohlcTs.getNewsProvider())).type(MeasureValueType.VARCHAR).build());
        measureValues.add(MeasureValue.builder().name("LASTTRADEPRICE").value(String.valueOf(ohlcTs.getLastTradePrice())).type(MeasureValueType.DOUBLE).build());

        measureValues.add(MeasureValue.builder().name("BESTBIDPRICE").value(String.valueOf(ohlcTs.getBestBidPrice())).type(MeasureValueType.DOUBLE).build());
        measureValues.add(MeasureValue.builder().name("BESTASKPRICE").value(String.valueOf(ohlcTs.getBestAskPrice())).type(MeasureValueType.DOUBLE).build());
        measureValues.add(MeasureValue.builder().name("TOTAL_STOCKS").value(String.valueOf(ohlcTs.getTotalStocks())).type(MeasureValueType.BIGINT).build());
        measureValues.add(MeasureValue.builder().name("TOTAL_BID_QTY").value(String.valueOf(ohlcTs.getTotalBidQty())).type(MeasureValueType.BIGINT).build());
        measureValues.add(MeasureValue.builder().name("TOTAL_ASK_QTY").value(String.valueOf(ohlcTs.getTotalAskQty())).type(MeasureValueType.BIGINT).build());

        measureValues.add(MeasureValue.builder().name("INCREMENT_ID").value(String.valueOf(ohlcTs.getIncrementId())).type(MeasureValueType.DOUBLE).build());
        measureValues.add(MeasureValue.builder().name("BEST_BID_QTY").value(String.valueOf(ohlcTs.getBestBidQty())).type(MeasureValueType.BIGINT).build());
        measureValues.add(MeasureValue.builder().name("BEST_ASK_QTY").value(String.valueOf(ohlcTs.getBestAskQty())).type(MeasureValueType.BIGINT).build());
        measureValues.add(MeasureValue.builder().name("VWAP").value(String.valueOf(ohlcTs.getVwap())).type(MeasureValueType.DOUBLE).build());
        measureValues.add(MeasureValue.builder().name("DOWN_TICK_VOLUME").value(String.valueOf(ohlcTs.getDownTickVolume())).type(MeasureValueType.BIGINT).build());

        measureValues.add(MeasureValue.builder().name("UP_TICK_VOLUME").value(String.valueOf(ohlcTs.getUpTickVolume())).type(MeasureValueType.BIGINT).build());
        measureValues.add(MeasureValue.builder().name("BEST_BID_PRICE").value(String.valueOf(ohlcTs.getBestBidPriceIdo())).type(MeasureValueType.DOUBLE).build());
        measureValues.add(MeasureValue.builder().name("BEST_ASK_PRICE").value(String.valueOf(ohlcTs.getBestAskPriceIdo())).type(MeasureValueType.DOUBLE).build());
        measureValues.add(MeasureValue.builder().name("BID_VWAP").value(String.valueOf(ohlcTs.getBidVwap())).type(MeasureValueType.DOUBLE).build());
        measureValues.add(MeasureValue.builder().name("ASK_VWAP").value(String.valueOf(ohlcTs.getAskVwap())).type(MeasureValueType.DOUBLE).build());

        measureValues.add(MeasureValue.builder().name("SEQUENCE_NUMBER").value(String.valueOf(ohlcTs.getSequenceNumber())).type(MeasureValueType.BIGINT).build());
        measureValues.add(MeasureValue.builder().name("PER").value(String.valueOf(ohlcTs.getPer())).type(MeasureValueType.DOUBLE).build());
        measureValues.add(MeasureValue.builder().name("PBR").value(String.valueOf(ohlcTs.getPbr())).type(MeasureValueType.DOUBLE).build());
        measureValues.add(MeasureValue.builder().name("PSR").value(String.valueOf(ohlcTs.getPsr())).type(MeasureValueType.DOUBLE).build());
        measureValues.add(MeasureValue.builder().name("PCR").value(String.valueOf(ohlcTs.getPcr())).type(MeasureValueType.DOUBLE).build());

        measureValues.add(MeasureValue.builder().name("DYR").value(String.valueOf(ohlcTs.getDyr())).type(MeasureValueType.DOUBLE).build());
        measureValues.add(MeasureValue.builder().name("DIVIDEND_AMOUNT").value(String.valueOf(ohlcTs.getDividendAmount())).type(MeasureValueType.DOUBLE).build());
        measureValues.add(MeasureValue.builder().name("CLOSING_BEST_BID").value(String.valueOf(ohlcTs.getClosingBestBid())).type(MeasureValueType.DOUBLE).build());
        measureValues.add(MeasureValue.builder().name("CLOSING_BEST_ASK").value(String.valueOf(ohlcTs.getClosingBestAsk())).type(MeasureValueType.DOUBLE).build());

        Record ohlcMetric = Record.builder()
                .measureName(ohlcTs.getSourceId())
                .measureValueType(MeasureValueType.VARCHAR)
                .measureValues(measureValues)
                .build();
        return ohlcMetric;
    }

    public void listDatabases(TimestreamWriteClient timestreamWriteClient) {
        System.out.println("Listing databases");
        ListDatabasesRequest request = ListDatabasesRequest.builder().maxResults(2).build();
        ListDatabasesIterable listDatabasesIterable = timestreamWriteClient.listDatabasesPaginator(request);
        for(ListDatabasesResponse listDatabasesResponse : listDatabasesIterable) {
            final List<Database> databases = listDatabasesResponse.databases();
            databases.forEach(database -> System.out.println(database.databaseName()));
        }
    }


    private void parseQueryResult(QueryResponse response) {
        final QueryStatus queryStatus = response.queryStatus();

        System.out.println("Query progress so far: " + queryStatus.progressPercentage() + "%");

        double bytesScannedSoFar = (double) queryStatus.cumulativeBytesScanned() / ONE_GB_IN_BYTES;
        System.out.println("Data scanned so far: " + bytesScannedSoFar + " GB");

        double bytesMeteredSoFar = (double) queryStatus.cumulativeBytesMetered() / ONE_GB_IN_BYTES;
        System.out.println("Data metered so far: " + bytesMeteredSoFar + " GB");

        List<ColumnInfo> columnInfo = response.columnInfo();
        List<Row> rows = response.rows();

        System.out.println("Metadata: " + columnInfo);
        System.out.println("Data: ");

        // iterate every row
        for (Row row : rows) {
            System.out.println(parseRow(columnInfo, row));
        }
    }

    private String parseRow(List<ColumnInfo> columnInfo, Row row) {
        List<Datum> data = row.data();
        List<String> rowOutput = new ArrayList<>();
        // iterate every column per row
        for (int j = 0; j < data.size(); j++) {
            ColumnInfo info = columnInfo.get(j);
            Datum datum = data.get(j);
            rowOutput.add(parseDatum(info, datum));
        }
        return String.format("{%s}", rowOutput.stream().map(Object::toString).collect(Collectors.joining(",")));
    }

    private String parseDatum(ColumnInfo info, Datum datum) {
        if (datum.nullValue() != null && datum.nullValue()) {
            return info.name() + "=" + "NULL";
        }
        Type columnType = info.type();
        // If the column is of TimeSeries Type
        if (columnType.timeSeriesMeasureValueColumnInfo() != null) {
            return parseTimeSeries(info, datum);
        }
        // If the column is of Array Type
        else if (columnType.arrayColumnInfo() != null) {
            List<Datum> arrayValues = datum.arrayValue();
            return info.name() + "=" + parseArray(info.type().arrayColumnInfo(), arrayValues);
        }
        // If the column is of Row Type
        else if (columnType.rowColumnInfo() != null && columnType.rowColumnInfo().size() > 0) {
            List<ColumnInfo> rowColumnInfo = info.type().rowColumnInfo();
            Row rowValues = datum.rowValue();
            return parseRow(rowColumnInfo, rowValues);
        }
        // If the column is of Scalar Type
        else {
            return parseScalarType(info, datum);
        }
    }

    private String parseScalarType(ColumnInfo info, Datum datum) {
        return parseColumnName(info) + datum.scalarValue();
    }
    private String parseColumnName(ColumnInfo info) {
        return info.name() == null ? "" : info.name() + "=";
    }

    private String parseArray(ColumnInfo arrayColumnInfo, List<Datum> arrayValues) {
        List<String> arrayOutput = new ArrayList<>();
        for (Datum datum : arrayValues) {
            arrayOutput.add(parseDatum(arrayColumnInfo, datum));
        }
        return String.format("[%s]", arrayOutput.stream().map(Object::toString).collect(Collectors.joining(",")));
    }

    private String parseTimeSeries(ColumnInfo info, Datum datum) {
        List<String> timeSeriesOutput = new ArrayList<>();
        for (TimeSeriesDataPoint dataPoint : datum.timeSeriesValue()) {
            timeSeriesOutput.add("{time=" + dataPoint.time() + ", value=" +
                    parseDatum(info.type().timeSeriesMeasureValueColumnInfo(), dataPoint.value()) + "}");
        }
        return String.format("[%s]", timeSeriesOutput.stream().map(Object::toString).collect(Collectors.joining(",")));
    }













}
