package madstodolist.controller;

import madstodolist.authentication.ManagerUserSession;
import madstodolist.dto.EquipoData;
import madstodolist.dto.UsuarioData;
import madstodolist.service.EquipoService;
import madstodolist.service.UsuarioService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Arrays;
import java.util.Collections;

import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
public class EquipoWebTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ManagerUserSession managerUserSession;

    @MockBean
    private UsuarioService usuarioService;

    @MockBean
    private EquipoService equipoService;

    @Test
    public void listadoEquiposSinLoginDevuelveUnauthorized() throws Exception {
        when(managerUserSession.usuarioLogeado()).thenReturn(null);

        this.mockMvc.perform(get("/equipos"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    public void listadoEquiposConLoginMuestraEquipos() throws Exception {
        UsuarioData usuario = new UsuarioData();
        usuario.setId(1L);
        usuario.setNombre("Ana García");

        EquipoData equipo1 = new EquipoData();
        equipo1.setId(1L);
        equipo1.setNombre("Proyecto Alfa");

        EquipoData equipo2 = new EquipoData();
        equipo2.setId(2L);
        equipo2.setNombre("Proyecto Beta");

        when(managerUserSession.usuarioLogeado()).thenReturn(1L);
        when(usuarioService.findById(1L)).thenReturn(usuario);
        when(equipoService.findAllOrdenadoPorNombre()).thenReturn(Arrays.asList(equipo1, equipo2));

        this.mockMvc.perform(get("/equipos"))
                .andExpect(status().isOk())
                .andExpect(content().string(allOf(
                        containsString("Listado de equipos"),
                        containsString("Proyecto Alfa"),
                        containsString("Proyecto Beta"),
                        containsString("href=\"/equipos/1\""),
                        containsString("href=\"/equipos/2\""),
                        containsString("Ver miembros")
                )));
    }

    @Test
    public void listadoEquiposVacio() throws Exception {
        UsuarioData usuario = new UsuarioData();
        usuario.setId(1L);
        usuario.setNombre("Ana García");

        when(managerUserSession.usuarioLogeado()).thenReturn(1L);
        when(usuarioService.findById(1L)).thenReturn(usuario);
        when(equipoService.findAllOrdenadoPorNombre()).thenReturn(Collections.emptyList());

        this.mockMvc.perform(get("/equipos"))
                .andExpect(status().isOk())
                .andExpect(content().string(allOf(
                        containsString("Listado de equipos"),
                        containsString("No hay equipos disponibles")
                )));
    }

    @Test
    public void detalleEquipoSinLoginDevuelveUnauthorized() throws Exception {
        when(managerUserSession.usuarioLogeado()).thenReturn(null);

        this.mockMvc.perform(get("/equipos/1"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    public void detalleEquipoNoExistenteDevuelveNotFound() throws Exception {
        UsuarioData usuario = new UsuarioData();
        usuario.setId(1L);

        when(managerUserSession.usuarioLogeado()).thenReturn(1L);
        when(usuarioService.findById(1L)).thenReturn(usuario);
        when(equipoService.recuperarEquipo(999L)).thenThrow(new madstodolist.service.EquipoServiceException("Equipo no encontrado"));

        this.mockMvc.perform(get("/equipos/999"))
                .andExpect(status().isNotFound());
    }

    @Test
    public void detalleEquipoMuestraMiembros() throws Exception {
        UsuarioData usuarioLogeado = new UsuarioData();
        usuarioLogeado.setId(1L);
        usuarioLogeado.setNombre("Ana García");

        EquipoData equipo = new EquipoData();
        equipo.setId(1L);
        equipo.setNombre("Proyecto Alfa");

        UsuarioData miembro1 = new UsuarioData();
        miembro1.setId(1L);
        miembro1.setNombre("Ana García");
        miembro1.setEmail("ana@ua");

        UsuarioData miembro2 = new UsuarioData();
        miembro2.setId(2L);
        miembro2.setNombre("Juan Pérez");
        miembro2.setEmail("juan@ua");

        when(managerUserSession.usuarioLogeado()).thenReturn(1L);
        when(usuarioService.findById(1L)).thenReturn(usuarioLogeado);
        when(equipoService.recuperarEquipo(1L)).thenReturn(equipo);
        when(equipoService.usuariosEquipo(1L)).thenReturn(Arrays.asList(miembro1, miembro2));

        this.mockMvc.perform(get("/equipos/1"))
                .andExpect(status().isOk())
                .andExpect(content().string(allOf(
                        containsString("Equipo: Proyecto Alfa"),
                        containsString("Participantes del equipo"),
                        containsString("Ana García"),
                        containsString("ana@ua"),
                        containsString("Juan Pérez"),
                        containsString("juan@ua"),
                        containsString("href=\"/equipos\"")
                )));
    }

    @Test
    public void detalleEquipoSinMiembrosMuestraAviso() throws Exception {
        UsuarioData usuarioLogeado = new UsuarioData();
        usuarioLogeado.setId(1L);
        usuarioLogeado.setNombre("Ana García");

        EquipoData equipo = new EquipoData();
        equipo.setId(1L);
        equipo.setNombre("Proyecto Vacío");

        when(managerUserSession.usuarioLogeado()).thenReturn(1L);
        when(usuarioService.findById(1L)).thenReturn(usuarioLogeado);
        when(equipoService.recuperarEquipo(1L)).thenReturn(equipo);
        when(equipoService.usuariosEquipo(1L)).thenReturn(Collections.emptyList());

        this.mockMvc.perform(get("/equipos/1"))
                .andExpect(status().isOk())
                .andExpect(content().string(allOf(
                        containsString("Equipo: Proyecto Vacío"),
                        containsString("No hay usuarios en este equipo")
                )));
    }
}
