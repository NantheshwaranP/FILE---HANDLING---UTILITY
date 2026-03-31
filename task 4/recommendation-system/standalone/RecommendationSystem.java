import java.util.*;
import java.util.stream.*;

/**
 * Standalone Product Recommendation System
 * Implements User-Based and Item-Based Collaborative Filtering
 * (same algorithms as Apache Mahout's GenericUserBasedRecommender
 *  and GenericItemBasedRecommender)
 *
 * No external dependencies — compile and run with plain javac/java.
 *
 * Compile: javac RecommendationSystem.java
 * Run:     java RecommendationSystem
 */
public class RecommendationSystem {

    // ---------------------------------------------------------------
    // Sample Data: userID -> { itemID -> rating }
    // Items: 101=Laptop 102=Phone 103=Headphones 104=Tablet 105=Smartwatch
    //        106=Camera 107=Speaker 108=Monitor 109=Keyboard 110=Mouse
    // ---------------------------------------------------------------
    private static final Map<Integer, Map<Integer, Double>> RATINGS = new LinkedHashMap<>();
    private static final Map<Integer, String> PRODUCTS = new LinkedHashMap<>();

    static {
        PRODUCTS.put(101, "Laptop");
        PRODUCTS.put(102, "Phone");
        PRODUCTS.put(103, "Headphones");
        PRODUCTS.put(104, "Tablet");
        PRODUCTS.put(105, "Smartwatch");
        PRODUCTS.put(106, "Camera");
        PRODUCTS.put(107, "Speaker");
        PRODUCTS.put(108, "Monitor");
        PRODUCTS.put(109, "Keyboard");
        PRODUCTS.put(110, "Mouse");

        addRatings(1, new int[]{101,102,103,105,107}, new double[]{5,3,4,4,2});
        addRatings(2, new int[]{101,102,104,106,108}, new double[]{4,5,3,4,3});
        addRatings(3, new int[]{103,105,107,109,110}, new double[]{5,4,5,3,4});
        addRatings(4, new int[]{101,102,103,104,106,108}, new double[]{3,4,2,5,4,5});
        addRatings(5, new int[]{102,103,105,107,109,110}, new double[]{5,4,3,4,5,4});
        addRatings(6, new int[]{101,104,106,108,110}, new double[]{4,3,5,4,3});
        addRatings(7, new int[]{101,103,105,107,109}, new double[]{5,4,5,3,4});
        addRatings(8, new int[]{102,104,106,108,110}, new double[]{3,4,3,5,4});
    }

    private static void addRatings(int userId, int[] items, double[] ratings) {
        Map<Integer, Double> userRatings = new LinkedHashMap<>();
        for (int i = 0; i < items.length; i++) {
            userRatings.put(items[i], ratings[i]);
        }
        RATINGS.put(userId, userRatings);
    }

    // ---------------------------------------------------------------
    // Pearson Correlation Similarity between two users
    // Range: [-1, 1] — higher means more similar taste
    // ---------------------------------------------------------------
    static double pearsonSimilarity(int userA, int userB) {
        Map<Integer, Double> ratingsA = RATINGS.get(userA);
        Map<Integer, Double> ratingsB = RATINGS.get(userB);

        // Find items rated by both users
        Set<Integer> common = new HashSet<>(ratingsA.keySet());
        common.retainAll(ratingsB.keySet());

        int n = common.size();
        if (n == 0) return 0.0;

        double sumA = 0, sumB = 0, sumA2 = 0, sumB2 = 0, sumAB = 0;
        for (int item : common) {
            double a = ratingsA.get(item);
            double b = ratingsB.get(item);
            sumA  += a;
            sumB  += b;
            sumA2 += a * a;
            sumB2 += b * b;
            sumAB += a * b;
        }

        double numerator   = sumAB - (sumA * sumB / n);
        double denominator = Math.sqrt((sumA2 - sumA * sumA / n) * (sumB2 - sumB * sumB / n));

        return denominator == 0 ? 0.0 : numerator / denominator;
    }

    // ---------------------------------------------------------------
    // Cosine Similarity between two items (based on user ratings)
    // Range: [0, 1] — higher means more similar items
    // ---------------------------------------------------------------
    static double cosineSimilarity(int itemA, int itemB) {
        // Build item -> user rating vectors
        List<Double> vecA = new ArrayList<>();
        List<Double> vecB = new ArrayList<>();

        for (Map<Integer, Double> userRatings : RATINGS.values()) {
            boolean hasA = userRatings.containsKey(itemA);
            boolean hasB = userRatings.containsKey(itemB);
            if (hasA || hasB) {
                vecA.add(hasA ? userRatings.get(itemA) : 0.0);
                vecB.add(hasB ? userRatings.get(itemB) : 0.0);
            }
        }

        double dot = 0, normA = 0, normB = 0;
        for (int i = 0; i < vecA.size(); i++) {
            dot   += vecA.get(i) * vecB.get(i);
            normA += vecA.get(i) * vecA.get(i);
            normB += vecB.get(i) * vecB.get(i);
        }

        double denom = Math.sqrt(normA) * Math.sqrt(normB);
        return denom == 0 ? 0.0 : dot / denom;
    }

