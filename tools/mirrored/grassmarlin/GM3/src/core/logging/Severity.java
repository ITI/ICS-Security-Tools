/*
 *  Copyright (C) 2011, 2012
 *  This file is part of GRASSMARLIN.
 */
package core.logging;

import javafx.scene.paint.Color;

public enum Severity {
    Success(Color.web("0x3C763D"), Color.web("0xDFF0D8")), // Green
    Information(Color.web("0x31708F"), Color.web("0xD9EDF7")), // Blue
    Warning(Color.web("0x8A6D3B"), Color.web("0xFCF8E3")), // Yellow
    Error(Color.web("0xA94442"), Color.web("0xF2DEDE")) // Red

    ;

    public Color text;
    public Color background;

    Severity(final Color text, final Color background) {
        this.background = background;
        this.text = text;
    }
}
