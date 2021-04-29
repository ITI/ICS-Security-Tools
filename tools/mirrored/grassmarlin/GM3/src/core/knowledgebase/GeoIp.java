package core.knowledgebase;

import core.logging.Logger;
import core.logging.Severity;
import ui.LocalIcon;
import util.Cidr;
import util.Launcher;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * GeoIp is used to identify the country associated with a given IP or CIDR.
 */
public abstract class GeoIp {
    private final static HashMap<Cidr, Integer> idFromCidr = new HashMap<>();
    private final static HashMap<Integer, String> nameFromId = new HashMap<>();

    public static void Initialize(String pathToCidrIdMapping, String pathToIdNameMapping) {
        final Path pathCidrId = Paths.get(pathToCidrIdMapping);
        if(Files.exists(pathCidrId)) {
            initCidrIdMap(pathCidrId);
        } else {
            Logger.log(GeoIp.class, Severity.Error, "Unable to locate Cidr -> GeoId data file.");
        }

        final Path pathNameId = Paths.get(pathToIdNameMapping);
        if(Files.exists(pathNameId)) {
            initNameIdMap(pathNameId);
        } else {
            Logger.log(GeoIp.class, Severity.Error, "Unable to locate GeoId -> Name data file.");
        }

        // Test the contents to ensure that every Id maps to a name and every name has an icon.
        // If we ever manage to get this to be error-free out-of-the-box, we can transition to using the logger to report the errors instead of hitting the console.
        idFromCidr.values().stream().distinct().forEach(id -> {
            if(!nameFromId.containsKey(id)) {
                String source = idFromCidr.entrySet().stream().filter(entry -> entry.getValue().equals(id)).map(entry -> entry.getKey().toString()).collect(Collectors.joining(", "));
                if(source.length() > 80) {
                    source = source.substring(0, 77) + "...";
                }
                Launcher.RecordLogMessage("GeoIp lookup for country Id " + id + " failed: " + source);
            }
        });
        nameFromId.values().stream().forEach(country -> {
            File pathImage = new File(("images|logical|country|" + country.replace(" ", "_").replaceAll("[^a-zA-Z_]", "") + ".png").replace("|", File.separator));
            if(!pathImage.exists()) {
                Launcher.RecordLogMessage("Unable to locate flag file for " + country + "(expected '" + pathImage.getAbsolutePath() + "')");
            }
        });
    }

    protected static void initCidrIdMap(Path src) {
        try {
            Logger.log(GeoIp.class, Severity.Information, "Loading Cidr -> GeoId Mapping from: " + src.toAbsolutePath().toString());

            idFromCidr.clear();
            Files.lines(src)
                    .filter(line -> !line.isEmpty())
                    .map(line -> line.split(","))
                    .filter(tokens -> tokens.length >= 2 && !tokens[1].isEmpty())
                    .forEach(tokens -> idFromCidr.put(new Cidr(tokens[0]), Integer.parseInt(tokens[1])));

            Logger.log(GeoIp.class, Severity.Information, "Cidr -> GeoId Mapping load complete.");
        } catch(Exception ex) {
            Logger.log(GeoIp.class, Severity.Warning, "Cidr -> GeoId Mapping load failed; GeoIp components may not function correctly: " + ex.getMessage());
        }
    }
    protected static void initNameIdMap(Path src) {
        try {
            Logger.log(GeoIp.class, Severity.Information, "Loading GeoId -> Name Mapping from: " + src.toAbsolutePath().toString());

            nameFromId.clear();
            Files.lines(src)
                    .filter(line -> !line.isEmpty())
                    .map(line -> line.split(","))
                    .filter(tokens -> tokens.length >= 2)
                    .forEach(tokens -> nameFromId.put(Integer.parseInt(tokens[0]), tokens[1]));

            Logger.log(GeoIp.class, Severity.Information, "GeoId -> Name Mapping load complete.");
        } catch(Exception ex) {
            Logger.log(GeoIp.class, Severity.Warning, "GeoId -> Name Mapping load failed; GeoIp components may not function correctly: " + ex.getMessage());
        }
    }

    public static String getCountryName(Cidr ip) {
        Optional<Integer> result = idFromCidr.entrySet().stream()
                .filter(set -> set.getKey().contains(ip))
                .map(set -> set.getValue())
                .findFirst();

        if(!result.isPresent()) {
            return null;
        }
        return nameFromId.get(result.get());
    }

    public static LocalIcon getFlagIcon(Cidr ip) {
        String country = getCountryName(ip).replace(" ", "_").replaceAll("[^a-zA-Z_]", "");
        //Adjust for various special cases...  which probably won't be done until we get a new GeoIP database.
        switch(country) {
            case "So_Tom_and_Prncipe":
                country = "Sao_Tome_and_Principe";
                break;
        }

        final String pathImage = ("images|logical|country|" + country + ".png").replace("|", File.separator);
        final File fileImage = new File(pathImage);

        if(!fileImage.exists()) {
            return LocalIcon.forPath("images|logical|country|_Not_Found.png");
        } else {
            return LocalIcon.forPath(pathImage);
        }
    }

}
