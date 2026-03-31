package com.recommender;

import org.apache.mahout.cf.taste.common.TasteException;
import org.apache.mahout.cf.taste.impl.neighborhood.NearestNUserNeighborhood;
import org.apache.mahout.cf.taste.impl.recommender.GenericItemBasedRecommender;
import org.apache.mahout.cf.taste.impl.recommender.GenericUserBasedRecommender;
import org.apache.mahout.cf.taste.impl.similarity.PearsonCorrelationSimilarity;
import org.apache.mahout.cf.taste.impl.similarity.TanimotoCoefficientSimilarity;
import org.apache.mahout.cf.taste.model.DataModel;
import org.apache.mahout.cf.taste.neighborhood.UserNeighborhood;
import org.apache.mahout.cf.taste.recommender.RecommendedItem;
import org.apache.mahout.cf.taste.recommender.Recommender;
import org.apache.mahout.cf.taste.similarity.ItemSimilarity;
import org.apache.mahout.cf.taste.similarity.UserSimilarity;

import java.util.List;

/**
 * Wraps Mahout recommenders: User-Based and Item-Based collaborative filtering.
 */
public class RecommendationEngine {

    private final DataModel dataModel;
    private final Recommender userBasedRecommender;
    private final Recommender itemBasedRecommender;

    /**
     * Initializes both recommenders using the provided DataModel.
     *
     * User-Based CF: finds similar users (via Pearson correlation) and recommends
     * items those neighbors liked.
     *
     * Item-Based CF: finds similar items (via Tanimoto coefficient) and recommends
     * items similar to what the user already rated highly.
     */
    public RecommendationEngine(DataModel dataModel) throws TasteException {
        this.dataModel = dataModel;

        // --- User-Based Recommender ---
        UserSimilarity userSimilarity = new PearsonCorrelationSimilarity(dataModel);
        // Consider the 3 most similar users as neighbors
        UserNeighborhood neighborhood = new NearestNUserNeighborhood(3, userSimilarity, dataModel);
        this.userBasedRecommender = new GenericUserBasedRecommender(dataModel, neighborhood, userSimilarity);

        // --- Item-Based Recommender ---
        ItemSimilarity itemSimilarity = new TanimotoCoefficientSimilarity(dataModel);
        this.itemBasedRecommender = new GenericItemBasedRecommender(dataModel, itemSimilarity);
    }

    /**
     * Returns top-N product recommendations for a user using User-Based CF.
     */
    public List<RecommendedItem> recommendUserBased(long userId, int topN) throws TasteException {
        return userBasedRecommender.recommend(userId, topN);
    }

    /**
     * Returns top-N product recommendations for a user using Item-Based CF.
     */
    public List<RecommendedItem> recommendItemBased(long userId, int topN) throws TasteException {
        return itemBasedRecommender.recommend(userId, topN);
    }

    /**
     * Returns the estimated preference score a user would give an item.
     */
    public float estimatePreference(long userId, long itemId) throws TasteException {
        return userBasedRecommender.estimatePreference(userId, itemId);
    }

    public DataModel getDataModel() {
        return dataModel;
    }
}
