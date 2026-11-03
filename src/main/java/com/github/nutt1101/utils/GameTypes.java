package com.github.nutt1101.utils;

import com.cryptomorin.xseries.XMaterial;
import com.cryptomorin.xseries.XSound;
import com.cryptomorin.xseries.particles.XParticle;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;

/** Resolve XSeries aliases against the running server; unavailable values remain explicit. */
public final class GameTypes {
    private GameTypes() {}

    public static Material material(String name) {
        return XMaterial.matchXMaterial(name).map(XMaterial::get).orElse(null);
    }

    public static Sound sound(String name) {
        return XSound.of(name).map(XSound::get).orElse(null);
    }

    public static Particle particle(String name) {
        return XParticle.of(name).map(XParticle::get).orElse(null);
    }
}
