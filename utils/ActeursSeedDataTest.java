package com.example.oussouye.utils;

import static org.junit.Assert.*;

import com.example.oussouye.model.Filiere;

import org.junit.Test;

import java.lang.reflect.Method;
import java.util.List;


public class ActeursSeedDataTest {

    /** Appelle la méthode privée ActeursSeedData.parseFilieres(String) par réflexion. */
    @SuppressWarnings("unchecked")
    private List<Filiere> parseFilieres(String texte) throws Exception {
        Method method = ActeursSeedData.class.getDeclaredMethod("parseFilieres", String.class);
        method.setAccessible(true);
        return (List<Filiere>) method.invoke(null, texte);
    }

    @Test
    public void texteAvecPlusieursFilieres_retourneToutesLesFilieres() throws Exception {
        List<Filiere> result = parseFilieres("Fruits, légumes, céréales, savon");

        assertTrue(result.contains(Filiere.FRUITS));
        assertTrue(result.contains(Filiere.LEGUMES));
        assertTrue(result.contains(Filiere.AUTRES_CEREALES));
        assertTrue(result.contains(Filiere.SAVON));
        assertEquals(4, result.size());
    }

    @Test
    public void texteVide_retourneListeVide() throws Exception {
        assertTrue(parseFilieres("").isEmpty());
    }

    @Test
    public void texteNull_retourneListeVide() throws Exception {
        assertTrue(parseFilieres(null).isEmpty());
    }

    @Test
    public void texteAvecAccents_estBienDetecte() throws Exception {
        List<Filiere> result = parseFilieres("légumes");
        assertTrue(result.contains(Filiere.LEGUMES));
    }

    @Test
    public void huileDePalme_estDetectee() throws Exception {
        assertTrue(parseFilieres("huile de palme").contains(Filiere.HUILE_PALME));
    }

    @Test
    public void savonSeul_neDetectePasAutreChose() throws Exception {
        List<Filiere> result = parseFilieres("savons");
        assertEquals(1, result.size());
        assertTrue(result.contains(Filiere.SAVON));
    }

    @Test
    public void texteInconnu_retourneListeVide() throws Exception {
        assertTrue(parseFilieres("xyz inconnu").isEmpty());
    }

    @Test
    public void miel_detecteApiculture() throws Exception {
        assertTrue(parseFilieres("production de miel").contains(Filiere.APICULTURE));
    }
}
