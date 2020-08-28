package org.mltooling.lab;

import org.mltooling.lab.services.managers.DockerServiceManager;


public class CleanLabLandscape {
    // ================ Constants =========================================== //

    // ================ Members ============================================= //

    // ================ Constructors & Main ================================= //

    // ================ Methods for/from SuperClass / Interfaces ============ //
    public static void main(String[] args) throws Exception {
        DockerServiceManager.cleanUpLab(true);
    }
    // ================ Public Methods ====================================== //

    // ================ Private Methods ===================================== //

    // ================ Getter & Setter ===================================== //

    // ================ Builder Pattern ===================================== //

    // ================ Inner & Anonymous Classes =========================== //
}
