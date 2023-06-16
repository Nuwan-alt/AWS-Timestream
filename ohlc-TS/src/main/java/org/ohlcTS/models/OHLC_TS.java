package org.ohlcTS.models;

import java.util.Date;
import com.fasterxml.jackson.annotation.JsonProperty;

public class OHLC_TS {

    @JsonProperty("ID")
    private String id;

    @JsonProperty("KEY")
    private String key;

    @JsonProperty("TICKER_SERIAL")
    private Long tickerSerial;

    @JsonProperty("TICKER_ID")
    private String tickerId;

    @JsonProperty("SOURCE_ID")
    private String sourceId;

    @JsonProperty("INSTRUMENT_TYPE_ID")
    private Long instrumentTypeId;

    @JsonProperty("TRANSACTION_DATE")
    private Date transactionDate;

    @JsonProperty("TRADE_TIME")
    private Date tradeTime;

    @JsonProperty("OPEN")
    private Double open;

    @JsonProperty("HIGH")
    private Double high;

    @JsonProperty("LOW")
    private Double low;

    @JsonProperty("CLOSE")
    private Double close;

    @JsonProperty("VOLUME")
    private Long volume;

    @JsonProperty("NUMBER_OF_TRADES")
    private Integer numberOfTrades;

    @JsonProperty("TURNOVER")
    private Double turnover;

    @JsonProperty("AVG_PRICE")
    private Double avgPrice;

    @JsonProperty("CHANGE")
    private Double change;

    @JsonProperty("PCT_CHANGE")
    private Double pctChange;

    @JsonProperty("PREV_CLOSED")
    private Double prevClosed;

    @JsonProperty("CF_IN_COUNT")
    private Integer cfInCount;

    @JsonProperty("CF_IN_VOLUME")
    private Long cfInVolume;

    @JsonProperty("CF_IN_TURNOVER")
    private Double cfInTurnover;

    @JsonProperty("CF_OUT_COUNT")
    private Integer cfOutCount;

    @JsonProperty("CF_OUT_VOLUME")
    private Long cfOutVolume;

    @JsonProperty("CF_OUT_TURNOVER")
    private Double cfOutTurnover;

    @JsonProperty("LAST_UPDATED_ON")
    private Date lastUpdatedOn;

    @JsonProperty("SNAPSHOT_TYPE")
    private String snapshotType;

    @JsonProperty("CONTRIBUTOR")
    private String contributor;

    @JsonProperty("ADJUSTED_DATE")
    private Date adjustedDate;

    @JsonProperty("MARKET_CAP")
    private Double marketCap;

    @JsonProperty("FREE_FLOAT_MARKET_CAP")
    private Double freeFloatMarketCap;

    @JsonProperty("IS_ANN")
    private Long isAnn;

    @JsonProperty("SPLIT_ACT")
    private String splitAct;

    @JsonProperty("NEWS_PROVIDER")
    private String newsProvider;

    @JsonProperty("LASTTRADEPRICE")
    private Double lastTradePrice;

    @JsonProperty("BESTBIDPRICE")
    private Double bestBidPrice;

    @JsonProperty("BESTASKPRICE")
    private Double bestAskPrice;

    @JsonProperty("TOTAL_STOCKS")
    private Long totalStocks;

    @JsonProperty("TOTAL_BID_QTY")
    private Long totalBidQty;

    @JsonProperty("TOTAL_ASK_QTY")
    private Long totalAskQty;

    @JsonProperty("INCREMENT_ID")
    private Double incrementId;

    @JsonProperty("BEST_BID_QTY")
    private Long bestBidQty;

    @JsonProperty("BEST_ASK_QTY")
    private Long bestAskQty;

    @JsonProperty("VWAP")
    private Double vwap;

    @JsonProperty("DOWN_TICK_VOLUME")
    private Long downTickVolume;

    @JsonProperty("UP_TICK_VOLUME")
    private Long upTickVolume;

