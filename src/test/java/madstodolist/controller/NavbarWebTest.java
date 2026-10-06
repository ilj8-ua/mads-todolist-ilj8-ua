package madstodolist.controller;

import madstodolist.authentication.ManagerUserSession;
import madstodolist.dto.UsuarioData;
import madstodolist.service.UsuarioService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
public class NavbarWebTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ManagerUserSession managerUserSession;

    @MockBean
    private UsuarioService usuarioService;

    @Test
    public void navbarSinLoginMuestraLoginYRegistroEnAbout() throws Exception {
        when(managerUserSession.usuarioLogeado()).thenReturn(null);

        this.mockMvc.perform(get("/about"))
                .andExpect(status().isOk())
                .andExpect(content().string(allOf(
                        containsString("ToDoList"),
                        containsString("href=\"/login\""),
                        containsString("href=\"/registro\"")
                )));
    }

    @Test
    public void navbarConUsuarioLogeadoMuestraTareasYMenuUsuarioEnAbout() throws Exception {
        UsuarioData usuario = new UsuarioData();
        usuario.setId(1L);
        usuario.setNombre("Ana García");
        usuario.setEmail("ana@ua");

        when(managerUserSession.usuarioLogeado()).thenReturn(1L);
        when(usuarioService.findById(1L)).thenReturn(usuario);

        this.mockMvc.perform(get("/about"))
                .andExpect(status().isOk())
                .andExpect(content().string(allOf(
                        containsString("ToDoList"),
                        containsString("href=\"/usuarios/1/tareas\""),
                        containsString("Ana García"),
                        containsString("Cuenta"),
                        containsString("Cerrar sesión Ana García"),
                        containsString("href=\"/logout\"")
                )));
    }
}
