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

public class ProgressCallbackConsoleImpl implements ProgressCallback {

    private Console console;
    private int lastPercent = -1;

    ProgressCallbackConsoleImpl(Console console) {
        this.console = console;
    }

    @Override
    public void bytesTransferred(long transferred, long total) {
        String transferredStr = toHumanReadableBytes(transferred);
        String totalStr = toHumanReadableBytes(total);
        int percent = (int) (100.0 * transferred / total);

        if (percent != this.lastPercent) {
            this.console.println("Transferred: " + transferredStr + "/" + totalStr + " (" + percent + "%)");
            this.lastPercent = percent;
        }
    }

    private String toHumanReadableBytes(long bytes) {
        if (bytes < 1024) {
            return "" + bytes + "B";
        }

        bytes = bytes / 1024;

        if (bytes < 1024) {
            return "" + bytes + "kB";
        }

        bytes = bytes / 1024;

        return "" + bytes + "MB";
    }
}