    @JsonProperty("BEST_BID_PRICE")
    private Double bestBidPriceIdo;

    @JsonProperty("BEST_ASK_PRICE")
    private Double bestAskPriceIdo;

    @JsonProperty("BID_VWAP")
    private Double bidVwap;

    @JsonProperty("ASK_VWAP")
    private Double askVwap;

    @JsonProperty("SEQUENCE_NUMBER")
    private Long sequenceNumber;

    @JsonProperty("PER")
    private Double per;

    @JsonProperty("PBR")
    private Double pbr;

    @JsonProperty("PSR")
    private Double psr;

    @JsonProperty("PCR")
    private Double pcr;

    @JsonProperty("DYR")
    private Double dyr;

    @JsonProperty("DIVIDEND_AMOUNT")
    private Double dividendAmount;

    @JsonProperty("CLOSING_BEST_BID")
    private Double closingBestBid;

    @JsonProperty("CLOSING_BEST_ASK")
    private Double closingBestAsk;

    private long timeStampValue;

    public long getTimeStampValue() {
        return timeStampValue;
    }

    public void setTimeStampValue(long timeStampValue) {
        this.timeStampValue = timeStampValue;
    }

    public OHLC_TS(String id, String key, Long tickerSerial, String tickerId, String sourceId, Long instrumentTypeId, Date transactionDate, Date tradeTime, Double open, Double high, Double low, Double close, Long volume, Integer numberOfTrades, Double turnover, Double avgPrice, Double change, Double pctChange, Double prevClosed, Integer cfInCount, Long cfInVolume, Double cfInTurnover, Integer cfOutCount, Long cfOutVolume, Double cfOutTurnover, Date lastUpdatedOn, String snapshotType, String contributor, Date adjustedDate, Double marketCap, Double freeFloatMarketCap, Long isAnn, String splitAct, String newsProvider, Double lastTradePrice, Double bestBidPrice, Double bestAskPrice, Long totalStocks, Long totalBidQty, Long totalAskQty, Double incrementId, Long bestBidQty, Long bestAskQty, Double vwap, Long downTickVolume, Long upTickVolume, Double bestBidPriceIdo, Double bestAskPriceIdo, Double bidVwap, Double askVwap, Long sequenceNumber, Double per, Double pbr, Double psr, Double pcr, Double dyr, Double dividendAmount, Double closingBestBid, Double closingBestAsk) {
        this.id = id;
        this.key = key;
        this.tickerSerial = tickerSerial;
        this.tickerId = tickerId;
        this.sourceId = sourceId;
        this.instrumentTypeId = instrumentTypeId;
        this.transactionDate = transactionDate;
        this.tradeTime = tradeTime;
        this.open = open;
        this.high = high;
        this.low = low;
        this.close = close;
        this.volume = volume;
        this.numberOfTrades = numberOfTrades;
        this.turnover = turnover;
        this.avgPrice = avgPrice;
        this.change = change;
        this.pctChange = pctChange;
        this.prevClosed = prevClosed;
        this.cfInCount = cfInCount;
        this.cfInVolume = cfInVolume;
        this.cfInTurnover = cfInTurnover;
        this.cfOutCount = cfOutCount;
        this.cfOutVolume = cfOutVolume;
        this.cfOutTurnover = cfOutTurnover;
        this.lastUpdatedOn = lastUpdatedOn;
        this.snapshotType = snapshotType;
        this.contributor = contributor;
        this.adjustedDate = adjustedDate;
        this.marketCap = marketCap;
        this.freeFloatMarketCap = freeFloatMarketCap;
        this.isAnn = isAnn;
        this.splitAct = splitAct;
        this.newsProvider = newsProvider;
        this.lastTradePrice = lastTradePrice;
        this.bestBidPrice = bestBidPrice;
        this.bestAskPrice = bestAskPrice;
        this.totalStocks = totalStocks;
        this.totalBidQty = totalBidQty;
        this.totalAskQty = totalAskQty;
        this.incrementId = incrementId;
        this.bestBidQty = bestBidQty;
        this.bestAskQty = bestAskQty;
        this.vwap = vwap;
        this.downTickVolume = downTickVolume;
        this.upTickVolume = upTickVolume;
        this.bestBidPriceIdo = bestBidPriceIdo;
        this.bestAskPriceIdo = bestAskPriceIdo;
        this.bidVwap = bidVwap;
        this.askVwap = askVwap;
        this.sequenceNumber = sequenceNumber;
        this.per = per;
        this.pbr = pbr;
        this.psr = psr;
        this.pcr = pcr;
        this.dyr = dyr;
        this.dividendAmount = dividendAmount;
        this.closingBestBid = closingBestBid;
        this.closingBestAsk = closingBestAsk;
    }

