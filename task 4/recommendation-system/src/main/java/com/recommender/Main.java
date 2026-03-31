package com.recommender;

import org.apache.mahout.cf.taste.common.TasteException;
import org.apache.mahout.cf.taste.model.DataModel;
import org.apache.mahout.cf.taste.recommender.RecommendedItem;

import java.io.IOException;
import java.util.List;

/**
 * Entry point for the Product Recommendation System.
 *
 * Demonstrates:
 *  1. Loading user-item ratings from CSV
 *  2. User-Based Collaborative Filtering recommendations
 *  3. Item-Based Collaborative Filtering recommendations
 *  4. Preference estimation for a specific user-item pair
 */
public class Main {

    private static final int TOP_N = 3; // number of recommendations to show

    public static void main(String[] args) {
        System.out.println("==============================================");
        System.out.println("   Product Recommendation System (Mahout)    ");
        System.out.println("==============================================\n");

        try {
            // Load ratings data
            DataModel dataModel = DataLoader.loadFromResource("/ratings.csv");
            System.out.printf("Loaded data: %d users, %d items%n%n",
                dataModel.getNumUsers(), dataModel.getNumItems());

            RecommendationEngine engine = new RecommendationEngine(dataModel);

            // Run recommendations for a few sample users
            long[] sampleUsers = {1L, 3L, 6L};

            for (long userId : sampleUsers) {
                System.out.println("----------------------------------------------");
                System.out.printf("Recommendations for User %d%n", userId);
                System.out.println("----------------------------------------------");

                // Show what the user has already rated
                System.out.println("Already rated:");
                dataModel.getItemIDsFromUser(userId).forEach(itemId ->
                    System.out.printf("  %-15s (item %d)%n",
                        DataLoader.getProductName(itemId), itemId)
                );

                // User-Based CF
                System.out.printf("%nUser-Based CF (top %d):%n", TOP_N);
                List<RecommendedItem> userRecs = engine.recommendUserBased(userId, TOP_N);
                printRecommendations(userRecs);

                // Item-Based CF
                System.out.printf("%nItem-Based CF (top %d):%n", TOP_N);
                List<RecommendedItem> itemRecs = engine.recommendItemBased(userId, TOP_N);
                printRecommendations(itemRecs);

                System.out.println();
            }

            // Demonstrate preference estimation
            System.out.println("==============================================");
            System.out.println("Preference Estimation Examples");
            System.out.println("==============================================");
            estimateAndPrint(engine, 1L, 104L); // User 1 -> Tablet
            estimateAndPrint(engine, 3L, 101L); // User 3 -> Laptop
            estimateAndPrint(engine, 6L, 103L); // User 6 -> Headphones

        } catch (IOException e) {
            System.err.println("Failed to load ratings data: " + e.getMessage());
        } catch (TasteException e) {
            System.err.println("Recommendation error: " + e.getMessage());
        }
    }

    private static void printRecommendations(List<RecommendedItem> recs) {
        if (recs.isEmpty()) {
            System.out.println("  No new recommendations (user has rated everything)");
        } else {
            for (int i = 0; i < recs.size(); i++) {
                RecommendedItem rec = recs.get(i);
                System.out.printf("  %d. %-15s (item %d) — score: %.3f%n",
                    i + 1,
                    DataLoader.getProductName(rec.getItemID()),
                    rec.getItemID(),
                    rec.getValue());
            }
        }
    }

    private static void estimateAndPrint(RecommendationEngine engine, long userId, long itemId)
            throws TasteException {
        float score = engine.estimatePreference(userId, itemId);
        System.out.printf("  User %d → %-15s : estimated rating = %.3f%n",
            userId, DataLoader.getProductName(itemId), score);
    }
}
