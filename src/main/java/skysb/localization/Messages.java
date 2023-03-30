package skysb.localization;
import org.bukkit.ChatColor;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.PropertyResourceBundle;
import java.util.ResourceBundle;
import java.util.ResourceBundle.Control;

public class Messages {

    private static String BUNDLE_NAME = "languages_fr_FR"; //$NON-NLS-1$

    private static ResourceBundle RESOURCE_BUNDLE = ResourceBundle.getBundle(BUNDLE_NAME, new UTF8Control());

    private Messages() {

    }

    public static String getString(String key) {
        try {
            return ChatColor.translateAlternateColorCodes('&', RESOURCE_BUNDLE.getString(key));
        } catch (MissingResourceException e) {
            return '!' + key + '!';
        }
    }

    public static void setLanguage() {

        try {
            //BUNDLE_NAME = "languages." + GroupManager.getGMConfig().getLanguage();
            BUNDLE_NAME = "languages_fr_FR";
            RESOURCE_BUNDLE = ResourceBundle.getBundle(BUNDLE_NAME, new UTF8Control());
        } catch (Exception ex) {
            //GroupManager.logger.log(Level.WARNING, "Missing or corrupt 'language' node. Using default settings");
            BUNDLE_NAME = "languages_fr_FR";
            RESOURCE_BUNDLE = ResourceBundle.getBundle(BUNDLE_NAME, new UTF8Control());
        }
    }

    static class UTF8Control extends Control {

        public ResourceBundle newBundle (String baseName, Locale locale, String format, ClassLoader loader, boolean reload)
                throws IOException
        {
            String bundleName = toBundleName(baseName, locale);
            String resourceName = toResourceName(bundleName, "properties");
            ResourceBundle bundle = null;
            InputStream stream = null;
            if (reload) {
                URL url = loader.getResource(resourceName);
                if (url != null) {
                    URLConnection connection = url.openConnection();
                    if (connection != null) {
                        connection.setUseCaches(false);
                        stream = connection.getInputStream();
                    }
                }
            } else {
                stream = loader.getResourceAsStream(resourceName);
            }
            if (stream != null) {
                try {
                    bundle = new PropertyResourceBundle(new InputStreamReader(stream, StandardCharsets.UTF_8));
                } finally {
                    stream.close();
                }
            }
            return bundle;
        }
    }
}