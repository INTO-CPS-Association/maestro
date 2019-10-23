package org.intocps.orchestration.coe.webapi.services;

import org.intocps.orchestration.coe.scala.Coe;

import java.io.File;

public class CoeService {

    public Coe create(File root) {
        return new Coe(root);
    }
}
