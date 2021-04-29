package core.knowledgebase;

import core.logging.Logger;
import core.logging.Severity;
import util.Mac;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class Manufacturer {
    protected static class Resolver {
        protected final long mask;
        protected final long value;
        protected final String manufacturer;

        protected Resolver(long mask, long value, String manufacturer) {
            this.mask = mask;
            this.value = value;
            this.manufacturer = manufacturer;
        }
        public boolean matches(long id) {
            return (id & mask) == value;
        }

        public String getManufacturer() {
            return manufacturer;
        }
    }

    protected static HashMap<Integer, List<Resolver>> resolvers = new HashMap<>();

    protected static long idFromMac(byte[] mac) {
        return
                ((long)mac[0] & 0xFF) << 40 |
                ((long)mac[1] & 0xFF) << 32 |
                ((long)mac[2] & 0xFF) << 24 |
                ((long)mac[3] & 0xFF) << 16 |
                ((long)mac[4] & 0xFF) <<  8 |
                ((long)mac[5] & 0xFF);
    }
    protected static long idFromMac(Mac mac) {
        final int[] value = mac.getValue();
        return
                (long)value[0] << 40 |
                (long)value[1] << 32 |
                (long)value[2] << 24 |
                (long)value[3] << 16 |
                (long)value[4] <<  8 |
                (long)value[5];
    }

    protected static int keyFromMac(byte[] mac) {
        //The map is built on the first 2 bytes.  This only works if
        return
                ((int)mac[0] & 0xFF) <<  8 |
                ((int)mac[1] & 0xFF);
    }
    protected static int keyFromMac(Mac mac) {
        final int[] value = mac.getValue();

        return (value[0] << 8) | value[1];
    }

    public static String forMac(Mac mac) {
        final int key = keyFromMac(mac);
        final long id = idFromMac(mac);

        if(!resolvers.containsKey(key)) {
            return null;
        }
        for(Resolver resolver : resolvers.get(key)) {
            if(resolver.matches(id)) {
                return resolver.getManufacturer();
            }
        }
        return null;
    }

    public static void parseSourceFile(Path file) {
        try(BufferedReader reader = Files.newBufferedReader(file, StandardCharsets.ISO_8859_1)) {
            String line;
            while((line = reader.readLine()) != null) {
                addResolverFromLine(line);
            }
            //TODO: sort by most precise to least precise for each bin
        } catch(IOException ioe) {
            Logger.log(Manufacturer.class, Severity.Error, "Unable to load MAC manufacturer reference: " + ioe.getMessage());
        }
    }

    protected static void addResolverFromLine(String source) {
        if(source == null) {
            return;
        }
        source = source.trim();
        // Blank or Comment
        if(source.isEmpty() || source.startsWith("#")) {
            return;
        }

        final String[] tokens = source.split("\t", 2);
        //If the manufacturer isn't listed, this entry serves no purpose.
        if(tokens.length < 2) {
            return;
        }
        final String[] names = tokens[1].split("#", 2);
        final String manufacturer = names[names.length - 1].trim();

        final String[] tokensValue = tokens[0].split("/", 2);
        final String txtValue = tokensValue[0];
        final String filter = txtValue.replaceAll("[^A-Fa-f0-9]", "").trim();
        final int bitsMask = tokensValue.length == 1 ? filter.length() * 4 : Integer.parseInt(tokensValue[1]);
        final byte[] value = new byte[6];
        for(int idx = 0; idx < filter.length(); idx += 2) {
            //Parse as int since values >= 0x80 will overflow a byte.
            value[idx / 2] = (byte)(Integer.parseInt(filter.substring(idx, idx + 2), 16) & 0xFF);
        }

        final long id = idFromMac(value);
        if(id == 0) {
            return;
        }
        final Resolver result = new Resolver(0x0000FFFFFFFFFFFFL & (0x0000FFFFFFFFFFFFL << (48 - bitsMask)), id, manufacturer);
        final int key = keyFromMac(value);
        if(!resolvers.containsKey(key)) {
            resolvers.put(keyFromMac(value), new ArrayList<>());
        }
        resolvers.get(key).add(result);
    }
}
