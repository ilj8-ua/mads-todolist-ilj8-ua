package madstodolist.controller;

import madstodolist.dto.UsuarioData;
import madstodolist.service.UsuarioService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
public class LoginWebTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UsuarioService usuarioService;

    @Test
    public void loginUsuarioNormalRedirigeATareas() throws Exception {
        UsuarioData usuario = new UsuarioData();
        usuario.setId(1L);
        usuario.setEmail("user@ua");
        usuario.setAdmin(false);

        when(usuarioService.login("user@ua", "123")).thenReturn(UsuarioService.LoginStatus.LOGIN_OK);
        when(usuarioService.findByEmail("user@ua")).thenReturn(usuario);

        this.mockMvc.perform(post("/login")
                        .param("eMail", "user@ua")
                        .param("password", "123"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/usuarios/1/tareas"));
    }

    @Test
    public void loginUsuarioAdminRedirigeARegistrados() throws Exception {
        UsuarioData admin = new UsuarioData();
        admin.setId(2L);
        admin.setEmail("admin@ua");
        admin.setAdmin(true);

        when(usuarioService.login("admin@ua", "admin123")).thenReturn(UsuarioService.LoginStatus.LOGIN_OK);
        when(usuarioService.findByEmail("admin@ua")).thenReturn(admin);

        this.mockMvc.perform(post("/login")
                        .param("eMail", "admin@ua")
                        .param("password", "admin123"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/registrados"));
    }

    @Test
    public void registroMuestraCheckboxAdminSiNoHayAdmin() throws Exception {
        when(usuarioService.hayAdmin()).thenReturn(false);

        this.mockMvc.perform(get("/registro"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Registrarse como administrador")));
    }

    @Test
    public void registroNoMuestraCheckboxAdminSiYaHayAdmin() throws Exception {
        when(usuarioService.hayAdmin()).thenReturn(true);

        this.mockMvc.perform(get("/registro"))
                .andExpect(status().isOk())
                .andExpect(content().string(not(containsString("Registrarse como administrador"))));
    }
}
