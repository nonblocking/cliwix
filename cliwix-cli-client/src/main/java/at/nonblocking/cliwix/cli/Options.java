/*
 * Copyright (c) 2014-2016
 * nonblocking.at gmbh [http://www.nonblocking.at]
 *
 * This file is part of Cliwix.
 *
 * Cliwix is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package at.nonblocking.cliwix.cli;

import org.json.simple.JSONObject;

import java.io.*;
import java.nio.file.Files;
import java.util.*;

public class Options {

    static final String COMMAND_HELP = "help";
    static final String COMMAND_CREATE_CONFIG = "create-config";
    static final String COMMAND_INFO = "info";
    static final String COMMAND_IMPORT = "import";
    static final String COMMAND_EXPORT = "export";

    static final List<String> COMMANDS = Arrays.asList(
            COMMAND_HELP,
            COMMAND_CREATE_CONFIG,
            COMMAND_INFO,
            COMMAND_EXPORT,
            COMMAND_IMPORT
    );

    private String command = null;
    private Properties defaultProperties = new Properties();
    private Properties customProperties = new Properties();
    private Map<String, String> cliOverrides = new HashMap<>();

    String getCommand() {
        return command;
    }

    boolean isDebug() {
        return getBooleanProperty("cli.debug");
    }

    String getServerCliwixUrl() {
        return getProperty("server.cliwix.url");
    }

    String getServerUsername() {
        return getProperty("server.liferay.omniadmin.username");
    }

    String getServerPassword() {
        return getProperty("server.liferay.omniadmin.password");
    }

    String getExportFolder() {
        return getProperty("export.folder");
    }

    Boolean isExportDeleteOnServerAfterTransfer() {
        return getBooleanProperty("export.deleteOnServerAfterTransfer");
    }

    int getTimeoutServerReadySec() {
        Integer timeout = getIntProperty("cli.timeout.server.ready.sec");
        if (timeout == null) {
            return Integer.MAX_VALUE;
        }
        return timeout;
    }

    int getTimeoutImportSec() {
        Integer timeout = getIntProperty("cli.timeout.import.sec");
        if (timeout == null) {
            return Integer.MAX_VALUE;
        }
        return timeout;
    }

    int getTimeoutExportSec() {
        Integer timeout = getIntProperty("cli.timeout.export.sec");
        if (timeout == null) {
            return Integer.MAX_VALUE;
        }
        return timeout;
    }

    boolean isExtractZip() {
        return getBooleanProperty("export.extract.zip");
    }

    JSONObject getExportSettings() {
        JSONObject exportSettings = new JSONObject();

        Enumeration<String> propertyNames = (Enumeration<String>) this.defaultProperties.propertyNames();
        while (propertyNames.hasMoreElements()) {
            String name = propertyNames.nextElement();
            if (!name.startsWith("export.settings.")) {
                continue;
            }
            exportSettings.put(name.substring("export.settings.".length()), getProperty(name));
        }

        return exportSettings;
    }

    String getImportInput() {
        return getProperty("import.input");
    }

    String getImportReportFolder() {
        return getProperty("import.report.folder");
    }

    Boolean isImportDeleteOnServerAfterTransfer() {
        return getBooleanProperty("import.deleteOnServerAfterTransfer");
    }

    JSONObject getImportSettings(String tmpFileName) {
        JSONObject importSettings = new JSONObject();

        importSettings.put("mode", "upload");
        importSettings.put("tmpFileName", tmpFileName);

        Enumeration<String> propertyNames = (Enumeration<String>) this.defaultProperties.propertyNames();
        while (propertyNames.hasMoreElements()) {
            String name = propertyNames.nextElement();
            if (!name.startsWith("import.settings.")) {
                continue;
            }
            importSettings.put(name.substring("import.settings.".length()), getProperty(name));
        }

        return importSettings;
    }

    void copyDefaultPropertiesTo(File file) throws CliwixCliClientArgumentException {
        try {
            Files.copy(getDefaultProperties(), file.toPath());
        } catch (IOException e) {
            throw new CliwixCliClientArgumentException("Unable to store config: " + file.getAbsolutePath());
        }
    }

    private String getProperty(String key) {
        String value = this.defaultProperties.getProperty(key);
        if (value != null && this.customProperties != null && this.customProperties.containsKey(key)) {
            value = this.customProperties.getProperty(key);
        }
        if (value != null && this.cliOverrides.containsKey(key)) {
            value = this.cliOverrides.get(key);
        }
        if (value == null || value.isEmpty()) {
            return null;
        }
        return value;
    }

    private boolean getBooleanProperty(String key) {
        String value = getProperty(key);
        return value != null && "true".equalsIgnoreCase(value);
    }

    private Integer getIntProperty(String key) {
        String value = getProperty(key);
        return value == null ? null : Integer.parseInt(value.trim());
    }

    static Options parseArgs(String[] args) throws CliwixCliClientArgumentException {
        Options result = new Options();

        if (args.length < 1) {
            throw new CliwixCliClientArgumentException("No command provided!");
        }

        String command = args[0];
        if (!COMMANDS.contains(command)) {
            throw new CliwixCliClientArgumentException("Invalid command: " + command + "!");
        }

        result.command = command;

        try {
            result.defaultProperties.load(getDefaultProperties());
        } catch (IOException e) {
            throw new RuntimeException("Unable to read default properties!", e);
        }

        if (!COMMAND_CREATE_CONFIG.equals(command) && args.length > 1) {
            try (FileInputStream fis = new FileInputStream(args[1])) {
                result.customProperties.load(fis);
            } catch (IOException e) {
                throw new CliwixCliClientArgumentException("Invalid property file: " + args[1] + "!");
            }

            for (int i = 2; i < args.length; i++) {
                String[] parts = args[i].split("=");
                if (parts.length != 2 || parts[0].isEmpty() || parts[1].isEmpty() || !parts[0].startsWith("--")) {
                    throw new CliwixCliClientArgumentException("Invalid command line argument: " + args[i]);
                }
                result.cliOverrides.put(parts[0].substring(2), parts[1]);
            }
        }

        return result;
    }

    static void printUsage() {
        CliwixCliClient.console.println("Usage: cliwix <command> <path to custom property file> [override properties]");
        CliwixCliClient.console.println("  Commands are:");
        CliwixCliClient.console.println("    " + COMMAND_HELP + ": Print this help");
        CliwixCliClient.console.println("    " + COMMAND_CREATE_CONFIG + ": Create a property file with given file name");
        CliwixCliClient.console.println("    " + COMMAND_INFO + ": Show server information");
        CliwixCliClient.console.println("    " + COMMAND_EXPORT + ": Export Liferay configuration with given properties");
        CliwixCliClient.console.println("    " + COMMAND_IMPORT + ": Import Liferay configuration with given properties");
        CliwixCliClient.console.println("  The custom property file override the defaults.");
        CliwixCliClient.console.println("  The override properties on the command line override also properties in the custom property file.");
        CliwixCliClient.console.println("");
        CliwixCliClient.console.println("  Example:");
        CliwixCliClient.console.println("    cliwix export my_custom_settings.properties --export.folder=foobar");
    }

    private static InputStream getDefaultProperties() {
        return Options.class.getResourceAsStream("/cliwix-cli-default.properties");
    }
}