    // ---------------------------------------------------------------
    // User-Based CF: find K nearest neighbors, predict ratings
    // ---------------------------------------------------------------
    static List<int[]> userBasedRecommend(int targetUser, int topN, int neighborCount) {
        Map<Integer, Double> targetRatings = RATINGS.get(targetUser);

        // Compute similarity to all other users
        List<int[]> neighbors = new ArrayList<>();
        for (int user : RATINGS.keySet()) {
            if (user == targetUser) continue;
            double sim = pearsonSimilarity(targetUser, user);
            if (sim > 0) neighbors.add(new int[]{user, (int)(sim * 10000)});
        }
        // Sort by similarity descending, take top K
        neighbors.sort((a, b) -> b[1] - a[1]);
        List<int[]> topNeighbors = neighbors.subList(0, Math.min(neighborCount, neighbors.size()));

        // Collect candidate items (not yet rated by target user)
        Set<Integer> candidates = new HashSet<>();
        for (int[] neighbor : topNeighbors) {
            candidates.addAll(RATINGS.get(neighbor[0]).keySet());
        }
        candidates.removeAll(targetRatings.keySet());

        // Predict score for each candidate using weighted average
        Map<Integer, Double> scores = new HashMap<>();
        for (int item : candidates) {
            double weightedSum = 0, simSum = 0;
            for (int[] neighbor : topNeighbors) {
                int neighborId = neighbor[0];
                double sim = neighbor[1] / 10000.0;
                Map<Integer, Double> neighborRatings = RATINGS.get(neighborId);
                if (neighborRatings.containsKey(item)) {
                    weightedSum += sim * neighborRatings.get(item);
                    simSum += sim;
                }
            }
            if (simSum > 0) scores.put(item, weightedSum / simSum);
        }

        // Sort by predicted score, return top N
        return scores.entrySet().stream()
            .sorted((a, b) -> Double.compare(b.getValue(), a.getValue()))
            .limit(topN)
            .map(e -> new int[]{e.getKey(), (int)(e.getValue() * 1000)})
            .collect(Collectors.toList());
    }

    // ---------------------------------------------------------------
    // Item-Based CF: find similar items to what user liked, recommend
    // ---------------------------------------------------------------
    static List<int[]> itemBasedRecommend(int targetUser, int topN) {
        Map<Integer, Double> targetRatings = RATINGS.get(targetUser);
        Set<Integer> allItems = PRODUCTS.keySet();

        // Candidate items not yet rated
        Set<Integer> candidates = new HashSet<>(allItems);
        candidates.removeAll(targetRatings.keySet());

        // Score each candidate by similarity to user's rated items (weighted by rating)
        Map<Integer, Double> scores = new HashMap<>();
        for (int candidate : candidates) {
            double weightedSim = 0, totalWeight = 0;
            for (Map.Entry<Integer, Double> rated : targetRatings.entrySet()) {
                double sim = cosineSimilarity(candidate, rated.getKey());
                double rating = rated.getValue();
                weightedSim += sim * rating;
                totalWeight += rating;
            }
            if (totalWeight > 0) scores.put(candidate, weightedSim / totalWeight);
        }

        return scores.entrySet().stream()
            .sorted((a, b) -> Double.compare(b.getValue(), a.getValue()))
            .limit(topN)
            .map(e -> new int[]{e.getKey(), (int)(e.getValue() * 1000)})
            .collect(Collectors.toList());
    }

    // ---------------------------------------------------------------
    // Main: run demo for sample users
    // ---------------------------------------------------------------
    public static void main(String[] args) {
        System.out.println("==============================================");
        System.out.println("   Product Recommendation System             ");
        System.out.println("   (Collaborative Filtering — no deps)       ");
        System.out.println("==============================================\n");

        int[] sampleUsers = {1, 3, 6};
        int TOP_N = 3;
        int NEIGHBORS = 3;

        for (int userId : sampleUsers) {
            System.out.println("----------------------------------------------");
            System.out.printf(" User %d%n", userId);
            System.out.println("----------------------------------------------");

            System.out.println("Already rated:");
            RATINGS.get(userId).forEach((item, rating) ->
                System.out.printf("  %-15s %.1f/5%n", PRODUCTS.get(item), rating)
            );

            System.out.printf("%nUser-Based CF (top %d, %d neighbors):%n", TOP_N, NEIGHBORS);
            List<int[]> userRecs = userBasedRecommend(userId, TOP_N, NEIGHBORS);
            printRecs(userRecs);

            System.out.printf("%nItem-Based CF (top %d):%n", TOP_N);
            List<int[]> itemRecs = itemBasedRecommend(userId, TOP_N);
            printRecs(itemRecs);

            System.out.println();
        }

        // Similarity matrix between all users
        System.out.println("==============================================");
        System.out.println(" User Similarity Matrix (Pearson Correlation)");
        System.out.println("==============================================");
        System.out.printf("%-8s", "");
        for (int u : RATINGS.keySet()) System.out.printf("User%-4d", u);
        System.out.println();
        for (int a : RATINGS.keySet()) {
            System.out.printf("User %-3d", a);
            for (int b : RATINGS.keySet()) {
                if (a == b) System.out.printf("  1.000  ");
                else System.out.printf("  %+.3f ", pearsonSimilarity(a, b));
            }
            System.out.println();
        }
    }

    static void printRecs(List<int[]> recs) {
        if (recs.isEmpty()) {
            System.out.println("  No recommendations available.");
        } else {
            for (int i = 0; i < recs.size(); i++) {
                System.out.printf("  %d. %-15s (score: %.3f)%n",
                    i + 1, PRODUCTS.get(recs.get(i)[0]), recs.get(i)[1] / 1000.0);
            }
        }
    }
}
