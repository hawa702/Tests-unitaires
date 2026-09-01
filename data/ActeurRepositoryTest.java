package com.example.oussouye.data;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import okhttp3.ResponseBody;
import okhttp3.MediaType;
import android.content.Context;

import com.example.oussouye.model.Acteur;
import com.example.oussouye.model.Filiere;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ActeurRepositoryTest {

    private Context mockContext;
    private AppDatabase mockDatabase;
    private ApiService mockApiService;
    private MockedStatic<RetrofitClient> mockedRetrofitClient;
    private ActeurDao mockActeurDao;
    private AuditCriteresDao mockAuditDao;
    private MockedStatic<AppDatabase> mockedAppDatabase;

    private ActeurRepository repository;

    @Before
    public void setUp() {
        mockContext = mock(Context.class);
        // Le constructeur appelle getApplicationContext() sur le Context
        when(mockContext.getApplicationContext()).thenReturn(mockContext);

        mockDatabase = mock(AppDatabase.class);
        mockActeurDao = mock(ActeurDao.class);
        mockAuditDao = mock(AuditCriteresDao.class);

        when(mockDatabase.acteurDao()).thenReturn(mockActeurDao);
        when(mockDatabase.auditCriteresDao()).thenReturn(mockAuditDao);

        // On intercepte l'appel statique AppDatabase.getInstance(...)
        mockedAppDatabase = Mockito.mockStatic(AppDatabase.class);
        mockedAppDatabase.when(() -> AppDatabase.getInstance(any())).thenReturn(mockDatabase);

        // Simule une base déjà pré-remplie pour éviter le seed automatique dans le constructeur
        when(mockActeurDao.count()).thenReturn(1);
        mockApiService = mock(ApiService.class);
        mockedRetrofitClient = Mockito.mockStatic(RetrofitClient.class);
        mockedRetrofitClient.when(RetrofitClient::getApiService).thenReturn(mockApiService);

        repository = new ActeurRepository(mockContext);
    }

    @After
    public void tearDown() {
        mockedAppDatabase.close();// indispensable pour libérer le mock statique après chaque test
        mockedRetrofitClient.close();
    }

    private Acteur creerActeur(String id, String nom, String prenom, String structure, String village) {
        Acteur a = new Acteur();
        a.setId(id);
        a.setNom(nom);
        a.setPrenom(prenom);
        a.setStructure(structure);
        a.setVillage(village);
        return a;
    }

    @Test
    public void getAll_delegueBienAuDao() {
        List<Acteur> attendu = new ArrayList<>();
        attendu.add(creerActeur("1", "Diatta", "Emma", "Jinaben Yo Afeo", "Oussouye"));
        when(mockActeurDao.getAll()).thenReturn(attendu);

        List<Acteur> resultat = repository.getAll();

        assertEquals(1, resultat.size());
        verify(mockActeurDao, times(1)).getAll();
    }

    @Test
    public void rechercherParNom_trouveParNom() {
        List<Acteur> tous = new ArrayList<>();
        tous.add(creerActeur("1", "Diatta", "Emma", "Jinaben Yo Afeo", "Oussouye"));
        tous.add(creerActeur("2", "Sagna", "Khadiatou", "Tessito Djimoutene", "Oussouye"));
        when(mockActeurDao.getAll()).thenReturn(tous);

        List<Acteur> resultat = repository.rechercherParNom("diatta");

        assertEquals(1, resultat.size());
        assertEquals("1", resultat.get(0).getId());
    }

    @Test
    public void rechercherParNom_trouveParVillage() {
        List<Acteur> tous = new ArrayList<>();
        tous.add(creerActeur("1", "Diatta", "Emma", "Jinaben Yo Afeo", "Elinkine"));
        when(mockActeurDao.getAll()).thenReturn(tous);

        List<Acteur> resultat = repository.rechercherParNom("elinkine");

        assertEquals(1, resultat.size());
    }

    @Test
    public void rechercherParNom_requeteVide_retourneListeVide() {
        List<Acteur> resultat = repository.rechercherParNom("   ");
        assertTrue(resultat.isEmpty());
        // On vérifie qu'on n'a même pas interrogé la base pour rien
        verify(mockActeurDao, never()).getAll();
    }

    @Test
    public void toApiActeur_convertitLesFilieresEnIds() {
        Acteur acteur = creerActeur("1", "Diatta", "Emma", "Jinaben Yo Afeo", "Oussouye");
        List<Filiere> filieres = new ArrayList<>();
        filieres.add(Filiere.OSTREICULTURE);
        filieres.add(Filiere.RIZ);
        acteur.setFilieres(filieres);

        Map<String, Integer> filiereCodeToId = new HashMap<>();
        filiereCodeToId.put("OSTREICULTURE", 1);
        filiereCodeToId.put("RIZ", 3);

        ApiActeur api = repository.toApiActeur(acteur, filiereCodeToId);

        assertEquals("Diatta", api.nom);
        assertEquals(2, api.filiereIds.size());
        assertTrue(api.filiereIds.contains(1));
        assertTrue(api.filiereIds.contains(3));
    }

    @Test
    public void toApiActeur_communeManquante_utiliseDepartementEnRepli() {
        Acteur acteur = creerActeur("1", "Diatta", "Emma", "Jinaben Yo Afeo", "Oussouye");
        acteur.setCommune("");
        acteur.setDepartement("Oussouye");
        acteur.setFilieres(new ArrayList<>());

        ApiActeur api = repository.toApiActeur(acteur, new HashMap<>());

        assertEquals("Oussouye", api.commune);
    }

    @Test
    public void getActeurByCompteId_retombeSurRechercheParTelephone() {
        when(mockActeurDao.getById("compte-123")).thenReturn(null);
        when(mockActeurDao.getByCreatedBy("compte-123")).thenReturn(null);

        Acteur trouve = creerActeur("1", "Diatta", "Emma", "Jinaben Yo Afeo", "Oussouye");
        when(mockActeurDao.getByTelephone("compte-123")).thenReturn(trouve);

        Acteur resultat = repository.getActeurByCompteId("compte-123");

        assertNotNull(resultat);
        assertEquals("1", resultat.getId());
    }

    @Test
    public void getActeurByCompteId_idNullOuVide_retourneNullSansAppelerLeDao() {
        assertNull(repository.getActeurByCompteId(null));
        assertNull(repository.getActeurByCompteId("   "));

        verify(mockActeurDao, never()).getById(anyString());
        verify(mockActeurDao, never()).getByCreatedBy(anyString());
        verify(mockActeurDao, never()).getByTelephone(anyString());
    }
    @Test
    public void deleteActeur_succes_appelleOnSuccessEtSupprimeEnLocal() throws InterruptedException {
        Call<Void> mockCall = mock(Call.class);
        when(mockApiService.deleteActeur("1")).thenReturn(mockCall);

        doAnswer(invocation -> {
            Callback<Void> callback = invocation.getArgument(0);
            callback.onResponse(mockCall, Response.success(null));
            return null;
        }).when(mockCall).enqueue(any());

        ActeurRepository.DeleteCallback resultat = mock(ActeurRepository.DeleteCallback.class);
        repository.deleteActeur("1", resultat);

        verify(resultat, timeout(500)).onSuccess();
        verify(mockActeurDao, timeout(500)).deleteById("1");
    }

    @Test
    public void deleteActeur_404_traiteCommeUnSucces() {
        Call<Void> mockCall = mock(Call.class);
        when(mockApiService.deleteActeur("1")).thenReturn(mockCall);

        ResponseBody errorBody = ResponseBody.create(MediaType.parse("application/json"), "{}");
        doAnswer(invocation -> {
            Callback<Void> callback = invocation.getArgument(0);
            callback.onResponse(mockCall, Response.error(404, errorBody));
            return null;
        }).when(mockCall).enqueue(any());

        ActeurRepository.DeleteCallback resultat = mock(ActeurRepository.DeleteCallback.class);
        repository.deleteActeur("1", resultat);

        verify(resultat, timeout(500)).onSuccess();
        verify(resultat, never()).onError(anyString());
    }

    @Test
    public void deleteActeur_erreurServeur_appelleOnError() {
        Call<Void> mockCall = mock(Call.class);
        when(mockApiService.deleteActeur("1")).thenReturn(mockCall);

        ResponseBody errorBody = ResponseBody.create(MediaType.parse("application/json"), "{}");
        doAnswer(invocation -> {
            Callback<Void> callback = invocation.getArgument(0);
            callback.onResponse(mockCall, Response.error(500, errorBody));
            return null;
        }).when(mockCall).enqueue(any());

        ActeurRepository.DeleteCallback resultat = mock(ActeurRepository.DeleteCallback.class);
        repository.deleteActeur("1", resultat);

        verify(resultat, timeout(500)).onError(anyString());
        verify(resultat, never()).onSuccess();
        verify(mockActeurDao, never()).deleteById(anyString());
    }

    @Test
    public void deleteActeur_pasDeReseau_appelleOnError() {
        Call<Void> mockCall = mock(Call.class);
        when(mockApiService.deleteActeur("1")).thenReturn(mockCall);

        doAnswer(invocation -> {
            Callback<Void> callback = invocation.getArgument(0);
            callback.onFailure(mockCall, new java.io.IOException("Pas de connexion"));
            return null;
        }).when(mockCall).enqueue(any());

        ActeurRepository.DeleteCallback resultat = mock(ActeurRepository.DeleteCallback.class);
        repository.deleteActeur("1", resultat);

        verify(resultat, timeout(500)).onError(contains("Pas de connexion"));
    }
}