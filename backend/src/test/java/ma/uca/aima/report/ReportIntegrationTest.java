package ma.uca.aima.report;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import ma.uca.aima.TestcontainersConfiguration;
import ma.uca.aima.referential.service.ReferentialQueryService;
import ma.uca.aima.support.ApiClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openpdf.text.pdf.PdfReader;
import org.openpdf.text.pdf.parser.PdfTextExtractor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.web.context.WebApplicationContext;

/** Génération du rapport PDF d'une évaluation (contenu vérifié en relisant le PDF produit). */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
class ReportIntegrationTest {

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private ReferentialQueryService referentialService;

    private ApiClient api;

    @BeforeEach
    void setUp() {
        api = new ApiClient(context);
    }

    @Test
    void generatesReadablePdfReport() throws Exception {
        String admin = api.adminToken();
        String orgId = api.createOrganization(admin, "Clinique Atlas");
        String orgUser = api.createUserAndLogin(admin, "ORG_USER", orgId);
        String base = "/api/v1/assessments/" + api.body(api.post(orgUser, "/api/v1/assessments",
                Map.of("organizationId", orgId, "title", "Rapport de test", "scope", "Chatbot patients")))
                .get("id").asString();
        api.put(orgUser, base + "/answers/LLM.3.2", Map.of("level", 1)).andExpect(status().isOk());
        api.put(orgUser, base + "/answers/LLM.3.1", Map.of("level", 3)).andExpect(status().isOk());
        // LLM brut = (4 + 4 + 2 + 4) / 4 = 3,5 → plafonné à 2,0 par LLM.3.2 (critique au niveau 1)
        for (String code : new String[] {"LLM.1.1", "LLM.2.1", "LLM.4.1"}) {
            api.put(orgUser, base + "/answers/" + code, Map.of("level", 4)).andExpect(status().isOk());
        }
        api.put(orgUser, base + "/answers/GOV.1.2", Map.of("level", 2, "evidence", "Charte IA v0.3"))
                .andExpect(status().isOk());
        // Texte contenant « ↔ », absent de l'encodage des polices PDF standard : doit être remplacé, pas planter
        api.put(orgUser, base + "/answers/DEV.4.2", Map.of("level", 4, "comment", "modèle ↔ données ≥ v2"))
                .andExpect(status().isOk());

        byte[] pdf = api.get(orgUser, base + "/report.pdf")
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "application/pdf"))
                .andExpect(header().string("Content-Disposition",
                        org.hamcrest.Matchers.containsString("rapport-aima-clinique-atlas")))
                .andReturn().getResponse().getContentAsByteArray();

        assertThat(new String(pdf, 0, 5, StandardCharsets.ISO_8859_1)).isEqualTo("%PDF-");

        try (PdfReader reader = new PdfReader(pdf)) {
            assertThat(reader.getNumberOfPages()).isGreaterThanOrEqualTo(3);
            PdfTextExtractor extractor = new PdfTextExtractor(reader);
            String page1 = extractor.getTextFromPage(1);
            String page2 = extractor.getTextFromPage(2);
            String page3 = extractor.getTextFromPage(3);
            assertThat(page1).contains("Rapport d'évaluation de maturité", "Clinique Atlas", "Chatbot patients",
                    "Banque et finance", "Profil de maturité par domaine", "RISK N/A");
            assertThat(page2).contains("Résultats par domaine", "Feuille de route", "Plafonné à 2,00 par LLM.3.2",
                    "(cible - niveau)");
            assertThat(page2).doesNotContain("?");
            assertThat(page3).contains("Annexe", "Charte IA v0.3", "modèle <-> données >= v2");
        }

        // Copie pour consultation manuelle : backend/target/rapport-exemple.pdf
        Files.write(Path.of("target", "rapport-exemple.pdf"), pdf);
        assertThat(referentialService.findActive()).isNotNull();
    }
}
