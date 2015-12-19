package org.honton.chas.checkstats;

import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import javax.annotation.Nonnull;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import lombok.Data;
import lombok.SneakyThrows;

@Data
public class ProjectStats {

    private Set<String> files = new TreeSet<>();
    private Map<String,Stat> stats = new HashMap<>();
    
    @SneakyThrows
    public static ProjectStats read(File file) {
        Reader reader = new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8);
        try {
            Gson gson = getGson();
            return gson.fromJson(reader, ProjectStats.class);
        } finally {
            reader.close();
        }
    }

    @SneakyThrows
    public void write(File file) {
        Writer writer = new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8);
        try {
            Gson gson = getGson();
            gson.toJson(this, writer);
        } finally {
            writer.close();
        }
    }

    public void addSrcFiles(File srcDir) {
        srcDir.listFiles(new FileFilter() {
            final StringBuilder sb = new StringBuilder();
            
            @Override
            public boolean accept(File file) {                
                int length = sb.length();
                sb.append(file.getName());
                if(file.isDirectory()) {
                    sb.append('/');
                    file.listFiles(this);
                }
                else {
                    files.add(sb.toString());
                }
                sb.setLength(length);
                return false;
            }
        });
    }
    
    private static Gson getGson() {
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        return gson;
    }
    
    public @Nonnull List<Failure> checkIsBetter(@Nonnull ProjectStats current) {
        Stat.Context context = new Stat.Context(files);

        for (Map.Entry<String, Stat> entry : current.stats.entrySet()) {
            Stat priorValue = stats.get(entry.getKey());
            if (priorValue == null) {
                continue;
            }
            Stat currentValue = entry.getValue();
            priorValue.checkIsBetter(currentValue, context);
        }
        return context.getFailures();
    }
    
    public int getSize() {
        return stats.size();
    }

    // for testing
    void addStat(String name, Stat stat) {
        stats.put(name, stat);
    }

    // for testing
    void addFile(String fileName) {
        files.add(fileName);
    }

}
