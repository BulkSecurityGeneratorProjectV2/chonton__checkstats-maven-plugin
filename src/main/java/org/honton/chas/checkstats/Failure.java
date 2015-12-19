package org.honton.chas.checkstats;

import lombok.AllArgsConstructor;

@AllArgsConstructor
public class Failure {
    final String fieldName;
    final Number currentValue;
    final Number priorValue;
}
