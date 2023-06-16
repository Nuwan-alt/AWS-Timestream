package org.ohlcTS;

//import com.amazonaws.services.dynamodbv2.document.Index;
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mubasher.regional.data.OHLC;


import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.reflect.Array;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.stream.Collectors;

//import com.mubasher.regional.system.Settings;


import org.apache.commons.math3.util.Precision;
import org.ohlcTS.TimeStream.Clients.ClientImpl;
import org.ohlcTS.TimeStream.FlushManager;
import org.ohlcTS.TimeStream.ServiceManager;
import org.ohlcTS.kafka.KafkaException;
import org.ohlcTS.kafka.KafkaProcessor;
import org.ohlcTS.kafka.KafkaUtils;
import org.ohlcTS.models.OHLCCandle;
import org.ohlcTS.models.OHLC_TS;
import org.ohlcTS.utils.Setting;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.services.timestreamwrite.TimestreamWriteClient;
import software.amazon.awssdk.services.timestreamwrite.model.*;
import software.amazon.awssdk.services.timestreamwrite.model.Record;

public class KafkaManager extends Thread implements KafkaProcessor {
    private ObjectMapper mapper;
//    private SolrInitializer solrInitializer;
    private KafkaUtils utils;
    private HashMap<String, List<String>> coreMappings;
    private HashMap<String, List<OHLC_TS>> dataMap;
//    private HashMap<String, List<RealtimeSolr>> realtimeDataMap;
    private final Object dataLock = new Object();
    private int batchSize;
    private HashMap<String, HashMap<String, Integer>> generatedIntervalRecordCountMap;
    private long lastStatusRecordTime;
    private Instant applicationStartTime;
    private List<String> enabledExchanges;
    private Map<String,String> Settings;
    private List<Record> ohlcTS_data;
    private boolean filterByExchangeEnabled;
    private ServiceManager serviceManager;
    private TimestreamWriteClient writeClient;

//    Ini setting = new Ini(new File("timestream_writer.ini"));
    private ThreadLocal<SimpleDateFormat> fullFormat = ThreadLocal.withInitial(() -> new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'"));
    private ThreadLocal<SimpleDateFormat> yyyyMMddHHmmssFormat = ThreadLocal.withInitial(() -> new SimpleDateFormat("yyyyMMddHHmmss"));

    public static final Logger kafkaDataLogger = LoggerFactory.getLogger("KafkaData");
    public static final Logger kafkaSummaryLogger = LoggerFactory.getLogger("KafkaSummary");
    public static final Logger mappedSummeryLogger = LoggerFactory.getLogger("mappedSummery");

    private String[] tableMapper;


    private  ExecutorService threadPool;
    private Map<String,LinkedList<Record>> exchange_map_ohlc;

    public KafkaManager(ExecutorService thereadPool) throws IOException {
        Setting setting = new Setting();
        this.Settings = setting.getSettings();
        this.tableMapper = Settings.get("TABLE_MAP").split(",");
        this.threadPool = thereadPool;
        this.exchange_map_ohlc = new HashMap<>();
        this.writeClient = new ClientImpl().buildWriteClient();
        this.serviceManager = new ServiceManager();
        this.ohlcTS_data = new ArrayList<>();
        this.Settings = setting.getSettings();
        init();
        this.setPriority(8);
        this.setName("Thread Name & Id >>>> KafkaManager :: " + this.getId());

        this.start();
//        LogManager.serverLogger.info("<KafkaManager> Thread Starting");
    }

    private void init() throws FileNotFoundException {

        this.generatedIntervalRecordCountMap = new HashMap<>();
        this.applicationStartTime = Instant.now();
        this.mapper = new ObjectMapper();
        this.mapper.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.NONE);
        this.mapper.setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.PUBLIC_ONLY);
//        this.solrInitializer = SolrInitializer.getInstance();
//        this.batchSize = solrInitializer.getSolr().getMaxSolrWrites();
        this.utils = new KafkaUtils(Arrays.stream(Settings.get("KAFKA_TOPICS").split(",")).filter(s-> !s.isEmpty())
                .collect(Collectors.toList()));
        this.utils.setProcessor(this);
//        this.coreMappings = HttpClient.getInstance().getCoreMappings();
        this.dataMap = new HashMap<>();
//        this.realtimeDataMap = new HashMap<>();
        this.enabledExchanges = Arrays.stream(Settings.get("EXCHANGE_LIST").split(","))
                .collect(Collectors.toList());
        this.filterByExchangeEnabled = Settings.get("IS_FILTER_BY_EXCHANGE_ENABLED").equals("1");

