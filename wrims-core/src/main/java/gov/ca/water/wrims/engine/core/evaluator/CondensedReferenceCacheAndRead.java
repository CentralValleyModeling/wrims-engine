package gov.ca.water.wrims.engine.core.evaluator;

import hec.heclib.dss.*;
import hec.heclib.util.HecTime;
import hec.io.TimeSeriesContainer;

import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.function.Function;

import static java.util.stream.Collectors.toMap;

public class CondensedReferenceCacheAndRead {

    public static CondensedReferenceCache createCondensedCache(String filename) {
        return createCondensedCache(filename, "*");
    }

    public static CondensedReferenceCache createCondensedCache(String filename, String pathnameFilter) {
        HecDssCatalog catalog = new HecDssCatalog(filename);
        CondensedReference[] condensedArray = catalog.getCondensedCatalog(pathnameFilter);
        SortedMap<String, CondensedReference> condensedReferences = Arrays.stream(condensedArray)
                .collect(toMap(CondensedReference::getNominalPathname, Function.identity(),
                        (v1, v2) -> v1, TreeMap::new));
        return new CondensedReferenceCache(filename, condensedReferences);
    }

    public static final class CondensedReferenceCache {
    	private final SortedMap<String, CondensedReference> condensedReferences;
        private final String fileName;

        private CondensedReferenceCache(String fileName, SortedMap<String, CondensedReference> condensedReferences) {
            this.condensedReferences = condensedReferences;
            this.fileName = fileName;
        }

        public Set<String> getAllPaths() {
            return this.condensedReferences.keySet();
        }

        public DSSPathname getNominalPathname(String pathname) {
            return condensedReferences.entrySet()
                    .stream()
                    .filter(e -> new DSSPathname(e.getKey()).isSamePathname(pathname, false))
                    .map(Map.Entry::getKey)
                    .map(DSSPathname::new)
                    .findAny()
                    .orElse(null);
        }

        public CondensedReference getCondensedReference(String pathname) {
            return condensedReferences.entrySet()
                    .stream()
                    .filter(e -> new DSSPathname(e.getKey()).isSamePathname(pathname, false))
                    .map(Map.Entry::getValue)
                    .findAny()
                    .orElse(null);
        }

        public TimeSeriesContainer readFullRecord(String pathname) {
            CondensedReference condensedReference = getCondensedReference(pathname);
            if (condensedReference==null) {
            	return null;
            }else {
            	HecTimeSeries ts = new HecTimeSeries(this.fileName);
            	condensedReference.generatePathnameList(ts, false);
            	TimeSeriesContainer tsc = new TimeSeriesContainer();
            	tsc.fileName = this.fileName;
            	tsc.fullName = condensedReference.getNominalPathname();
            	HecTime start = new HecTime();
            	HecTime end = new HecTime();
            	condensedReference.getPathnameTimes(new LocalDssDataManager(), start, end);
            	ts.setTimeWindow(start, end);
            	boolean removeMissing = false;
            	ts.read(tsc, removeMissing);
            	return tsc;
            }
        }
    }
}