package org.honton.chas.checkstats;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.annotation.Nonnull;

import com.google.gson.TypeAdapter;
import com.google.gson.annotations.JsonAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * A statistic is named and can be structured.
 */
@JsonAdapter(Stat.Adapter.class)
public class Stat {
    private final Map<String, Object> values = new HashMap<>();

    public boolean put(@Nonnull String name, @Nonnull Number value) {
        return values.put(name, value) == null;
    }

    public boolean put(@Nonnull String name, @Nonnull Stat value) {
        return values.put(name, value) == null;
    }

    public Stat getStat(String key) {
        return (Stat) values.get(key);
    }

    public Number getNumber(String key) {
        return (Number) values.get(key);
    }

    @RequiredArgsConstructor
    private static class ConstantStat extends Stat {
        private final Integer intConstant;
        private final Double doubleConstant;

        @Override
        Object getEffectivePrior(Context context, Map.Entry<String, Object> entry) {
            Object currentValue = entry.getValue();
            if (currentValue instanceof Stat) {
                return this;
            } else if (currentValue instanceof Integer) {
                return intConstant;
            } else if (currentValue instanceof Double) {
                return doubleConstant;
            } else {
                throw new IllegalStateException("unsupported type " + currentValue.getClass().getCanonicalName());
            }
        }
    }

    private static ConstantStat MAX_VALUES = new ConstantStat(Integer.MAX_VALUE, Double.MAX_VALUE);
    private static ConstantStat ZEROS = new ConstantStat(0, 0.0);

    Object getEffectivePrior(Context context, Map.Entry<String, Object> entry) {
        Object value = values.get(entry.getKey());
        if (value != null) {
            return value;
        }
        return context.getEffectivePrior(entry);
    }

    private void checkSingleValueIsBetter(Context context, Map.Entry<String, Object> entry) {
        Object priorValue = getEffectivePrior(context, entry);
        if (priorValue == null) {
            return;
        }

        Object currentValue = entry.getValue();
        if (currentValue instanceof Stat) {
            ((Stat) priorValue).checkIsBetter((Stat) currentValue, context);
        } else {
            Number cv = (Number) currentValue;
            Number pv = (Number) priorValue;

            int cmp;
            if (cv instanceof Double || pv instanceof Double) {
                cmp = Double.compare(cv.doubleValue(), pv.doubleValue());
            } else {
                cmp = Integer.compare(cv.intValue(), pv.intValue());
            }
            if (cmp > 0) {
                context.addFailure(cv, pv);
            }
        }
    }

    void checkIsBetter(@Nonnull Stat current, @Nonnull Context context) {
        context.pushLevel();

        for (Map.Entry<String, Object> entry : current.values.entrySet()) {
            context.setName(entry.getKey());
            checkSingleValueIsBetter(context, entry);
        }
        context.popLevel();
    }

    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    static class State {
        private final State prior;
        private final int depth;
        private final int length;

        public State push(int length) {
            return new State(this, depth + 1, length);
        }
    }

    @RequiredArgsConstructor
    static class Context {
        final private Set<String> files;
        final private StringBuilder sb = new StringBuilder();
        @Getter
        final private List<Failure> failures = new ArrayList<>();

        State state = new State(null, 0, 0);

        void pushLevel() {
            int length = sb.length();
            if (length > 0) {
                sb.append('.');
                ++length;
            }
            state = state.push(length);
        }

        public Object getEffectivePrior(Entry<String, Object> entry) {
            if (state.depth != 1) {
                return null;
            }
            ConstantStat constantStat = files.contains(entry.getKey()) ? ZEROS : MAX_VALUES;
            return constantStat.getEffectivePrior(this, entry);
        }

        void setName(String name) {
            sb.setLength(state.length);
            sb.append(name);
        }

        void addFailure(Number cv, Number pv) {
            failures.add(new Failure(sb.toString(), cv, pv));
        }

        void popLevel() {
            state = state.prior;
        }
    }

    // for testing
    @Nonnull
    List<Failure> checkIsBetter(@Nonnull Stat current) {
        Context context = new Context(Collections.<String>emptySet());
        checkIsBetter(current, context);
        return context.getFailures();
    }

    @Override
    public int hashCode() {
        return values.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Stat) {
            return values.equals(((Stat) obj).values);
        }
        return false;
    }

    @Override
    public String toString() {
        return values.toString();
    }

    public static class Adapter extends TypeAdapter<Stat> {
        @Override
        public void write(JsonWriter out, Stat stat) throws IOException {
            out.beginObject();
            for (Map.Entry<String, Object> entry : stat.values.entrySet()) {
                out.name(entry.getKey());
                Object value = entry.getValue();
                if (value instanceof Stat) {
                    write(out, (Stat) value);
                } else if (value instanceof Integer) {
                    out.value(((Integer) value).intValue());
                } else if (value instanceof Double) {
                    out.value(((Double) value).doubleValue());
                } else {
                    throw new IllegalStateException(value.getClass().getCanonicalName() + " is not supported");
                }
            }
            out.endObject();
        }

        static Number stringToNumber(String value) {
            if (value.indexOf('.') < 0) {
                return new Integer(value);
            }
            return new Double(value);
        }

        @Override
        public Stat read(JsonReader in) throws IOException {
            Stat stat = new Stat();
            in.beginObject();
            while (in.hasNext()) {
                String name = in.nextName();
                JsonToken value = in.peek();
                if (value == JsonToken.BEGIN_OBJECT) {
                    stat.put(name, read(in));
                } else {
                    String number = in.nextString();
                    stat.put(name, stringToNumber(number));
                }
            }
            in.endObject();
            return stat;
        }
    }

    public int getSize() {
        return values.size();
    }
}