        new Timer().scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                if (Settings.get("KAFKA_TOPIC_NAME_PREFIX").equals("stock")){
//                    flushRealtimeData();
                } else {
                   scheduledFlushData();
                }
            }
        }, 0, 60000);

    }

    public void run() {
        while (true) {
            pollForOHLC();
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
//                LogManager.serverLogger.error("<KafkaManager> Error occurred while sleep", e);
            }
        }
    }

    public void pollForOHLC() {

        try {
//            printStatistics();
            utils.pollRecords();
        } catch (Exception e) {
//            LogManager.serverLogger.error("<KafkaManager> Fail to poll and process ohlc ", e);
            System.out.println("went wrong");
            System.out.println(e);
        }
    }

    @Override
    public void processRecords(String record, String topic) throws KafkaException {


        if (topic.equals("stock")){
        } else {
            String period = topic.replace(Settings.get("KAFKA_TOPIC_NAME_PREFIX"), "");

            if (period.isEmpty()) {
                period = "ido";
            }

            if (!generatedIntervalRecordCountMap.containsKey(period)) {
                generatedIntervalRecordCountMap.put(period, new HashMap<>());
            }

            HashMap<String, Integer> intervalCountMap = generatedIntervalRecordCountMap.get(period);

            if (!period.isEmpty()) {

//                System.out.println(record);
                Optional<OHLC> ohlc = Optional.empty();

                try {
//                    LogManager.inLogger.info("<KafkaManager> Received new record " + record);
                    ohlc = Optional.of(mapper.readValue(record, OHLCCandle.class));

                    if (ohlc.isPresent()) {
                        OHLC ohlcRecord = ohlc.get();
                        String exchange = ohlcRecord.exchange;

                        if (filterByExchangeEnabled && !enabledExchanges.contains(exchange)) {
                            return;
                        }

                        if (!intervalCountMap.containsKey(exchange)) {
                            intervalCountMap.put(exchange, 0);
                        }

                        int countForExchange = intervalCountMap.get(exchange);
                        intervalCountMap.put(exchange, ++countForExchange);


                        long ohlcCount = ohlcTS_data.size();
                        if (ohlcRecord.time > 0) {
                            kafkaSummaryLogger.debug("trade time - {}    key - {}~{}",(Instant.parse(Instant.ofEpochSecond(ohlcRecord.time * 60).toString())),ohlcRecord.exchange,ohlcRecord.symbol);
                            OHLC_TS ohlcTs = mapToTimestream(ohlcRecord);
                            kafkaDataLogger.trace(ohlc_tsToString(ohlcTs));
                            this.exchangeDataManager(ohlcTs);
                        }
                    }
                } catch (Exception e) {
                    throw new KafkaException("Failed to process record");
                }
            }
        }
    }
    
    private void exchangeDataManager(OHLC_TS ohlcTs){

        String exchange = ohlcTs.getSourceId();
        String symbol = ohlcTs.getTickerId();
        int table_map_index = this.tableCalculator(symbol);
        String table_name = Settings.get("TEST_ENABLE") + exchange + "-" + (String) Array.get(tableMapper, table_map_index);


        if ((exchange_map_ohlc.containsKey(table_name))) {
            exchange_map_ohlc.get(table_name).add(buildTimestreamRecords(ohlcTs));
        } else {
            LinkedList<Record> newList = new LinkedList<>();
            newList.add(buildTimestreamRecords(ohlcTs));
            exchange_map_ohlc.put(table_name, newList);
        }

        LinkedList<Record> recordsForSingleExchange = exchange_map_ohlc.get(table_name);

        System.out.println(recordsForSingleExchange.size() + "  " + exchange);

        if (recordsForSingleExchange.size() == 95 ){
            this.flushData(recordsForSingleExchange,table_name);
        }
    }

    public int tableCalculator(String ticker_Id){
        int databaseIndex = -1;
        int ascii = Character.toLowerCase( ticker_Id.charAt(0));

        if(48 <=ascii && ascii <=57){
            databaseIndex = ascii - 22;
        } else if (97 <=ascii && ascii <= 122) {
            databaseIndex = ascii - 97;
        }
        return Math.floorDiv(databaseIndex,6);
    }
    private void scheduledFlushData(){
        for (String key:exchange_map_ohlc.keySet()) {
            this.flushData(exchange_map_ohlc.get(key),key);
        }
    }

    private Record buildTimestreamRecords(OHLC_TS ohlcTs){

        List<MeasureValue> measureValues = new LinkedList<>();

//        measureValues.add(MeasureValue.builder().name("ID").value(ohlcTs.getId()).type(MeasureValueType.VARCHAR).build());
//        measureValues.add(MeasureValue.builder().name("KEY").value(ohlcTs.getKey()).type(MeasureValueType.VARCHAR).build());
//        measureValues.add(MeasureValue.builder().name("TICKER_SERIAL").value(String.valueOf(ohlcTs.getTickerSerial())).type(MeasureValueType.BIGINT).build());
//        measureValues.add(MeasureValue.builder().name("TICKER_ID").value(ohlcTs.getTickerId()).type(MeasureValueType.VARCHAR).build());
//        measureValues.add(MeasureValue.builder().name("SOURCE_ID").value(ohlcTs.getSourceId()).type(MeasureValueType.VARCHAR).build());

//        measureValues.add(MeasureValue.builder().name("INSTRUMENT_TYPE_ID").value(String.valueOf(ohlcTs.getInstrumentTypeId())).type(MeasureValueType.BIGINT).build());
//        measureValues.add(MeasureValue.builder().name("TRANSACTION_DATE").value(String.valueOf(ohlcTs.getTransactionDate())).type(MeasureValueType.VARCHAR).build());
        measureValues.add(MeasureValue.builder().name("TRADE_TIME").value(String.valueOf(ohlcTs.getTradeTime())).type(MeasureValueType.VARCHAR).build());
        measureValues.add(MeasureValue.builder().name("OPEN").value(String.valueOf(ohlcTs.getOpen())).type(MeasureValueType.DOUBLE).build());
        measureValues.add(MeasureValue.builder().name("HIGH").value(String.valueOf(ohlcTs.getHigh())).type(MeasureValueType.DOUBLE).build());

        measureValues.add(MeasureValue.builder().name("LOW").value(String.valueOf(ohlcTs.getLow())).type(MeasureValueType.DOUBLE).build());
        measureValues.add(MeasureValue.builder().name("CLOSE").value(String.valueOf(ohlcTs.getClose())).type(MeasureValueType.DOUBLE).build());
        measureValues.add(MeasureValue.builder().name("VOLUME").value(String.valueOf(ohlcTs.getVolume())).type(MeasureValueType.BIGINT).build());
        measureValues.add(MeasureValue.builder().name("NUMBER_OF_TRADES").value(String.valueOf(ohlcTs.getNumberOfTrades())).type(MeasureValueType.BIGINT).build());
        measureValues.add(MeasureValue.builder().name("TURNOVER").value(String.valueOf(ohlcTs.getTurnover())).type(MeasureValueType.DOUBLE).build());

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
        measureValues.add(MeasureValue.builder().name("ADJUSTED_DATE").value(String.valueOf(ohlcTs.getAdjustedDate())).type(MeasureValueType.VARCHAR).build());
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

//        measureValues.add(MeasureValue.builder().name("SEQUENCE_NUMBER").value(String.valueOf(ohlcTs.getSequenceNumber())).type(MeasureValueType.BIGINT).build());
        measureValues.add(MeasureValue.builder().name("PER").value(String.valueOf(ohlcTs.getPer())).type(MeasureValueType.DOUBLE).build());
        measureValues.add(MeasureValue.builder().name("PBR").value(String.valueOf(ohlcTs.getPbr())).type(MeasureValueType.DOUBLE).build());
        measureValues.add(MeasureValue.builder().name("PSR").value(String.valueOf(ohlcTs.getPsr())).type(MeasureValueType.DOUBLE).build());
        measureValues.add(MeasureValue.builder().name("PCR").value(String.valueOf(ohlcTs.getPcr())).type(MeasureValueType.DOUBLE).build());

        measureValues.add(MeasureValue.builder().name("DYR").value(String.valueOf(ohlcTs.getDyr())).type(MeasureValueType.DOUBLE).build());
        measureValues.add(MeasureValue.builder().name("DIVIDEND_AMOUNT").value(String.valueOf(ohlcTs.getDividendAmount())).type(MeasureValueType.DOUBLE).build());
        measureValues.add(MeasureValue.builder().name("CLOSING_BEST_BID").value(String.valueOf(ohlcTs.getClosingBestBid())).type(MeasureValueType.DOUBLE).build());
        measureValues.add(MeasureValue.builder().name("CLOSING_BEST_ASK").value(String.valueOf(ohlcTs.getClosingBestAsk())).type(MeasureValueType.DOUBLE).build());


        Record ohlcMetric = Record.builder()
                .measureName(ohlcTs.getKey())
                .measureValueType(MeasureValueType.MULTI)
                .measureValues(measureValues)
                .dimensions(Dimension.builder().name("Key").value(ohlcTs.getKey()).build())
                .time(String.valueOf(ohlcTs.getTimeStampValue()))
                .build();
        System.currentTimeMillis();

        mappedSummeryLogger.trace("trade time - {}  Key - {} mapped to OHLC_TS",ohlcTs.getTradeTime(),ohlcTs.getKey());
        return ohlcMetric;
    }

    private String ohlc_tsToString(OHLC_TS ohlcTs){
        return "<key>- " + ohlcTs.getKey() +
                " <TRADE_TIME>- " + ohlcTs.getTradeTime() +
                " <HIGH>- " + ohlcTs.getHigh() +
                " <CF_OUT_VOLUME>- " + ohlcTs.getCfOutVolume() +
                " <CLOSING_BEST_ASK>- " + ohlcTs.getClosingBestAsk() +
                " <CLOSE>- " + ohlcTs.getClose() +
                " <CLOSING_BEST_BID>- " + ohlcTs.getClosingBestBid() +
                " <CF_IN_VOLUME>- " + ohlcTs.getCfInVolume() +
                " <DIVIDEND_AMOUNT>- " + ohlcTs.getDividendAmount() +
                " <CF_IN_COUNT>- " + ohlcTs.getCfInCount() +
                " <INCREMENT_ID>- " + ohlcTs.getIncrementId() +
                " <VWAP>- " + ohlcTs.getVwap() +
                " <TOTAL_STOCKS>- " + ohlcTs.getTotalStocks() +
                " <LASTTRADEPRICE>- " + ohlcTs.getLastTradePrice() +
                " <PBR>- " + ohlcTs.getPbr() +
                " <TOTAL_BID_QTY>- " + ohlcTs.getTotalBidQty() +
                " <BEST_BID_PRICE>- " + ohlcTs.getBestBidPrice() +
                " <VOLUME>- " + ohlcTs.getVolume() +
                " <ADJUSTED_DATE>- " + ohlcTs.getAdjustedDate() +
                " <LAST_UPDATED_ON>- " + ohlcTs.getLastUpdatedOn() +
                " <PSR>- " + ohlcTs.getPsr() +
                " <CF_IN_TURNOVER>- " + ohlcTs.getCfInTurnover() +
                " <LOW>- " + ohlcTs.getLow() +
                " <MARKET_CAP>- " + ohlcTs.getMarketCap() +
                " <CF_OUT_TURNOVER>- " + ohlcTs.getCfOutTurnover() +
                " <PCR>- " + ohlcTs.getPcr() +
                " <SNAPSHOT_TYPE>- " + ohlcTs.getSnapshotType() +
                " <BEST_BID_QTY>- " + ohlcTs.getBestBidQty() +
                " <NUMBER_OF_TRADES>- " + ohlcTs.getNumberOfTrades() +
                " <BEST_ASK_PRICE>- " + ohlcTs.getBestAskPrice() +
                " <BESTBIDPRICE>- " + ohlcTs.getBestBidPrice() +
                " <TOTAL_ASK_QTY>- " + ohlcTs.getTotalAskQty() +
                " <AVG_PRICE>- " + ohlcTs.getAvgPrice() +
                " <PREV_CLOSED>- " + ohlcTs.getPrevClosed() +
                " <UP_TICK_VOLUME>- " + ohlcTs.getUpTickVolume() +
                " <NEWS_PROVIDER>- " + ohlcTs.getNewsProvider() +
                " <CHANGE>- " + ohlcTs.getChange() +
                " <BESTASKPRICE>- " + ohlcTs.getBestAskPrice() +
                " <DYR>- " + ohlcTs.getDyr() +
                " <CONTRIBUTOR>- " + ohlcTs.getContributor() +
                " <IS_ANN>- " + ohlcTs.getIsAnn() +
                " <TURNOVER>- " + ohlcTs.getTurnover() +
                " <CF_OUT_COUNT>- " + ohlcTs.getCfOutCount() +
                " <ASK_VWAP>- " + ohlcTs.getAskVwap() +
                " <DOWN_TICK_VOLUME>- " + ohlcTs.getDownTickVolume() +
                " <SPLIT_ACT>- " + ohlcTs.getSplitAct() +
                " <FREE_FLOAT_MARKET_CAP>- " + ohlcTs.getFreeFloatMarketCap() +
                " <OPEN>- " + ohlcTs.getOpen() +
                " <BID_VWAP>- " + ohlcTs.getBidVwap() +
                " <PCT_CHANGE>- " + ohlcTs.getPctChange() +
                " <BEST_ASK_QTY>- " + ohlcTs.getBestAskQty() +
                " <PER>- " + ohlcTs.getPer() ;

    }

    private OHLC_TS mapToTimestream(OHLC ohlc) {
        OHLC_TS ohlcTs = new OHLC_TS();
        ohlcTs.setTimeStampValue(ohlc.time* 60*1000L);
        ohlcTs.setLastUpdatedOn(new Date(Instant.now().toEpochMilli()));
        ohlcTs.setId(ohlc.exchange + "~" + ohlc.symbol + "~" + Instant.parse(Instant.ofEpochSecond(ohlc.time * 60).toString())
                .atOffset(ZoneOffset.UTC)
                .format(
                        DateTimeFormatter.ofPattern("uuuu-MM-dd HH:mm")
                ));
        ohlcTs.setKey(ohlc.exchange + "~" + ohlc.symbol);
        ohlcTs.setSourceId(ohlc.exchange);
        ohlcTs.setTickerId(ohlc.symbol);
        ohlcTs.setTradeTime(new Date(Instant.ofEpochSecond(ohlc.time * 60).toEpochMilli()));
        System.currentTimeMillis();

        if (ohlc.getDecimalCorrectedOpen() > 0) {
            ohlcTs.setOpen(setPrecision(ohlc.getDecimalCorrectedOpen(), ohlc.decimalCorrFactor));
        } else {
            ohlcTs.setOpen(setPrecision(0, ohlc.decimalCorrFactor));
        }

        if (ohlc.getDecimalCorrectedHigh() > 0) {
            ohlcTs.setHigh(setPrecision(ohlc.getDecimalCorrectedHigh(), ohlc.decimalCorrFactor));
        } else {
            ohlcTs.setHigh(setPrecision(0, ohlc.decimalCorrFactor));
        }

        if (ohlc.getDecimalCorrectedLow() > 0) {
            ohlcTs.setLow(setPrecision(ohlc.getDecimalCorrectedLow(), ohlc.decimalCorrFactor));
        } else {
            ohlcTs.setLow(setPrecision(0, ohlc.decimalCorrFactor));
        }

        if (ohlc.getDecimalCorrectedClose() > 0) {
            ohlcTs.setClose(setPrecision(ohlc.getDecimalCorrectedClose(), ohlc.decimalCorrFactor));
        } else {
            ohlcTs.setClose(setPrecision(0, ohlc.decimalCorrFactor));
        }

        if (ohlc.volume > 0) {
            ohlcTs.setVolume(ohlc.volume);
        } else {
            ohlcTs.setVolume(0L);
        }

        if (ohlc.noOfTrades > 0) {
            ohlcTs.setNumberOfTrades(ohlc.noOfTrades);
        } else {
            ohlcTs.setNumberOfTrades(0);
        }

        if (ohlc.getDecimalCorrectedTurnover() > 0) {
            ohlcTs.setTurnover(setPrecision(ohlc.getDecimalCorrectedTurnover(), ohlc.decimalCorrFactor));
        } else {
            ohlcTs.setTurnover(setPrecision(0, ohlc.decimalCorrFactor));
        }

        if (ohlc.vwap > 0) {
            ohlcTs.setAvgPrice(setPrecision(ohlc.getDecimalCorrectedVwap(), ohlc.decimalCorrFactor));
        } else {
            ohlcTs.setAvgPrice(setPrecision(0, ohlc.decimalCorrFactor));
        }

        if (ohlc.cashInNoOfTrades > 0) {
            ohlcTs.setCfInCount(ohlc.cashInNoOfTrades);
        } else {
            ohlcTs.setCfInCount(0);
        }

        if (ohlc.cashInTurnover > 0) {
            ohlcTs.setCfInTurnover(setPrecision(ohlc.getDecimalCorrectedOHLCCashInTurnover(), ohlc.decimalCorrFactor));
        } else {
            ohlcTs.setCfInTurnover(setPrecision(0, ohlc.decimalCorrFactor));
        }

        if (ohlc.cashInVolume > 0) {
            ohlcTs.setCfInVolume(ohlc.cashInVolume);
        } else {
            ohlcTs.setCfInVolume(0L);
        }

        if (ohlc.cashOutNoOfTrades > 0) {
            ohlcTs.setCfOutCount(ohlc.cashOutNoOfTrades);
        } else {
            ohlcTs.setCfOutCount(0);
        }

        if (ohlc.cashOutTurnover > 0) {
            ohlcTs.setCfOutTurnover(setPrecision(ohlc.getDecimalCorrectedOHLCCashOutTurnover(), ohlc.decimalCorrFactor));
        } else {
            ohlcTs.setCfOutTurnover(setPrecision(0, ohlc.decimalCorrFactor));
        }

        if (ohlc.cashOutVolume > 0) {
            ohlcTs.setCfOutVolume(ohlc.cashOutVolume);
        } else {
            ohlcTs.setCfOutVolume(0L);
        }

        if (ohlc.bidQty > 0) {
            ohlcTs.setTotalBidQty(ohlc.bidQty);
        } else {
            ohlcTs.setTotalBidQty(0L);
        }

        if (ohlc.askQty > 0) {
            ohlcTs.setTotalAskQty(ohlc.askQty);
        } else {
            ohlcTs.setTotalAskQty(0L);
        }

        if (ohlc.bidVWAP > 0) {
            ohlcTs.setBidVwap(setPrecision(ohlc.getDecimalCorrectedBidVWAP(), ohlc.decimalCorrFactor));
        } else {
            ohlcTs.setBidVwap(setPrecision(0, ohlc.decimalCorrFactor));
        }

        if (ohlc.askVWAP > 0) {
            ohlcTs.setAskVwap(setPrecision(ohlc.getDecimalCorrectedAskVWAP(), ohlc.decimalCorrFactor));
        } else {
            ohlcTs.setAskVwap(setPrecision(0, ohlc.decimalCorrFactor));
        }

        if (ohlc.bestBid > 0) {
            ohlcTs.setBestBidPriceIdo(setPrecision(ohlc.getDecimalCorrectedBestBid(), ohlc.decimalCorrFactor));
        } else {
            ohlcTs.setBestBidPriceIdo(setPrecision(0, ohlc.decimalCorrFactor));
        }

        if (ohlc.bestAsk > 0) {
            ohlcTs.setBestAskPriceIdo(setPrecision(ohlc.getDecimalCorrectedBestAsk(), ohlc.decimalCorrFactor));
        } else {
            ohlcTs.setBestAskPriceIdo(setPrecision(0, ohlc.decimalCorrFactor));
        }

        if (ohlc.bestBidQty > 0) {
            ohlcTs.setBestBidQty(ohlc.bestBidQty);
        } else {
            ohlcTs.setBestBidQty(0L);
        }

        if (ohlc.bestAskQty > 0) {
            ohlcTs.setBestAskQty(ohlc.bestAskQty);
        } else {
            ohlcTs.setBestAskQty(0L);
        }

        if (ohlc.upTickVolume > 0) {
            ohlcTs.setUpTickVolume(ohlc.upTickVolume);
        } else {
            ohlcTs.setUpTickVolume(0L);
        }

        if (ohlc.downTickVolume > 0) {
            ohlcTs.setDownTickVolume(ohlc.downTickVolume);
        } else {
            ohlcTs.setDownTickVolume(0L);
        }

        if (ohlc.closeBestBid > 0) {
            ohlcTs.setClosingBestBid(setPrecision((double)(ohlc.closeBestBid / (float)ohlc.decimalCorrFactor), ohlc.decimalCorrFactor));
        } else {
            ohlcTs.setClosingBestBid(setPrecision(0, ohlc.decimalCorrFactor));
        }

        if (ohlc.closeBestAsk > 0) {
            ohlcTs.setClosingBestAsk(setPrecision((double)(ohlc.closeBestAsk / (float)ohlc.decimalCorrFactor), ohlc.decimalCorrFactor));
        } else {
            ohlcTs.setClosingBestAsk(setPrecision(0, ohlc.decimalCorrFactor));
        }

        ohlcTs.setChange(setPrecision(ohlc.change, ohlc.decimalCorrFactor));
        ohlcTs.setPctChange(setPrecision(ohlc.percentChange, ohlc.decimalCorrFactor));
        return ohlcTs;
    }

    private double setPrecision(double value, int scale) {
        if (value == Double.NEGATIVE_INFINITY || value == Double.POSITIVE_INFINITY) {
            value = 0.0;
        }
        int precision = ((Double) Math.log10(scale)).intValue() + 2;
        return Precision.round(value, precision);
    }

    public void flushData(LinkedList<Record> records, String table_name) {

        System.out.println(table_name);
        System.out.println("============================   going to send data ==============================");
        LinkedList copyOfDataForUpload;

        if (records.size() > 0){
            synchronized (records) {
                copyOfDataForUpload = (LinkedList)records.clone();
                records.clear();
            }
            try {
                Runnable flushManager = new FlushManager(this.serviceManager,this.writeClient,copyOfDataForUpload, table_name);
                threadPool.execute(flushManager);
            }catch (Exception e){
                System.out.println(e);
            }
        }
    }

    private void printStatistics() {
        if (System.currentTimeMillis() - lastStatusRecordTime > 2 * 1000 * 5) {
            lastStatusRecordTime = System.currentTimeMillis();

            generatedIntervalRecordCountMap.keySet().forEach(k -> {
                HashMap<String, Integer> intervalCountMap = generatedIntervalRecordCountMap.get(k);

                intervalCountMap.keySet().forEach(e -> {
                    System.out.println("<<>KafkaManager> " + e + " received " + intervalCountMap.get(e) + " " + k +  "min ohlc records from "
                            + applicationStartTime.toString()  + " to " +Instant.now().toString() + "\n");
                });
            });
        }
    }
}
