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

public class ConsoleDefaultImpl implements Console {

    private static final String OS_NAME = System.getProperty("os.name");

    private static final boolean ANSI_SUPPORTED = !OS_NAME.toLowerCase().contains("windows");

    //ANSI escape codes
    private static final String ANSI_BLACK = "\u001B[30m";
    private static final String ANSI_RED = "\u001B[31m";
    private static final String ANSI_GREEN = "\u001B[32m";
    private static final String ANSI_YELLOW = "\u001B[33m";
    private static final String ANSI_BLUE = "\u001B[34m";
    private static final String ANSI_PURPLE = "\u001B[35m";
    private static final String ANSI_CYAN = "\u001B[36m";
    private static final String ANSI_WHITE = "\u001B[37m";
    private static final String ANSI_RESET = "\u001B[0m";

    @Override
    public void println(String message) {
        System.out.println(message);
    }

    @Override
    public void printlnError(String message) {
        if (ANSI_SUPPORTED) {
            System.err.println(ANSI_RED + message + ANSI_RESET);
        } else {
            System.err.println(message);
        }
    }

    @Override
    public void printStacktrace(Throwable throwable) {
        throwable.printStackTrace(System.err);
    }

    @Override
    public void exit(int status) {
        System.exit(status);
    }
}
