package util;

import model.FoodQueryResponse;
import org.apache.http.HttpEntity;
import org.apache.http.HttpException;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.apache.log4j.Logger;
import org.codehaus.jackson.map.ObjectMapper;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Mephalay on 10/25/2016.
 */
public class WebAPIUtils {
    private static String FOOD_QUERY_API_KEY = "mGKWC6iiQy5XABlrCzmJp5Qpyw6HUPByfnu2AT1I";
    private static Logger logger = Logger.getLogger(WebAPIUtils.class);
    private static final long FOOD_API_WAIT_TIME = 3600L;
    private static final Object lock = new Object();
    private static long lastApiCallTime = 0L;

    public static String queryFood(String foodName) throws URISyntaxException, IOException, HttpException {
        HttpClient httpClient = new DefaultHttpClient();
        if (foodName.contains(" ")) {
            foodName = foodName.replaceAll(" ", "%20");
        }
        HttpGet request = new HttpGet("http://api.nal.usda.gov/ndb/search/?format=json&q=" + foodName + "&sort=n&max=10000&offset=0&api_key=" + FOOD_QUERY_API_KEY);
        request.addHeader("Content-Type", "application/json; charset=utf-8");
        waitApiLimit();
        HttpResponse response = httpClient.execute(request);
        HttpEntity entity = response.getEntity();
        String responseString = EntityUtils.toString(entity);
        return responseString;
    }

    public static String queryFoodReport(String ndbno) throws URISyntaxException, IOException, HttpException {
        HttpClient httpClient = new DefaultHttpClient();
        HttpGet request = new HttpGet("http://api.nal.usda.gov/ndb/reports/?ndbno=" + ndbno + "&type=b&format=json&api_key=" + FOOD_QUERY_API_KEY);
        request.addHeader("Content-Type", "application/json; charset=utf-8");
        waitApiLimit();
        HttpResponse response = httpClient.execute(request);
        HttpEntity entity = response.getEntity();
        String responseString = EntityUtils.toString(entity);
        return responseString;
    }

    public static List<FoodQueryResponse> queryAllFoods() {
        List<FoodQueryResponse> foodQueryResponses = new ArrayList<>();
        Integer offset = 0;
        try {
            String resp = queryForOffset(offset);
            ObjectMapper om = new ObjectMapper();
            FoodQueryResponse foodQueryResponse = om.readValue(resp, FoodQueryResponse.class);
            foodQueryResponses.add(foodQueryResponse);
            Integer total = foodQueryResponse.getList().getTotal();
            Integer end = foodQueryResponse.getList().getEnd();
            offset += end;
            while (total > offset) {
                try {
//                    if (offset >= 150)
//                        break; //TODO Remove this section.
                    logger.info("Requesting food query for offset:" + offset);
                    resp = queryForOffset(offset);
                    foodQueryResponse = om.readValue(resp, FoodQueryResponse.class);
                    foodQueryResponses.add(foodQueryResponse);
                    total = foodQueryResponse.getList().getTotal();
                    end = foodQueryResponse.getList().getEnd();
                } catch (Throwable t) {
                    logger.error("Failed to retrieve list for current offset:" + offset + ", trying next offset");
                }
                offset += end;
            }
        } catch (Throwable t) {
            logger.fatal("Failed to fetch food query response...Needs inspection", t);
        }
        return foodQueryResponses;
    }


    private static String queryForOffset(Integer offset) throws URISyntaxException, IOException, HttpException {
        HttpClient httpClient = new DefaultHttpClient();
        HttpGet request = new HttpGet("http://api.nal.usda.gov/ndb/search/?format=json&sort=n&max=10000&offset=" + offset + "&api_key=" + FOOD_QUERY_API_KEY);
        request.addHeader("Content-Type", "application/json; charset=utf-8");
        waitApiLimit();
        HttpResponse response = httpClient.execute(request);
        HttpEntity entity = response.getEntity();
        String responseString = EntityUtils.toString(entity);
        return responseString;
    }


    public static int getFoodNum() throws HttpException, IOException, URISyntaxException {
        String offsetQuery = queryForOffset(0);
        ObjectMapper om = new ObjectMapper();
        FoodQueryResponse foodQueryResponse = om.readValue(offsetQuery, FoodQueryResponse.class);
        return foodQueryResponse.getList().getTotal();
    }

    private static void waitApiLimit() {
        try {
            synchronized (lock) {
                if (lastApiCallTime == 0L) {
                    lastApiCallTime = System.currentTimeMillis();
                } else {
                    long now = System.currentTimeMillis();
                    if (now < lastApiCallTime + FOOD_API_WAIT_TIME) {
                        long differ = (lastApiCallTime + FOOD_API_WAIT_TIME) - now;
                        Thread.sleep(differ);
                    }
                    lastApiCallTime = System.currentTimeMillis();
                }
            }
        } catch (Throwable t) {
            logger.error("Failed to wait for api limit", t);
        }
    }

    public static String getProcessedFoods() throws IOException, HttpException, URISyntaxException {
        HttpClient httpClient = new DefaultHttpClient();
        HttpGet request = new HttpGet("http://46.196.100.145/healthTracker/getFoodItems");
        request.addHeader("Content-Type", "application/json; charset=utf-8");
        HttpResponse response = httpClient.execute(request);
        HttpEntity entity = response.getEntity();
        String responseString = EntityUtils.toString(entity);
        return responseString;
    }
}
