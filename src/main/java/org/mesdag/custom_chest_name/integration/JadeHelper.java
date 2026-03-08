package org.mesdag.custom_chest_name.integration;

import snownee.jade.Jade;

public class JadeHelper {
    public static boolean shouldDisplayTooltip() {
        return Jade.CONFIG.get().getGeneral().shouldDisplayTooltip();
    }
}
