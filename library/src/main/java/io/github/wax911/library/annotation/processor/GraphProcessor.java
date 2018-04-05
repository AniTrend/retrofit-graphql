package io.github.wax911.library.annotation.processor;

import android.content.Context;
import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.annotation.Annotation;
import java.util.Map;
import java.util.WeakHashMap;

import io.github.wax911.library.annotation.GraphQuery;

/**
 * Created by max on 2018/03/12.
 * GraphQL annotation processor
 */
public class GraphProcessor {

    private final Map<String, String> graphFiles;
    private String qualifier = ".graphql", rootFolder = "graphql";

    /**
     * Creates a new GraphQL annotation processor,
     * that looks for a rootFolder called graphql
     * containing .graphql files
     * <br/>
     *
     * @param context Any valid application context
     */
    public GraphProcessor(Context context) {
        this.graphFiles = new WeakHashMap<>();
        findAllFiles(rootFolder, context);
    }

    /**
     * Creates a new GraphQL annotation processor
     * <br/>
     *
     * @param context Any valid application context
     * @param qualifier The extension of the file to look for (default is .graphql)
     */
    public GraphProcessor(Context context, String qualifier) {
        this.graphFiles = new WeakHashMap<>();
        this.qualifier = qualifier;
        findAllFiles(rootFolder, context);
    }

    /**
     * Creates a new GraphQL annotation processor
     * <br/>
     *
     * @param context Any valid application context
     * @param qualifier The extension of the file to look for (default is .graphql)
     * @param rootFolder The name of the root folder to search
     */
    public GraphProcessor(Context context, String qualifier, String rootFolder) {
        this.graphFiles = new WeakHashMap<>();
        this.qualifier = qualifier;
        this.rootFolder = rootFolder;
        findAllFiles(rootFolder, context);
    }

    public String getQuery(Annotation[] annotations) {
        GraphQuery graphQuery = null;

        for (Annotation annotation: annotations)
            if(annotation instanceof GraphQuery) {
                graphQuery = (GraphQuery) annotation;
                break;
            }

        if(graphFiles != null && graphQuery != null) {
            String fileName = String.format("%s%s", graphQuery.value(), qualifier);
            if(graphFiles.containsKey(fileName))
                return graphFiles.get(fileName);
            Log.w(this.toString(), String.format("The request query %s could not be found!", graphQuery.value()));
        }
        return null;
    }

    private void findAllFiles(String path, Context context) {
        String[] paths;
        try {
            paths = context.getAssets().list(path);
            if (paths.length > 0) {
                for (String item : paths) {
                    String absolute = path + "/" + item;
                    if(!item.endsWith(qualifier))
                        findAllFiles(absolute, context);
                    else
                        graphFiles.put(item, getFileContents(context.getAssets().open(absolute)));
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private String getFileContents(InputStream inputStream) {
        StringBuilder queryBuffer = new StringBuilder();
        try {
            InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
            BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
            for(String line; (line = bufferedReader.readLine()) != null;)
                queryBuffer.append(line);
            inputStreamReader.close();
            bufferedReader.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return queryBuffer.toString();
    }
}
