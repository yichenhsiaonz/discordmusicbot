package org.example.youtube;

import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.youtube.YouTube;
import com.google.api.services.youtube.model.ResourceId;
import com.google.api.services.youtube.model.SearchListResponse;
import com.google.api.services.youtube.model.SearchResult;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;

public class YouTubeSearch {

    private static final String KEYPATH = "youtubeapikey.txt";
    private final static Charset ENCODING = StandardCharsets.UTF_8;
    private static final long NUMBER_OF_VIDEOS_RETURNED = 1;
    private static String KEY = readPropertiesFile(KEYPATH).get(0) ;

    public static String videoIdSearch(String term) {

        Properties properties = new Properties();
        properties.put("youtube.apikey",KEY);

        try {
            YouTube youtube = new YouTube.Builder(new NetHttpTransport(), new JacksonFactory(), request -> {
            }).setApplicationName("enetmusic-youtube-search").build();
            String queryTerm = setInputQuery(term);
            YouTube.Search.List search = youtube.search().list("id,snippet");
            String apiKey = properties.getProperty("youtube.apikey");
            search.setKey(apiKey);
            search.setQ(queryTerm);
            search.setType("video");
            search.setFields("items(id/kind,id/videoId,snippet/title,snippet/thumbnails/default/url)");
            search.setMaxResults(NUMBER_OF_VIDEOS_RETURNED);
            SearchListResponse searchResponse = search.execute();
            List<SearchResult> searchResultList = searchResponse.getItems();
            if (searchResultList != null) {
                return getVideo(searchResultList.iterator());
            }
        } catch (GoogleJsonResponseException e) {
            System.err.println("There was a service error: " + e.getDetails().getCode() + " : "
                    + e.getDetails().getMessage());
        } catch (IOException e) {
            System.err.println("There was an IO error: " + e.getCause() + " : " + e.getMessage());
        } catch (Throwable t) {
            t.printStackTrace();
        }
        return "blank";
    }

    private static List<String> readPropertiesFile(String aFileName){
        try{
            Path path = Paths.get(aFileName);
            return Files.readAllLines(path, ENCODING);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    private static String setInputQuery(String s){
        String inputQuery = "";
        if(s.length() > 0)
            inputQuery = s;
        return inputQuery;
    }

    private static String getVideo(Iterator<SearchResult> iteratorSearchResults) {
        if (!iteratorSearchResults.hasNext()) {
            return "blank";
        }

        SearchResult singleVideo = iteratorSearchResults.next();
        ResourceId rId = singleVideo.getId();
        return rId.getVideoId();
    }
}