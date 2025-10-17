package wrimsv2.results;

import wrimsv2.exceptions.StringNotFoundException;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Path;
import java.util.*;

public class CsvResult  {
    protected final int[] ids;
    protected final String[] partAs;
    protected final String[] partFs;
    protected final String[] timesteps;
    protected final String[] units;
    protected final String[] dateTimes;
    protected final String[] variables;
    protected final String[] kinds;
    protected final float[] values;
    private static final int COLUMN_COUNT = 9;
    private final HashMap<String, int[]> variableIndexCache = new HashMap<>();

    public CsvResult(String sourceFile) throws IOException {
        List<String[]> stringArray = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader(sourceFile))) {
            String line = br.readLine();
            if (line == null) {
                throw new IOException("Empty CSV file: " + sourceFile);
            }
            // Read all rows
            while ((line = br.readLine()) != null) {
                if (line.trim().isEmpty()) continue; // skip blank lines
                stringArray.add(line.split(","));
            }
        }

        // Allocate arrays
        int readRowCount = stringArray.size();
        ids = new int[readRowCount];
        partAs = new String[readRowCount];
        partFs = new String[readRowCount];
        timesteps = new String[readRowCount];
        units = new String[readRowCount];
        dateTimes = new String[readRowCount];
        variables = new String[readRowCount];
        kinds = new String[readRowCount];
        values = new float[readRowCount];

        // Fill arrays
        for (int i = 0; i < readRowCount; i++) {
            String[] row = stringArray.get(i);
            // Defensive: ensure row has expected number of columns
            if (row.length < COLUMN_COUNT) {
                throw new IOException(String.format(
                        "Invalid row %d: expected %d columns, got %d", i + 1, COLUMN_COUNT, row.length
                ));
            }
            ids[i] = Integer.parseInt(row[0].trim());
            partAs[i] = row[1].trim();
            partFs[i] = row[2].trim();
            timesteps[i] = row[3].trim();
            units[i] = row[4].trim();
            dateTimes[i] = row[5].trim();
            variables[i] = row[6].trim();
            kinds[i] = row[7].trim();
            values[i] = Float.parseFloat(row[8].trim());
        }
    }
    public CsvResult(Path sourceFile) throws IOException {
        this(sourceFile.toAbsolutePath().toString());
    }

    public Set<String> getUniqueVariables() {
        return new HashSet<>(Arrays.asList(variables));
    }

    private int[] getVariableIndexes(String variable) throws StringNotFoundException {
        int[] matchIndexes;
        if (!variableIndexCache.containsKey(variable)) {
            matchIndexes = new int[this.variables.length]; // max possible size
            int matchCount = 0;
            for (int i = 0; i < this.variables.length; i++) {
                if (this.variables[i].equals(variable)) {
                    matchIndexes[matchCount++] = i;
                }
            }
            if (matchCount == 0) {
                throw new StringNotFoundException(String.format("Couldn't find variable: %s", variable));
            }
            matchIndexes = Arrays.copyOfRange(matchIndexes, 0, matchCount);
            variableIndexCache.put(variable, matchIndexes);
            return matchIndexes;
        } else {
            return variableIndexCache.get(variable);
        }
    }

    public String[] getTimestepArray(String variable) throws StringNotFoundException {
        int[] indexes = this.getVariableIndexes(variable);
        String[] result = new String[indexes.length];
        for (int i = 0; i < indexes.length; i++) {
            result[i] = this.timesteps[indexes[i]];
        }
        return result;
    }

    public float[] getValueArray(String variable) throws StringNotFoundException {
        int[] indexes = this.getVariableIndexes(variable);
        float[] result = new float[indexes.length];
        for (int i = 0; i < indexes.length; i++) {
            result[i] = this.values[indexes[i]];
        }
        return result;
    }

    public float getSum(String variable) throws StringNotFoundException {
        float[] variableValues = this.getValueArray(variable);
        float sum = 0;
        for (float val : variableValues) {
            sum += val;
        }
        return sum;
    }

    public float getAverage(String variable) throws StringNotFoundException {
        float[] variableValues = this.getValueArray(variable);
        float sum = this.getSum(variable);
        return sum / variableValues.length;
    }
}
