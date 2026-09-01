package com.example.oussouye.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import android.content.Context;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class SessionManagerTest {

    private SessionManager sessionManager;

    @Before
    public void setUp() {
        Context context = ApplicationProvider.getApplicationContext();
        sessionManager = new SessionManager(context);
        sessionManager.clearSession(); // état propre avant chaque test
    }

    @Test
    public void nouveauCompte_sansFiche_getActeurIdEstNull() {
        sessionManager.saveSession("compte-test-1", "test1@test.com", "ACTEUR", true);
        assertNull(sessionManager.getActeurId());
    }

    @Test
    public void apresCreationFiche_lienPersisteApresDeconnexion() {
        sessionManager.saveSession("compte-test-2", "test2@test.com", "ACTEUR", true);
        sessionManager.setActeurId("fiche-456");

        sessionManager.clearSession(); // déconnexion
        assertFalse(sessionManager.isLoggedIn());

        // reconnexion avec le même compte
        sessionManager.saveSession("compte-test-2", "test2@test.com", "ACTEUR", true);
        assertEquals("fiche-456", sessionManager.getActeurId());
    }

    @Test
    public void comptesDifferents_ontDesLiensSepares() {
        // Compte A crée sa fiche
        sessionManager.saveSession("compte-A", "a@test.com", "ACTEUR", true);
        sessionManager.setActeurId("fiche-A");
        sessionManager.clearSession();

        // Compte B se connecte sur le même appareil : ne doit PAS hériter du lien de A
        sessionManager.saveSession("compte-B", "b@test.com", "ACTEUR", true);
        assertNull(sessionManager.getActeurId());
    }

    @Test
    public void isAgent_retourneTrue_pourRoleAgent() {
        sessionManager.saveSession("compte-agent", "agent@test.com", "AGENT", true);
        assertTrue(sessionManager.isAgent());
        assertFalse(sessionManager.isActeur());
    }

    @Test
    public void isAgent_retourneTrue_pourRoleAdministrateur() {
        sessionManager.saveSession("compte-admin", "admin@test.com", "ADMINISTRATEUR", true);
        assertTrue(sessionManager.isAgent());
    }

    @Test
    public void isActeur_retourneTrue_pourRoleActeur() {
        sessionManager.saveSession("compte-acteur", "acteur@test.com", "ACTEUR", true);
        assertTrue(sessionManager.isActeur());
        assertFalse(sessionManager.isAgent());
    }

    @Test
    public void clearSession_remetIsLoggedInAFalse() {
        sessionManager.saveSession("compte-x", "x@test.com", "AGENT", true);
        assertTrue(sessionManager.isLoggedIn());

        sessionManager.clearSession();
        assertFalse(sessionManager.isLoggedIn());
    }

    @Test
    public void getCompteId_correspondAuCompteConnecte() {
        sessionManager.saveSession("compte-unique-id", "y@test.com", "AGENT", true);
        assertEquals("compte-unique-id", sessionManager.getCompteId());
    }
}