    public OHLC_TS() {

    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public Long getTickerSerial() {
        return (tickerSerial == null)?-1:tickerSerial;
    }

    public void setTickerSerial(Long tickerSerial) {
        this.tickerSerial = tickerSerial;
    }

    public String getTickerId() {
        return tickerId;
    }

    public void setTickerId(String tickerId) {
        this.tickerId = tickerId;
    }

    public String getSourceId() {
        return (sourceId==null) ? "" :sourceId ;
    }

    public void setSourceId(String sourceId) {
        this.sourceId = sourceId;
    }

    public Long getInstrumentTypeId() {
        return (instrumentTypeId == null)?-1:instrumentTypeId;
    }

    public void setInstrumentTypeId(Long instrumentTypeId) {
        this.instrumentTypeId = instrumentTypeId;
    }

    public Date getTransactionDate() {
        return transactionDate;
    }

    public void setTransactionDate(Date transactionDate) {
        this.transactionDate = transactionDate;
    }

    public Date getTradeTime() {
        return tradeTime;
    }

    public void setTradeTime(Date tradeTime) {
        this.tradeTime = tradeTime;
    }

    public Double getOpen() {
        return (open==null)?0.0:open;
    }

    public void setOpen(Double open) {
        this.open = open;
    }

    public Double getHigh() {
        return (high==null)?0.0:high;
    }

    public void setHigh(Double high) {
        this.high = high;
    }

    public Double getLow() {
        return (low==null)?0.0:low;
    }

    public void setLow(Double low) {
        this.low = low;
    }

    public Double getClose() {
        return (close==null)?0.0:close;
    }

    public void setClose(Double close) {
        this.close = close;
    }

    public Long getVolume() {
        return volume;
    }

    public void setVolume(Long volume) {
        this.volume = volume;
    }

    public Integer getNumberOfTrades() {
        return numberOfTrades;
    }

    public void setNumberOfTrades(Integer numberOfTrades) {
        this.numberOfTrades = numberOfTrades;
    }

    public Double getTurnover() {
        return (turnover==null)?0.0:turnover;
    }

    public void setTurnover(Double turnover) {
        this.turnover = turnover;
    }

    public Double getAvgPrice() {
        return (avgPrice==null)?0.0:avgPrice;
    }

    public void setAvgPrice(Double avgPrice) {
        this.avgPrice = avgPrice;
    }

    public Double getChange() {
        return (change==null)?0.0:change;
    }

    public void setChange(Double change) {
        this.change = change;
    }

    public Double getPctChange() {
        return (pctChange==null)?0.0:pctChange;
    }

    public void setPctChange(Double pctChange) {
        this.pctChange = pctChange;
    }

    public Double getPrevClosed() {
        return (prevClosed==null)?0.0:prevClosed;
    }

    public void setPrevClosed(Double prevClosed) {
        this.prevClosed = prevClosed;
    }

    public Integer getCfInCount() {
        return (cfInCount==null)?-1:cfInCount;
    }

    public void setCfInCount(Integer cfInCount) {
        this.cfInCount = cfInCount;
    }

    public Long getCfInVolume() {
        return (cfInVolume == null)?-1:cfInVolume;
    }

    public void setCfInVolume(Long cfInVolume) {
        this.cfInVolume = cfInVolume;
    }

    public Double getCfInTurnover() {
        return (cfInTurnover==null)?0.0:cfInTurnover;
    }

    public void setCfInTurnover(Double cfInTurnover) {
        this.cfInTurnover = cfInTurnover;
    }

    public Integer getCfOutCount() {
        return (cfOutCount==null)?-1:cfOutCount;
    }

    public void setCfOutCount(Integer cfOutCount) {
        this.cfOutCount = cfOutCount;
    }

    public Long getCfOutVolume() {
        return (cfOutVolume==null)?-1:cfOutVolume;
    }

    public void setCfOutVolume(Long cfOutVolume) {
        this.cfOutVolume = cfOutVolume;
    }

    public Double getCfOutTurnover() {
        return (cfOutTurnover==null)?0.0:cfOutTurnover;
    }

    public void setCfOutTurnover(Double cfOutTurnover) {
        this.cfOutTurnover = cfOutTurnover;
    }

    public Date getLastUpdatedOn() {
        return lastUpdatedOn;
    }

    public void setLastUpdatedOn(Date lastUpdatedOn) {
        this.lastUpdatedOn = lastUpdatedOn;
    }

    public String getSnapshotType() {
        return snapshotType;
    }

    public void setSnapshotType(String snapshotType) {
        this.snapshotType = snapshotType;
    }

    public String getContributor() {
        return contributor;
    }

    public void setContributor(String contributor) {
        this.contributor = contributor;
    }

    public Date getAdjustedDate() {
        return adjustedDate;
    }

    public void setAdjustedDate(Date adjustedDate) {
        this.adjustedDate = adjustedDate;
    }

    public Double getMarketCap() {
        return (marketCap==null)?0.0:marketCap;
    }

    public void setMarketCap(Double marketCap) {
        this.marketCap = marketCap;
    }

    public Double getFreeFloatMarketCap() {
        return (freeFloatMarketCap==null)?0.0:freeFloatMarketCap;
    }

    public void setFreeFloatMarketCap(Double freeFloatMarketCap) {
        this.freeFloatMarketCap = freeFloatMarketCap;
    }

    public Long getIsAnn() {
        return (isAnn==null)?-1:isAnn;
    }

    public void setIsAnn(Long isAnn) {
        this.isAnn = isAnn;
    }

    public String getSplitAct() {
        return splitAct;
    }

    public void setSplitAct(String splitAct) {
        this.splitAct = splitAct;
    }

    public String getNewsProvider() {
        return newsProvider;
    }

    public void setNewsProvider(String newsProvider) {
        this.newsProvider = newsProvider;
    }

    public Double getLastTradePrice() {
        return (lastTradePrice==null)?0.0:lastTradePrice;
    }

    public void setLastTradePrice(Double lastTradePrice) {
        this.lastTradePrice = lastTradePrice;
    }

    public Double getBestBidPrice() {
        return (bestBidPrice==null)?0.0:bestBidPrice;
    }

    public void setBestBidPrice(Double bestBidPrice) {
        this.bestBidPrice = bestBidPrice;
    }

    public Double getBestAskPrice() {
        return (bestAskPrice==null)?0.0:bestAskPrice;
    }

    public void setBestAskPrice(Double bestAskPrice) {
        this.bestAskPrice = bestAskPrice;
    }

    public Long getTotalBidQty() {
        return (totalBidQty==null)?-1:totalBidQty;
    }

    public void setTotalBidQty(Long totalBidQty) {
        this.totalBidQty = totalBidQty;
    }

    public Long getTotalAskQty() {
        return (totalAskQty==null)?-1:totalAskQty;
    }

    public void setTotalAskQty(Long totalAskQty) {
        this.totalAskQty = totalAskQty;
    }

    public Double getIncrementId() {
        return (incrementId==null)?0.0:incrementId;
    }

    public void setIncrementId(Double incrementId) {
        this.incrementId = incrementId;
    }

    public Long getBestBidQty() {
        return (bestBidQty==null)?-1:bestBidQty;
    }

    public void setBestBidQty(Long bestBidQty) {
        this.bestBidQty = bestBidQty;
    }

    public Long getBestAskQty() {
        return (bestAskQty==null)?-1:bestAskQty;
    }

    public void setBestAskQty(Long bestAskQty) {
        this.bestAskQty = bestAskQty;
    }

    public Double getVwap() {
        return (vwap==null)?0.0:vwap;
    }

    public void setVwap(Double vwap) {
        this.vwap = vwap;
    }

    public Long getDownTickVolume() {
        return (downTickVolume==null?-1:downTickVolume);
    }

    public void setDownTickVolume(Long downTickVolume) {
        this.downTickVolume = downTickVolume;
    }

    public Long getUpTickVolume() {
        return (upTickVolume==null?-1:upTickVolume);
    }

    public void setUpTickVolume(Long upTickVolume) {
        this.upTickVolume = upTickVolume;
    }

    public Double getBestBidPriceIdo() {
        return (bestBidPriceIdo==null)?0.0:bestBidPriceIdo;
    }

    public void setBestBidPriceIdo(Double bestBidPriceIdo) {
        this.bestBidPriceIdo = bestBidPriceIdo;
    }

    public Double getBestAskPriceIdo() {
        return (bestAskPriceIdo==null)?0.0:bestAskPriceIdo;
    }

    public void setBestAskPriceIdo(Double bestAskPriceIdo) {
        this.bestAskPriceIdo = bestAskPriceIdo;
    }

    public Double getBidVwap() {
        return (bidVwap==null)?0.0:bidVwap;
    }

    public void setBidVwap(Double bidVwap) {
        this.bidVwap = bidVwap;
    }

    public Double getAskVwap() {
        return (askVwap==null)?0.0:askVwap;
    }

    public void setAskVwap(Double askVwap) {
        this.askVwap = askVwap;
    }

    public Long getSequenceNumber() {
        return (sequenceNumber==null)?-1:sequenceNumber;
    }

    public void setSequenceNumber(Long sequenceNumber) {
        this.sequenceNumber = sequenceNumber;
    }

    public Double getPer() {
        return (per==null)?0.0:per;
    }

    public void setPer(Double per) {
        this.per = per;
    }

    public Double getPbr() {
        return (pbr==null)?0.0:pbr;
    }

    public void setPbr(Double pbr) {
        this.pbr = pbr;
    }

    public Double getPsr() {
        return (psr==null)?0.0:psr;
    }

    public void setPsr(Double psr) {
        this.psr = psr;
    }

    public Double getPcr() {
        return (pcr==null)?0.0:pcr;
    }

    public void setPcr(Double pcr) {
        this.pcr = pcr;
    }

    public Double getDyr() {
        return (dyr==null)?0.0:dyr;
    }

    public void setDyr(Double dyr) {
        this.dyr = dyr;
    }

    public Double getDividendAmount() {
        return (dividendAmount==null)?0.0:dividendAmount;
    }

    public void setDividendAmount(Double dividendAmount) {
        this.dividendAmount = dividendAmount;
    }

    public Double getClosingBestBid() {
        return (closingBestBid==null)?0.0:closingBestBid;
    }

    public void setClosingBestBid(Double closingBestBid) {
        this.closingBestBid = closingBestBid;
    }

    public Double getClosingBestAsk() {
        return (closingBestAsk==null)?0.0:closingBestAsk;
    }

    public void setClosingBestAsk(Double closingBestAsk) {
        this.closingBestAsk = closingBestAsk;
    }

    public Long getTotalStocks() {
        return (totalStocks==null)?-1:totalStocks;
    }

    public void setTotalStocks(Long totalStocks) {
        this.totalStocks = totalStocks;
    }
}


