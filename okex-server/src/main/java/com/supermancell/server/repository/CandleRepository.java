package com.supermancell.server.repository;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.IndexOptions;
import com.mongodb.client.model.Indexes;
import com.supermancell.common.model.Candle;
import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Repository;

import javax.annotation.PostConstruct;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Repository
public class CandleRepository {

    private static final Logger log = LoggerFactory.getLogger(CandleRepository.class);
    private static final String COLLECTION_NAME = "candles";

    private final MongoTemplate mongoTemplate;

    public CandleRepository(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    @PostConstruct
    public void init() {
        createIndexes();
    }

    private void createIndexes() {
        try {
            MongoDatabase database = mongoTemplate.getDb();
            MongoCollection<Document> collection = database.getCollection(COLLECTION_NAME);

            // Compound index: symbol + interval + timestamp (unique)
            collection.createIndex(
                    Indexes.compoundIndex(
                            Indexes.ascending("symbol"),
                            Indexes.ascending("interval"),
                            Indexes.descending("timestamp")
                    ),
                    new IndexOptions().unique(true)
            );

            // Index on timestamp for time-based queries
            collection.createIndex(Indexes.descending("timestamp"));

            log.info("MongoDB indexes created for collection: {}", COLLECTION_NAME);
        } catch (Exception e) {
            log.error("Failed to create indexes", e);
        }
    }

    public void save(Candle candle) {
        try {
            Document doc = new Document()
                    .append("symbol", candle.getSymbol())
                    .append("timestamp", candle.getTimestamp())
                    .append("interval", candle.getInterval())
                    .append("open", candle.getOpen())
                    .append("high", candle.getHigh())
                    .append("low", candle.getLow())
                    .append("close", candle.getClose())
                    .append("volume", candle.getVolume())
                    .append("confirm", candle.getConfirm())
                    .append("created_at", candle.getCreatedAt() != null ? candle.getCreatedAt() : Instant.now());

            mongoTemplate.getCollection(COLLECTION_NAME).insertOne(doc);
            log.debug("Saved candle: {} {} at {}", candle.getSymbol(), candle.getInterval(), candle.getTimestamp());
        } catch (com.mongodb.MongoWriteException e) {
            // Ignore duplicate key errors (11000)
            if (e.getCode() == 11000) {
                log.trace("Duplicate candle ignored: {} {} at {}", 
                        candle.getSymbol(), candle.getInterval(), candle.getTimestamp());
            } else {
                log.error("Failed to save candle", e);
            }
        } catch (Exception e) {
            log.error("Failed to save candle", e);
        }
    }

    public void saveBatch(List<Candle> candles) {
        if (candles == null || candles.isEmpty()) {
            return;
        }

        List<Document> documents = new ArrayList<>(candles.size());
        for (Candle candle : candles) {
            Document doc = new Document()
                    .append("symbol", candle.getSymbol())
                    .append("timestamp", candle.getTimestamp())
                    .append("interval", candle.getInterval())
                    .append("open", candle.getOpen())
                    .append("high", candle.getHigh())
                    .append("low", candle.getLow())
                    .append("close", candle.getClose())
                    .append("volume", candle.getVolume())
                    .append("confirm", candle.getConfirm())
                    .append("created_at", candle.getCreatedAt() != null ? candle.getCreatedAt() : Instant.now());
            documents.add(doc);
        }

        try {
            mongoTemplate.getCollection(COLLECTION_NAME).insertMany(documents);
            log.info("Saved {} candles in batch", documents.size());
        } catch (com.mongodb.MongoBulkWriteException e) {
            log.warn("Batch insert completed with {} duplicates ignored", 
                    e.getWriteErrors().size());
        } catch (Exception e) {
            log.error("Failed to save candles in batch", e);
        }
    }

    public List<Candle> findCandles(String symbol, String interval, int limit) {
        List<Candle> candles = new ArrayList<>();
        try {
            Document filter = new Document()
                    .append("symbol", symbol)
                    .append("interval", interval);

            Document sort = new Document("timestamp", -1);

            mongoTemplate.getCollection(COLLECTION_NAME)
                    .find(filter)
                    .sort(sort)
                    .limit(limit)
                    .forEach(doc -> {
                        Candle candle = new Candle();
                        candle.setSymbol(doc.getString("symbol"));
                        candle.setTimestamp(doc.get("timestamp", Instant.class));
                        candle.setInterval(doc.getString("interval"));
                        candle.setOpen(doc.getDouble("open"));
                        candle.setHigh(doc.getDouble("high"));
                        candle.setLow(doc.getDouble("low"));
                        candle.setClose(doc.getDouble("close"));
                        candle.setVolume(doc.getDouble("volume"));
                        candle.setConfirm(doc.getString("confirm"));
                        candle.setCreatedAt(doc.get("created_at", Instant.class));
                        candles.add(candle);
                    });

            // Reverse to get chronological order (oldest first)
            java.util.Collections.reverse(candles);

        } catch (Exception e) {
            log.error("Failed to query candles", e);
        }
        return candles;
    }
}
